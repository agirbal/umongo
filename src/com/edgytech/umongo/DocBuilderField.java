/**
 * Copyright (C) 2010 EdgyTech Inc.
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

import com.edgytech.swingfast.*;
import com.edgytech.umongo.DocBuilderField.Item;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.logging.Level;
import javax.swing.JButton;
import javax.swing.JToggleButton;

/**
 *
 * @author antoine
 */
public class DocBuilderField extends Div implements EnumListener, FocusListener {

    enum Item {

        jsonText,
        edit,
        validate,
        expandText,
        expandTextArea,
        convertFromJS,
        indent
    }
    @Serial
    public String dialogId;
    @Serial
    public boolean nonEmpty;
    DBObject doc;

    /**
     * Creates a new instance of FieldFile
     */
    public DocBuilderField() {
        nonEmpty = false;

        try {
            xmlLoad(Resource.getXmlDir(), Resource.File.docBuilderField, null);
            // need to still load fields from other config
            setState(State.NORMAL);
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, null, ex);
        }
        setEnumBinding(Item.values(), this);
    }

    @Override
    public void actionPerformed(Enum enm, XmlComponentUnit unit, Object src) {
    }
    
    public void edit(ButtonBase button) {
        String txt = getComponentStringFieldValue(Item.jsonText);
        try {
            doc = (DBObject) JSON.parse(txt);
        } catch (Exception ex) {
            // this could be because of binary in field
            getLogger().log(Level.INFO, null, ex);
        }

        DocBuilderDialog dialog = UMongo.instance.getGlobalStore().getDocBuilderDialog();
        dialog.setDBObject(doc);
        if (!dialog.show()) {
            return;
        }

        doc = dialog.getDBObject();
        setComponentStringFieldValue(Item.jsonText, doc.toString());
        notifyListener(getComponent());
    }

    public void expandText(ButtonBase button) {
        String txt = getComponentStringFieldValue(Item.jsonText);
        FormDialog dia = (FormDialog) button.getDialog();
        setStringFieldValue(Item.expandTextArea, txt);
        if (dia.show()) {
            setComponentStringFieldValue(Item.jsonText, getStringFieldValue(Item.expandTextArea));
        }
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
    public void focusGained(FocusEvent e) {
    }

    @Override
    public void focusLost(FocusEvent e) {
        notifyListener(getComponent());
    }

    ////////////////////////////////////////////////////////////////////////
    // Component
    ////////////////////////////////////////////////////////////////////////
    @Override
    protected boolean checkComponentCustom(BoxPanel comp) {
//        String txt = _field.getText().trim();
        String txt = getComponentStringFieldValue(Item.jsonText);
        if (nonEmpty && txt.isEmpty()) {
            setDisplayError("Field cannot be empty");
            return false;
        }

        if (!getComponentBooleanFieldValue(Item.validate)) {
            return true;
        }

        try {
            JSON.parse(txt);
            return true;
        } catch (Exception e) {
            // this could be because of binary in field
            getLogger().log(Level.INFO, null, e);
        }
        setDisplayError("Invalid JSON format: correct or disable validation");

        return false;
    }

    @Override
    protected void commitComponentCustom(BoxPanel comp) {
        // here we want to commit the string value, but doc is already uptodate
        try {
//            value = _field.getText();
            String txt = getComponentStringFieldValue(Item.jsonText);
            doc = (DBObject) JSON.parse(txt);
        } catch (Exception e) {
            // this could be because of binary in field
            // in this case the doc already has the correct inner value
            getLogger().log(Level.INFO, null, e);
        }
    }

    public void setDBObject(DBObject obj) {
        // it's safe to use obj, not a copy, since builder will build its own
        doc = obj;
        String txt = doc != null ? doc.toString() : "";
        setStringFieldValue(Item.jsonText, txt);
    }

    public DBObject getDBObject() {
        return doc;
    }
}
