/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.edgytech.jmongobrowser;

import com.edgytech.swingfast.PopUpMenu;
import com.edgytech.swingfast.TreeNodeLabel;

/**
 *
 * @author antoine
 */
public class TreeNodeDocumentField extends TreeNodeLabel {
    String key;
    Object value;

    public TreeNodeDocumentField(String key, Object value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (key != null)
            sb.append(key).append(": ");
        sb.append(MongoUtils.getObjectString(value));
        return sb.toString();
    }

    public String getKey() {
        return key;
    }

    public Object getValue() {
        return value;
    }
    
    @Override
    public PopUpMenu getPopUpMenu() {
        return (PopUpMenu) JMongoBrowser.instance.getGlobalStore().getBoundUnit(GlobalStore.Item.documentFieldMenu);
    }

}
