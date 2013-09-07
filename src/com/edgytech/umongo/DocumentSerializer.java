/**
 * Copyright (C) 2010 EdgyTech Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.edgytech.umongo;

import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.bson.BSON;

/**
 *
 * @author antoine
 */
public class DocumentSerializer {

    public enum Format {

        JSON,
        JSON_ARRAY,
        CSV,
        BSON
    }
    Format format;
    OutputStream os;
    boolean first = true;
    String fields;
    String[] filter;
    File file;
    String delimiter = ",";
    String header;

    public DocumentSerializer(Format format, String fields) {
        this.format = format;

        this.fields = fields;
        if (fields != null) {
            filter = fields.split(",");
            for (int i = 0; i < filter.length; ++i) {
                filter[i] = filter[i].trim();
            }
        }
    }

    public Format getFormat() {
        return format;
    }

    public String getFields() {
        return fields;
    }

    public void setOutputStream(OutputStream os) {
        this.os = os;
    }

    public OutputStream getOutputStream() {
        return os;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    private Object getFieldValueRecursive(DBObject obj, String field) {
        if (field.indexOf(".") < 0)
            return obj.get(field);
        
        String[] tokens = field.split("\\.");
        for (int i = 0; i < tokens.length - 1; ++i) {
            String f = tokens[i];
            if (!obj.containsField(f))
                return null;
            Object o = obj.get(f);
            if (! DBObject.class.isInstance(o))
                return null;
            obj = (DBObject) o;
        }
        return obj.get(tokens[tokens.length - 1]);
    }

    public void writeObject(DBObject obj) throws IOException {
        if (os == null) {
            os = new FileOutputStream(file);
        }

        if (first) {
            first = false;
            if (format == Format.CSV) {
                if (header != null && !header.isEmpty())
                    os.write(header.getBytes());
                else
                    os.write(fields.getBytes());
                os.write('\n');
            } else if (format == Format.JSON_ARRAY) {
                os.write('[');
            }
        } else {
            if (format == Format.JSON_ARRAY) {
                os.write(',');
            }
        }

        if (format == Format.CSV) {
            for (int i = 0; i < filter.length; ++i) {
                if (i != 0) {
                    os.write(delimiter.getBytes());
                }
                String field = filter[i];
                os.write(JSON.serialize(getFieldValueRecursive(obj, field)).getBytes());
            }
        } else if (format == Format.BSON) {
            os.write(BSON.encode(obj));
        } else {
            os.write(obj.toString().getBytes());
        }

        if (format == Format.JSON || format == Format.CSV) {
            os.write('\n');
        }
    }

    public void close() throws IOException {
        if (first == false && format == Format.JSON_ARRAY) {
            os.write(']');
        }
        os.close();
    }

    void setDelimiter(String delimiter) {
        if (!delimiter.trim().isEmpty())
            this.delimiter = delimiter.substring(0, 1);
    }
    
    void setHeader(String header) {
        if (!header.trim().isEmpty())
            this.header = header;
    }
    
}
