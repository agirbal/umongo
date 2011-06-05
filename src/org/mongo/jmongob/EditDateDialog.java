/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.mongo.jmongob;

import com.edgytech.swingfast.DateField;
import java.util.Date;

/**
 *
 * @author antoine
 */
public class EditDateDialog extends EditFieldDialog {
    enum Item {
        value
    }

    public EditDateDialog() {
        setEnumBinding(Item.values(), null);
    }

    @Override
    public Object getValue() {
        return getDateFieldValue(Item.value);
    }

    @Override
    public void setValue(Object value) {
        setDateFieldValue(Item.value, (Date)value);
    }
}
