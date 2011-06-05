/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.mongo.jmongob;

import com.edgytech.swingfast.FormDialog;
import com.mongodb.Bytes;
import com.mongodb.MongoException;
import com.mongodb.MongoOptions;
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
        writeTimeout,
        fsync
    }

    public OptionDialog() {
        setEnumBinding(Item.values(), null);
    }

    void update(int options, WriteConcern wc) {
        setBooleanFieldValue(Item.tailable, (options & Bytes.QUERYOPTION_TAILABLE) != 0);
        setBooleanFieldValue(Item.slaveOk, (options & Bytes.QUERYOPTION_SLAVEOK) != 0);
        setBooleanFieldValue(Item.opLogReplay, (options & Bytes.QUERYOPTION_OPLOGREPLAY) != 0);
        setBooleanFieldValue(Item.noTimeout, (options & Bytes.QUERYOPTION_NOTIMEOUT) != 0);
        setBooleanFieldValue(Item.awaitData, (options & Bytes.QUERYOPTION_AWAITDATA) != 0);
        setBooleanFieldValue(Item.exhaust, (options & Bytes.QUERYOPTION_EXHAUST) != 0);
        
        setIntFieldValue(Item.writeFactor, wc.getW());
        setIntFieldValue(Item.writeTimeout, wc.getWtimeout());
        setBooleanFieldValue(Item.fsync, wc.fsync());
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
        int wtimeout = getIntFieldValue(Item.writeTimeout);
        boolean fsync = getBooleanFieldValue(Item.fsync);
        return new WriteConcern(w, wtimeout, fsync);
    }
}
