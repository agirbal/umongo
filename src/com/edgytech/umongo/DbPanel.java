/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.edgytech.umongo;

import com.edgytech.swingfast.EnumListener;
import com.edgytech.swingfast.XmlComponentUnit;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoException;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;
import java.io.File;
import java.io.IOException;
import javax.swing.JPanel;
import org.bson.types.Code;
import com.edgytech.umongo.DbPanel.Item;
import com.edgytech.swingfast.*;
import com.mongodb.*;
import java.util.ArrayList;

/**
 *
 * @author antoine
 */
public class DbPanel extends BasePanel implements EnumListener<Item> {

    enum Item {

        icon,
        name,
        queryOptions,
        writeConcern,
        readPreference,
        stats,
        statsCmd,
        refresh,
        dropDatabase,
        uploadFile,
        uploadFileDialog,
        uploadFilePath,
        uploadFileName,
        uploadContentType,
        uploadMetadata,
        downloadFile,
        downloadQuery,
        downloadFileName,
        downloadFilePath,
        listFiles,
        deleteFile,
        deleteQuery,
        deleteFileName,
        commandCmd,
        commandStr,
        commandJson,
        commandHelp,
        listCommands,
        eval,
        evalCode,
        evalNoLock,
        options,
        authenticate,
        authUser,
        authPassword,
        manageUsers,
        userDialog,
        userList,
        addUser,
        removeUser,
        editUser,
        userChange,
        createCollection,
        createCollName,
        createCollCapped,
        createCollSize,
        createCollCount,
        createCollAutoIndex,
        enableSharding,
        movePrimary,
        mvpToShard,
        shardingInfo,
        profile,
        profileLevel,
        profileSlowMS,
        repair,
        addJSFunction,
        addJSFunctionName,
        addJSFunctionCode,
        findJSFunction,
        findJSFunctionName
    }
    GridFS gridFS;

    public DbPanel() {
        setEnumBinding(Item.values(), this);
    }

    public GridFS getGridFS() {
        if (gridFS == null) {
            gridFS = new GridFS(getDbNode().getDb());
        }
        return gridFS;
    }

    public DbNode getDbNode() {
        return (DbNode) getNode();
    }

    @Override
    protected void updateComponentCustom(JPanel old) {
        try {
            DB db = getDbNode().getDb();
            setStringFieldValue(Item.name, db.getName());
            setStringFieldValue(Item.queryOptions, MongoUtils.queryOptionsToString(db.getOptions()));
            ((DocField) getBoundUnit(Item.writeConcern)).setDoc(db.getWriteConcern().getCommand());
            ((DocField) getBoundUnit(Item.readPreference)).setDoc(db.getReadPreference().toDBObject());
            ((CmdField) getBoundUnit(Item.stats)).updateFromCmd(db);
        } catch (Exception e) {
            UMongo.instance.showError(this.getClass().getSimpleName() + " update", e);
        }
    }

    public void actionPerformed(Item enm, XmlComponentUnit unit, Object src) {
    }

    public void command() {
        final DB db = getDbNode().getDb();
        final String scmd = getStringFieldValue(Item.commandStr);
        final DBObject cmd = !scmd.isEmpty() ? new BasicDBObject(scmd, 1)
                : ((DocBuilderField) getBoundUnit(Item.commandJson)).getDBObject();
        boolean help = getBooleanFieldValue(Item.commandHelp);
        if (help)
            cmd.put("help", true);
        new DocView(null, "Command", db, cmd).addToTabbedDiv();
    }

    public void listCommands() {
        new DocView(null, "List Cmd", getDbNode().getDb(), "listCommands").addToTabbedDiv();
    }
    
    public void eval() {
        final DB db = getDbNode().getDb();
        final String sfunc = getStringFieldValue(Item.evalCode);
        final boolean noLock = getBooleanFieldValue(Item.evalNoLock);
        BasicDBObject cmd = new BasicDBObject("$eval", sfunc);
        if (noLock)
            cmd.put("nolock", true);
        new DocView(null, "Eval", db, cmd).addToTabbedDiv();

//        new DbJob() {
//
//            @Override
//            public Object doRun() {
//                return db.eval(sfunc);
//            }
//
//            @Override
//            public String getNS() {
//                return db.getName();
//            }
//
//            @Override
//            public String getShortName() {
//                return "Eval";
//            }
//
//            @Override
//            public Object getRoot(Object result) {
//                return sfunc;
//            }
//        }.addJob();
    }

