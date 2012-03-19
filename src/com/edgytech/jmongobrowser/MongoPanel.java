/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.edgytech.jmongobrowser;

import com.edgytech.swingfast.EnumListener;
import com.edgytech.swingfast.XmlComponentUnit;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.ServerAddress;
import java.io.IOException;
import java.util.List;
import javax.swing.JPanel;
import com.edgytech.jmongobrowser.MongoPanel.Item;

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
        od.update(mongo.getOptions(), mongo.getWriteConcern(), mongo.getReadPreference());
        if (!od.show()) {
            return;
        }
        mongo.setOptions(od.getQueryOptions());
        mongo.setWriteConcern(od.getWriteConcern());
        mongo.setReadPreference(od.getReadPreference());
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
        CollectionPanel.doFind(mongo.getDB("admin").getCollection("$cmd.sys.inprog"), query);
    }

    public void killOp() {
        final Mongo mongo = getMongoNode().getMongo();
        final int opid = getIntFieldValue(Item.killOpId);
        final DBObject query = new BasicDBObject("op", opid);
        CollectionPanel.doFind(mongo.getDB("admin").getCollection("$cmd.sys.killop"), query);
    }

    public void isMaster() {
        new DocView(null, "Is Master", getMongoNode().getMongo().getDB("admin"), "isMaster").addToTabbedDiv();
    }

}
