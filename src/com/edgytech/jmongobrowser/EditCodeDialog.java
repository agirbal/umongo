/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.edgytech.jmongobrowser;

import com.edgytech.swingfast.TextField;
import com.mongodb.BasicDBObject;
import com.mongodb.util.JSON;
import org.bson.BSONObject;
import org.bson.types.Code;
import org.bson.types.CodeWScope;

/**
 *
 * @author antoine
 */
public class EditCodeDialog extends EditFieldDialog {
    enum Item {
        value,
        scope
    }

    public EditCodeDialog() {
        setEnumBinding(Item.values(), null);
    }

    @Override
    public Object getValue() {
        String code = getStringFieldValue(Item.value);
        String scope = getStringFieldValue(Item.scope);
        if (!scope.isEmpty())
            return new CodeWScope(code, (BSONObject) JSON.parse(scope));
        return new Code(code);
    }

    @Override
    public void setValue(Object value) {
        Code code = (Code) value;
        setStringFieldValue(Item.value, code.getCode());
        if (code instanceof CodeWScope) {
            BasicDBObject scope = (BasicDBObject) ((CodeWScope)code).getScope();
            setStringFieldValue(Item.scope, scope.toString());
        }
    }
}
