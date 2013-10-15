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

import com.edgytech.swingfast.Text;
import com.mongodb.DBObject;
import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JLabel;

/**
 *
 * @author antoine
 */
public class DocField extends Text implements MouseListener {

    @Serial
    public int limit;
    DBObject _doc;

    public DocField() {
        field = true;
        limit = 75;
    }

    @Override
    protected boolean checkComponentCustom(JLabel comp) {
        return true;
    }

//    @Override
//    protected BoxPanel createComponent() {
//        BoxPanel panel = new BoxPanel(Common.Axis.X, true);
//        _field = new JLabel("");
//        panel.add(_field);
//
////        _button = new JButton(SwingFast.createImageIcon("/img/zoomIn.png"));
//        _button = new JButton(">");
//        _button.addActionListener(this);
//        panel.add(_button);
//        return panel;
//    }
    @Override
    protected void structureComponentCustom(JLabel comp) {
        comp.addMouseListener(this);
        comp.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        icon = "zoomIn.png";
        iconGroup = "icons";
    }

    public void setDoc(DBObject doc) {
        _doc = doc;
        if (_doc == null) {
            value = null;
            return;
        }

        value = MongoUtils.getObjectString(doc);
    }

    public DBObject getDoc() {
        return _doc;
    }

    public void addView() {
        new DocView(null, getLabelToDisplay(), null, getLabelToDisplay(), _doc).addToTabbedDiv();
    }

    public void mouseClicked(MouseEvent e) {
        addView();
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

}
