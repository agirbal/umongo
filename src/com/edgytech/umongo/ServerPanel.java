/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.edgytech.umongo;

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
        setLogLevelValue
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
            ((Text)getBoundUnit(Item.replication)).showIcon = master;

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

    public void rsStepDown() {        
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

    public void rsFreeze() {
        int sec = getIntFieldValue(Item.rsFreezeTime);
        DBObject cmd = new BasicDBObject("replSetFreeze", sec);
        new DocView(null, "RS Freeze", getServerNode().getServerMongo().getDB("admin"), cmd).addToTabbedDiv();
    }
    
    public void serverStatus() {
        new DocView(null, "Server Status", getServerNode().getServerMongo().getDB("admin"), "serverStatus").addToTabbedDiv();
    }

    public void serverBuildInfo() {
        new DocView(null, "Server Build Info", getServerNode().getServerMongo().getDB("admin"), "buildinfo").addToTabbedDiv();
    }

    public void isMaster() {
        new DocView(null, "Is Master", getServerNode().getServerMongo().getDB("admin"), "isMaster").addToTabbedDiv();
    }

    public void rsConfig() {
        final DBCollection col = getServerNode().getServerMongo().getDB("local").getCollection("system.replset");
        CollectionPanel.doFind(col, null);
    }

    public void rsStatus() {
        new DocView(null, "RS Status", getServerNode().getServerMongo().getDB("admin"), "replSetGetStatus").addToTabbedDiv();
    }

    public void rsOplogInfo() {
        new DocView(null, "Oplog Info", MongoUtils.getReplicaSetInfo(getServerNode().getServerMongo()), "Oplog of " + getServerNode().getServerAddress(), null).addToTabbedDiv();
    }

    public void setParameter() {
        BasicDBObject cmd = new BasicDBObject("setParameter", 1);
        DBObject param = ((DocBuilderField)getBoundUnit(Item.setParameterValue)).getDBObject();
        cmd.putAll(param);
        new DocView(null, "Set Param", getServerNode().getServerMongo().getDB("admin"), cmd).addToTabbedDiv();
    }

    public void setLogLevel() {
        BasicDBObject cmd = new BasicDBObject("setParameter", 1);
        int level = getIntFieldValue(Item.setLogLevelValue);
        cmd.put("logLevel", level);
        new DocView(null, "Log Level", getServerNode().getServerMongo().getDB("admin"), cmd).addToTabbedDiv();
    }

    public void currentOps() {
        final Mongo mongo = getServerNode().getServerMongo();
        final DBObject query = ((DocBuilderField) getBoundUnit(Item.currentOpsQuery)).getDBObject();
        CollectionPanel.doFind(mongo.getDB("admin").getCollection("$cmd.sys.inprog"), query);
    }

    public void killOp() {
        final Mongo mongo = getServerNode().getServerMongo();
        final int opid = getIntFieldValue(Item.killOpId);
        final DBObject query = new BasicDBObject("op", opid);
        CollectionPanel.doFind(mongo.getDB("admin").getCollection("$cmd.sys.killop"), query);
    }

}
