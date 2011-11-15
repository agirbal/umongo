/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.mongo.jmongob;

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
        mongo.getDatabaseNames();
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
    protected void updateNode(List<ImageIcon> overlays) {
        label = getName();
    }
    
}
