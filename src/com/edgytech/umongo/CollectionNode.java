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
        label += " (" + res.getLong("count") + "/" + res.getLong("size") + ")";
        if (res.getBoolean("sharded"))
            overlays.add(SwingFast.createIcon("overlay/superman.png", iconGroup));
    }

}
