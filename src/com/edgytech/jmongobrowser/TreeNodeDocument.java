/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.edgytech.jmongobrowser;

import com.edgytech.swingfast.PopUpMenu;
import com.edgytech.swingfast.TreeNodeLabel;
import com.mongodb.DBObject;

/**
 *
 * @author antoine
 */
public class TreeNodeDocument extends TreeNodeLabel {
    DBObject dbobject;
    DbJob job;

    public TreeNodeDocument(DBObject dbobject, DbJob job) {
        this.dbobject = dbobject;
        this.job = job;
        MongoUtils.addChildrenToTreeNode(getTreeNode(), dbobject);
        forced = true;
    }

    public DBObject getDBObject() {
        return dbobject;
    }

    @Override
    public String toString() {
        String str = MongoUtils.getObjectString(dbobject);
        if (job != null)
            str += " in " + job.getRunTime() + "ms";
        return str;
    }

    @Override
    public PopUpMenu getPopUpMenu() {
        return (PopUpMenu) JMongoBrowser.instance.getGlobalStore().getBoundUnit(GlobalStore.Item.documentMenu);
    }
    
}
