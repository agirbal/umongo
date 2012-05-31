/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.edgytech.umongo;

import com.mongodb.BasicDBList;
import com.mongodb.DBObject;

/**
 *
 * @author antoine
 */
public class DocFieldArray extends DocFieldObject {
    public DocFieldArray(String id, String key, Object value, DocFieldObject parent) {
        super(id, key, value, parent);
        getJComponentBoundUnit(Item.addKey).visible = false;
    }

    @Override
    protected DBObject createDBObject() {
        return new BasicDBList();
    }

    @Override
    protected void addField(String key, Object val) {
        BasicDBList list = (BasicDBList) value;
        list.add(val);
    }

    @Override
    public void addField() {
        String type = getStringFieldValue(Item.addType);
        addNewField(null, type);
    }

}
