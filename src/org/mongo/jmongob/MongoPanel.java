/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mongo.jmongob;

import com.edgytech.swingfast.EnumListener;
import com.edgytech.swingfast.FormDialog;
import com.edgytech.swingfast.InfoDialog;
import com.edgytech.swingfast.XmlComponentUnit;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import javax.swing.JPanel;
import org.bson.types.BSONTimestamp;
import org.bson.types.MaxKey;
import org.bson.types.MinKey;
import org.mongo.jmongob.MongoPanel.Item;

/**
 *
 * @author antoine
 */
public class MongoPanel extends BasePanel implements EnumListener<Item> {

    enum Item {

        icon,
        version,
        connectPoint,
        masterServer,
        seedServers,
        activeServers,
        queryOptions,
        writeConcern,
        serverStatus,
        fsync,
        fsyncAndLock,
        options,
        refresh,
        close,
        createDB,
        createDbName,
        authenticate,
        authUser,
        authPassword,
        maxObjectSize,
        logRotate,
        showLog,
        shutdown,
        cloneDB,
        cloneDBHost,
        cloneDBFrom,
        cloneDBTo,
        cloneDBSlaveOk,
        currentOps,
        currentOpsQuery,
        killOp,
        killOpId,
        isMaster,
        regenConfigDB,
        regenServers,
        regenDB,
        regenCollection,
        regenShardKey,
        regenKeyUnique,
        regenRSList,
        regenRSListArea,
        regenDeleteChunks,
        regenConfirm
    }

    public MongoPanel() {
        setEnumBinding(Item.values(), this);
    }

    public MongoNode getMongoNode() {
        return (MongoNode) node;
    }

    private String getServerAddressString(ServerAddress addr) {
        return addr.getHost() + ":" + addr.getPort() + " (" + addr.getSocketAddress().getAddress().toString() + ")";
    }

    @Override
    protected void updateComponentCustom(JPanel old) {
        try {
            Mongo mongo = getMongoNode().getMongo();
            setStringFieldValue(Item.version, mongo.getVersion());

            ServerAddress master = mongo.getAddress();
            if (master != null) {
                setStringFieldValue(Item.masterServer, getServerAddressString(master));
            }
            List<ServerAddress> addrs = mongo.getAllAddress();
            String html = "<html>";
            for (ServerAddress addr : addrs) {
                html += getServerAddressString(addr) + "<br/>";
            }
            html += "</html>";
            setStringFieldValue(Item.seedServers, html);

            addrs = mongo.getServerAddressList();
            html = "<html>";
            for (ServerAddress addr : addrs) {
                String ports = MongoUtils.makeInfoString("used", mongo.getConnector().getDBPortPool(addr).inUse(),
                        "available", mongo.getConnector().getDBPortPool(addr).available(),
                        "created", mongo.getConnector().getDBPortPool(addr).everCreated());
                String txt = getServerAddressString(addr) + " - " + ports;
                html += txt + "<br/>";
            }
            setStringFieldValue(Item.activeServers, html);

            setStringFieldValue(Item.queryOptions, MongoUtils.queryOptionsToString(mongo.getOptions()));
            ((DocField) getBoundUnit(Item.writeConcern)).setDoc(mongo.getWriteConcern().getCommand());
            setStringFieldValue(Item.maxObjectSize, String.valueOf(mongo.getMaxBsonObjectSize()));

            setBooleanFieldValue(Item.fsyncAndLock, mongo.isLocked());
        } catch (Exception e) {
            JMongoBrowser.instance.showError(this.getClass().getSimpleName() + " update", e);
        }
    }

    public void actionPerformed(Item enm, XmlComponentUnit unit, Object src) {
    }

    public void close() {
        JMongoBrowser.instance.disconnect(getMongoNode());
        node = null;
    }

    public void fsync() {
        DBObject cmd = new BasicDBObject("fsync", 1);
        if (false) {
            cmd.put("async", 1);
        }
        DB admin = getMongoNode().getMongo().getDB("admin");
        new DocView(null, "Fsync", admin, cmd).addToTabbedDiv();
    }

    public void fsyncAndLock() {
        new DbJob() {

            @Override
            public Object doRun() throws IOException {
                Mongo mongo = getMongoNode().getMongo();
                boolean locked = mongo.isLocked();
                if (locked) {
                    return mongo.unlock();
                }

                return mongo.fsyncAndLock();
            }

            @Override
            public String getNS() {
                return null;
            }

            @Override
            public String getShortName() {
                return "FSync And Lock";
            }

            @Override
            public void wrapUp(Object res) {
                try {
                    // looks like the unlock doesnt take effect right away
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                }
                updateComponent();
                super.wrapUp(res);
            }
        }.addJob();
    }

