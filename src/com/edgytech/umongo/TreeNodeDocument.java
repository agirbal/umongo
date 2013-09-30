/**
 *      Copyright (C) 2010 EdgyTech Inc.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.edgytech.umongo;

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
        markStructured();
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
        return (PopUpMenu) UMongo.instance.getGlobalStore().getBoundUnit(GlobalStore.Item.documentMenu);
    }
    
}
