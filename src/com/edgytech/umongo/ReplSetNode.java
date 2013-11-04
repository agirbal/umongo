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

import com.mongodb.*;
import java.util.List;
import java.util.logging.Level;

/**
 *
 * @author antoine
 */
public class ReplSetNode extends BaseTreeNode {

    Mongo mongo;
    String name;
    String shardName;

    public ReplSetNode(String name, Mongo mongo, String shardName) {
        this.mongo = mongo;
        this.name = name != null ? name : "Replica Set";
        this.shardName = shardName;
        try {
            xmlLoad(Resource.getXmlDir(), Resource.File.replSetNode, null);
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, null, ex);
        }
        markStructured();
    }

    public ReplSetNode(String name, List<ServerAddress> addrs, MongoOptions opts, String shardName) {
        this.mongo = new Mongo(addrs, opts);
        this.name = name != null ? name : "Replica Set";
        this.shardName = shardName;
        try {
            xmlLoad(Resource.getXmlDir(), Resource.File.replSetNode, null);
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, null, ex);
        }
    }

    @Override
    protected void populateChildren() {
        // need to make a query to update server list
        try {
            mongo.getDatabaseNames();
        } catch (Exception e) {
            getLogger().log(Level.WARNING, null, e);
        }

        // need to pull servers from configuration to see hidden
//        List<ServerAddress> addrs = mongo.getServerAddressList();
        final DBCollection col = mongo.getDB("local").getCollection("system.replset");
        DBObject config = col.findOne();
        if (config == null) {
            getLogger().log(Level.WARNING, "No replica set configuration found");
            return;
        }
        
        BasicDBList members = (BasicDBList) config.get("members");
        for (int i = 0; i < members.size(); ++i) {
            String host = (String) ((DBObject)members.get(i)).get("host");
            try {
                // this will create new Mongo instance, catch any exception
                addChild(new ServerNode(host, mongo.getMongoOptions(), true, false));
            } catch (Exception e) {
                getLogger().log(Level.WARNING, null, e);
            }
        }
    }

    public Mongo getMongo() {
        return mongo;
    }

    public String getName() {
        return name;
    }

    public String getShardName() {
        return shardName;
    }

    @Override
    protected void updateNode() {
        label = "";
        if (shardName != null)
            label += "Shard: " + shardName + " / ";
        label += "ReplSet: " + getName();
    }

    @Override
    protected void refreshNode() {
    }

    String[] getReplicaNames() {
        List<ServerAddress> addrs = mongo.getServerAddressList();
        String[] names = new String[addrs.size()];
        int i = 0;
        for (ServerAddress addr : addrs)
            names[i++] = addr.toString();
        return names;
    }
    
}
