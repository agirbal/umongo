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

import com.mongodb.BasicDBObject;
import com.mongodb.Bytes;
import com.mongodb.CommandResult;
import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.ServerAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;

/**
 *
 * @author antoine
 */
public class ServerNode extends BaseTreeNode {

    String host;
    ServerAddress serverAddress;
    MongoClient serverMongo;
    BasicDBObject stats;
    boolean isReplica = false;
    boolean isConfig = false;

    public ServerNode(MongoClient mongo, boolean isReplica, boolean isConfig) {
        serverMongo = mongo;
        serverAddress = mongo.getAddress();
        setLabel(serverAddress != null ? serverAddress.toString() : "?");
        this.isReplica = isReplica;
        this.isConfig = isConfig;

        try {
            xmlLoad(Resource.getXmlDir(), Resource.File.serverNode, null);
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, null, ex);
        }

        markStructured();
    }
    
    public ServerNode(ServerAddress serverAddress, MongoClientOptions opts, boolean isReplica, boolean isConfig) {
        setLabel(serverAddress.toString());
        this.serverAddress = serverAddress;
        serverMongo = new MongoClient(serverAddress, opts);
        serverMongo.addOption( Bytes.QUERYOPTION_SLAVEOK );
        this.isReplica = isReplica;
        this.isConfig = isConfig;
        
        try {
            xmlLoad(Resource.getXmlDir(), Resource.File.serverNode, null);
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, null, ex);
        }

        markStructured();
    }
    
    public ServerNode(String host, MongoClientOptions opts, boolean isReplica, boolean isConfig) throws UnknownHostException {
        setLabel(host);
        this.host = host;
        this.serverAddress = new ServerAddress(host);
        serverMongo = new MongoClient(serverAddress, opts);
        serverMongo.addOption( Bytes.QUERYOPTION_SLAVEOK );
        this.isReplica = isReplica;
        this.isConfig = isConfig;
        
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

    public MongoClient getServerMongoClient() {
        return serverMongo;
    }

    public DB getServerDB() {
        return serverMongo.getDB("admin");
    }

    @Override
    protected void populateChildren() {
    }

    @Override
    protected void updateNode() {
        boolean isArbiter = false;
        if (stats != null) {
            isArbiter = stats.getBoolean("arbiterOnly", false);
            if (isArbiter) {
                icon = "a.png";
            } else if (stats.getBoolean("ismaster", false)) {
                if (!isConfig)
                    addOverlay("overlay/tick_circle_tiny.png");
            } else if (!stats.getBoolean("secondary")) {
                addOverlay("overlay/error.png");
            }
            
            if (stats.getBoolean("hidden", false)) {
                addOverlay("overlay/hidden.png");
            }

    //        if (res.containsField("dur")) {
    //            overlays.add(SwingFast.createIcon("overlay/shield_blue_tiny.png", iconGroup));
    //        }        
        }
        
        if (isConfig)
            label = "ConfigDB";
        else if (isArbiter)
            label = "Arbiter";
        else
            label = "MongoD";

        label += ": ";
        if (host != null)
            label += host;
        else if (serverAddress != null)
            label += serverAddress.toString();
        else
            label += "?";
    }

    @Override
    protected void refreshNode() {
        CommandResult res = getServerMongoClient().getDB("local").command("isMaster");
        res.throwOnError();
        stats = res;
    }
    
    boolean isReplica() {
        return isReplica;
    }
    
    boolean isConfig() {
        return isConfig;
    }
}
