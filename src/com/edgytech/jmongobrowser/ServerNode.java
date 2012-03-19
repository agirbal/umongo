/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.edgytech.jmongobrowser;

import com.edgytech.swingfast.SwingFast;
import com.mongodb.BasicDBObject;
import com.mongodb.Bytes;
import com.mongodb.CommandResult;
import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoOptions;
import com.mongodb.ServerAddress;
import java.util.List;
import java.util.logging.Level;
import javax.swing.ImageIcon;

/**
 *
 * @author antoine
 */
public class ServerNode extends BaseTreeNode {

    ServerAddress serverAddress;
    Mongo serverMongo;

    public ServerNode(Mongo mongo) {
        serverMongo = mongo;
        serverAddress = mongo.getAddress();
        try {
            xmlLoad(Resource.getXmlDir(), Resource.File.serverNode, null);
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, null, ex);
        }
        markStructured();
    }
    
    public ServerNode(ServerAddress serverAddress, MongoOptions opts) {
        this.serverAddress = serverAddress;
        serverMongo = new Mongo(serverAddress, opts);
        serverMongo.addOption( Bytes.QUERYOPTION_SLAVEOK );
        
        try {
            xmlLoad(Resource.getXmlDir(), Resource.File.serverNode, null);
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, null, ex);
        }
        markStructured();
    }

    public ServerAddress getServerAddress() {
        return serverAddress;
    }

    public Mongo getServerMongo() {
        return serverMongo;
    }

    public DB getServerDB() {
        return serverMongo.getDB("admin");
    }

    @Override
    protected void populateChildren() {
    }

    @Override
    protected void updateNode(List<ImageIcon> overlays) {
        label = serverAddress.toString();
        CommandResult res = getServerMongo().getDB("local").command("serverStatus");
        if (res.containsField("repl")) {
            if (((BasicDBObject) res.get("repl")).getBoolean("ismaster", false)) {
                overlays.add(SwingFast.createIcon("overlay/tick_circle.png", iconGroup));
            }
        }
        if (res.containsField("dur")) {
            overlays.add(SwingFast.createIcon("overlay/shield_blue.png", iconGroup));
        }
    }
}
