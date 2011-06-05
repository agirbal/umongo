/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mongo.jmongob;

import com.edgytech.swingfast.Button;
import com.edgytech.swingfast.EnumListener;
import com.edgytech.swingfast.Showable;
import com.edgytech.swingfast.XmlComponentUnit;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import java.io.IOException;
import javax.swing.JPanel;
import org.mongo.jmongob.IndexPanel.Item;

/**
 *
 * @author antoine
 */
public class IndexPanel extends BasePanel implements EnumListener<Item> {

    enum Item {

        icon,
        name,
        ns,
        key,
        stats,
        version,
        refresh,
        drop,
        shardCollection
    }

    public IndexPanel() {
        setEnumBinding(Item.values(), this);
    }

    public IndexNode getIndexNode() {
        return (IndexNode) getNode();
    }

    @Override
    protected void updateComponentCustom(JPanel comp) {
        try {
            DBObject index = getIndexNode().getIndex();
            setStringFieldValue(Item.name, (String) index.get("name"));
            setStringFieldValue(Item.ns, (String) index.get("ns"));
            setStringFieldValue(Item.version, ((Integer) index.get("v")).toString());
            ((DocField) getBoundUnit(Item.key)).setDoc((DBObject) index.get("key"));
            ((CmdField) getBoundUnit(Item.stats)).updateFromCmd(getIndexNode().getStatsCollection());
        } catch (Exception e) {
            JMongoBrowser.instance.showError(this.getClass().getSimpleName() + " update", e);
        }
    }

    public void actionPerformed(Item enm, XmlComponentUnit unit, Object src) {
    }

    public void drop() {
        final IndexNode indexNode = getIndexNode();
        new DbJob() {

            @Override
            public Object doRun() throws IOException {
                indexNode.getCollectionNode().getCollection().dropIndex(indexNode.getName());
                return null;
            }

            @Override
            public String getNS() {
                return getIndexNode().getIndexedCollection().getFullName();
            }

            @Override
            public String getShortName() {
                return "Drop Index";
            }

            @Override
            public Object getRoot(Object result) {
                return indexNode.getIndex();
            }

            @Override
            public void wrapUp(Object res) {
                super.wrapUp(res);
                indexNode.removeNode();
            }
        }.addJob();
    }

    public void stats() {
        new DocView(null, "Collection Stats", getIndexNode().getStatsCollection(), "collstats").addToTabbedDiv();
    }

    public void shardCollection() {
        DBCollection col = getIndexNode().getIndexedCollection();
        Mongo m = col.getDB().getMongo();
        DB admin = m.getDB("admin");
        DBObject cmd = new BasicDBObject("shardCollection", getIndexNode().getCollectionNode().getCollection().getFullName());
        cmd.put("key", (DBObject) getIndexNode().getIndex().get("key"));
        new DocView(null, "Shard Collection", admin, cmd).addToTabbedDiv();
    }
}