    public void options() {
        Mongo mongo = getMongoNode().getMongo();
        OptionDialog od = JMongoBrowser.instance.getGlobalStore().getOptionDialog();
        od.update(mongo.getOptions(), mongo.getWriteConcern());
        if (!od.show()) {
            return;
        }
        mongo.setOptions(od.getQueryOptions());
        mongo.setWriteConcern(od.getWriteConcern());
        refresh();
    }

    public void createDB() {
        final String name = getStringFieldValue(Item.createDbName);
        final Mongo mongo = getMongoNode().getMongo();
        mongo.getDB(name);
        getMongoNode().structureComponent();
    }

    public void authenticate() {
        final Mongo mongo = getMongoNode().getMongo();
        final String user = getStringFieldValue(Item.authUser);
        final String passwd = getStringFieldValue(Item.authPassword);

        new DbJob() {

            @Override
            public Object doRun() throws IOException {
                mongo.getDB("admin").authenticateCommand(user, passwd.toCharArray());
                return null;
            }

            @Override
            public String getNS() {
                return "Mongo";
            }

            @Override
            public String getShortName() {
                return "Auth";
            }

            @Override
            public void wrapUp(Object res) {
                super.wrapUp(res);
                if (res == null) {
                    // need to refresh tree
                    refresh();
                }
            }
        }.addJob();

    }

    public void serverStatus() {
        new DocView(null, "Server Status", getMongoNode().getMongo().getDB("admin"), "serverStatus").addToTabbedDiv();
    }

    public void logRotate() {
        new DocView(null, "Log Rotate", getMongoNode().getMongo().getDB("admin"), "logRotate").addToTabbedDiv();
    }

    public void showLog() {
        new DocView(null, "Show Log", getMongoNode().getMongo().getDB("admin"), new BasicDBObject("getLog", "global")).addToTabbedDiv();
    }

    public void shutdown() {
        new DocView(null, "Shutdown", getMongoNode().getMongo().getDB("admin"), "shutdown").addToTabbedDiv();
    }

    public void cloneDB() {
        final Mongo mongo = getMongoNode().getMongo();
        final String host = getStringFieldValue(Item.cloneDBHost);
        final String from = getStringFieldValue(Item.cloneDBFrom);
        final String to = getStringFieldValue(Item.cloneDBTo);
        final boolean slaveOk = getBooleanFieldValue(Item.cloneDBSlaveOk);
        final BasicDBObject cmd = new BasicDBObject("copydb", 1);
        cmd.put("fromhost", host);
        cmd.put("fromdb", from);
        if (!to.isEmpty()) {
            cmd.put("todb", to);
        }
        if (slaveOk) {
            cmd.put("slaveOk", slaveOk);
        }

        new DocView(null, "Clone DB", mongo.getDB("admin"), cmd, null, this, null).addToTabbedDiv();
    }

    public void currentOps() {
        final Mongo mongo = getMongoNode().getMongo();
        final DBObject query = ((DocBuilderField) getBoundUnit(Item.currentOpsQuery)).getDBObject();
        CollectionPanel.doFind(mongo.getDB("admin").getCollection("$cmd.sys.inprog"), query, null, null, 0, 0, 0, false);
    }

    public void killOp() {
        final Mongo mongo = getMongoNode().getMongo();
        final int opid = getIntFieldValue(Item.killOpId);
        final DBObject query = new BasicDBObject("op", opid);
        CollectionPanel.doFind(mongo.getDB("admin").getCollection("$cmd.sys.killop"), query, null, null, 0, 0, 0, false);
    }

    public void isMaster() {
        new DocView(null, "Is Master", getMongoNode().getMongo().getDB("admin"), "isMaster").addToTabbedDiv();
    }

