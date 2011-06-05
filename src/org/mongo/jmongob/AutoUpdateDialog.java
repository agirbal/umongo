/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.mongo.jmongob;

import com.edgytech.swingfast.FormDialog;

/**
 *
 * @author antoine
 */
public class AutoUpdateDialog extends FormDialog {
    enum Item {
        autoType,
        autoInterval,
        autoCount
    }

    public AutoUpdateDialog() {
        setEnumBinding(Item.values(), null);
    }

    
}
