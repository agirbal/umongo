/**
 *      Copyright (C) 2010 EdgyTech Inc.
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

import com.edgytech.swingfast.BoxPanel;
import com.edgytech.swingfast.Common;
import com.edgytech.swingfast.JTextAreaScroll;
import com.edgytech.swingfast.SwingFast;
import com.edgytech.swingfast.XmlUnit;
import com.edgytech.swingfast.XmlUnitField;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.logging.Level;
import javax.swing.JButton;
import javax.swing.JToggleButton;

/**
 *
 * @author antoine
 */
public class DocBuilderField extends XmlUnitField<XmlUnit, BoxPanel> implements ActionListener, FocusListener {

    @Serial
    public String dialogId;
    @Serial
    public int rows;
    @Serial
    public boolean nonEmpty;
    @SerialStar
    public String value;
    
    JTextAreaScroll _field;
    JButton _button;
    JToggleButton _validate;
    DBObject doc;

    /**
     * Creates a new instance of FieldFile
     */
    public DocBuilderField() {
        columns = 20;
        rows = 3;
        nonEmpty = false;
        value = "";
    }

    /**
     * Creates a new instance of FieldFile
     */
    public DocBuilderField(String id, String value) {
        this();
        setId(id);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            doc = (DBObject) JSON.parse(_field.getText());
        } catch (Exception ex) {
            // this could be because of binary in field
            getLogger().log(Level.INFO, null, ex);
        }

        if (e.getSource() == _button) {
            DocBuilderDialog dialog = UMongo.instance.getGlobalStore().getDocBuilderDialog();
            dialog.setDBObject(doc);
            if (!dialog.show()) {
                return;
            }

            doc = dialog.getDBObject();
            _field.setText(doc.toString());
            notifyListener(getComponent());
        }
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
        String txt = _field.getText().trim();
            if (nonEmpty && txt.isEmpty()) {
                setDisplayError("Field cannot be empty");
                return false;
            }

        if (!_validate.isSelected())
            return true;
        
        try {
            JSON.parse(_field.getText());
            return true;
        } catch (Exception e) {
            // this could be because of binary in field
            getLogger().log(Level.INFO, null, e);
        }
        setDisplayError("Invalid JSON format: correct or disable validation");

        return false;
    }

    @Override
    protected BoxPanel createComponent() {
        BoxPanel panel = new BoxPanel(Common.Axis.X, true);
        _field = new JTextAreaScroll(rows, columns);
        _field.getTextArea().setLineWrap(true);
        _field.getTextArea().setWrapStyleWord(false);
        _field.addFocusListener(this);
        panel.add(_field);

        _button = new JButton(SwingFast.createIcon("edit.png", "icons"));
        _button.addActionListener(this);
        panel.add(_button);

        _validate = new JToggleButton(SwingFast.createIcon("overlay/check.png", "icons"), true);
        _validate.addActionListener(this);
        _validate.setToolTipText("Validate the JSON format");
        panel.add(_validate);
        return panel;
    }

    @Override
    protected void structureComponentCustom(BoxPanel comp) {
    }

    @Override
    protected void updateComponentCustom(BoxPanel comp) {
        _field.setText(value);
    }

    @Override
    protected void commitComponentCustom(BoxPanel comp) {
        // here we want to commit the string value, but doc is already uptodate
        try {
            value = _field.getText();
            doc = (DBObject) JSON.parse(value);
        } catch (Exception e) {
            // this could be because of binary in field
            // in this case the doc already has the correct inner value
            getLogger().log(Level.INFO, null, e);
        }
    }

    public void setDBObject(DBObject obj) {
        // it's safe to use obj, not a copy, since builder will build its own
        doc = obj;
        value = doc != null ? doc.toString() : "";
    }

    public DBObject getDBObject() {
        return doc;
    }

}
