/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.edgytech.jmongobrowser;

import com.edgytech.swingfast.TreeNodeLabel;
import com.mongodb.DBObject;

/**
 *
 * @author antoine
 */
public class TreeNodeDBObject extends TreeNodeLabel {
    DBObject dbobject;
    DbJob job;

    public TreeNodeDBObject(DBObject dbobject, DbJob job) {
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
        String str = MongoUtils.dbObjectToString(dbobject);
        if (job != null)
            str += " in " + job.getRunTime() + "ms";
        return str;
    }
}