    public void regenConfigDB() throws UnknownHostException {
        Mongo cmongo = getMongoNode().getMongo();
        String servers = getStringFieldValue(Item.regenServers);
        final String db = getStringFieldValue(Item.regenDB);
        final String col = getStringFieldValue(Item.regenCollection);
        final String ns = db + "." + col;
        final BasicDBObject shardKey = ((DocBuilderField) getBoundUnit(Item.regenShardKey)).getDBObject();
        final boolean unique = getBooleanFieldValue(Item.regenKeyUnique);
        final BasicDBObject result = new BasicDBObject();
        result.put("ns", ns);
        result.put("shardKey", shardKey);
        result.put("unique", unique);

        // create direct mongo for each replica set
        String[] serverList = servers.split("\n");
        List<ServerAddress> list = new ArrayList<ServerAddress>();
        String txt = "";
        String primaryShard = null;
        final BasicDBObject shardList = new BasicDBObject();
        HashMap<Mongo, String> mongoToShard = new HashMap<Mongo, String>();
        sLoop:
        for (String server : serverList) {
            server = server.trim();
            String[] tokens = server.split("/");
            if (tokens.length != 2) {
                new InfoDialog(null, "Error", null, "Server format must be like 'hostname:port/shard', one by line").show();
                return;
            }
            server = tokens[0];
            String shard = tokens[1];
            if (primaryShard == null) {
                primaryShard = shard;
            }
            ServerAddress addr = new ServerAddress(server);

            // filter out if replset already exists
            for (Mongo replset : mongoToShard.keySet()) {
                if (replset.getServerAddressList().contains(addr)) {
                    continue sLoop;
                }
            }

            list.clear();
            list.add(addr);
            Mongo mongo = new Mongo(list);
//            JMongoBrowser.instance.addMongo(mongo, null);
            // make request to force server detection
            mongo.getDatabaseNames();
            mongoToShard.put(mongo, shard);

            String desc = null;
            if (!mongo.getDatabaseNames().contains(db)
                    || !mongo.getDB(db).getCollectionNames().contains(col)) {
                desc = "Collection not present!";
            } else {
                // try to see if shard key has index
                DBObject index = mongo.getDB(db).getCollection("system.indexes").findOne(new BasicDBObject("key", shardKey));
                if (index != null) {
                    desc = "shard key found";
                } else {
                    desc = "shard key NOT found!";
                }
            }
            txt += mongo.toString() + " shard=" + shard + " - " + desc + "\n";
            BasicDBObject shardObj = new BasicDBObject("servers", mongo.toString());
            shardObj.put("status", desc);
            if (shardList.containsField(shard)) {
                new InfoDialog(null, "Error", null, "Duplicate Shard name " + shard).show();
                return;
            }
            shardList.put(shard, shardObj);
        }
        result.put("shards", shardList);

        FormDialog dia = (FormDialog) getBoundUnit(Item.regenRSList);
        dia.setStringFieldValue(Item.regenRSListArea, txt);
        if (!dia.show()) {
            return;
        }

        DB config = cmongo.getDB("config");

        // add database record
        BasicDBObject doc = new BasicDBObject("_id", db);
        doc.put("partitioned", true);
        doc.put("primary", primaryShard);
        config.getCollection("databases").save(doc);

        // add collection record
        doc = new BasicDBObject("_id", ns);
        doc.put("lastmod", new Date());
        doc.put("dropped", false);
        doc.put("key", shardKey);
        doc.put("unique", unique);
        config.getCollection("collections").save(doc);

        final DBCollection chunks = config.getCollection("chunks");
        long count = chunks.count(new BasicDBObject("ns", ns));
        if (count > 0) {
            dia = (FormDialog) getBoundUnit(Item.regenDeleteChunks);
            if (dia.show()) {
                chunks.remove(new BasicDBObject("ns", ns));
            } else {
                return;
            }
        }

        // add temp collection to sort chunks with shard key
        final DBCollection tmpchunks = config.getCollection("_tmpchunks_" + col);
        tmpchunks.drop();
        // should be safe environment, and dup keys should be ignored
        tmpchunks.setWriteConcern(WriteConcern.NORMAL);
        // can use shardKey as unique _id
//        tmpchunks.ensureIndex(shardKey, "shardKey", true);

        // create filter for shard fields
        final DBObject shardKeyFilter = new BasicDBObject();
//        final DBObject shardKeyDescend = new BasicDBObject();
        boolean hasId = false;
        for (String key : shardKey.keySet()) {
            shardKeyFilter.put(key, 1);
            if (key.equals("_id")) {
                hasId = true;
            }
        }
        if (!hasId) {
            shardKeyFilter.put("_id", 0);
        }

        dia = (FormDialog) getBoundUnit(Item.regenConfirm);
        if (!dia.show()) {
            return;
        }

        // now fetch all records from each shard
        final AtomicInteger todo = new AtomicInteger(mongoToShard.size());
        for (Map.Entry<Mongo, String> entry : mongoToShard.entrySet()) {
            final Mongo mongo = entry.getKey();
            final String shard = entry.getValue();
            new DbJob() {

                @Override
                public Object doRun() throws Exception {
                    BasicDBObject shardObj = (BasicDBObject) shardList.get(shard);
                    long count = mongo.getDB(db).getCollection(col).count();
                    shardObj.put("count", count);
                    DBCursor cur = mongo.getDB(db).getCollection(col).find(new BasicDBObject(), shardKeyFilter);
                    long i = 0;
                    int inserted = 0;
                    long start = System.currentTimeMillis();
                    while (cur.hasNext() && !isCancelled()) {
                        BasicDBObject key = (BasicDBObject) cur.next();
                        setProgress((int) ((++i * 100.0f) / count));
                        try {
                            BasicDBObject entry = new BasicDBObject("_id", key);
                            entry.put("_shard", shard);
                            tmpchunks.insert(entry);
                            ++inserted;
                        } catch (Exception e) {
                            getLogger().log(Level.WARNING, e.getMessage(), e);
                        }
                    }

                    if (isCancelled()) {
                        shardObj.put("cancelled", true);
                    }
                    shardObj.put("inserted", inserted);
                    shardObj.put("scanTime", System.currentTimeMillis() - start);
                    todo.decrementAndGet();
                    return null;
                }

                @Override
                public String getNS() {
                    return tmpchunks.getFullName();
                }

                @Override
                public String getShortName() {
                    return "Scanning " + shard;
                }

                @Override
                public boolean isDeterminate() {
                    return true;
                }
            }.addJob();

        }

        new DbJob() {

            @Override
            public Object doRun() throws Exception {
                // wait for all shards to be done
                long start = System.currentTimeMillis();
                while (todo.get() > 0 && !isCancelled()) {
                    Thread.sleep(2000);
                }

                if (isCancelled()) {
                    result.put("cancelled", true);
                    return result;
                }

                // find highest current timestamp
                DBCursor cur = chunks.find().sort(new BasicDBObject("lastmod", -1)).batchSize(-1);
                BasicDBObject chunk = (BasicDBObject) (cur.hasNext() ? cur.next() : null);
                BSONTimestamp ts = (BSONTimestamp) (chunk != null ? chunk.get("lastmod") : null);

                // now infer chunk ranges
                long count = tmpchunks.count();
                result.put("uniqueKeys", count);
                int numChunks = 0;
                cur = tmpchunks.find().sort(new BasicDBObject("_id", 1));
                BasicDBObject prev = (BasicDBObject) cur.next();
                BasicDBObject next = null;
                // snap prev to minkey
                BasicDBObject theid = (BasicDBObject) prev.get("_id");
                for (String key : shardKey.keySet()) {
                    theid.put(key, new MinKey());
                }
                String currentShard = prev.getString("_shard");

                int i = 1;
                while (cur.hasNext()) {
                    next = (BasicDBObject) cur.next();
                    setProgress((int) ((++i * 100.0f) / count));
                    String newShard = next.getString("_shard");
                    if (newShard.equals(currentShard))
                        continue;

                    // add chunk
                    ts = getNextTimestamp(ts);
                    chunk = getChunk(ns, shardKey, prev, next, ts);
                    chunks.insert(chunk);
                    prev = next;
                    currentShard = prev.getString("_shard");
                    ++numChunks;
                }

                // build max
                next = new BasicDBObject();
                for (String key : shardKey.keySet()) {
                    next.put(key, new MaxKey());
                }
                next = new BasicDBObject("_id", next);
                ts = getNextTimestamp(ts);
                chunk = getChunk(ns, shardKey, prev, next, ts);
                chunks.insert(chunk);
                ++numChunks;
                result.put("numChunks", numChunks);
                result.put("totalTime", System.currentTimeMillis() - start);
                return result;
            }

            @Override
            public String getNS() {
                return chunks.getFullName();
            }

            @Override
            public String getShortName() {
                return "Creating Chunks";
            }

            @Override
            public boolean isDeterminate() {
                return true;
            }
        }.addJob();
    }

    BasicDBObject getChunk(String ns, BasicDBObject shardKey, BasicDBObject min, BasicDBObject max, BSONTimestamp ts) {
        BasicDBObject chunk = new BasicDBObject();
        BasicDBObject themin = (BasicDBObject) min.get("_id");
        BasicDBObject themax = (BasicDBObject) max.get("_id");
        String _id = ns;
        for (String key : shardKey.keySet()) {
            _id += "-" + key + "_";
            Object val = themin.get(key);
            _id += (val != null ? val.toString() : "null");
        }
        chunk.put("_id", _id);
        chunk.put("lastmod", ts);
        chunk.put("ns", ns);
        chunk.put("min", themin);
        chunk.put("max", themax);
        chunk.put("shard", min.getString("_shard"));
        return chunk;
    }

    BSONTimestamp getNextTimestamp(BSONTimestamp ts) {
        if (ts == null) {
            return new BSONTimestamp(1000, 0);
        }
        return new BSONTimestamp(ts.getTime(), ts.getInc() + 1);
    }
}
