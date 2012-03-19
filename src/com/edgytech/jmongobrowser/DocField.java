/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.edgytech.jmongobrowser;

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
    int limit;
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

        value = MongoUtils.dbObjectToString(doc);
    }

    public DBObject getDoc() {
        return _doc;
    }

    public void addView() {
        new DocView(null, getLabelToDisplay(), _doc, getLabelToDisplay(), null).addToTabbedDiv();
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
