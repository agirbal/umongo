/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.edgytech.jmongobrowser;

/**
 *
 * @author antoine
 */
public class EditStringDialog extends EditFieldDialog {

    enum Item {
        value
    }

    public EditStringDialog() {
        setEnumBinding(Item.values(), null);
    }

    @Override
    public Object getValue() {
        return getStringFieldValue(Item.value);
    }

    @Override
    public void setValue(Object value) {
        setStringFieldValue(Item.value, (String)value);
    }
}
