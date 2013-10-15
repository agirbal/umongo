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
import javax.swing.ImageIcon;

/**
 *
 * @author antoine
 */
public class ReplSetNode extends BaseTreeNode {

    Mongo mongo;
    String name;

    public ReplSetNode(String name, Mongo mongo) {
        this.mongo = mongo;
        this.name = name != null ? name : "Replica Set";
        try {
            xmlLoad(Resource.getXmlDir(), Resource.File.replSetNode, null);
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, null, ex);
        }
        markStructured();
    }

    public ReplSetNode(String name, List<ServerAddress> addrs, MongoOptions opts) {
        this.mongo = new Mongo(addrs, opts);
        this.name = name != null ? name : "Replica Set";
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
        } catch (Exception e) {}

        List<ServerAddress> addrs = mongo.getServerAddressList();
        for (ServerAddress addr : addrs) {
            addChild(new ServerNode(addr, mongo.getMongoOptions()));
        }
    }

    public Mongo getMongo() {
        return mongo;
    }

    public String getName() {
        return name;
    }

    @Override
    protected void updateNode() {
        label = "ReplSet: " + getName();
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
