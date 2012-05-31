/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.edgytech.umongo;

import com.mongodb.CommandResult;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import javax.swing.ImageIcon;
import org.xml.sax.SAXException;

/**
 *
 * @author antoine
 */
public class IndexNode extends BaseTreeNode {

    DBCollection indexedCol;
    DBObject index;

    public IndexNode(DBCollection indexedCol, DBObject index) {
        this.indexedCol = indexedCol;
        this.index = index;
        try {
            xmlLoad(Resource.getXmlDir(), Resource.File.indexNode, null);
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, null, ex);
        }
        markStructured();
    }

    public DBObject getIndex() {
        return index;
    }

    public DBCollection getIndexedCollection() {
        return indexedCol;
    }

    public DBCollection getStatsCollection() {
        return indexedCol.getDB().getCollection(indexedCol.getName() + ".$" + getName());
    }

    public String getName() {
        return (String) index.get("name");
    }

    public CollectionNode getCollectionNode() {
        return (CollectionNode) getParentNode();
    }

    @Override
    protected void populateChildren() {
    }

    @Override
    protected void updateNode(List<ImageIcon> overlays) {
        label = getName();
        CommandResult res = getStatsCollection().getStats();
        res.throwOnError();
        label += " (" + res.getInt("count") + "/" + res.getInt("size") + ")";
    }
}
