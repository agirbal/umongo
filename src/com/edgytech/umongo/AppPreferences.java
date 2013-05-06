/**
 *      Copyright (C) 2010 EdgyTech Inc.
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
