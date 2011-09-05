/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mongo.jmongob;

import com.edgytech.swingfast.EnumListener;
import com.edgytech.swingfast.XmlComponentUnit;
import com.edgytech.swingfast.XmlUnit;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;
import java.io.IOException;
import java.util.ArrayList;
import javax.swing.JPanel;
import org.mongo.jmongob.ReplSetPanel.Item;

/**
 *
 * @author antoine
 */
public class ReplSetPanel extends BasePanel implements EnumListener<Item> {

    enum Item {
        refresh,
        name,
        replicaSetStatus,
        oplogInfo,
        maxObjectSize,
        compareReplicas,
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
            setStringFieldValue(Item.maxObjectSize, String.valueOf(getReplSetNode().getMongo().getReplicaSetStatus().getMaxBsonObjectSize()));
        } catch (Exception e) {
            JMongoBrowser.instance.showError(this.getClass().getSimpleName() + " update", e);
        }
    }

    public void actionPerformed(Item enm, XmlComponentUnit unit, Object src) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void replicaSetStatus() {
        new DocView(null, "RS Status", getReplSetNode().getMongo().getDB("admin"), "replSetGetStatus").addToTabbedDiv();
    }
    
    public void oplogInfo() {
        new DocView(null, "Oplog Info", MongoUtils.getReplicaSetInfo(getReplSetNode().getMongo()),  "Oplog of " + getReplSetNode().getMongo().toString(), null).addToTabbedDiv();
    }

    public void compareReplicas() {
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
                        BasicDBObject counts = new BasicDBObject();
                        boolean same = true;
                        long value = -1;
                        for (Mongo svrm : svrs) {
                            DBCollection svrcol = svrm.getDB(dbname).getCollection(colname);
                            long count = svrcol.count();
                            counts.append(svrm.getConnectPoint(), count);
                            if (value < 0)
                                value = count;
                            else if (value != count)
                                same = false;
                        }
                        if (!same) {
                            colres.append("count", counts);
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

}
