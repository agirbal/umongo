/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.mongo.jmongob;

import com.edgytech.swingfast.FormDialog;
import java.io.File;
import org.mongo.jmongob.ExportFile.Format;

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