    public void uploadFile() {
        final DbNode dbNode = getDbNode();
        final DB db = dbNode.getDb();

        final String path = getStringFieldValue(Item.uploadFilePath);
        if (path.isEmpty()) {
            return;
        }
        final File src = new File(path);
        final String fileName = getStringFieldValue(Item.uploadFileName);
        final String contentType = getStringFieldValue(Item.uploadContentType);
        final DBObject metadata = ((DocBuilderField) getBoundUnit(Item.uploadMetadata)).getDBObject();

        new DbJob() {

            @Override
            public Object doRun() throws IOException {
                final GridFSInputFile file = getGridFS().createFile(src);
                if (!fileName.isEmpty()) {
                    file.setFilename(fileName);
                }
                if (!contentType.isEmpty()) {
                    file.setContentType(contentType);
                }
                if (metadata != null) {
                    file.setMetaData(metadata);
                }
                file.save();
                return file;
            }

            @Override
            public String getNS() {
                return db.getName();
            }

            @Override
            public String getShortName() {
                return "Upload File";
            }

            @Override
            public Object getRoot(Object result) {
                return " path=" + path;
            }

            @Override
            public void wrapUp(Object res) {
                super.wrapUp(res);
                // may have new collections
                dbNode.structureComponent();
            }
        }.addJob();
    }

    public void downloadFile() {
        final DB db = getDbNode().getDb();
        final DBObject query = ((DocBuilderField)getBoundUnit(Item.downloadQuery)).getDBObject();
        final String fname = getStringFieldValue(Item.downloadFileName);
        final String dpath = getStringFieldValue(Item.downloadFilePath);
        if (dpath.isEmpty()) {
            return;
        }
        final File dfile = new File(dpath);

        new DbJob() {

            @Override
            public Object doRun() throws IOException {
                GridFSDBFile dbfile = null;
                if (query != null)
                    dbfile = getGridFS().findOne(query);
                else
                    dbfile = getGridFS().findOne(fname);
                if (dbfile == null) {
                    throw new MongoException("GridFS cannot find file " + fname);
                }
                dbfile.writeTo(dfile);
                return dbfile;
            }

            @Override
            public String getNS() {
                return db.getName();
            }

            @Override
            public String getShortName() {
                return "Download File";
            }

            @Override
            public Object getRoot(Object result) {
                return "filename=" + fname + ", query=" + query + ", path=" + dpath;
            }
        }.addJob();
    }

    public void listFiles() {
        final DB db = getDbNode().getDb();
        final DBCollection col = db.getCollection("fs.files");
        
        new DbJob() {

            @Override
            public Object doRun() throws IOException {
                return col.find();
            }

            @Override
            public String getNS() {
                return col.getName();
            }

            @Override
            public String getShortName() {
                return "List Files";
            }

        }.addJob();
    }

    public void deleteFile() {
        final DB db = getDbNode().getDb();
        final DBObject query = ((DocBuilderField)getBoundUnit(Item.downloadQuery)).getDBObject();
        final String fname = getStringFieldValue(Item.downloadFileName);

        new DbJob() {

            @Override
            public Object doRun() throws IOException {
                if (query != null)
                    getGridFS().remove(query);
                else
                    getGridFS().remove(fname);
                return true;
            }

            @Override
            public String getNS() {
                return db.getName();
            }

            @Override
            public String getShortName() {
                return "Delete File";
            }

            @Override
            public Object getRoot(Object result) {
                return "filename=" + fname + ", query=" + query;
            }
        }.addJob();
    }

    public void dropDatabase() {
        final DbNode node = getDbNode();
        final DB db = getDbNode().getDb();
        new DbJob() {

            @Override
            public Object doRun() {
                db.dropDatabase();
                return null;
            }

            @Override
            public String getNS() {
                return db.getName();
            }

            @Override
            public String getShortName() {
                return "Drop";
            }

            @Override
            public void wrapUp(Object res) {
                node.removeNode();
                node = null;
                super.wrapUp(res);
            }
        }.addJob();
    }

