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

import com.edgytech.swingfast.ButtonBase;
import com.edgytech.swingfast.EnumListener;
import com.edgytech.swingfast.FormDialog;
import com.edgytech.swingfast.MenuItem;
import com.edgytech.swingfast.XmlComponentUnit;
import com.mongodb.DBObject;
import java.io.IOException;
import javax.swing.JPanel;
import com.edgytech.umongo.IndexPanel.Item;
import com.mongodb.BasicDBObject;

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
        settings,
        expireAfterSeconds
    }

    public IndexPanel() {
        setEnumBinding(Item.values(), this);
    }

    public IndexNode getIndexNode() {
        return (IndexNode) getNode();
    }

    public DBObject getIndexInfo() {
        BasicDBObject match = new BasicDBObject();
        match.put("ns", getIndexNode().getIndexedCollection().getFullName());
        match.put("key", getIndexNode().getKey());
        return getIndexNode().getCollectionNode().getCollection().getDB().getCollection("system.indexes").findOne(match);
    }
    
    @Override
    protected void updateComponentCustom(JPanel comp) {
        try {
            DBObject index = getIndexInfo();
            setStringFieldValue(Item.name, (String) index.get("name"));
            setStringFieldValue(Item.ns, (String) index.get("ns"));
            ((DocField) getBoundUnit(Item.key)).setDoc((DBObject) index.get("key"));
            ((DocField) getBoundUnit(Item.info)).setDoc(index);
        } catch (Exception e) {
            UMongo.instance.showError(this.getClass().getSimpleName() + " update", e);
        }
    }

    public void actionPerformed(Item enm, XmlComponentUnit unit, Object src) {
    }

    public void drop(ButtonBase button) {
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
            public DBObject getRoot(Object result) {
                return indexNode.getIndex();
            }

            @Override
            public void wrapUp(Object res) {
                super.wrapUp(res);
                indexNode.removeNode();
            }
        }.addJob();
    }

    public void stats(ButtonBase button) {
        new DbJobCmd(getIndexNode().getStatsCollection(), "collstats").addJob();
    }

    public void settings(ButtonBase button) {
        FormDialog dialog = (FormDialog) ((MenuItem) getBoundUnit(Item.settings)).getDialog();
        BasicDBObject index = (BasicDBObject) getIndexInfo();
        boolean isTTL = false;
        long ttl = 0;
        if (index.containsField("expireAfterSeconds")) {
            isTTL = true;
            ttl = index.getLong("expireAfterSeconds");
        }
        setLongFieldValue(Item.expireAfterSeconds, ttl);
        if (!dialog.show())
            return;
        
        long newTTL = getLongFieldValue(Item.expireAfterSeconds);
        if (newTTL != ttl) {
            BasicDBObject cmd = new BasicDBObject("collMod", getIndexNode().getCollectionNode().getCollection().getName());
            BasicDBObject param = new BasicDBObject();
            param.put("keyPattern", (DBObject) index.get("key"));
            param.put("expireAfterSeconds", newTTL);
            cmd.put("index", param);
            new DbJobCmd(getIndexNode().getCollectionNode().getCollection().getDB(), cmd).addJob();
        }
    }
}
