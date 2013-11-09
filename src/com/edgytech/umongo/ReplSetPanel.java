/**
 *      Copyright (C) 2010 EdgyTech LLC.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.edgytech.umongo;

import com.edgytech.swingfast.EnumListener;
import com.edgytech.swingfast.XmlComponentUnit;
import com.edgytech.swingfast.XmlUnit;
import com.mongodb.BasicDBObject;
import com.mongodb.CommandResult;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;
import java.util.ArrayList;
import javax.swing.JPanel;
import com.edgytech.umongo.ReplSetPanel.Item;
import com.edgytech.swingfast.*;
import com.mongodb.*;
import java.util.logging.Level;

/**
 *
 * @author antoine
 */
public class ReplSetPanel extends BasePanel implements EnumListener<Item> {

    enum Item {
        refresh,
        name,
        replicas,
        initiate,
        initConfig,
        reconfigure,
        reconfConfig,
        addReplica,
        rsConfig,
        rsStatus,
        rsOplogInfo,
        compareReplicas,
        crStat,
        queryOplog,
        qoStart,
        qoEnd,
        qoQuery,
        sharding,
        manageTags,
        tagList,
        addTag,
        atTag,
        removeTag
    }

    public ReplSetPanel() {
        setEnumBinding(Item.values(), this);
    }

    public ReplSetNode getReplSetNode() {
        return (ReplSetNode) node;
    }

    @Override
    protected void updateComponentCustom(JPanel comp) {
        try {
            if (getReplSetNode().getShardName() == null) {
                ((Menu)getBoundUnit(Item.sharding)).enabled = false;
            }
            
            setStringFieldValue(Item.name, getReplSetNode().getName());
            String replicas = "";
            for (String replica : getReplSetNode().getReplicaNames()) {
                replicas += replica + ",";
            }
            replicas = replicas.substring(0, replicas.length() - 1);
            setStringFieldValue(Item.replicas, replicas);
        } catch (Exception e) {
            UMongo.instance.showError(this.getClass().getSimpleName() + " update", e);
        }
    }

    @Override
    public void actionPerformed(Item enm, XmlComponentUnit unit, Object src) {
    }

    public void rsConfig(ButtonBase button) {
        final DBCollection col = getReplSetNode().getMongoClient().getDB("local").getCollection("system.replset");
        CollectionPanel.doFind(col, null);
    }
    
    public void rsStatus(ButtonBase button) {
        new DbJobCmd(getReplSetNode().getMongoClient().getDB("admin"), "replSetGetStatus").addJob();
    }
    
    public void rsOplogInfo(ButtonBase button) {
        new DbJob() {

            @Override
            public Object doRun() {
                return MongoUtils.getReplicaSetInfo(getReplSetNode().getMongoClient());
            }

            @Override
            public String getNS() {
                return null;
            }

            @Override
            public String getShortName() {
                return "Oplog Info";
            }
        }.addJob();
    }
    
    public void initiate(ButtonBase button) {
        DBObject config = ((DocBuilderField)getBoundUnit(Item.initConfig)).getDBObject();
        DBObject cmd = new BasicDBObject("replSetInitiate", config);
        DB admin = getReplSetNode().getMongoClient().getDB("admin");
        new DbJobCmd(admin, cmd, this, null).addJob();
    }
    
    public void reconfigure(ButtonBase button) {
        final DBCollection col = getReplSetNode().getMongoClient().getDB("local").getCollection("system.replset");
        DBObject oldConf = col.findOne();
        if (oldConf == null) {
            new InfoDialog(null, "reconfig error", null, "No existing replica set configuration").show();
            return;
        }
        ((DocBuilderField)getBoundUnit(Item.reconfConfig)).setDBObject(oldConf);
        if (!((MenuItem)getBoundUnit(Item.reconfigure)).getDialog().show())
            return;
        
        DBObject config = ((DocBuilderField)getBoundUnit(Item.reconfConfig)).getDBObject();
        reconfigure(getReplSetNode(), config);
    }

