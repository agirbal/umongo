/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.edgytech.umongo;

import java.util.UUID;

/**
 *
 * @author antoine
 */
public class EditUuidDialog extends EditFieldDialog {
    enum Item {
        value,
    }

    public EditUuidDialog() {
        setEnumBinding(Item.values(), null);
    }

    @Override
    public Object getValue() {
        return UUID.fromString(getStringFieldValue(Item.value));
    }

    @Override
    public void setValue(Object value) {
        String str = ((UUID)value).toString();
        setStringFieldValue(Item.value, str);
    }
}
