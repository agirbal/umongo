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

import com.edgytech.swingfast.FileSelectorField;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bson.types.Binary;
import sun.misc.IOUtils;

/**
 *
 * @author antoine
 */
public class EditBinaryDialog extends EditFieldDialog {
    enum Item {
        inputFile
    }

    public EditBinaryDialog() {
        setEnumBinding(Item.values(), null);
    }

    @Override
    public Object getValue() {
        FileInputStream fis = null;
        try {
            String path = ((FileSelectorField) getBoundComponentUnit(Item.inputFile)).getPath();
            fis = new FileInputStream(path);
            byte[] bytes = IOUtils.readFully(fis, -1, true);
            return new Binary((byte)0, bytes);
        } catch (Exception ex) {
            getLogger().log(Level.WARNING, null, ex);
        } finally {
            try {
                fis.close();
            } catch (IOException ex) {
                Logger.getLogger(EditBinaryDialog.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return null;
    }

    @Override
    public void setValue(Object value) {
        // nothing to do here...
    }
}