    static public void reconfigure(final ReplSetNode rsNode, DBObject config) {
        final DBCollection col = rsNode.getMongoClient().getDB("local").getCollection("system.replset");
        DBObject oldConf = col.findOne();
        int version = ((Integer) oldConf.get("version")) + 1;
        config.put("version", version);
        
        // reconfig usually triggers an error as connections are bounced.. try to absorb it
        final DBObject cmd = new BasicDBObject("replSetReconfig", config);
        final DB admin = rsNode.getMongoClient().getDB("admin");

        new DbJob() {

            @Override
            public Object doRun() {
                Object res = null;
                try {
                    res = admin.command(cmd);
                } catch (MongoException.Network e) {
                    res = new BasicDBObject("msg", "Operation was likely successful, but connection was bounced");
                }
                
                try {
                    // sleep a bit since it takes time for driver to see change
                    Thread.sleep(6000);
                } catch (InterruptedException ex) {
                    getLogger().log(Level.WARNING, null, ex);
                }
                return res;
            }

            @Override
            public String getNS() {
                return null;
            }

            @Override
            public String getShortName() {
                return "RS Reconfig";
            }

            @Override
            public DBObject getRoot(Object result) {
                return cmd;
            }

            @Override
            public void wrapUp(Object res) {
                // try to restructure but changes arent seen for a few seconds
                super.wrapUp(res);
                rsNode.structureComponent();
            }
        }.addJob();
    }

    public void addReplica(ButtonBase button) {
        final DBCollection col = getReplSetNode().getMongoClient().getDB("local").getCollection("system.replset");
        DBObject config = col.findOne();
        if (config == null) {
            new InfoDialog(null, "reconfig error", null, "No existing replica set configuration").show();
            return;
        }
        
        BasicDBList members = (BasicDBList) config.get("members");
        int max = 0;
        for (int i = 0; i < members.size(); ++i) {
            int id = (Integer)((DBObject)members.get(i)).get("_id");
            if (id > max)
                max = id;
        }
        
        ReplicaDialog dia = UMongo.instance.getGlobalStore().getReplicaDialog();
        if (!dia.show())
            return;
        BasicDBObject conf = dia.getReplicaConfig(max + 1);
        members.add(conf);
        reconfigure(getReplSetNode(), config);
    }

    public void compareReplicas(ButtonBase button) {
        final String stat = getStringFieldValue(Item.crStat);
        new DbJob() {

            @Override
            public Object doRun() {
                ReplSetNode node = getReplSetNode();
                if (!node.hasChildren())
                    return null;

                ArrayList<MongoClient> svrs = new ArrayList<MongoClient>();
                for (XmlUnit unit : node.getChildren()) {
                    ServerNode svr = (ServerNode) unit;
                    MongoClient svrm = svr.getServerMongoClient();
                    try {
                        svrm.getDatabaseNames();
                    } catch (Exception e) {
                        continue;
                    }
                    svrs.add(svrm);
                }

                BasicDBObject res = new BasicDBObject();
                MongoClient m = getReplSetNode().getMongoClient();
                for (String dbname : m.getDatabaseNames()) {
                    DB db = m.getDB(dbname);
                    BasicDBObject dbres = new BasicDBObject();
                    for (String colname: db.getCollectionNames()) {
                        DBCollection col = db.getCollection(colname);
                        BasicDBObject colres = new BasicDBObject();
                        BasicDBObject values = new BasicDBObject();
                        boolean same = true;
                        long ref = -1;
                        for (MongoClient svrm : svrs) {
                            DBCollection svrcol = svrm.getDB(dbname).getCollection(colname);
                            long value = 0;
                            if (stat.startsWith("Count")) {
                                value = svrcol.count();
                            } else if (stat.startsWith("Data Size")) {
                                CommandResult stats = svrcol.getStats();
                                value = stats.getLong("size");
                            }
                            values.append(svrm.getConnectPoint(), value);
                            if (ref < 0)
                                ref = value;
                            else if (ref != value)
                                same = false;
                        }
                        if (!same) {
                            colres.append("values", values);
                            dbres.append(colname, colres);
                        }
                    }
                    if (!dbres.isEmpty()) {
                        res.append(dbname, dbres);
                    }
                }

                return res;
            }

            @Override
            public String getNS() {
                return "*";
            }

            @Override
            public String getShortName() {
                return "Compare Replicas";
            }
        }.addJob();
    }


