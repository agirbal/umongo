/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.edgytech.jmongobrowser;

import com.edgytech.swingfast.AppUtils;
import com.edgytech.swingfast.FormDialog;
import com.edgytech.swingfast.TextField;

/**
 *
 * @author antoine
 */
public abstract class EditFieldDialog extends FormDialog {

    String key;

    public EditFieldDialog() {
        setLabel(AppUtils.convertIdToLabel(this.getClass().getSimpleName()));
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public abstract Object getValue();
    public abstract void setValue(Object value);
}
