/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.mongo.jmongob;

import com.edgytech.swingfast.EnumListener;
import com.edgytech.swingfast.XmlComponentUnit;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
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
}
