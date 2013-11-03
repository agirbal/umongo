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
        qoQuery
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
        final DBCollection col = getReplSetNode().getMongo().getDB("local").getCollection("system.replset");
        CollectionPanel.doFind(col, null);
    }
    
    public void rsStatus(ButtonBase button) {
        new DbJobCmd(getReplSetNode().getMongo().getDB("admin"), "replSetGetStatus").addJob();
    }
    
    public void rsOplogInfo(ButtonBase button) {
        new DbJob() {

            @Override
            public Object doRun() {
                return MongoUtils.getReplicaSetInfo(getReplSetNode().getMongo());
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
        DB admin = getReplSetNode().getMongo().getDB("admin");
        new DbJobCmd(admin, cmd, this, null).addJob();
    }
    
    public void reconfigure(ButtonBase button) {
        final DBCollection col = getReplSetNode().getMongo().getDB("local").getCollection("system.replset");
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
        final DBCollection col = rsNode.getMongo().getDB("local").getCollection("system.replset");
        DBObject oldConf = col.findOne();
        int version = ((Integer) oldConf.get("version")) + 1;
        config.put("version", version);
        
        // reconfig usually triggers an error as connections are bounced.. try to absorb it
        final DBObject cmd = new BasicDBObject("replSetReconfig", config);
        final DB admin = rsNode.getMongo().getDB("admin");

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
        final DBCollection col = getReplSetNode().getMongo().getDB("local").getCollection("system.replset");
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

                ArrayList<Mongo> svrs = new ArrayList<Mongo>();
                for (XmlUnit unit : node.getChildren()) {
                    ServerNode svr = (ServerNode) unit;
                    Mongo svrm = svr.getServerMongo();
                    try {
                        svrm.getDatabaseNames();
                    } catch (Exception e) {
                        continue;
                    }
                    svrs.add(svrm);
                }

                BasicDBObject res = new BasicDBObject();
                Mongo m = getReplSetNode().getMongo();
                for (String dbname : m.getDatabaseNames()) {
                    DB db = m.getDB(dbname);
                    BasicDBObject dbres = new BasicDBObject();
                    for (String colname: db.getCollectionNames()) {
                        DBCollection col = db.getCollection(colname);
                        BasicDBObject colres = new BasicDBObject();
                        BasicDBObject values = new BasicDBObject();
                        boolean same = true;
                        long ref = -1;
                        for (Mongo svrm : svrs) {
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
        final DBCollection oplog = getReplSetNode().getMongo().getDB("local").getCollection("oplog.rs");
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
}
