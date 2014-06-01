/**
 * Copyright (C) 2010 EdgyTech LLC.
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
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
        JSON_SINGLE_DOC,
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
    String delimiter = ",";
    String quote = "\"";
    Pattern pattern;

    public DocumentDeserializer(Format format, String fields) {
        this.format = format;

        this.fields = fields;
        if (fields != null) {
            // this is from the form, always comma separated
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

    public InputStream getInputStream() {
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

    private List<String> splitByCommasNotInQuotes(String s) {
        List<String> list = new ArrayList<String>();
        if (pattern == null) {
            pattern = Pattern.compile("[" + quote + delimiter + "]");
        }
        Matcher m = pattern.matcher(s);
        int pos = 0;
        boolean quoteMode = false;
        boolean wasQuoted = false;
        while (m.find()) {
            String sep = m.group();
            if (quote.equals(sep)) {
                int qpos = m.start();
                if (!quoteMode) {
                    // only turn on quote mode if previous char was the delimiter
                    if (qpos == pos) {
                        quoteMode = true;
                        wasQuoted = true;
                    }
                } else {
                    quoteMode = false;
                }
            } else if (!quoteMode && delimiter.equals(sep)) {
                int toPos = m.start();
                String token = s.substring(pos, toPos);
                if (wasQuoted) {
                    token = token.substring(quote.length(), token.length() - quote.length());
                }
                list.add(token);
                pos = m.end();
                wasQuoted = false;
            }
        }
        
        // previous loop always finishes on last limiter, need to add last token
        if (pos <= s.length()) {
            list.add(s.substring(pos));
        }
        return list;
    }

    public DBObject readObject() throws IOException {
        if (first) {
            if (format != Format.BSON) {
                if (is == null) {
                    FileReader fr = new FileReader(file);
                    br = new BufferedReader(fr);
                } else {
                    br = new BufferedReader(new InputStreamReader(is));
                }
                if (format == Format.CSV) {
                    fields = br.readLine();
                    if (fields != null) {
                        filter = fields.split(delimiter);
                        // field names are never quoted
                        for (int i = 0; i < filter.length; ++i) {
                            filter[i] = filter[i].trim();
                        }
                    }
                }
            } else {
                if (is == null) {
                    is = new FileInputStream(file);
                }
                callback = new DefaultDBCallback(null);
                decoder = new BasicBSONDecoder();
            }

            if (format == Format.JSON_ARRAY) {
                String line = br.readLine();
                BasicDBList list = (BasicDBList) JSON.parse(line);
                iterator = list.iterator();
            }

            first = false;
        }

        if (format == Format.JSON_ARRAY) {
            if (iterator == null || !iterator.hasNext()) {
                return null;
            }
            return (DBObject) iterator.next();
        }

        DBObject obj = null;
        if (format != Format.BSON) {
            String line = br.readLine();

            if (line == null) {
                return null;
            }

            if (format == Format.JSON_SINGLE_DOC) {
                // keep reading all lines
                String line2 = null;
                while ((line2 = br.readLine()) != null) {
                    line += line2;
                }
            }

            if (format == Format.CSV) {
                List<String> values = splitByCommasNotInQuotes(line);
                if (template == null) {
                    obj = new BasicDBObject();
                    // set each field defined
                    for (int i = 0; i < filter.length; ++i) {
                        String val = values.get(i);
                        // string values are always quoted
                        obj.put(filter[i], JSON.parse(val));
                    }
                } else {
                    obj = (BasicDBObject) template.copy();
                    fillInTemplate(obj, values);
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

    private void fillInTemplate(DBObject obj, List<String> values) {
        for (String field : obj.keySet()) {
            Object val = obj.get(field);
            if (val instanceof BasicDBObject) {
                fillInTemplate((BasicDBObject) val, values);
            } else if (val instanceof BasicDBList) {
                fillInTemplate((BasicDBList) val, values);
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
                    while (index < filter.length && !filter[index].equals(ref)) {
                        ++index;
                    }
                    if (index >= filter.length) {
                        continue;
                    }
                    String value = values.get(index);

                    try {
                        if (type == null || "JSON".equals(type)) {
                            // this is typically used for quoted Strings
                            obj.put(field, JSON.parse(value));
                        } else if ("String".equals(type)) {
                            obj.put(field, value);
                        } else if ("Date".equals(type)) {
                            Long time = Long.valueOf(value);
                            obj.put(field, new Date(time));
                        } else if ("Boolean".equals(type)) {
                            obj.put(field, Boolean.valueOf(value));
                        } else if ("Integer".equals(type)) {
                            obj.put(field, Integer.valueOf(value));
                        } else if ("Long".equals(type)) {
                            obj.put(field, Long.valueOf(value));
                        } else if ("Double".equals(type)) {
                            obj.put(field, Double.valueOf(value));
                        }
                    } catch (Exception ex) {
                        Logger.getLogger(DocumentDeserializer.class.getName()).log(Level.WARNING, null, ex);
                    }
                } else {
                    // this is a static value
                    obj.put(field, val);
                }
            } else {
                // this is a static value
                obj.put(field, val);
            }
        }
    }

    void setDelimiter(String delimiter) {
        if (!delimiter.trim().isEmpty()) {
            this.delimiter = delimiter.substring(0, 1);
        }
    }

    void setQuote(String quote) {
        if (!quote.trim().isEmpty()) {
            this.quote = quote.substring(0, 1);
        }
    }
}
