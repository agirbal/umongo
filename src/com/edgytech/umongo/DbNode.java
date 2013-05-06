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
import com.mongodb.BasicDBObject;
import com.mongodb.CommandResult;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import java.util.List;
import java.util.logging.Level;
import javax.swing.ImageIcon;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 *
 * @author antoine
 */
public class DbNode extends BaseTreeNode {

    DB db;

    public DbNode(DB db) {
        this.db = db;
        try {
            xmlLoad(Resource.getXmlDir(), Resource.File.dbNode, null);
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, null, ex);
        }
        markStructured();
    }

    public DB getDb() {
        return db;
    }

    public MongoNode getMongoNode() {
        return (MongoNode) ((DefaultMutableTreeNode) getTreeNode().getParent()).getUserObject();
    }

    @Override
    protected void populateChildren() {
        for (String colname : db.getCollectionNames()) {
            DBCollection col = db.getCollection(colname);
            try {
                addChild(new CollectionNode(col));
            } catch (Exception ex) {
                getLogger().log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    protected void updateNode(List<ImageIcon> overlays) {
        label = db.getName();
        // db.getStats is too slow.. just do a quick command
        CommandResult res = db.getStats();
//        CommandResult res = db.command(new BasicDBObject("profile", -1));
        res.throwOnError();
        label += " (" + res.getInt("objects") + "/" + res.getInt("dataSize") + ")";

        if (db.isAuthenticated())
            overlays.add(SwingFast.createIcon("overlay/unlock.png", iconGroup));
    }

}
