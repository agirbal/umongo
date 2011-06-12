/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mongo.jmongob;

import com.edgytech.swingfast.EnumListener;
import com.edgytech.swingfast.XmlComponentUnit;
import javax.swing.JPanel;
import org.mongo.jmongob.ReplSetPanel.Item;

/**
 *
 * @author antoine
 */
public class ReplSetPanel extends BasePanel implements EnumListener<Item> {

    enum Item {
        refresh,
        name,
        replicaSetStatus,
        oplogInfo,
        maxObjectSize
    }

    public ReplSetPanel() {
        setEnumBinding(Item.values(), this);
    }

    public ReplSetNode getReplSetNode() {
        return (ReplSetNode) node;
    }

    @Override
    protected void updateComponentCustom(JPanel comp) {
        try {
            setStringFieldValue(Item.name, getReplSetNode().getName());
            setStringFieldValue(Item.maxObjectSize, String.valueOf(getReplSetNode().getMongo().getReplicaSetStatus().getMaxBsonObjectSize()));
        } catch (Exception e) {
            JMongoBrowser.instance.showError(this.getClass().getSimpleName() + " update", e);
        }
    }

    public void actionPerformed(Item enm, XmlComponentUnit unit, Object src) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void replicaSetStatus() {
        new DocView(null, "RS Status", getReplSetNode().getMongo().getDB("admin"), "replSetGetStatus").addToTabbedDiv();
    }
    
    public void oplogInfo() {
        new DocView(null, "Oplog Info", MongoUtils.getReplicaSetInfo(getReplSetNode().getMongo()),  "Oplog of " + getReplSetNode().getMongo().toString(), null).addToTabbedDiv();
    }
}
