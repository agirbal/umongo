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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.mongodb.*;
import com.mongodb.util.JSONSerializers;
import com.mongodb.util.ObjectSerializer;
import java.util.Date;
import java.util.Map;
import javax.swing.tree.DefaultMutableTreeNode;
import org.bson.LazyDBList;
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

    public static void addChildrenToTreeNode(DefaultMutableTreeNode node, DBObject obj) {
        for (String key : obj.keySet()) {
            Object val = obj.get(key);
//            if (val == null) {
//                continue;
//            }

            DefaultMutableTreeNode child = new DefaultMutableTreeNode(new TreeNodeDocumentField(key, val));
            if (val instanceof DBObject) {
                addChildrenToTreeNode(child, (DBObject) val);
            } else if (val instanceof ObjectId) {
                // break it down
                ObjectId id = (ObjectId) val;
                child.add(new DefaultMutableTreeNode("Time: " + id.getTime() + " = " + new Date(id.getTime()).toString()));
                child.add(new DefaultMutableTreeNode("Machine: " + (id.getMachine() & 0xFFFFFFFFL)));
                child.add(new DefaultMutableTreeNode("Inc: " + (id.getInc() & 0xFFFFFFFFL)));
            }
            node.add(child);
        }
    }

    public static String getObjectString(Object obj) {
        return getObjectString(obj, 0);
    }

    public static String getObjectString(Object obj, int limit) {
        String str;
        if (obj == null) {
            str = "null";
        } else if (obj instanceof DBObject || obj instanceof byte[]) {
            // get rid of annoying scientific format
            str = MongoUtils.getJSON(obj);
        } else if (obj instanceof Double) {
            // get rid of annoying scientific format
            str = String.format("%f", obj);
        } else if (obj instanceof String) {
            // should show quotes to be JSON like
            str = "\"" + obj + "\"";
        } else {
            str = obj.toString();
        }
        return limitString(str, limit);
    }

    public static String limitString(String str, int limit) {
        if (limit <= 0) {
            limit = UMongo.instance.getPreferences().getInlineDocumentLength();
        }
        if (str.length() > limit && limit > 0) {
            int max = Math.max(0, limit - 3);
            str = str.substring(0, max) + " ..";
        }
        return str;
    }

    static ObjectSerializer getSerializer() {
        return JSONSerializers.getStrict();
    }
    
    static String getJSONPreview(Object value) {
        return MongoUtils.limitString(getSerializer().serialize(value), 80);
    }
    
    static String getJSON(Object value) {
        return getSerializer().serialize(value);
    }
    
    public static DBObject getReplicaSetInfo(MongoClient mongo) {
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

    public static boolean isBalancerOn(MongoClient mongo) {
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
    
    public static DBObject checkObject( DBObject o , boolean canBeNull , boolean query ){
        if ( o == null ){
            if ( canBeNull )
                return null;
            throw new IllegalArgumentException( "can't be null" );
        }

        if ( o.isPartialObject() && ! query )
            throw new IllegalArgumentException( "can't save partial objects" );

        if ( ! query ){
            checkKeys(o);
        }
        return o;
    }

    /**
     * Checks key strings for invalid characters.
     */
    public static void checkKeys( DBObject o ) {
        if ( o instanceof LazyDBObject || o instanceof LazyDBList )
            return;

        for ( String s : o.keySet() ){
            validateKey ( s );
            Object inner = o.get( s );
            if ( inner instanceof DBObject ) {
                checkKeys( (DBObject)inner );
            } else if ( inner instanceof Map ) {
                checkKeys( (Map<String, Object>)inner );
            }
        }
    }

    /**
     * Checks key strings for invalid characters.
     */
    public static void checkKeys( Map<String, Object> o ) {
        for ( String s : o.keySet() ){
            validateKey ( s );
            Object inner = o.get( s );
            if ( inner instanceof DBObject ) {
                checkKeys( (DBObject)inner );
            } else if ( inner instanceof Map ) {
                checkKeys( (Map<String, Object>)inner );
            }
        }
    }

    /**
     * Check for invalid key names
     * @param s the string field/key to check
     * @exception IllegalArgumentException if the key is not valid.
     */
    public static void validateKey(String s ) {
        if ( s.contains( "." ) )
            throw new IllegalArgumentException( "fields stored in the db can't have . in them. (Bad Key: '" + s + "')" );
        if ( s.startsWith( "$" ) )
            throw new IllegalArgumentException( "fields stored in the db can't start with '$' (Bad Key: '" + s + "')" );
    }

    
    private static JsonParser jsonParser;
    public static JsonParser getJsonParser() {
        if (jsonParser == null) {
            jsonParser = new JsonParser();        
        }
        return jsonParser;
    }
    
    private static Gson gson;
    public static Gson getGson() {
        if (gson == null) {
            gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        }
        return gson;
    }
}
