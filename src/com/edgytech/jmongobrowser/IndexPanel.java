/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.edgytech.jmongobrowser;

import com.edgytech.swingfast.EnumListener;
import com.edgytech.swingfast.XmlComponentUnit;
import com.mongodb.DBObject;
import java.io.IOException;
import javax.swing.JPanel;
import com.edgytech.jmongobrowser.IndexPanel.Item;

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
        info,
        stats,
        refresh,
        drop,
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
            ((DocField) getBoundUnit(Item.key)).setDoc((DBObject) index.get("key"));
            ((DocField) getBoundUnit(Item.info)).setDoc(index);
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
}
