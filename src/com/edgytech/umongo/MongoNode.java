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

import com.edgytech.swingfast.XmlUnit;
import com.mongodb.*;
import java.util.List;
import java.util.logging.Level;
import javax.swing.ImageIcon;

/**
 *
 * @author antoine
 */
public class MongoNode extends BaseTreeNode {

    Mongo mongo;
    boolean specifiedDb;
    List<String> dbs;

    public MongoNode(Mongo mongo, List<String> dbs) {
        this.mongo = mongo;
        this.dbs = dbs;
        this.specifiedDb = dbs != null;
        
        try {
            xmlLoad(Resource.getXmlDir(), Resource.File.mongoNode, null);
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, null, ex);
        }
        markStructured();
    }

    public Mongo getMongo() {
        return mongo;
    }

    @Override
    protected void populateChildren() {
        // first ask list of db, will also trigger discovery of nodes
        List<String> dbnames = null;
        try {
            dbnames = mongo.getDatabaseNames();
        } catch (Exception e) {
            getLogger().log(Level.INFO, e.getMessage(), e);
        }

        List<ServerAddress> addrs = mongo.getServerAddressList();
        
        if (addrs.size() <= 1) {
            // check if mongos
            boolean added = false;
            ServerAddress addr = addrs.get(0);
            ServerNode node = new ServerNode(mongo, false, false);
            try {
                CommandResult res = node.getServerDB().command("isdbgrid");
                if (res.ok()) {
                    addChild(new RouterNode(addr, mongo));
                    added = true;
                }
            } catch (Exception e) {
                getLogger().log(Level.INFO, e.getMessage(), e);
            }

            // could be replset of 1, check
            try {
                CommandResult res = node.getServerDB().command(new BasicDBObject("isMaster", 1), mongo.getOptions());
                if (res.containsField("setName")) {
                    addChild(new ReplSetNode(mongo.getReplicaSetStatus().getName(), mongo, false));
                    added = true;
                }
            } catch (Exception e) {
                getLogger().log(Level.INFO, e.getMessage(), e);
            }
            
            if (!added)
                addChild(node);
        } else {
            addChild(new ReplSetNode(mongo.getReplicaSetStatus().getName(), mongo, false));
        }

        if (specifiedDb) {
            // user specified list of DB
            dbnames = dbs;
        } else {
            dbs = dbnames;
            
            if (dbnames.size() == 0) {
                // could not get any dbs, add test at least
                dbnames.add("test");
            }
        }

        if (dbnames != null) {
            // get all DBs to populate map
            for (String dbname : dbnames) {
                addChild(new DbNode(mongo.getDB(dbname)));
            }
        }
    }

    @Override
    protected void updateNode() {
        label = "Mongo: " + mongo.getConnectPoint();
    }

    @Override
    protected void refreshNode() {
        // do dummy command to pick up exception
        mongo.getDatabaseNames();
    }

    BasicDBList getShards() {
        XmlUnit child = getChild(0);
        if (child instanceof RouterNode) {
            return ((RouterNode)child).getShards();
        }
        return null;
    }
    
    String[] getShardNames() {
        XmlUnit child = getChild(0);
        if (child instanceof RouterNode) {
            return ((RouterNode)child).getShardNames();
        }
        return null;        
    }
}
