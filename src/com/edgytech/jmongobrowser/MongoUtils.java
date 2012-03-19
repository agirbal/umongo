/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.edgytech.jmongobrowser;

import com.mongodb.BasicDBObject;
import com.mongodb.Bytes;
import com.mongodb.CommandResult;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.util.JSON;
import java.util.Date;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import org.bson.types.BSONTimestamp;
import org.bson.types.ObjectId;

/**
 *
 * @author antoine
 */
public class MongoUtils {

    public static String queryOptionsToString(int options) {
        String opt = "";
        if ((options & Bytes.QUERYOPTION_TAILABLE) != 0) {
            opt += "TAILABLE ";
        }
        if ((options & Bytes.QUERYOPTION_SLAVEOK) != 0) {
            opt += "SLAVEOK ";
        }
        if ((options & Bytes.QUERYOPTION_OPLOGREPLAY) != 0) {
            opt += "OPLOGREPLAY ";
        }
        if ((options & Bytes.QUERYOPTION_NOTIMEOUT) != 0) {
            opt += "NOTIMEOUT ";
        }
        if ((options & Bytes.QUERYOPTION_AWAITDATA) != 0) {
            opt += "AWAITDATA ";
        }
        if ((options & Bytes.QUERYOPTION_EXHAUST) != 0) {
            opt += "EXHAUST ";
        }
        return opt;
    }

    public static DefaultMutableTreeNode dbObjectToTreeNode(DBObject obj) {
        return dbObjectToTreeNode(null, obj);
    }

    public static DefaultMutableTreeNode dbObjectToTreeNode(Object objkey, DBObject obj) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(new DBObjectWrapper(objkey, obj));
        addChildrenToTreeNode(node, obj);
        return node;
    }

    public static void addChildrenToTreeNode(DefaultMutableTreeNode node, DBObject obj) {
        for (String key : obj.keySet()) {
            Object val = obj.get(key);
            if (val == null) {
                continue;
            }

            DefaultMutableTreeNode child = null;
            if (val instanceof DBObject) {
                child = dbObjectToTreeNode(key, (DBObject) val);
            } else {
                String str = key + " : " + JSON.serialize(val);
                child = new DefaultMutableTreeNode(limitString(str, 0));
            }

            if (val instanceof ObjectId) {
                // break it down
                ObjectId id = (ObjectId) val;
                child.add(new DefaultMutableTreeNode("Time: " + id.getTime() + " = " + new Date(id.getTime()).toString()));
                child.add(new DefaultMutableTreeNode("Machine: " + (id.getMachine() & 0xFFFFFFFFL)));
                child.add(new DefaultMutableTreeNode("Inc: " + (id.getInc() & 0xFFFFFFFFL)));
            }
            node.add(child);
        }
    }

    public static String dbObjectToString(DBObject obj) {
        return dbObjectToString(obj, 0);
    }

    public static String dbObjectToString(DBObject obj, int limit) {
        return limitString(obj.toString(), limit);
    }

    public static String limitString(String str, int limit) {
        if (limit <= 0) {
            limit = JMongoBrowser.instance.getPreferences().getInlineDocumentLength();
        }
        if (str.length() > limit && limit > 0) {
            int max = Math.max(0, limit - 3);
            str = str.substring(0, max) + " ..";
        }
        return str;
    }

    public static DBObject getReplicaSetInfo(Mongo mongo) {
        DB db = mongo.getDB("local");
        DBObject result = new BasicDBObject();
        DBCollection namespaces = db.getCollection("system.namespaces");
        String oplogName;
        if (namespaces.findOne(new BasicDBObject("name", "local.oplog.rs")) != null) {
            oplogName = "oplog.rs";
        } else if (namespaces.findOne(new BasicDBObject("name", "local.oplog.$main")) != null) {
            oplogName = "oplog.$main";
        } else {
            return null;
        }
        DBObject olEntry = namespaces.findOne(new BasicDBObject("name", "local." + oplogName));
        if (olEntry != null && olEntry.containsField("options")) {
            BasicDBObject options = (BasicDBObject) olEntry.get("options");
            long size = options.getLong("size");
            result.put("logSizeMB", Float.valueOf(String.format("%.2f", size / 1048576f)));
        } else {
            return null;
        }
        DBCollection oplog = db.getCollection(oplogName);
        int size = oplog.getStats().getInt("size");
        result.put("usedMB", Float.valueOf(String.format("%.2f", size / 1048576f)));

        DBCursor firstc = oplog.find().sort(new BasicDBObject("$natural", 1)).limit(1);
        DBCursor lastc = oplog.find().sort(new BasicDBObject("$natural", -1)).limit(1);
        if (!firstc.hasNext() || !lastc.hasNext()) {
            return null;
        }
        BasicDBObject first = (BasicDBObject) firstc.next();
        BasicDBObject last = (BasicDBObject) lastc.next();
        BSONTimestamp tsfirst = (BSONTimestamp) first.get("ts");
        BSONTimestamp tslast = (BSONTimestamp) last.get("ts");
        if (tsfirst == null || tslast == null) {
            return null;
        }

        int ftime = tsfirst.getTime();
        int ltime = tslast.getTime();
        int timeDiffSec = ltime - ftime;
        result.put("timeDiff", timeDiffSec);
        result.put("timeDiffHours", Float.valueOf(String.format("%.2f", timeDiffSec / 3600f)));
        result.put("tFirst", new Date(ftime * 1000l));
        result.put("tLast", new Date(ltime * 1000l));
        result.put("now", new Date());
        return result;
    }

    public static boolean isBalancerOn(Mongo mongo) {
        final DB config = mongo.getDB("config");
        final DBCollection settings = config.getCollection("settings");
        BasicDBObject res = (BasicDBObject) settings.findOne(new BasicDBObject("_id", "balancer"));
        if (res == null || !res.containsField("stopped"))
            return true;
        return !res.getBoolean("stopped");
    }
    
    static String makeInfoString(Object ... args) {
        String info = "";
        for (int i = 0; i < args.length; i += 2) {
            if (i > 0)
                info += ", ";
            info += args[i] + "=[" + args[i + 1] + "]";
        }
        return info;
    }
}
