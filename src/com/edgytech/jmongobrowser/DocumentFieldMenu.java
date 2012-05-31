/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.edgytech.jmongobrowser;

import com.edgytech.jmongobrowser.DocumentFieldMenu.Item;
import com.edgytech.swingfast.EnumListener;
import com.edgytech.swingfast.InfoDialog;
import com.edgytech.swingfast.PopUpMenu;
import com.edgytech.swingfast.XmlComponentUnit;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

/**
 *
 * @author antoine
 */
public class DocumentFieldMenu extends PopUpMenu implements EnumListener<Item>  {

    enum Item {
        set,
        unset,
        copyField,
        copyValue
    }
    
    public DocumentFieldMenu() {
        setEnumBinding(Item.values(), this);
    }
    
    @Override
    public void actionPerformed(Item enm, XmlComponentUnit unit, Object src) {
    }
    
    public void set() {
        final DocView dv = (DocView) (JMongoBrowser.instance.getTabbedResult().getSelectedUnit());
        TreeNodeDocumentField field = (TreeNodeDocumentField) dv.getSelectedNode().getUserObject();
        DBObject doc = dv.getSelectedDocument();
        String path = dv.getSelectedDocumentPath();
        Object newValue = JMongoBrowser.instance.getGlobalStore().editValue(field.getKey(), field.getValue());

        if (newValue == null) {
            new InfoDialog(null, null, null, "Cannot edit this type of data.").show();
            return;
        }
        if (dv.getDBCursor() == null) {
            // local data
            new InfoDialog(null, null, null, "Cannot do in-place update on local data.").show();
            return;
        }
        
        final DBObject query = doc.containsField("_id") ? new BasicDBObject("_id", doc.get("_id")) : doc;
        DBObject setValue = new BasicDBObject(path, newValue);
        final DBObject update = new BasicDBObject("$set", setValue);
        
        final DBCollection col = dv.getDBCursor().getCollection();
        new DbJob() {

            @Override
            public Object doRun() {
                return col.update(query, update, false, false);
            }

            @Override
            public String getNS() {
                return col.getFullName();
            }

            @Override
            public String getShortName() {
                return "Update";
            }

            @Override
            public Object getRoot(Object result) {
                StringBuilder sb = new StringBuilder();
                sb.append("query=").append(query);
                sb.append(", update=").append(update);
                return sb.toString();
            }

            @Override
            public void wrapUp(Object res) {
                super.wrapUp(res);
                dv.updateCursor();
            }
        }.addJob();
    }
    
    public void unset() {
        final DocView dv = (DocView) (JMongoBrowser.instance.getTabbedResult().getSelectedUnit());
        DBObject doc = dv.getSelectedDocument();
        String path = dv.getSelectedDocumentPath();

        if (dv.getDBCursor() == null) {
            // local data
            new InfoDialog(null, null, null, "Cannot do in-place update on local data.").show();
            return;
        }
        
        final DBObject query = doc.containsField("_id") ? new BasicDBObject("_id", doc.get("_id")) : doc;
        DBObject setValue = new BasicDBObject(path, 1);
        final DBObject update = new BasicDBObject("$unset", setValue);
        
        final DBCollection col = dv.getDBCursor().getCollection();
        new DbJob() {

            @Override
            public Object doRun() {
                return col.update(query, update, false, false);
            }

            @Override
            public String getNS() {
                return col.getFullName();
            }

            @Override
            public String getShortName() {
                return "Update";
            }

            @Override
            public Object getRoot(Object result) {
                StringBuilder sb = new StringBuilder();
                sb.append("query=").append(query);
                sb.append(", update=").append(update);
                return sb.toString();
            }

            @Override
            public void wrapUp(Object res) {
                super.wrapUp(res);
                dv.updateCursor();
            }
        }.addJob();
    }
    
    public void copyField() {
        final DocView dv = (DocView) (JMongoBrowser.instance.getTabbedResult().getSelectedUnit());
        TreeNodeDocumentField node = (TreeNodeDocumentField) dv.getSelectedNode().getUserObject();
        StringSelection data = new StringSelection(node.getKey().toString());
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(data, data);
    }

    public void copyValue() {
        final DocView dv = (DocView) (JMongoBrowser.instance.getTabbedResult().getSelectedUnit());
        TreeNodeDocumentField node = (TreeNodeDocumentField) dv.getSelectedNode().getUserObject();
        StringSelection data = new StringSelection(node.getValue().toString());
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(data, data);
    }

}
