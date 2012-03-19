/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.edgytech.jmongobrowser;

import com.edgytech.swingfast.XmlUnit;
import com.mongodb.BasicDBList;
import com.mongodb.CommandResult;
import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.ServerAddress;
import java.util.List;
import java.util.logging.Level;
import javax.swing.ImageIcon;

/**
 *
 * @author antoine
 */
public class MongoNode extends BaseTreeNode {

    Mongo mongo;
    List<String> dbs;

    public MongoNode(Mongo mongo, List<String> dbs) {
        this.mongo = mongo;
        this.dbs = dbs;
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
        }

        List<ServerAddress> addrs = mongo.getServerAddressList();
        if (addrs.size() <= 1) {
            // check if mongos
            boolean added = false;
            ServerAddress addr = addrs.get(0);
            ServerNode node = new ServerNode(mongo);
            try {
                CommandResult res = node.getServerDB().command("isdbgrid");
                if (res.ok()) {
                    addChild(new RouterNode(addr, mongo));
                    added = true;
                }
            } catch (Exception e) {}
            if (!added)
                addChild(node);
        } else {
            addChild(new ReplSetNode(mongo.getReplicaSetStatus().getName(), mongo));
        }

        if (dbs != null) {
            // user specified list of DB
            dbnames = dbs;
        }

        if (dbnames != null) {
            // get all DBs to populate map
            for (String dbname : dbnames) {
                mongo.getDB(dbname);
            }
        }

        // get local and remote dbs
        for (DB db : mongo.getUsedDatabases()) {
            addChild(new DbNode(db));
        }
    }

    @Override
    protected void updateNode(List<ImageIcon> overlays) {
        label = mongo.toString();
        List list = mongo.getDatabaseNames();
        label += " (" + list.size() + ")";
    }

    BasicDBList getShards() {
        XmlUnit child = getChild(0);
        if (child instanceof RouterNode) {
            return ((RouterNode)child).getShards();
        }
        return null;
    }
}
