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

import com.mongodb.*;
import com.mongodb.util.JSON;
import java.io.*;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bson.BSONDecoder;
import org.bson.BasicBSONDecoder;

/**
 *
 * @author antoine
 */
public class DocumentDeserializer {

    public enum Format {

        JSON,
        JSON_ARRAY,
        CSV,
        BSON
    }
    Format format;
    boolean first = true;
    String fields;
    String[] filter;
    File file;
    BufferedReader br;
    InputStream is;
    DBCallback callback;
    BSONDecoder decoder;
    Iterator iterator;
    BasicDBObject template;

    public DocumentDeserializer(Format format, String fields) {
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

    void setTemplate(BasicDBObject template) {
        this.template = template;
    }

    public void setInputStream(InputStream is) {
        this.is = is;
    }

    public InputStream getOutputStream() {
        return is;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    public class DocumentIterator implements java.util.Iterator<DBObject> {

        public boolean hasNext() {
//                try {
//                    if (fis != null)
//                        return fis.available() > 0;
//                } catch (IOException ex) {
//                    Logger.getLogger(Serializer.class.getName()).log(Level.SEVERE, null, ex);
//                }
//                return false;
            return true;
        }

        public DBObject next() {
            try {
                return readObject();
            } catch (IOException ex) {
                Logger.getLogger(DocumentDeserializer.class.getName()).log(Level.SEVERE, null, ex);
            }
            return null;
        }

        public void remove() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    public DocumentIterator iterator() {
        return new DocumentIterator();
    }

    public DBObject readObject() throws IOException {
        if (first) {
            if (format != Format.BSON) {
                FileReader fr = new FileReader(file);
                br = new BufferedReader(fr);
                if (format == Format.CSV) {
                    fields = br.readLine();
                    if (fields != null) {
                        filter = fields.split(",");
                        for (int i = 0; i < filter.length; ++i) {
                            filter[i] = filter[i].trim();
                        }
                    }
                }
            } else {
                is = new FileInputStream(file);
                callback = new DefaultDBCallback(null);
                decoder = new BasicBSONDecoder();
            }
            first = false;
        }

        DBObject obj = null;
        if (format != Format.BSON) {
            String line = br.readLine();
            if (line == null) {
                return null;
            }

            if (format == Format.JSON_ARRAY && iterator == null) {
                BasicDBList list = (BasicDBList) JSON.parse(line);
                iterator = list.iterator();
            }

            if (format == Format.CSV) {
                String[] values = line.split(",");
                if (template == null) {
                    obj = new BasicDBObject();
                    for (int i = 0; i < filter.length; ++i) {
                        String val = values[i];
                        obj.put(filter[i], JSON.parse(val));
                    }
                } else {
                    obj = (BasicDBObject) template.copy();
                    fillInTemplate(obj, values);
                }
            } else if (format == Format.JSON_ARRAY) {
                if (iterator.hasNext()) {
                    obj = (DBObject) iterator.next();
                }
            } else {
                obj = (DBObject) JSON.parse(line);
            }
        } else {
            // BSON is binary
            callback.reset();
            try {
                decoder.decode(is, callback);
            } catch (IOException e) {
                // most likely EOF
                return null;
            }
            obj = (DBObject) callback.get();

//                // read length
//                byte[] buf = new byte[4096];
//                int n = fis.read(buf, 0, 4);
//                if (n <= 0) {
//                    return null;
//                }
//                int len = Bits.readInt(buf);
//
//                ByteArrayOutputStream baos = new ByteArrayOutputStream();
//                baos.write(buf, 0, 4);
//                int toread = len;
//                while (toread > 0) {
//                    n = fis.read(buf, 0, Math.min(toread, buf.length));
//                    if (n <= 0) {
//                        break;
//                    }
//                    baos.write(buf, 0, n);
//                    toread -= n;
//                }
//                if (baos.size() != len)
//                    throw new IOException("Lenght of read object " + baos.size() + " does not match expected size " + len);
//                obj = new BasicDBObject((BasicBSONObject) BSON.decode(baos.toByteArray()));
        }

        return obj;
    }

    public void close() throws IOException {
        if (br != null) {
            br.close();
        }
        if (is != null) {
            is.close();
        }

    }

    private void fillInTemplate(DBObject obj, String[] values) {
        for (String field : obj.keySet()) {
            Object val = obj.get(field);
            if (val instanceof BasicDBObject) {
                fillInTemplate((BasicDBObject)val, values);
            } else if (val instanceof BasicDBList) {
                fillInTemplate((BasicDBList)val, values);
            } else if (val instanceof String) {
                String str = (String) val;
                if (str.startsWith("$")) {
                    str = str.substring(1);
                    int slash = str.indexOf("/");
                    String ref = str;
                    String type = null;
                    if (slash > 0) {
                        ref = str.substring(0, slash);
                        type = str.substring(slash + 1);
                    }
                    
                    // find field index
                    int index = 0;
                    while (index < filter.length && !filter[index].equals(ref)) { ++index; }
                    if (index >= filter.length)
                        continue;
                    String value = values[index];
                    
                    if (type == null || "String".equals(type)) {
                        obj.put(field, value);
                    } else if ("Integer".equals(type)) {
                        obj.put(field, Integer.valueOf(value));
                    } else if ("Long".equals(type)) {
                        obj.put(field, Long.valueOf(value));
                    } else if ("Double".equals(type)) {
                        obj.put(field, Double.valueOf(value));
                    }
                }
            }
        }
    }

}
