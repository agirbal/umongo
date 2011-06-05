/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.mongo.jmongob;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.CommandResult;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.ServerAddress;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import javax.swing.ImageIcon;
import org.xml.sax.SAXException;

/**
 *
 * @author antoine
 */
public class RouterNode extends BaseTreeNode {

    Mongo mongo;
    ServerAddress addr;

    public RouterNode(ServerAddress addr, Mongo mongo) throws IOException, SAXException {
        this.addr = addr;
        this.mongo = mongo;
        xmlLoad(Resource.getXmlDir(), Resource.File.routerNode, null);
    }

    @Override
    protected void populateChildren() {
        CommandResult res = mongo.getDB("admin").command("listShards");
        BasicDBList shards = (BasicDBList) res.get("shards");
        for (Object obj : shards) {
            try {
                DBObject shard = (DBObject) obj;
                String id = (String) shard.get("_id");
                String host = (String) shard.get("host");
                String repl = null;
                int slash = host.indexOf('/');
                if (slash >= 0) {
                    repl = host.substring(0, slash);
                    host = host.substring(slash + 1);
                }
                int colon = host.indexOf(':');
                ServerAddress addr = null;
                if (colon >= 0) {
                    addr = new ServerAddress(host.substring(0, colon), Integer.parseInt(host.substring(colon + 1)));
                } else {
                    addr = new ServerAddress(host);
                }

                if (repl != null) {
                    ArrayList<ServerAddress> addrs = new ArrayList<ServerAddress>();
                    addrs.add(addr);
                    addChild(new ReplSetNode(repl, addrs));
                } else {
                    addChild(new ServerNode(addr));
                }
            } catch (Exception e) {
                getLogger().log(Level.WARNING, null, e);
            }
        }
    }

    public ServerAddress getAddress() {
        return addr;
    }

    public Mongo getMongo() {
        return mongo;
    }

    @Override
    protected void updateNode(List<ImageIcon> overlays) {
        label = getAddress().toString();
    }

}
