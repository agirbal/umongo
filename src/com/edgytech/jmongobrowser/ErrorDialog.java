/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.edgytech.jmongobrowser;

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
        getLogger().log(Level.WARNING, null, exception);
    }

}
