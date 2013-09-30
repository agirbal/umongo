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

import com.mongodb.BasicDBObject;
import com.mongodb.CommandResult;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import java.util.List;
import java.util.logging.Level;
import javax.swing.ImageIcon;

/**
 *
 * @author antoine
 */
public class IndexNode extends BaseTreeNode {

    DBCollection indexedCol;
    DBObject index;
    BasicDBObject stats;

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

    public DBObject getKey() {
        return (DBObject) index.get("key");
    }

    public CollectionNode getCollectionNode() {
        return (CollectionNode) getParentNode();
    }

    @Override
    protected void populateChildren() {
    }

    @Override
    protected void updateNode() {
        label = getName();
        if (stats != null)
            label += " (" + stats.getInt("count") + "/" + stats.getInt("size") + ")";
    }

    @Override
    protected void refreshNode() {
        CommandResult res = getStatsCollection().getStats();
        res.throwOnError();
        stats = res;
    }
}