    public void options() {
        final DB db = getDbNode().getDb();
        OptionDialog od = UMongo.instance.getGlobalStore().getOptionDialog();
        od.update(db.getOptions(), db.getWriteConcern(), db.getReadPreference());
        if (!od.show()) {
            return;
        }
        db.setOptions(od.getQueryOptions());
        db.setWriteConcern(od.getWriteConcern());
        db.setReadPreference(od.getReadPreference());
        refresh();
    }

    public void authenticate() {
        final DbNode dbNode = getDbNode();
        final DB db = dbNode.getDb();
        final String user = getStringFieldValue(Item.authUser);
        final String pass = getStringFieldValue(Item.authPassword);
        new DbJob() {

            @Override
            public Object doRun() {
                db.authenticateCommand(user, pass.toCharArray());
                return null;
            }

            @Override
            public String getNS() {
                return db.getName();
            }

            @Override
            public String getShortName() {
                return "Auth";
            }

            @Override
            public void wrapUp(Object res) {
                super.wrapUp(res);
                if (dbNode.getDb().getName().equals("admin")) {
                    // now we can list dbs, refresh whole mongo
                    dbNode.getMongoNode().structureComponent();
                } else {
                    dbNode.structureComponent();
                }
            }
        }.addJob();
    }

    void refreshUserList() {
        List list = (List) getBoundUnit(Item.userList);        
        final DB db = getDbNode().getDb();
        DBCursor cur = db.getCollection("system.users").find().sort(new BasicDBObject("user", 1));
        ArrayList users = new ArrayList();
        while (cur.hasNext()) {
            BasicDBObject user = (BasicDBObject) cur.next();
            users.add(user.getString("user"));
        }
        
        list.items = (String[]) users.toArray(new String[users.size()]);
        list.structureComponent();
    }
    
    public void manageUsers() {
        FormDialog dialog = (FormDialog) ((MenuItem) getBoundUnit(Item.manageUsers)).getDialog();
        refreshUserList();

        if (!dialog.show())
            return;
    }
    
    public void addUser() {
        final DB db = getDbNode().getDb();
        final DBCollection col = db.getCollection("system.users");

        UserDialog ud = (UserDialog) getBoundUnit(Item.userDialog);
        ud.resetForNew();
        if (!ud.show())
            return;
        
        final BasicDBObject newUser = ud.getUser(null);
        
        new DbJob() {

            @Override
            public Object doRun() throws IOException {
                return col.insert(newUser);
            }

            @Override
            public String getNS() {
                return "system.users";
            }

            @Override
            public String getShortName() {
                return "Add User";
            }

            @Override
            public void wrapUp(Object res) {
                super.wrapUp(res);
                refreshUserList();
            }
        }.addJob();   
    }

    public void removeUser() {
        final DB db = getDbNode().getDb();
        
        final String user = getComponentStringFieldValue(Item.userList);        
        if (user == null) {
            return;
        }
        if (!((ConfirmDialog) getBoundUnit(Item.userChange)).show())
            return;
        
        new DbJob() {

            @Override
            public Object doRun() throws IOException {
                return db.removeUser(user);
            }

            @Override
            public String getNS() {
                return "system.users";
            }

            @Override
            public String getShortName() {
                return "Remove User";
            }

            @Override
            public void wrapUp(Object res) {
                super.wrapUp(res);
                refreshUserList();
            }
        }.addJob();        
    }

    public void editUser() {
        final DB db = getDbNode().getDb();
        final DBCollection col = db.getCollection("system.users");
        
        final String user = getComponentStringFieldValue(Item.userList);        
        if (user == null) {
            return;
        }
        
        final BasicDBObject userObj = (BasicDBObject) col.findOne(new BasicDBObject("user", user));
        UserDialog ud = (UserDialog) getBoundUnit(Item.userDialog);
        ud.resetForEdit(userObj);
        if (!ud.show())
            return;
        
        final BasicDBObject newUser = ud.getUser(userObj);

        new DbJob() {

            @Override
            public Object doRun() throws IOException {
                return col.save(newUser);
            }

            @Override
            public String getNS() {
                return col.getName();
            }

            @Override
            public String getShortName() {
                return "Edit User";
            }

            @Override
            public void wrapUp(Object res) {
                super.wrapUp(res);
                refreshUserList();
            }
        }.addJob();
        
    }
    
