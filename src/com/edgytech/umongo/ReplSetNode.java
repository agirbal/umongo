/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
            addChild(new ServerNode(addr, mongo.getMongoOptions(), null));
        }
    }

    public Mongo getMongo() {
        return mongo;
    }

    public String getName() {
        return name;
    }

    @Override
    protected void updateNode(List<ImageIcon> overlays) {
        label = getName();
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
