/**
 * Copyright (C) 2010 EdgyTech LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.edgytech.umongo;

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
        dbHash,
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
        analyzeProfilingData,
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

    public void command(final ButtonBase button) {
        final DB db = getDbNode().getDb();
        final String scmd = getStringFieldValue(Item.commandStr);
        final DBObject cmd = !scmd.isEmpty() ? new BasicDBObject(scmd, 1)
                : ((DocBuilderField) getBoundUnit(Item.commandJson)).getDBObject();
        boolean help = getBooleanFieldValue(Item.commandHelp);
        if (help) {
            cmd.put("help", true);
        }

        new DbJobCmd(db, cmd, null, button).addJob();
    }

    public void listCommands(ButtonBase button) {
        new DbJobCmd(getDbNode().getDb(), "listCommands").addJob();
    }

    public void eval(final ButtonBase button) {
        final DB db = getDbNode().getDb();
        final String sfunc = getStringFieldValue(Item.evalCode);
        final boolean noLock = getBooleanFieldValue(Item.evalNoLock);
        final BasicDBObject cmd = new BasicDBObject("$eval", sfunc);
        if (noLock) {
            cmd.put("nolock", true);
        }
        new DbJobCmd(db, cmd, null, button).addJob();
    }

    public void uploadFile(final ButtonBase button) {
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
            public DBObject getRoot(Object result) {
                return new BasicDBObject("path", path);
            }

            @Override
            public void wrapUp(Object res) {
                super.wrapUp(res);
                // may have new collections
                dbNode.structureComponent();
            }

            @Override
            public ButtonBase getButton() {
                return button;
            }
        }.addJob();
    }

    public void downloadFile(final ButtonBase button) {
        final DB db = getDbNode().getDb();
        final DBObject query = ((DocBuilderField) getBoundUnit(Item.downloadQuery)).getDBObject();
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
                if (query != null) {
                    dbfile = getGridFS().findOne(query);
                } else {
                    dbfile = getGridFS().findOne(fname);
                }
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
            public DBObject getRoot(Object result) {
                BasicDBObject obj = new BasicDBObject("filename", fname);
                obj.put("query", query);
                obj.put("path", dpath);
                return obj;
            }

            @Override
            public ButtonBase getButton() {
                return button;
            }
        }.addJob();
    }

    public void listFiles(ButtonBase button) {
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

    public void deleteFile(final ButtonBase button) {
        final DB db = getDbNode().getDb();
        final DBObject query = ((DocBuilderField) getBoundUnit(Item.downloadQuery)).getDBObject();
        final String fname = getStringFieldValue(Item.downloadFileName);

        new DbJob() {

            @Override
            public Object doRun() throws IOException {
                if (query != null) {
                    getGridFS().remove(query);
                } else {
                    getGridFS().remove(fname);
                }
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
            public DBObject getRoot(Object result) {
                BasicDBObject obj = new BasicDBObject("filename", fname);
                obj.put("query", query);
                return obj;
            }

            @Override
            public ButtonBase getButton() {
                return button;
            }
        }.addJob();
    }

    public void dropDatabase(ButtonBase button) {
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

    public void options(ButtonBase button) {
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

    public void authenticate(final ButtonBase button) {
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

            @Override
            public ButtonBase getButton() {
                return button;
            }
        }.addJob();
    }

    void refreshUserList() {
        ListArea list = (ListArea) getBoundUnit(Item.userList);
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

    public void manageUsers(ButtonBase button) {
        FormDialog dialog = (FormDialog) ((MenuItem) getBoundUnit(Item.manageUsers)).getDialog();
        refreshUserList();

        if (!dialog.show()) {
            return;
        }
    }

    public void addUser(ButtonBase button) {
        final DB db = getDbNode().getDb();
        final DBCollection col = db.getCollection("system.users");

        UserDialog ud = (UserDialog) getBoundUnit(Item.userDialog);
        ud.resetForNew();
        if (!ud.show()) {
            return;
        }

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

    public void removeUser(ButtonBase button) {
        final DB db = getDbNode().getDb();

        final String user = getComponentStringFieldValue(Item.userList);
        if (user == null) {
            return;
        }
        if (!((ConfirmDialog) getBoundUnit(Item.userChange)).show()) {
            return;
        }

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

    public void editUser(ButtonBase button) {
        final DB db = getDbNode().getDb();
        final DBCollection col = db.getCollection("system.users");

        final String user = getComponentStringFieldValue(Item.userList);
        if (user == null) {
            return;
        }

        final BasicDBObject userObj = (BasicDBObject) col.findOne(new BasicDBObject("user", user));
        UserDialog ud = (UserDialog) getBoundUnit(Item.userDialog);
        ud.resetForEdit(userObj);
        if (!ud.show()) {
            return;
        }

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

    public void createCollection(final ButtonBase button) {
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
                if (!autoIndexId) {
                    opt.put("autoIndexId", false);
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

            @Override
            public ButtonBase getButton() {
                return button;
            }
        }.addJob();

    }

    public void stats(ButtonBase button) {
        new DbJobCmd(getDbNode().getDb(), "dbstats").addJob();
    }

    public void dbHash(ButtonBase button) {
        new DbJobCmd(getDbNode().getDb(), "dbHash").addJob();
    }

    public void enableSharding(ButtonBase button) {
        Mongo m = getDbNode().getMongoNode().getMongo();
        DB admin = m.getDB("admin");
        DBObject cmd = new BasicDBObject("enableSharding", getDbNode().getDb().getName());
        new DbJobCmd(admin, cmd, this, null).addJob();
    }

    public void profile(ButtonBase button) {
        DB db = getDbNode().getDb();
        int level = getIntFieldValue(Item.profileLevel);
        final DBObject cmd = new BasicDBObject("profile", level);
        if (level == 1) {
            int slow = getIntFieldValue(Item.profileSlowMS);
            if (slow > 0) {
                cmd.put("slowms", slow);
            }
        }
        new DbJobCmd(db, cmd).addJob();
    }

    public void analyzeProfilingData(ButtonBase button) {
        new DbJob() {

            @Override
            public Object doRun() throws Exception {
                DBCollection prof = getDbNode().getDb().getCollection("system.profile");
                BasicDBObject out = new BasicDBObject();
                CommandResult res = null;
                DocumentDeserializer dd = null;
                BasicDBObject aggCmd = null;


                // response time by operation type
                dd = new DocumentDeserializer(DocumentDeserializer.Format.JSON_SINGLE_DOC, null);
                dd.setInputStream(DbPanel.class.getResourceAsStream("/json/profOperationType.json"));
                aggCmd = (BasicDBObject) dd.readObject();
                res = getDbNode().getDb().command(aggCmd);
                out.put("byOperationType", res.get("result"));
                dd.close();

                // slowest by namespace
                dd = new DocumentDeserializer(DocumentDeserializer.Format.JSON_SINGLE_DOC, null);
                dd.setInputStream(DbPanel.class.getResourceAsStream("/json/profNamespace.json"));
                aggCmd = (BasicDBObject) dd.readObject();
                res = getDbNode().getDb().command(aggCmd);
                out.put("byNamespace", res.get("result"));
                dd.close();

                // slowest by client
                dd = new DocumentDeserializer(DocumentDeserializer.Format.JSON_SINGLE_DOC, null);
                dd.setInputStream(DbPanel.class.getResourceAsStream("/json/profClient.json"));
                aggCmd = (BasicDBObject) dd.readObject();
                res = getDbNode().getDb().command(aggCmd);
                out.put("byClient", res.get("result"));
                dd.close();

                // summary moved vs non-moved
                dd = new DocumentDeserializer(DocumentDeserializer.Format.JSON_SINGLE_DOC, null);
                dd.setInputStream(DbPanel.class.getResourceAsStream("/json/profMoved.json"));
                aggCmd = (BasicDBObject) dd.readObject();
                res = getDbNode().getDb().command(aggCmd);
                out.put("movedVsNonMoved", res.get("result"));
                dd.close();

                // response time analysis
                dd = new DocumentDeserializer(DocumentDeserializer.Format.JSON_SINGLE_DOC, null);
                dd.setInputStream(DbPanel.class.getResourceAsStream("/json/profResponseTimeAnalysis.json"));
                aggCmd = (BasicDBObject) dd.readObject();
                res = getDbNode().getDb().command(aggCmd);
                out.put("responseTimeAnalysis", res.get("result"));
                dd.close();

                return out;
            }

            @Override
            public String getNS() {
                return getDbNode().getDb().getName();
            }

            @Override
            public String getShortName() {
                return "Analyze Profiling Data";
            }
        }.addJob();
    }

    public void repair(ButtonBase button) {
        DB db = getDbNode().getDb();
        final DBObject cmd = new BasicDBObject("repairDatabase", 1);
        if (!UMongo.instance.getGlobalStore().confirmLockingOperation()) {
            return;
        }
        new DbJobCmd(db, cmd).addJob();
    }

    public void shardingInfo(ButtonBase button) {
        final DB db = getDbNode().getDb();
        final DB config = db.getSisterDB("config");
        final DBCollection col = config.getCollection("databases");
        CollectionPanel.doFind(col, new BasicDBObject("_id", db.getName()));
    }

    public void movePrimary(ButtonBase button) {
        FormDialog dialog = (FormDialog) ((MenuItem) getBoundUnit(Item.movePrimary)).getDialog();
        ComboBox combo = (ComboBox) getBoundUnit(Item.mvpToShard);
        combo.value = 0;
        combo.items = getDbNode().getMongoNode().getShardNames();
        combo.structureComponent();

        if (!dialog.show()) {
            return;
        }

        Mongo m = getDbNode().getMongoNode().getMongo();
        DB admin = m.getDB("admin");
        String shard = getStringFieldValue(Item.mvpToShard);
        DBObject cmd = new BasicDBObject("movePrimary", getDbNode().getDb().getName());
        cmd.put("to", shard);
        new DbJobCmd(admin, cmd, this, null).addJob();
    }

    public void findJSFunction(ButtonBase button) {
        final DB db = getDbNode().getDb();
        final DBCollection col = db.getCollection("system.js");
        final String name = getStringFieldValue(Item.findJSFunctionName);
        DBObject query = new BasicDBObject();
        if (name != null && !name.isEmpty()) {
            query.put("_id", name);
        }
        CollectionPanel.doFind(col, query);
    }

    public void addJSFunction(final ButtonBase button) {
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

            @Override
            public ButtonBase getButton() {
                return null;
            }
        }.addJob();
    }
}