    public void createCollection() {
        final DbNode node = getDbNode();
        final DB db = getDbNode().getDb();
        final String name = getStringFieldValue(Item.createCollName);
        final boolean capped = getBooleanFieldValue(Item.createCollCapped);
        final int size = getIntFieldValue(Item.createCollSize);
        final int count = getIntFieldValue(Item.createCollCount);
        final boolean autoIndexId = getBooleanFieldValue(Item.createCollAutoIndex);

        new DbJob() {

            @Override
            public Object doRun() throws IOException {
                DBObject opt = new BasicDBObject("capped", capped);
                if (capped) {
                    if (size > 0) {
                        opt.put("size", size);
                    }
                    if (count > 0) {
                        opt.put("max", count);
                    }
                }
                if (!autoIndexId)
                    opt.put("autoIndexId", false);
                db.createCollection(name, opt);
                return null;
            }

            @Override
            public String getNS() {
                return db.getName();
            }

            @Override
            public String getShortName() {
                return "Create Collection";
            }

            @Override
            public void wrapUp(Object res) {
                super.wrapUp(res);
                node.structureComponent();
            }
        }.addJob();

    }

    public void stats() {
        new DocView(null, "DB Stats", getDbNode().getDb(), "dbstats").addToTabbedDiv();
    }

    public void enableSharding() {
        Mongo m = getDbNode().getMongoNode().getMongo();
        DB admin = m.getDB("admin");
        DBObject cmd = new BasicDBObject("enableSharding", getDbNode().getDb().getName());
        new DocView(id, "Enable Sharding", admin, cmd, null, this, null).addToTabbedDiv();
    }

    public void profile() {
        DB db = getDbNode().getDb();
        int level = getIntFieldValue(Item.profileLevel);
        final DBObject cmd = new BasicDBObject("profile", level);
        if (level == 1) {
            int slow = getIntFieldValue(Item.profileSlowMS);
            if (slow > 0)
                cmd.put("slowms", slow);
        }
        new DocView(null, "Profile", db, cmd).addToTabbedDiv();
    }

    public void repair() {
        DB db = getDbNode().getDb();
        final DBObject cmd = new BasicDBObject("repairDatabase", 1);
        if (!UMongo.instance.getGlobalStore().confirmLockingOperation())
            return;
        new DocView(null, "Repair", db, cmd).addToTabbedDiv();
    }

    public void shardingInfo() {
        final DB db = getDbNode().getDb();
        final DB config = db.getSisterDB("config");
        final DBCollection col = config.getCollection("databases");
        CollectionPanel.doFind(col, new BasicDBObject("_id", db.getName()));
    }
    
    public void movePrimary() {
        FormDialog dialog = (FormDialog) ((MenuItem) getBoundUnit(Item.movePrimary)).getDialog();
        ComboBox combo = (ComboBox) getBoundUnit(Item.mvpToShard);
        combo.value = 0;
        combo.items = getDbNode().getMongoNode().getShardNames();
        combo.structureComponent();

        if (!dialog.show())
            return;
        
        Mongo m = getDbNode().getMongoNode().getMongo();
        DB admin = m.getDB("admin");
        String shard = getStringFieldValue(Item.mvpToShard);
        DBObject cmd = new BasicDBObject("movePrimary", getDbNode().getDb().getName());
        cmd.put("to", shard);
        new DocView(id, "Move Primary", admin, cmd, null, this, null).addToTabbedDiv();
    }

    public void findJSFunction() {
        final DB db = getDbNode().getDb();
        final DBCollection col = db.getCollection("system.js");
        final String name = getStringFieldValue(Item.findJSFunctionName);
        DBObject query = new BasicDBObject();
        if (name != null && !name.isEmpty())
            query.put("_id", name);
        CollectionPanel.doFind(col, query);
    }

    public void addJSFunction() {
        final DB db = getDbNode().getDb();
        final DBCollection col = db.getCollection("system.js");
        final String name = getStringFieldValue(Item.addJSFunctionName);
        final String code = getStringFieldValue(Item.addJSFunctionCode);
        CollectionPanel.doFind(col, new BasicDBObject("_id", name));

        new DbJob() {

            @Override
            public Object doRun() throws Exception {
                DBObject obj = new BasicDBObject("_id", name);
                obj.put("value", new Code(code));
                return col.insert(obj);
            }

            @Override
            public String getNS() {
                return col.getFullName();
            }

            @Override
            public String getShortName() {
                return "Add JS Function";
            }
        }.addJob();
    }
}
