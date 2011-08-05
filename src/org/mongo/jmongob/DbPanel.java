/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mongo.jmongob;

import com.edgytech.swingfast.ConfirmDialog;
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
import com.mongodb.util.JSON;
import java.io.File;
import java.io.IOException;
import javax.swing.JPanel;
import org.bson.types.Code;
import org.mongo.jmongob.DbPanel.Item;

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
        stats,
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
        command,
        commandStr,
        commandJson,
        commandHelp,
        listCommands,
        eval,
        evalCode,
        options,
        authenticate,
        authUser,
        authPassword,
        addUser,
        auUser,
        auPassword,
        auReadOnly,
        removeUser,
        ruUser,
        createCollection,
        createCollName,
        createCollCapped,
        createCollSize,
        createCollCount,
        enableSharding,
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
            ((CmdField) getBoundUnit(Item.stats)).updateFromCmd(db);
        } catch (Exception e) {
            JMongoBrowser.instance.showError(this.getClass().getSimpleName() + " update", e);
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
        BasicDBObject cmd = new BasicDBObject("$eval", sfunc);
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
        final BasicDBObject metadata = ((DocBuilderField) getBoundUnit(Item.uploadMetadata)).getDBObject();

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
                return "Upload";
            }

            @Override
            public Object getRoot(Object result) {
                return "path=" + path;
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
        dfile.mkdirs();

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
                return "Download";
            }

            @Override
            public Object getRoot(Object result) {
                return "filename=" + fname + " path=" + dpath;
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
                super.wrapUp(res);
                node.removeNode();
            }
        }.addJob();
    }

    public void options() {
        final DB db = getDbNode().getDb();
        OptionDialog od = JMongoBrowser.instance.getGlobalStore().getOptionDialog();
        od.update(db.getOptions(), db.getWriteConcern());
        if (!od.show()) {
            return;
        }
        db.setOptions(od.getQueryOptions());
        db.setWriteConcern(od.getWriteConcern());
        refresh();
    }

    public void authenticate() {
        final DB db = getDbNode().getDb();
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
            }
        }.addJob();
    }

    public void addUser() {
        final DB db = getDbNode().getDb();
        final String user = getStringFieldValue(Item.auUser);
        final String pass = getStringFieldValue(Item.auPassword);
        final boolean ro = getBooleanFieldValue(Item.auReadOnly);
        new DbJob() {

            @Override
            public Object doRun() {
                db.addUser(user, pass.toCharArray(), ro);
                return null;
            }

            @Override
            public String getNS() {
                return db.getName();
            }

            @Override
            public String getShortName() {
                return "Add User";
            }
        }.addJob();
    }

    public void removeUser() {
        final DB db = getDbNode().getDb();
        final String user = getStringFieldValue(Item.ruUser);
        new DbJob() {

            @Override
            public Object doRun() {
                db.removeUser(user);
                return null;
            }

            @Override
            public String getNS() {
                return db.getName();
            }

            @Override
            public String getShortName() {
                return "Delete User";
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
        new DocView(null, "Repair", db, cmd).addToTabbedDiv();
    }

    public void shardingInfo() {
        final DB db = getDbNode().getDb();
        final DB config = db.getSisterDB("config");
        final DBCollection col = config.getCollection("databases");
        CollectionPanel.doFind(col, new BasicDBObject("_id", db.getName()), null, null, 0, 0, 0, false);
    }

    public void findJSFunction() {
        final DB db = getDbNode().getDb();
        final DBCollection col = db.getCollection("system.js");
        final String name = getStringFieldValue(Item.findJSFunctionName);
        DBObject query = new BasicDBObject();
        if (name != null && !name.isEmpty())
            query.put("_id", name);
        CollectionPanel.doFind(col, query, null, null, 0, 0, 0, false);
    }

    public void addJSFunction() {
        final DB db = getDbNode().getDb();
        final DBCollection col = db.getCollection("system.js");
        final String name = getStringFieldValue(Item.addJSFunctionName);
        final String code = getStringFieldValue(Item.addJSFunctionCode);
        CollectionPanel.doFind(col, new BasicDBObject("_id", name), null, null, 0, 0, 0, false);

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
