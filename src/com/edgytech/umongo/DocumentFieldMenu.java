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

import com.edgytech.swingfast.*;
import com.edgytech.umongo.DocumentFieldMenu.Item;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;
import org.bson.types.Binary;

/**
 *
 * @author antoine
 */
public class DocumentFieldMenu extends PopUpMenu implements EnumListener<Item>  {

    enum Item {
        set,
        unset,
        compressionStats,
        compressionStatsOriginal,
        compressionStatsDeflate,
        compressionStatsGZip,
        saveBinaryToFile,
        saveBinaryOutputFile,
        decodeBinary,
        decodeBinaryText,
        copyKey,
        copyValue
    }
    
    public DocumentFieldMenu() {
        setEnumBinding(Item.values(), this);
    }
    
    @Override
    public void actionPerformed(Item enm, XmlComponentUnit unit, Object src) {
    }
    
    public void set(ButtonBase button) {
        final DocView dv = (DocView) (UMongo.instance.getTabbedResult().getSelectedUnit());
        TreeNodeDocumentField field = (TreeNodeDocumentField) dv.getSelectedNode().getUserObject();
        DBObject doc = dv.getSelectedDocument();
        String path = dv.getSelectedDocumentPath();
        Object newValue = UMongo.instance.getGlobalStore().editValue(field.getKey(), field.getValue());

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
            public DBObject getRoot(Object result) {
                BasicDBObject obj = new BasicDBObject("query", query);
                obj.put("update", update);
                return obj;
            }

            @Override
            public void wrapUp(Object res) {
                super.wrapUp(res);
                dv.refresh(null);
            }
        }.addJob();
    }
    
    public void unset(ButtonBase button) {
        final DocView dv = (DocView) (UMongo.instance.getTabbedResult().getSelectedUnit());
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
            public DBObject getRoot(Object result) {
                BasicDBObject obj = new BasicDBObject("query", query);
                obj.put("update", update);
                return obj;
            }

            @Override
            public void wrapUp(Object res) {
                super.wrapUp(res);
                dv.refresh(null);
            }
        }.addJob();
    }
    
    public void copyKey(ButtonBase button) {
        final DocView dv = (DocView) (UMongo.instance.getTabbedResult().getSelectedUnit());
        TreeNodeDocumentField node = (TreeNodeDocumentField) dv.getSelectedNode().getUserObject();
        StringSelection data = new StringSelection(node.getKey().toString());
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(data, data);
    }

    public void copyValue(ButtonBase button) {
        final DocView dv = (DocView) (UMongo.instance.getTabbedResult().getSelectedUnit());
        TreeNodeDocumentField node = (TreeNodeDocumentField) dv.getSelectedNode().getUserObject();
        StringSelection data = new StringSelection(MongoUtils.getJSON(node.getValue()));
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(data, data);
    }

    public void compressionStats(ButtonBase button) throws IOException {
        final DocView dv = (DocView) (UMongo.instance.getTabbedResult().getSelectedUnit());
        TreeNodeDocumentField node = (TreeNodeDocumentField) dv.getSelectedNode().getUserObject();
        Object value = node.getValue();
        byte[] bytes = null;
        if (value instanceof byte[]) {
            bytes = (byte[]) value;
        } else if (value instanceof Binary) {
            bytes = ((Binary)value).getData();
        } else {
            bytes = MongoUtils.getJSON(value).toString().getBytes(Charset.forName("UTF-8"));
        }
        
        setStringFieldValue(Item.compressionStatsOriginal, String.valueOf(bytes.length));
        ByteArrayOutputStream baos = new ByteArrayOutputStream(bytes.length);
//        System.out.println("Bytes before: " + bytes.length);
        
        baos.reset();
        DeflaterOutputStream dos = new DeflaterOutputStream(baos);
        dos.write(bytes);
        dos.close();
        byte[] deflate = baos.toByteArray();
        setStringFieldValue(Item.compressionStatsDeflate, String.valueOf(deflate.length));
//        System.out.println("Bytes deflate: " + deflate.length);
        
        baos.reset();
        GZIPOutputStream gos = new GZIPOutputStream(baos);
        gos.write(bytes);
        gos.close();
        byte[] gzip = baos.toByteArray();
        setStringFieldValue(Item.compressionStatsGZip, String.valueOf(gzip.length));
//        System.out.println("Bytes gzip: " + gzip.length);
        
        Showable dia = ((MenuItem)getBoundComponentUnit(Item.compressionStats)).getDialog();
        dia.show();
    }

    public void saveBinaryToFile(ButtonBase button) throws IOException {
        String path = ((FileSelectorField)getBoundComponentUnit(Item.saveBinaryOutputFile)).getPath();

        final DocView dv = (DocView) (UMongo.instance.getTabbedResult().getSelectedUnit());
        TreeNodeDocumentField node = (TreeNodeDocumentField) dv.getSelectedNode().getUserObject();
        Object value = node.getValue();
        byte[] bytes = null;
        if (value instanceof byte[]) {
            bytes = (byte[]) value;
        } else if (value instanceof Binary) {
            bytes = ((Binary)value).getData();
        } else {
            bytes = MongoUtils.getJSON(value).toString().getBytes(Charset.forName("UTF-8"));
        }
        
        FileOutputStream fos = new FileOutputStream(path);
        fos.write(bytes);
        fos.close();
    }

    public void decodeBinary(ButtonBase button) throws IOException {
        final DocView dv = (DocView) (UMongo.instance.getTabbedResult().getSelectedUnit());
        TreeNodeDocumentField node = (TreeNodeDocumentField) dv.getSelectedNode().getUserObject();
        Object value = node.getValue();
        byte[] bytes = null;
        if (value instanceof byte[]) {
            bytes = (byte[]) value;
        } else if (value instanceof Binary) {
            bytes = ((Binary)value).getData();
        } else {
            bytes = MongoUtils.getJSON(value).toString().getBytes(Charset.forName("UTF-8"));
        }
        
        BinaryDecoder dec = UMongo.instance.getBinaryDecoder();
        setStringFieldValue(Item.decodeBinaryText, dec.getText(bytes));
        FormDialog dia = (FormDialog) button.getDialog();
        dia.show();
    }
}
