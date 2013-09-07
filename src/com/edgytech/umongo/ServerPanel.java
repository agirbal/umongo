/**
 * Copyright (C) 2010 EdgyTech Inc.
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
import com.edgytech.swingfast.Text;
import com.edgytech.swingfast.XmlComponentUnit;
import com.mongodb.BasicDBObject;
import com.mongodb.CommandResult;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.ServerAddress;
import javax.swing.JPanel;
import com.edgytech.umongo.ServerPanel.Item;
import com.mongodb.*;
import java.util.logging.Level;

/**
 *
 * @author antoine
 */
public class ServerPanel extends BasePanel implements EnumListener<Item> {

    enum Item {

        icon,
        host,
        address,
        maxObjectSize,
        journaling,
        replication,
        clientPorts,
        rsConfig,
        rsStatus,
        rsOplogInfo,
        refresh,
        rsStepDown,
        rsFreeze,
        rsFreezeTime,
        rsRemove,
        isMaster,
        serverStatus,
        serverBuildInfo,
        currentOps,
        currentOpsQuery,
        killOp,
        killOpId,
        setParameter,
        setParameterValue,
        setLogLevel,
        setLogLevelValue,
        getLog,
        getLogType
    }

    public ServerPanel() {
        setEnumBinding(Item.values(), this);
    }

    public ServerNode getServerNode() {
        return (ServerNode) getNode();
    }

    @Override
    protected void updateComponentCustom(JPanel comp) {
        try {
            Mongo svrMongo = getServerNode().getServerMongo();
            ServerAddress addr = getServerNode().getServerAddress();
            setStringFieldValue(Item.host, addr.toString());
            setStringFieldValue(Item.address, addr.getSocketAddress().toString());

            CommandResult res = svrMongo.getDB("local").command("isMaster");
            boolean master = res.getBoolean("ismaster");
            String replication = MongoUtils.makeInfoString("master", master,
                    "secondary", res.getBoolean("secondary"),
                    "passive", res.getBoolean("passive"));
            setStringFieldValue(Item.replication, replication);
            ((Text) getBoundUnit(Item.replication)).showIcon = master;

            setStringFieldValue(Item.maxObjectSize, String.valueOf(svrMongo.getMaxBsonObjectSize()));

//            ((CmdField) getBoundUnit(Item.serverStatus)).updateFromCmd(svrMongo);
//
//            DBObject svrStatus = ((DocField) getBoundUnit(Item.serverStatus)).getDoc();
//            boolean dur = svrStatus.containsField("dur");
//            ((Text)getBoundUnit(Item.journaling)).setStringValue(dur ? "On" : "Off");
//            ((Text)getBoundUnit(Item.journaling)).showIcon = dur;
        } catch (Exception e) {
            UMongo.instance.showError(this.getClass().getSimpleName() + " update", e);
        }
    }

    public void actionPerformed(Item enm, XmlComponentUnit unit, Object src) {
    }

    public void rsStepDown(ButtonBase button) {
        final DBObject cmd = new BasicDBObject("replSetStepDown", 1);
        final DB admin = getServerNode().getServerMongo().getDB("admin");

        new DbJob() {

            @Override
            public Object doRun() {
                Object res = null;
                try {
                    res = admin.command(cmd);
                } catch (MongoException.Network e) {
                    res = "Operation was likely successful, but connection error: " + e.toString();
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
                return "RS Step Down";
            }

            @Override
            public Object getRoot(Object result) {
                return cmd.toString();
            }
        }.addJob();
    }

    public void rsFreeze(ButtonBase button) {
        int sec = getIntFieldValue(Item.rsFreezeTime);
        DBObject cmd = new BasicDBObject("replSetFreeze", sec);
        new DbJobCmd(getServerNode().getServerMongo().getDB("admin"), cmd).addJob();
    }

    public void getLog(ButtonBase button) {
        final DB db = getServerNode().getServerMongo().getDB("admin");
        final String type = getStringFieldValue(Item.getLogType);
        final DBObject cmd = new BasicDBObject("getLog", type);
        new DbJob() {

            @Override
            public Object doRun() throws Exception {
                CommandResult res = db.command(cmd);
                res.throwOnError();
                StringBuilder sb = new StringBuilder();
                BasicDBList list = (BasicDBList) res.get("log");
                for (Object str : list){
                    sb.append(str);
                    sb.append("\n");
                }
                return sb.toString();
            }

            @Override
            public String getNS() {
                return db.getName();
            }

            @Override
            public String getShortName() {
                return cmd.keySet().iterator().next();
            }

        }.addJob();
    }

    public void serverStatus(ButtonBase button) {
        new DbJobCmd(getServerNode().getServerMongo().getDB("admin"), "serverStatus").addJob();
    }

    public void serverBuildInfo(ButtonBase button) {
        new DbJobCmd(getServerNode().getServerMongo().getDB("admin"), "buildinfo").addJob();
    }

    public void isMaster(ButtonBase button) {
        new DbJobCmd(getServerNode().getServerMongo().getDB("admin"), "isMaster").addJob();
    }

    public void rsConfig(ButtonBase button) {
        final DBCollection col = getServerNode().getServerMongo().getDB("local").getCollection("system.replset");
        CollectionPanel.doFind(col, null);
    }

    public void rsStatus(ButtonBase button) {
        new DbJobCmd(getServerNode().getServerMongo().getDB("admin"), "replSetGetStatus").addJob();
    }

    public void rsOplogInfo(ButtonBase button) {
        new DocView(null, "Oplog Info", null, "Oplog of " + getServerNode().getServerAddress(), MongoUtils.getReplicaSetInfo(getServerNode().getServerMongo())).addToTabbedDiv();
    }

    public void setParameter(ButtonBase button) {
        BasicDBObject cmd = new BasicDBObject("setParameter", 1);
        DBObject param = ((DocBuilderField) getBoundUnit(Item.setParameterValue)).getDBObject();
        cmd.putAll(param);
        new DbJobCmd(getServerNode().getServerMongo().getDB("admin"), cmd).addJob();
    }

    public void setLogLevel(ButtonBase button) {
        BasicDBObject cmd = new BasicDBObject("setParameter", 1);
        int level = getIntFieldValue(Item.setLogLevelValue);
        cmd.put("logLevel", level);
        new DbJobCmd(getServerNode().getServerMongo().getDB("admin"), cmd).addJob();
    }

    public void currentOps(ButtonBase button) {
        final Mongo mongo = getServerNode().getServerMongo();
        final DBObject query = ((DocBuilderField) getBoundUnit(Item.currentOpsQuery)).getDBObject();
        CollectionPanel.doFind(mongo.getDB("admin").getCollection("$cmd.sys.inprog"), query);
    }

    public void killOp(ButtonBase button) {
        final Mongo mongo = getServerNode().getServerMongo();
        final int opid = getIntFieldValue(Item.killOpId);
        final DBObject query = new BasicDBObject("op", opid);
        CollectionPanel.doFind(mongo.getDB("admin").getCollection("$cmd.sys.killop"), query);
    }
}
