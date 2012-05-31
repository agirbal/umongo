/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.edgytech.umongo;

import com.edgytech.swingfast.FormDialog;
import java.io.File;
import com.edgytech.umongo.ExportFile.Format;

/**
 *
 * @author antoine
 */
public class ImportDialog extends FormDialog {
    enum Item {

        inputFile,
        format,
        dropCollection,
        continueOnError,
        upsert,
        upsertFields,
        bulk
    }

    public ImportDialog() {
        setEnumBinding(Item.values(), null);
    }

    public ExportFile getExportFile() {
        ExportFile ef = new ExportFile(new File(getStringFieldValue(Item.inputFile)),
                Format.values()[getIntFieldValue(Item.format)]);
        return ef;
    }

}
