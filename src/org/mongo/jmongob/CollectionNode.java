/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mongo.jmongob;

import com.edgytech.swingfast.SwingFast;
import com.mongodb.CommandResult;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import java.io.IOException;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.tree.DefaultMutableTreeNode;
import org.xml.sax.SAXException;

/**
 *
 * @author antoine
 */
public class CollectionNode extends BaseTreeNode {

    DBCollection collection;

    public CollectionNode(DBCollection collection) throws IOException, SAXException {
        this.collection = collection;
        xmlLoad(Resource.getXmlDir(), Resource.File.collectionNode, null);
        markStructured();
    }

    public DBCollection getCollection() {
        return collection;
    }

    public DbNode getDbNode() {
        return (DbNode) ((DefaultMutableTreeNode) getTreeNode().getParent()).getUserObject();
    }

    @Override
    protected void populateChildren() {
        for (DBObject index : collection.getIndexInfo()) {
            addChild(new IndexNode(collection, index));
        }
    }

    @Override
    protected void updateNode(List<ImageIcon> overlays) {
        label = collection.getName();
        CommandResult res = collection.getStats();
        res.throwOnError();
        label += " (" + res.getInt("count") + "/" + res.getInt("size") + ")";
        if (res.getBoolean("sharded"))
            overlays.add(SwingFast.createIcon("overlay/superman.png", iconGroup));
    }

}
