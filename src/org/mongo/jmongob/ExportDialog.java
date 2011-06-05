/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mongo.jmongob;

import com.edgytech.swingfast.FieldChecker;
import com.edgytech.swingfast.FormDialog;
import com.edgytech.swingfast.TextField;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import org.mongo.jmongob.ExportFile.Format;

/**
 *
 * @author antoine
 */
public class ExportDialog extends FormDialog {

    enum Item {
        outputFile,
        format,
        fields,
        continueOnError
    }

    public ExportDialog() {
        setEnumBinding(Item.values(), null);
    }

    @Override
    public boolean checkComponent(FieldChecker checker) {
        if (getComponentIntFieldValue(Item.format) == Format.CSV.ordinal() && getComponentStringFieldValue(Item.fields).trim().isEmpty()) {
            ((TextField) getBoundUnit(Item.fields)).setDisplayError("Fields must be specified for CSV format");
            return false;
        }
        return super.checkComponent(checker);
    }

    public ExportFile.ExportFileOutputStream getOutputStream() {
        ExportFile ef = new ExportFile(new File(getStringFieldValue(Item.outputFile)),
                Format.values()[getIntFieldValue(Item.format)]);
        return ef.getOutputStream(getStringFieldValue(Item.fields));
    }


}
