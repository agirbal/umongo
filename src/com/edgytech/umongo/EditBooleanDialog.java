/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.edgytech.umongo;

/**
 *
 * @author antoine
 */
public class EditBooleanDialog extends EditFieldDialog {

    enum Item {
        value
    }

    public EditBooleanDialog() {
        setEnumBinding(Item.values(), null);
    }

    @Override
    public Object getValue() {
        return getBooleanFieldValue(Item.value);
    }

    @Override
    public void setValue(Object value) {
        setBooleanFieldValue(Item.value, (Boolean)value);
    }
}
