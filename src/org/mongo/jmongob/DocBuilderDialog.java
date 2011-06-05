/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.mongo.jmongob;

import com.edgytech.swingfast.FormDialog;
import com.edgytech.swingfast.Scroller;
import com.edgytech.swingfast.XmlComponentUnit;
import com.mongodb.BasicDBObject;

/**
 *
 * @author antoine
 */
public class DocBuilderDialog extends FormDialog {

    enum Item {
        div
    }

    DocFieldObject _root;

    public DocBuilderDialog() {
        setEnumBinding(Item.values(), null);
    }

    void setDBObject(BasicDBObject doc) {
        _root = new DocFieldObject(null, null, doc, null);
        XmlComponentUnit div = getComponentBoundUnit(Item.div);
        div.removeAllChildren();
        div.addChild(_root);
        div.structureComponent();
    }

    BasicDBObject getDBObject() {
        return (BasicDBObject) _root.getValue();
    }
}
