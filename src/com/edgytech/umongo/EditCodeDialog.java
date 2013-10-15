/**
 *      Copyright (C) 2010 EdgyTech LLC.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.edgytech.umongo;

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
