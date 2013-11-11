/**
 * Copyright (C) 2010 EdgyTech Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.edgytech.umongo;

import com.edgytech.swingfast.FormDialog;
import com.edgytech.swingfast.TextField;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 *
 * @author antoine
 */
public class ReplicaDialog extends FormDialog {
    enum Item {
        host,
        arbiterOnly,
        hidden,
        priority,
        votes,
        tags,
        slaveDelay,
        ignoreIndexes
    }

    public ReplicaDialog() {
        setEnumBinding(Item.values(), null);
    }
    
    public void updateFromReplicaConfig(BasicDBObject member) {
        // reset
        xmlLoadCheckpoint();
        ((TextField)getBoundUnit(Item.host)).editable = false;
        
        setStringFieldValue(Item.host, member.getString("host"));
        setBooleanFieldValue(Item.arbiterOnly, member.getBoolean("arbiterOnly", false));
        setBooleanFieldValue(Item.hidden, member.getBoolean("hidden", false));
        setBooleanFieldValue(Item.ignoreIndexes, !member.getBoolean("buildIndexes", true));

        if (member.containsField("priority")) {
            setDoubleFieldValue(Item.priority, member.getDouble("priority"));
        }
        if (member.containsField("slaveDelay")) {
            setIntFieldValue(Item.slaveDelay, member.getInt("slaveDelay"));
        }
        if (member.containsField("votes")) {
            setIntFieldValue(Item.votes, member.getInt("votes"));
        }
        if (member.containsField("tags")) {
            ((DocBuilderField)getBoundUnit(Item.tags)).setDBObject((DBObject) member.get("tags"));
        }
    }

    public BasicDBObject getReplicaConfig(int id) {
        BasicDBObject member = new BasicDBObject("_id", id);
        member.put("host", getStringFieldValue(Item.host));
        if (getBooleanFieldValue(Item.arbiterOnly)) member.put("arbiterOnly", true);
        if (getBooleanFieldValue(Item.hidden)) member.put("hidden", true);
        if (getBooleanFieldValue(Item.ignoreIndexes)) member.put("buildIndexes", false);

        double priority = getDoubleFieldValue(Item.priority);
        if (priority != 1.0) member.put("priority", priority);
        int slaveDelay = getIntFieldValue(Item.slaveDelay);
        if (slaveDelay > 0) member.put("slaveDelay", slaveDelay);
        int votes = getIntFieldValue(Item.votes);
        if (votes != 1) member.put("votes", votes);

        DBObject tags = ((DocBuilderField)getBoundUnit(Item.tags)).getDBObject();
        if (tags != null) member.put("tags", tags);
        return member;
    }
}
