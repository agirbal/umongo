/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mongo.jmongob;

import com.edgytech.swingfast.BoxPanel;
import com.edgytech.swingfast.Div;
import com.edgytech.swingfast.EnumListener;
import com.edgytech.swingfast.XmlComponentUnit;
import com.mongodb.DBObject;
import com.mongodb.DBRefBase;
import com.mongodb.util.JSON;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;
import java.util.logging.Level;
import java.util.regex.Pattern;
import org.bson.types.BSONTimestamp;
import org.bson.types.Binary;
import org.bson.types.Code;
import org.bson.types.CodeWScope;
import org.bson.types.ObjectId;
import org.mongo.jmongob.DocFieldText.Item;
import org.xml.sax.SAXException;

/**
 *
 * @author antoine
 */
public class DocFieldText extends Div implements EnumListener<Item> {

    enum Item {
        fields,
        value,
        edit,
        up,
        down,
        remove,
        addField,
        addKey,
        addType
    }
    DocFieldObject _object;
    String key;
    Object value;

    /**
     * Creates a new instance of FieldFile
     */
    public DocFieldText() {
        try {
            xmlLoad(Resource.getXmlDir(), Resource.File.docFieldText, null);
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, null, ex);
        }
        setEnumBinding(Item.values(), this);
    }

    /**
     * Creates a new instance of FieldFile
     */
    public DocFieldText(String id, String key, Object value, DocFieldObject object) {
        this();
        setId(id);
        setLabel(key);
        this.key = key;
        this.value = value;
        this._object = object;
        setStringFieldValue(Item.value, JSON.serialize(value));
        if (value == null)
            getJComponentBoundUnit(Item.edit).visible = false;
    }

    public void edit() {
        Class ceditor = null;
        if (value == null) {
            ceditor = null;
        } else if (value instanceof String) {
            ceditor = EditStringDialog.class;
        } else if (value instanceof Binary) {
            ceditor = EditBinaryDialog.class;
        } else if (value instanceof ObjectId || value instanceof DBRefBase) {
            ceditor = EditObjectIdDialog.class;
        } else if (value instanceof Boolean) {
            ceditor = EditBooleanDialog.class;
        } else if (value instanceof Code || value instanceof CodeWScope) {
            ceditor = EditCodeDialog.class;
        } else if (value instanceof Date) {
            ceditor = EditDateDialog.class;
        } else if (value instanceof Double || value instanceof Float) {
            ceditor = EditDoubleDialog.class;
        } else if (value instanceof Long || value instanceof Integer) {
            ceditor = EditLongDialog.class;
        } else if (value instanceof Pattern) {
            ceditor = EditPatternDialog.class;
        } else if (value instanceof BSONTimestamp) {
            ceditor = EditTimestampDialog.class;
        } else if (value instanceof UUID) {
            ceditor = EditUuidDialog.class;
        }

        if (ceditor == null)
            return;
        EditFieldDialog editor = (EditFieldDialog) JMongoBrowser.instance.getGlobalStore().getFirstChildOfClass(ceditor, null);
        editor.setKey(key);
        editor.setValue(value);

        if (!editor.show()) {
            return;
        }
        value = editor.getValue();
        setStringFieldValue(Item.value, JSON.serialize(value));
        System.out.println(value.toString());
        System.out.println(JSON.serialize(value));
        updateComponent();
        _object.commitComponent();
    }

    public void remove() {
        _object.remove(key);
    }

    public void moveUp() {
        _object.moveUp(key);
    }

    public void moveDown() {
        _object.moveDown(key);
    }

    public void actionPerformed(Item enm, XmlComponentUnit unit, Object src) {
    }

    public Object getValue() {
        return value;
    }

    public String getKey() {
        return key;
    }

}
