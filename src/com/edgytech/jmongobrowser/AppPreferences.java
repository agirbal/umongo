/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.edgytech.jmongobrowser;

import com.edgytech.swingfast.CheckBox;
import com.edgytech.swingfast.FormDialog;
import com.edgytech.swingfast.FormDialogListener;
import com.edgytech.swingfast.IntFieldInterface;

/**
 *
 * @author antoine
 */
public class AppPreferences extends FormDialog implements FormDialogListener {

    enum Item {
        getMoreSize,
        inlineDocumentLength,
        useSystemLook,
        treeUpdateRate,
    }

    public AppPreferences() {
        setEnumBinding(Item.values(), null);
    }

    public void start() {
        // apply settings at start up
        formOkCbk();
    }

    public void formOkCbk() {
    }

    public void formCancelCbk() {
    }

    public void formResetCbk() {
    }

    public boolean getUseSystemLook() {
        return ((CheckBox) getBoundUnit(Item.useSystemLook)).getBooleanValue();
    }

    public int getGetMoreSize() {
        return ((IntFieldInterface) getBoundUnit(Item.getMoreSize)).getIntValue();
    }

    public int getInlineDocumentLength() {
        return ((IntFieldInterface) getBoundUnit(Item.inlineDocumentLength)).getIntValue();
    }

    public int getTreeUpdateRate() {
        return ((IntFieldInterface) getBoundUnit(Item.treeUpdateRate)).getIntValue();
    }
}
