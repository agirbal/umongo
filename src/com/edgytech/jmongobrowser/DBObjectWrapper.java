/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.edgytech.jmongobrowser;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 *
 * @author antoine
 */
public class DBObjectWrapper {
    Object key;
    DBObject obj;

    public DBObjectWrapper(Object key, DBObject obj) {
        this.key = key;
        this.obj = obj;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (key != null)
            sb.append(key).append(": ");
        sb.append(MongoUtils.dbObjectToString(obj));
        return sb.toString();
    }

    public Object getKey() {
        return key;
    }

    public DBObject getDBObject() {
        return obj;
    }

    
}