    public void queryOplog(ButtonBase button) {
        final DBCollection oplog = getReplSetNode().getMongoClient().getDB("local").getCollection("oplog.rs");
        DBObject start = ((DocBuilderField) getBoundUnit(Item.qoStart)).getDBObject();
        DBObject end = ((DocBuilderField) getBoundUnit(Item.qoEnd)).getDBObject();
        DBObject extra = ((DocBuilderField) getBoundUnit(Item.qoQuery)).getDBObject();
        
        BasicDBObject query = new BasicDBObject();
        BasicDBObject range = new BasicDBObject();
        if (start != null)
            range.put("$gte", start.get("ts"));
        if (end != null)
            range.put("$lte", end.get("ts"));
        
        query.put("ts", range);
        if (extra != null)
            query.putAll(extra);
        
        CollectionPanel.doFind(oplog, query, null, null, 0, 0, 0, false, null, Bytes.QUERYOPTION_OPLOGREPLAY);
    }
    
    void refreshTagList() {
        String shardName = getReplSetNode().getShardName();
        if (shardName == null)
            return;
        
        ListArea list = (ListArea) getBoundUnit(Item.tagList);
        final DB db = ((RouterNode)getReplSetNode().getParentNode()).getMongoClient().getDB("config");
        DBObject shard = db.getCollection("shards").findOne(new BasicDBObject("_id", shardName));
        if (shard.containsField("tags")) {
            BasicDBList tags = (BasicDBList) shard.get("tags");
            if (tags.size() > 0) {
                String[] array = new String[tags.size()];
                int i = 0;
                for (Object tag : tags) {
                    array[i++] = (String) tag;
                }
                list.items = array;
                list.structureComponent();
                return;
            }
        }
        list.items = null;
        list.structureComponent();
    }

    public void manageTags(ButtonBase button) {
        FormDialog dialog = (FormDialog) ((MenuItem) getBoundUnit(Item.manageTags)).getDialog();
        refreshTagList();
        dialog.show();
    }
    
    public void addTag(ButtonBase button) {
        final DB config = ((RouterNode)getReplSetNode().getParentNode()).getMongoClient().getDB("config");
        final DBCollection col = config.getCollection("shards");
        
        ((DynamicComboBox)getBoundUnit(Item.atTag)).items = TagRangeDialog.getExistingTags(config);
        FormDialog dia = (FormDialog) button.getDialog();
        if (!dia.show())
            return;
        final String tag = getStringFieldValue(Item.atTag);
        final DBObject query = new BasicDBObject("_id", getReplSetNode().getShardName());
        final DBObject update = new BasicDBObject("$addToSet", new BasicDBObject("tags", tag));

        new DbJob() {

            @Override
            public Object doRun() {
                return col.update(query, update);
            }

            @Override
            public String getNS() {
                return col.getFullName();
            }

            @Override
            public String getShortName() {
                return "Add Tag";
            }

            @Override
            public DBObject getRoot(Object result) {
                BasicDBObject obj = new BasicDBObject("query", query);
                obj.put("update", update);
                return obj;
            }
            
            @Override
            public void wrapUp(Object res) {
                super.wrapUp(res);
                refreshTagList();
            }
        }.addJob();
    }
    
    public void removeTag(ButtonBase button) {
        final DB db = ((RouterNode)getReplSetNode().getParentNode()).getMongoClient().getDB("config");
        final DBCollection col = db.getCollection("shards");
        final String tag = getComponentStringFieldValue(Item.tagList);
        if (tag == null)
            return;
        
        final DBObject query = new BasicDBObject("_id", getReplSetNode().getShardName());
        final DBObject update = new BasicDBObject("$pull", new BasicDBObject("tags", tag));

        new DbJob() {

            @Override
            public Object doRun() {
                return col.update(query, update);
            }

            @Override
            public String getNS() {
                return col.getFullName();
            }

            @Override
            public String getShortName() {
                return "Remove Tag";
            }

            @Override
            public DBObject getRoot(Object result) {
                BasicDBObject obj = new BasicDBObject("query", query);
                obj.put("update", update);
                return obj;
            }

            @Override
            public void wrapUp(Object res) {
                super.wrapUp(res);
                refreshTagList();
            }
        }.addJob();
    }
}
