/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.mongo.jmongob;

import com.edgytech.swingfast.EnumListener;
import com.edgytech.swingfast.XmlComponentUnit;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;
import com.mongodb.ServerAddress;
import java.io.IOException;
import javax.swing.JPanel;
import org.mongo.jmongob.RouterPanel.Item;

/**
 *
 * @author antoine
 */
public class RouterPanel extends BasePanel implements EnumListener<Item> {

    enum Item {
        refresh,
        host,
        address,
        listShards,
        addShard,
        asHost,
        asPort,
        asShardName,
        asReplSetName,
        asMaxSize,
        autoBalance,
    }

    public RouterPanel() {
        setEnumBinding(Item.values(), this);
    }

    RouterNode getRouterNode() {
        return (RouterNode) node;
    }

    @Override
    protected void updateComponentCustom(JPanel comp) {
        try {
            ServerAddress addr = getRouterNode().getAddress();
            setStringFieldValue(Item.host, addr.getHost() + ":" + addr.getPort());
            setStringFieldValue(Item.address, addr.getSocketAddress().toString());
            final Mongo mongo = getRouterNode().getMongo();
            setBooleanFieldValue(Item.autoBalance, MongoUtils.isBalancerOn(mongo));
        } catch (Exception e) {
            JMongoBrowser.instance.showError(this.getClass().getSimpleName() + " update", e);
        }
    }


    public void actionPerformed(Item enm, XmlComponentUnit unit, Object src) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void addShard() {
        final RouterNode router = getRouterNode();
        final String host = getStringFieldValue(Item.asHost);
        final int port = getIntFieldValue(Item.asPort);
        final String shardName = getStringFieldValue(Item.asShardName);
        final String replSetName = getStringFieldValue(Item.asReplSetName);
        final int maxsize = getIntFieldValue(Item.asMaxSize);
        String server = host;
        if (port > 0)
            server += ":" + port;
        if (replSetName != null)
            server = replSetName + "/" + server;

        final BasicDBObject cmd = new BasicDBObject("addshard", server);
        if (shardName != null)
            cmd.put("name", shardName);
        if (maxsize > 0)
            cmd.put("maxSize", maxsize);
        final DB db = router.getMongo().getDB("admin");
        new DocView(null, "Add Shard", db, cmd, null, this, null).addToTabbedDiv();
    }

    public void listShards() {
        new DocView(null, "List Shards", getRouterNode().getMongo().getDB("admin"), "listShards").addToTabbedDiv();
    }

    public void autoBalance() {
        final Mongo mongo = getRouterNode().getMongo();
        final DB config = mongo.getDB("config");
        final DBCollection settings = config.getCollection("settings");

        new DbJob() {

            @Override
            public Object doRun() throws IOException {
                boolean on = MongoUtils.isBalancerOn(mongo);
                BasicDBObject query = new BasicDBObject("_id", "balancer");
                BasicDBObject update = new BasicDBObject("stopped", on);
                update = new BasicDBObject("$set", update);
                return settings.update(query, update, true, false);
            }

            @Override
            public String getNS() {
                return settings.getFullName();
            }

            @Override
            public String getShortName() {
                return "Enable Balancer";
            }

            @Override
            public void wrapUp(Object res) {
                updateComponent();
                super.wrapUp(res);
            }
        }.addJob();
    }

}
