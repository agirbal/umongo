/**
 *      Copyright (C) 2010 EdgyTech LLC.
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
import com.edgytech.swingfast.FileSelectorField;
import com.edgytech.swingfast.FormDialog;
import com.edgytech.swingfast.FormDialogListener;
import com.edgytech.swingfast.IntFieldInterface;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.XMLFormatter;

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
        activityLog,
        activityLogFlag,
        activityLogFile,
        activityLogFirstResult,
        applicationLog,
        applicationLogFlag,
        applicationLogFile,
        applicationLogSize,
        applicationLogCount,
        applicationLogFormat,
        applicationLogLevel,
        pluginFolder,
        allowPlugins
    }

    public AppPreferences() {
        setEnumBinding(Item.values(), null);
        setFormDialogListener(this);
    }

    public void start() {
        // apply settings at start up
        formOkCbk();
    }

    public void formOkCbk() {
        UMongo.instance.updateLogging();
        UMongo.instance.updatePlugins();
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
    
    public String getActivityLogFile() {
        if (getBooleanFieldValue(Item.activityLogFlag)) {
            String path = getStringFieldValue(Item.activityLogFile);
            if (path != null && !path.trim().isEmpty()) {
                return path;
            }
        }
        return null;
    }
    
    public boolean getActivityLogFirstResult() {
        return getBooleanFieldValue(Item.activityLogFirstResult);
    }
    
    public Handler getApplicationLogHandler() {
        if (!getBooleanFieldValue(Item.applicationLogFlag)) {
            return null;
        }

        String path = getStringFieldValue(Item.applicationLogFile);
        if (path == null || path.trim().isEmpty()) {
            return null;
        }
        
        Handler handler;
        try {
            handler = new FileHandler(path, getIntFieldValue(Item.applicationLogSize) * 1024 * 1024, getIntFieldValue(Item.applicationLogCount), true);
            Level lvl = Level.WARNING;
            String lvlStr = getStringFieldValue(Item.applicationLogLevel);
            if (lvlStr.equals("OFF"))
                lvl = Level.OFF;
            else if (lvlStr.equals("ALL"))
                lvl = Level.ALL;
            else if (lvlStr.equals("INFO"))
                lvl = Level.INFO;
            else if (lvlStr.equals("WARNING"))
                lvl = Level.WARNING;
            else if (lvlStr.equals("SEVERE"))
                lvl = Level.SEVERE;
            handler.setLevel(lvl);

            Formatter fmt = new SimpleFormatter();
            if (getStringFieldValue(Item.applicationLogFormat).equals("XML"))
                fmt = new XMLFormatter();
            handler.setFormatter(fmt);
            return handler;
        } catch (Exception ex) {
            getLogger().log(Level.WARNING, null, ex);
        }        
        return null;
    }
    
    public String getPluginFolder() {
        if (getBooleanFieldValue(Item.allowPlugins)) {
            return getStringFieldValue(Item.pluginFolder);
        }
        return null;
    }
}
