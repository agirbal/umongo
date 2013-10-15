/**
 * Copyright (C) 2010 EdgyTech LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.edgytech.umongo;

import com.edgytech.swingfast.ButtonBase;
import com.edgytech.swingfast.EnumListener;
import com.edgytech.swingfast.FormDialog;
import com.edgytech.swingfast.XmlComponentUnit;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

/**
 *
 * @author antoine
 */
public class JSONTextDialog extends FormDialog implements EnumListener<JSONTextDialog.Item> {

    enum Item {
        expandTextArea,
        convertFromJS,
        indent,
        help
    }

    public JSONTextDialog() {
        setEnumBinding(Item.values(), this);
    }
    
    public void setText(String text) {
        setStringFieldValue(Item.expandTextArea, text);
    }
    
    public String getText() {
        return getStringFieldValue(Item.expandTextArea);
    }

    public void convertFromJS(ButtonBase button) {
        String txt = getComponentStringFieldValue(Item.expandTextArea);
        txt = txt.replaceAll("ISODate\\(([^\\)]*)\\)", "{ \"\\$date\": $1 }");
        txt = txt.replaceAll("ObjectId\\(([^\\)]*)\\)", "{ \"\\$oid\": $1 }");
        txt = txt.replaceAll("NumberLong\\(([^\\)]*)\\)", "$1");
//        txt = txt.replaceAll("ISODate", "\\$date");
        setComponentStringFieldValue(Item.expandTextArea, txt);
    }
    
    public void indent(ButtonBase button) {
        String txt = getComponentStringFieldValue(Item.expandTextArea);
        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(txt);
        String prettyJsonString = gson.toJson(je);
        setComponentStringFieldValue(Item.expandTextArea, prettyJsonString);        
    }

    @Override
    public void actionPerformed(Item enm, XmlComponentUnit unit, Object src) {
    }

}
