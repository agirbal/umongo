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

import com.edgytech.swingfast.InfoDialog;
import com.mongodb.MongoException;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.logging.Level;

/**
 *
 * @author antoine
 */
public class ErrorDialog extends InfoDialog {
    enum Item {
        errorIn,
        errorMsg,
        errorCode,
        errorTrace
    }

    Exception exception;

    public ErrorDialog() {
        setEnumBinding(Item.values(), null);
    }

    public void setException(Exception exception, String in) {
        this.exception = exception;
        label = in;
        setStringFieldValue(Item.errorIn, in);
        setStringFieldValue(Item.errorMsg, exception.getMessage());
        String code = "?";
        if (exception instanceof MongoException) {
            MongoException me = (MongoException) exception;
            code = String.valueOf(me.getCode());
        }

        setStringFieldValue(Item.errorCode, code);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(baos);
        exception.printStackTrace(pw);
        pw.flush();
        setStringFieldValue(Item.errorTrace, baos.toString());
        updateComponent();
//        getLogger().log(Level.WARNING, null, exception);
    }

}
