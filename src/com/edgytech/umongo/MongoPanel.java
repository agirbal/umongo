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

import com.edgytech.swingfast.ButtonBase;
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
import com.edgytech.umongo.MongoPanel.Item;
import com.mongodb.MongoClient;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

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
        readPreference,
        serverStatus,
        readWriteOptions,
        refresh,
        close,
        createDB,
        createDbName,
        authenticate,
        authUser,
        authPassword,
        maxObjectSize,
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
        summarizeData
    }

    public MongoPanel() {
        setEnumBinding(Item.values(), this);
    }

    public MongoNode getMongoNode() {
        return (MongoNode) node;
    }

    private String getServerAddressString(ServerAddress addr) {
        String ip = "unknown";
        try {
            ip = addr.getSocketAddress().getAddress().toString();
        } catch (UnknownHostException ex) {
            Logger.getLogger(MongoPanel.class.getName()).log(Level.WARNING, null, ex);
        }
        return addr.getHost() + ":" + addr.getPort() + " (" + ip + ")";
    }

    @Override
    protected void updateComponentCustom(JPanel old) {
        try {
            MongoClient mongo = getMongoNode().getMongoClient();
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
//                String ports = MongoUtils.makeInfoString("used", mongo.getConnector().getDBPortPool(addr).inUse(),
//                        "available", mongo.getConnector().getDBPortPool(addr).available(),
//                        "created", mongo.getConnector().getDBPortPool(addr).everCreated());
                String txt = getServerAddressString(addr);
                html += txt + "<br/>";
            }
            setStringFieldValue(Item.activeServers, html);

            setStringFieldValue(Item.queryOptions, MongoUtils.queryOptionsToString(mongo.getOptions()));
            ((DocField) getBoundUnit(Item.writeConcern)).setDoc(mongo.getWriteConcern().getCommand());
            ((DocField) getBoundUnit(Item.readPreference)).setDoc(mongo.getReadPreference().toDBObject());
            setStringFieldValue(Item.maxObjectSize, String.valueOf(mongo.getMaxBsonObjectSize()));
        } catch (Exception e) {
            UMongo.instance.showError(this.getClass().getSimpleName() + " update", e);
        }
    }

    public void actionPerformed(Item enm, XmlComponentUnit unit, Object src) {
    }

    public void close(ButtonBase button) {
        UMongo.instance.disconnect(getMongoNode());
    }

    public void readWriteOptions(ButtonBase button) {
        MongoClient mongo = getMongoNode().getMongoClient();
        OptionDialog od = UMongo.instance.getGlobalStore().getOptionDialog();
        od.update(mongo.getOptions(), mongo.getWriteConcern(), mongo.getReadPreference());
        if (!od.show()) {
            return;
        }
        mongo.setOptions(od.getQueryOptions());
        mongo.setWriteConcern(od.getWriteConcern());
        mongo.setReadPreference(od.getReadPreference());
        refresh();
    }

    public void createDB(ButtonBase button) {
        final String name = getStringFieldValue(Item.createDbName);
        final MongoClient mongo = getMongoNode().getMongoClient();
        // need to do a command to actually create on server
        final DB db = mongo.getDB(name);

        new DbJob() {
            @Override
            public Object doRun() throws IOException {
                db.getStats();
                return null;
            }

            @Override
            public String getNS() {
                return db.getName();
            }

            @Override
            public String getShortName() {
                return "Create DB";
            }

            @Override
            public void wrapUp(Object res) {
                super.wrapUp(res);
                getMongoNode().structureComponent();
            }
        }.addJob();
    }

    public void authenticate(final ButtonBase button) {
        final MongoClient mongo = getMongoNode().getMongoClient();
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

            @Override
            public ButtonBase getButton() {
                return button;
            }
        }.addJob();

    }

    public void serverStatus(ButtonBase button) {
        new DbJobCmd(getMongoNode().getMongoClient().getDB("admin"), "serverStatus").addJob();
    }

    public void showLog(ButtonBase button) {
        new DbJobCmd(getMongoNode().getMongoClient().getDB("admin"), new BasicDBObject("getLog", "global")).addJob();
    }

    public void cloneDB(ButtonBase button) {
        final MongoClient mongo = getMongoNode().getMongoClient();
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

        new DbJobCmd(mongo.getDB("admin"), cmd, this, null).addJob();
    }

    public void currentOps(ButtonBase button) {
        final MongoClient mongo = getMongoNode().getMongoClient();
        final DBObject query = ((DocBuilderField) getBoundUnit(Item.currentOpsQuery)).getDBObject();
        CollectionPanel.doFind(mongo.getDB("admin").getCollection("$cmd.sys.inprog"), query);
    }

    public void killOp(ButtonBase button) {
        final MongoClient mongo = getMongoNode().getMongoClient();
        final int opid = getIntFieldValue(Item.killOpId);
        final DBObject query = new BasicDBObject("op", opid);
        CollectionPanel.doFind(mongo.getDB("admin").getCollection("$cmd.sys.killop"), query);
    }

    public void isMaster(ButtonBase button) {
        new DbJobCmd(getMongoNode().getMongoClient().getDB("admin"), "isMaster").addJob();
    }

    public void summarizeData(final ButtonBase button) {
        final MongoNode mnode = getMongoNode();
        
        new DbJob() {
            @Override
            public Object doRun() throws IOException {
                return mnode.summarizeData();
            }

            @Override
            public String getNS() {
                return "Mongo";
            }

            @Override
            public String getShortName() {
                return "Summarize Data";
            }
        }.addJob();

    }

}
