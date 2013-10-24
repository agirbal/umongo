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

import com.edgytech.swingfast.ComboBox;
import com.edgytech.swingfast.FormDialog;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.Bytes;
import com.mongodb.DBObject;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import java.util.List;

/**
 *
 * @author antoine
 */
public class OptionDialog extends FormDialog {
    enum Item {
        tailable,
        slaveOk,
        opLogReplay,
        noTimeout,
        awaitData,
        exhaust,
        partial,
        writeFactor,
        writePolicy,
        writeTimeout,
        jsync,
        rpPreference,
        rpTag
    }
    
    enum ReadPref {
        primary,
        primaryPreferred,
        secondary,
        secondaryPreferred,
        nearest
    }

    public OptionDialog() {
        setEnumBinding(Item.values(), null);
    }

    void update(int options, WriteConcern wc, ReadPreference rp) {
        // reset
        xmlLoadCheckpoint();
        
        setBooleanFieldValue(Item.tailable, (options & Bytes.QUERYOPTION_TAILABLE) != 0);
        setBooleanFieldValue(Item.slaveOk, (options & Bytes.QUERYOPTION_SLAVEOK) != 0);
        setBooleanFieldValue(Item.opLogReplay, (options & Bytes.QUERYOPTION_OPLOGREPLAY) != 0);
        setBooleanFieldValue(Item.noTimeout, (options & Bytes.QUERYOPTION_NOTIMEOUT) != 0);
        setBooleanFieldValue(Item.awaitData, (options & Bytes.QUERYOPTION_AWAITDATA) != 0);
        setBooleanFieldValue(Item.exhaust, (options & Bytes.QUERYOPTION_EXHAUST) != 0);
        setBooleanFieldValue(Item.partial, (options & Bytes.QUERYOPTION_PARTIAL) != 0);

        Object w = wc.getWObject();
        int wInt = (Integer) (w instanceof Integer ? w : 0);
        String wStr = (String) (w instanceof String ? w : "");
        setIntFieldValue(Item.writeFactor, wInt);
        setStringFieldValue(Item.writePolicy, wStr);
        setIntFieldValue(Item.writeTimeout, wc.getWtimeout());
//        setBooleanFieldValue(Item.fsync, wc.fsync());
        
        DBObject rpObj = rp.toDBObject();
        ComboBox readBox = (ComboBox) getBoundUnit(Item.rpPreference);
        ReadPref rpEnm = ReadPref.primary;
        if (rp != null)
            rpEnm = ReadPref.valueOf(rp.getName());
        readBox.value = rpEnm.ordinal();
        if (rpObj.containsField("tags")) {
            List tags = (List) rpObj.get("tags");
            if (tags.size() > 0) {
                ((DocBuilderField) getBoundComponentUnit(Item.rpTag)).setDBObject((DBObject) tags.get(0));
            }
        }
    }

    int getQueryOptions() {
        int options = 0;
        if (getBooleanFieldValue(Item.tailable)) options |= Bytes.QUERYOPTION_TAILABLE;
        if (getBooleanFieldValue(Item.slaveOk)) options |= Bytes.QUERYOPTION_SLAVEOK;
        if (getBooleanFieldValue(Item.opLogReplay)) options |= Bytes.QUERYOPTION_OPLOGREPLAY;
        if (getBooleanFieldValue(Item.noTimeout)) options |= Bytes.QUERYOPTION_NOTIMEOUT;
        if (getBooleanFieldValue(Item.awaitData)) options |= Bytes.QUERYOPTION_AWAITDATA;
        if (getBooleanFieldValue(Item.exhaust)) options |= Bytes.QUERYOPTION_EXHAUST;
        if (getBooleanFieldValue(Item.partial)) options |= Bytes.QUERYOPTION_PARTIAL;
        return options;
    }

    WriteConcern getWriteConcern() {
        int w = getIntFieldValue(Item.writeFactor);
        String wPolicy = getStringFieldValue(Item.writePolicy);
        int wtimeout = getIntFieldValue(Item.writeTimeout);
//        boolean fsync = getBooleanFieldValue(Item.fsync);
        boolean jsync = getBooleanFieldValue(Item.jsync);
        if (!wPolicy.trim().isEmpty())
            return new WriteConcern(wPolicy, wtimeout, false, jsync);
        return new WriteConcern(w, wtimeout, false, jsync);
    }
    
    ReadPreference getReadPreference() {
        ComboBox readBox = (ComboBox) getBoundUnit(Item.rpPreference);
        ReadPref rpEnm = ReadPref.values()[readBox.value];
        BasicDBObject tag = (BasicDBObject) ((DocBuilderField) getBoundComponentUnit(Item.rpTag)).getDBObject();
        if (tag != null)
            return ReadPreference.valueOf(rpEnm.name(), tag);
        return ReadPreference.valueOf(rpEnm.name());
    }
}
