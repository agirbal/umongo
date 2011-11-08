/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.mongo.jmongob;

import com.edgytech.swingfast.ComboBox;
import com.edgytech.swingfast.FormDialog;
import com.mongodb.Bytes;
import com.mongodb.MongoException;
import com.mongodb.MongoOptions;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;

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
        writeFactor,
        writePolicy,
        writeTimeout,
        fsync,
        jsync,
        rpPreference
    }

    public OptionDialog() {
        setEnumBinding(Item.values(), null);
    }

    void update(int options, WriteConcern wc, ReadPreference rp) {
        setBooleanFieldValue(Item.tailable, (options & Bytes.QUERYOPTION_TAILABLE) != 0);
        setBooleanFieldValue(Item.slaveOk, (options & Bytes.QUERYOPTION_SLAVEOK) != 0);
        setBooleanFieldValue(Item.opLogReplay, (options & Bytes.QUERYOPTION_OPLOGREPLAY) != 0);
        setBooleanFieldValue(Item.noTimeout, (options & Bytes.QUERYOPTION_NOTIMEOUT) != 0);
        setBooleanFieldValue(Item.awaitData, (options & Bytes.QUERYOPTION_AWAITDATA) != 0);
        setBooleanFieldValue(Item.exhaust, (options & Bytes.QUERYOPTION_EXHAUST) != 0);

        Object w = wc.getWObject();
        int wInt = (Integer) (w instanceof Integer ? w : 0);
        String wStr = (String) (w instanceof String ? w : "");
        setIntFieldValue(Item.writeFactor, wInt);
        setStringFieldValue(Item.writePolicy, wStr);
        setIntFieldValue(Item.writeTimeout, wc.getWtimeout());
        setBooleanFieldValue(Item.fsync, wc.fsync());
        
        ComboBox readBox = (ComboBox) getBoundUnit(Item.rpPreference);
        if (rp == null)
            readBox.value = 0;
        else if (rp instanceof ReadPreference.PrimaryReadPreference)
            readBox.value = 1;
        else if (rp instanceof ReadPreference.SecondaryReadPreference)
            readBox.value = 2;
    }

    int getQueryOptions() {
        int options = 0;
        if (getBooleanFieldValue(Item.tailable)) options |= Bytes.QUERYOPTION_TAILABLE;
        if (getBooleanFieldValue(Item.slaveOk)) options |= Bytes.QUERYOPTION_SLAVEOK;
        if (getBooleanFieldValue(Item.opLogReplay)) options |= Bytes.QUERYOPTION_OPLOGREPLAY;
        if (getBooleanFieldValue(Item.noTimeout)) options |= Bytes.QUERYOPTION_NOTIMEOUT;
        if (getBooleanFieldValue(Item.awaitData)) options |= Bytes.QUERYOPTION_AWAITDATA;
        if (getBooleanFieldValue(Item.exhaust)) options |= Bytes.QUERYOPTION_EXHAUST;
        return options;
    }

    WriteConcern getWriteConcern() {
        int w = getIntFieldValue(Item.writeFactor);
        String wPolicy = getStringFieldValue(Item.writePolicy);
        int wtimeout = getIntFieldValue(Item.writeTimeout);
        boolean fsync = getBooleanFieldValue(Item.fsync);
        boolean jsync = getBooleanFieldValue(Item.jsync);
        if (!wPolicy.trim().isEmpty())
            return new WriteConcern(wPolicy, wtimeout, fsync, jsync);
        return new WriteConcern(w, wtimeout, fsync, jsync);
    }
    
    ReadPreference getReadPreference() {
        ComboBox readBox = (ComboBox) getBoundUnit(Item.rpPreference);
        if (readBox.value == 0)
            return null;
        if (readBox.value == 1)
            return ReadPreference.PRIMARY;
        if (readBox.value == 2)
            return ReadPreference.SECONDARY;
        return null;
    }
}
