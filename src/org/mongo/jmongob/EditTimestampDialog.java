/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.mongo.jmongob;

import org.bson.types.BSONTimestamp;

/**
 *
 * @author antoine
 */
public class EditTimestampDialog extends EditFieldDialog {
    enum Item {
        time,
        increment
    }

    public EditTimestampDialog() {
        setEnumBinding(Item.values(), null);
    }

    @Override
    public Object getValue() {
        return new BSONTimestamp(getIntFieldValue(Item.time), getIntFieldValue(Item.increment));
    }

    @Override
    public void setValue(Object value) {
        BSONTimestamp ts = (BSONTimestamp) value;
        setIntFieldValue(Item.time, ts.getTime());
        setIntFieldValue(Item.increment, ts.getInc());
    }
}
