/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.edgytech.jmongobrowser;

import org.bson.types.Binary;

/**
 *
 * @author antoine
 */
public class EditBinaryDialog extends EditFieldDialog {
    enum Item {
        size
    }

    public EditBinaryDialog() {
        setEnumBinding(Item.values(), null);
    }

    @Override
    public Object getValue() {
        int size = getIntFieldValue(Item.size);
        return new Binary((byte)0, new byte[size]);
    }

    @Override
    public void setValue(Object value) {
        Binary bin = (Binary) value;
        setIntFieldValue(Item.size, bin.length());
    }
}
