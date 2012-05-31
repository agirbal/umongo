/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.edgytech.umongo;

/**
 *
 * @author antoine
 */
public class EditLongDialog extends EditFieldDialog {
    enum Item {
        value
    }

    public EditLongDialog() {
        setEnumBinding(Item.values(), null);
    }

    @Override
    public Object getValue() {
        return getLongFieldValue(Item.value);
    }

    @Override
    public void setValue(Object value) {
        long val = 0;
        if (value instanceof Long)
            val = ((Long)value).longValue();
        else if (value instanceof Integer)
            val = ((Integer)value).intValue();
        setLongFieldValue(Item.value, val);
    }
}
