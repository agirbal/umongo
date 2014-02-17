/**
 *      Copyright (C) 2010 EdgyTech LLC.
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

import com.edgytech.swingfast.SwingFast;
import com.mongodb.BasicDBObject;
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
    BasicDBObject stats;

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
    protected void updateNode() {
        label = collection.getName();
        if (stats != null) {
            label += " (" + stats.getLong("count") + "/" + stats.getLong("size") + ")";
            if (stats.getBoolean("sharded"))
                addOverlay("overlay/star_grey.png");
        }
    }

    @Override
    protected void refreshNode() {
        CommandResult res = collection.getStats();
        res.throwOnError();
        stats = res;
    }
    
    public BasicDBObject getStats() {
        BasicDBObject cmd = new BasicDBObject("collStats", getCollection().getName());
        return getCollection().getDB().command(cmd);
    }
    
    DBObject summarizeData() {
        BasicDBObject sum = new BasicDBObject("ns", getCollection().getFullName());
        sum.put("sampleDoc", getCollection().findOne());
        sum.put("stats", getStats());
        return sum;
    }
}
