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

import com.edgytech.swingfast.FieldChecker;
import com.edgytech.swingfast.FormDialog;
import com.edgytech.swingfast.TextField;
import com.edgytech.umongo.DocumentSerializer.Format;
import java.io.File;

/**
 *
 * @author antoine
 */
public class ExportDialog extends FormDialog {

    enum Item {
        outputFile,
        format,
        fields,
        continueOnError,
        delimiter,
        header
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

    public DocumentSerializer getDocumentSerializer() {
        DocumentSerializer ds = new DocumentSerializer(Format.values()[getIntFieldValue(Item.format)], getStringFieldValue(Item.fields));
        ds.setFile(new File(getStringFieldValue(Item.outputFile)));
        ds.setDelimiter(getStringFieldValue(Item.delimiter));
        ds.setHeader(getStringFieldValue(Item.header));
        return ds;
    }


}
