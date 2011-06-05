/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.mongo.jmongob;

import com.edgytech.swingfast.FieldChecker;
import com.edgytech.swingfast.XmlUnitField;
import java.util.logging.Level;
import java.util.regex.Pattern;

/**
 *
 * @author antoine
 */
public class EditPatternDialog extends EditFieldDialog implements FieldChecker {

    enum Item {
        value
    }

    public EditPatternDialog() {
        setEnumBinding(Item.values(), null);
        setFieldChecker(this);
    }

    @Override
    public Object getValue() {
        String str = getStringFieldValue(Item.value);
        return Pattern.compile(str);
    }

    @Override
    public void setValue(Object value) {
        Pattern p = (Pattern) value;
        setStringFieldValue(Item.value, p.toString());
    }

    public boolean formCheckField(Enum enm, XmlUnitField field) {
        if (enm == Item.value) {
            try {
                Pattern.compile(getComponentStringFieldValue(Item.value));
                return true;
            } catch (Exception e) {
                getLogger().log(Level.WARNING, null, e);
            }
            field.setDisplayError("Cannot compile pattern");
            return false;
        }
        return true;
    }

}
