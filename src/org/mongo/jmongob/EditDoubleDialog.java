/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.mongo.jmongob;

/**
 *
 * @author antoine
 */
public class EditDoubleDialog extends EditFieldDialog {
    enum Item {
        value
    }

    public EditDoubleDialog() {
        setEnumBinding(Item.values(), null);
    }

    @Override
    public Object getValue() {
        return getDoubleFieldValue(Item.value);
    }

    @Override
    public void setValue(Object value) {
        double val = 0;
        if (value instanceof Double)
            val = ((Double)value).doubleValue();
        else if (value instanceof Float)
            val = ((Float)value).floatValue();
        setDoubleFieldValue(Item.value, val);
    }
}
