/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.edgytech.umongo;

import com.edgytech.swingfast.BoxPanel;
import com.edgytech.swingfast.Div;
import com.edgytech.swingfast.InfoDialog;
import com.edgytech.swingfast.Text;
import com.edgytech.swingfast.XmlUnitField;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import java.awt.Color;
import java.util.Date;
import java.util.Iterator;
import java.util.UUID;
import java.util.regex.Pattern;
import org.bson.types.BSONTimestamp;
import org.bson.types.Binary;
import org.bson.types.Code;
import org.bson.types.CodeWScope;
import org.bson.types.MaxKey;
import org.bson.types.MinKey;
import org.bson.types.ObjectId;

/**
 *
 * @author antoine
 */
public class DocFieldObject extends DocFieldText {

    public DocFieldObject(String id, String key, Object value, DocFieldObject parent) {
        super(id, key, value, parent);
        getJComponentBoundUnit(Item.edit).visible = false;
        getJComponentBoundUnit(Item.addField).visible = true;
        ((Div)getJComponentBoundUnit(Item.fields)).borderSize = 1;
        if (parent == null) {
            field = false;
            getJComponentBoundUnit(Item.up).visible = false;
            getJComponentBoundUnit(Item.down).visible = false;
            getJComponentBoundUnit(Item.remove).visible = false;
        }
    }

    @Override
    protected void structureComponentCustom(BoxPanel old) {
        Div fields = (Div) getBoundUnit(Item.fields);
        fields.removeAllChildren();
        DBObject doc = (DBObject) value;
        if (doc == null || doc.keySet().isEmpty()) {
            fields.addChild(new Text(null, "Empty"));
        } else {
            for (String key : doc.keySet()) {
                Object val = doc.get(key);
                if (val instanceof BasicDBObject) {
                    fields.addChild(new DocFieldObject(key, key, val, this));
                } else if (val instanceof BasicDBList) {
                    fields.addChild(new DocFieldArray(key, key, val, this));
                } else {
                    fields.addChild(new DocFieldText(key, key, val, this));
                }
            }
        }
        fields.structureComponent();
        super.structureComponentCustom(old);
    }

    @Override
    protected void commitComponentCustom(BoxPanel comp) {
        Div fields = (Div) getBoundUnit(Item.fields);
        DBObject doc = createDBObject();
        if (fields.hasChildren()) {
            for (Object child : fields.getChildren()) {
                if (child instanceof DocFieldText) {
                    DocFieldText text = (DocFieldText) child;
                    doc.put(text.getKey(), text.getValue());
                }
            }
        }
        value = doc;
    }

    void remove(String key) {
        DBObject doc = (DBObject) value;
        doc.removeField(key);
        structureComponent();
    }

    void moveUp(String key) {
        DBObject doc = (DBObject) value;
        value = createDBObject();
        Iterator<String> it = doc.keySet().iterator();
        String prev = it.next();
        while (it.hasNext()) {
            String cur = it.next();
            if (cur.equals(key)) {
                addField(cur, doc.get(cur));
                cur = prev;
            } else {
                addField(prev, doc.get(prev));
            }
            prev = cur;
        }
        addField(prev, doc.get(prev));
        structureComponent();
    }

    void moveDown(String key) {
        DBObject doc = (DBObject) value;
        value = createDBObject();
        Iterator<String> it = doc.keySet().iterator();
        while (it.hasNext()) {
            String cur = it.next();
            if (cur.equals(key) && it.hasNext()) {
                String next = it.next();
                addField(next, doc.get(next));
            }
            addField(cur, doc.get(cur));
        }
        structureComponent();
    }

    public void addNewField(String key, String type) {
        Object val = "";
        if (type.equals("Integer")) {
            val = new Integer(0);
        } else if (type.startsWith("Long")) {
            val = new Long(0);
        } else if (type.equals("Binary")) {
            val = new Binary((byte) 0, new byte[1]);
        } else if (type.startsWith("ObjectId")) {
            val = new ObjectId();
        } else if (type.equals("Boolean")) {
            val = new Boolean(true);
        } else if (type.equals("Code")) {
            val = new Code("");
        } else if (type.equals("Date")) {
            val = new Date();
        } else if (type.startsWith("Double")) {
            val = new Double(0.0);
        } else if (type.equals("Pattern")) {
            val = Pattern.compile("");
        } else if (type.equals("Timestamp")) {
            val = new BSONTimestamp((int) (System.currentTimeMillis() / 1000), 0);
        } else if (type.equals("Object")) {
            val = new BasicDBObject();
        } else if (type.equals("Array")) {
            val = new BasicDBList();
        } else if (type.equals("Null")) {
            val = null;
        } else if (type.equals("UUID")) {
            val = UUID.randomUUID();
        } else if (type.equals("MinKey")) {
            val = new MinKey();
        } else if (type.equals("MaxKey")) {
            val = new MaxKey();
        }

        if (value == null) {
            value = createDBObject();
        }
        addField(key, val);
        structureComponent();
    }

    public void addField() {
        String key = getStringFieldValue(Item.addKey);
        String type = getStringFieldValue(Item.addType);
        DBObject doc = (DBObject) value;
        if (key.isEmpty() || (doc != null && doc.containsField(key))) {
            new InfoDialog(null, "Invalid Key", null, "Please provide a unique key for this field").show();
            return;
        }
        addNewField(key, type);
    }

    protected void addField(String key, Object val) {
        DBObject doc = (DBObject) value;
        doc.put(key, val);
    }

    protected DBObject createDBObject() {
        return new BasicDBObject();
    }


}
