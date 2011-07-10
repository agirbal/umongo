/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mongo.jmongob;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCallback;
import com.mongodb.DBObject;
import com.mongodb.DefaultDBCallback;
import com.mongodb.util.JSON;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bson.BSON;
import org.bson.BSONDecoder;
import org.bson.BSONObject;
import org.bson.BasicBSONDecoder;
import org.bson.BasicBSONObject;
import org.bson.io.Bits;

/**
 *
 * @author antoine
 */
public class ExportFile {

    public enum Format {

        JSON,
        JSON_ARRAY,
        CSV,
        BSON
    }
    File file;
    Format format;

    public ExportFile(File file, Format format) {
        this.file = file;
        this.format = format;
    }

    public File getFile() {
        return file;
    }

    public Format getFormat() {
        return format;
    }

    public ExportFileOutputStream getOutputStream(String fields) {
        return new ExportFileOutputStream(fields);
    }

    public ExportFileInputStream getInputStream() throws IOException {
        return new ExportFileInputStream();
    }

    class ExportFileOutputStream {

        FileOutputStream fos;
        boolean first = true;
        String fields;
        String[] filter;

        public ExportFileOutputStream(String fields) {
            this.fields = fields;
            if (fields != null) {
                filter = fields.split(",");
                for (int i = 0; i < filter.length; ++i) {
                    filter[i] = filter[i].trim();
                }
            }
        }

        public void writeObject(DBObject obj) throws IOException {
            if (fos == null) {
                fos = new FileOutputStream(file);
            }

            if (first) {
                first = false;
                if (format == Format.CSV) {
                    fos.write(fields.getBytes());
                    fos.write('\n');
                } else if (format == Format.JSON_ARRAY) {
                    fos.write('[');
                }
            } else {
                if (format == Format.JSON_ARRAY) {
                    fos.write(',');
                }
            }

            if (format == Format.CSV) {
                for (int i = 0; i < filter.length; ++i) {
                    if (i != 0) {
                        fos.write(',');
                    }
                    String field = filter[i];
                    if (obj.containsField(field)) {
                        fos.write(JSON.serialize(obj.get(field)).getBytes());
                    }
                }
            } else if (format == Format.BSON) {
                fos.write(BSON.encode(obj));
            } else {
                fos.write(obj.toString().getBytes());
            }

            if (format == Format.JSON || format == Format.CSV) {
                fos.write('\n');
            }
        }

        public void close() throws IOException {
            if (first == false && format == Format.JSON_ARRAY) {
                fos.write(']');
            }
            fos.close();
        }
    }

    class ExportFileInputStream {

        public class ExportFileIterator implements java.util.Iterator<DBObject> {

            public boolean hasNext() {
//                try {
//                    if (fis != null)
//                        return fis.available() > 0;
//                } catch (IOException ex) {
//                    Logger.getLogger(ExportFile.class.getName()).log(Level.SEVERE, null, ex);
//                }
//                return false;
                return true;
            }

            public DBObject next() {
                try {
                    return readObject();
                } catch (IOException ex) {
                    Logger.getLogger(ExportFile.class.getName()).log(Level.SEVERE, null, ex);
                }
                return null;
            }

            public void remove() {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        }
        BufferedReader br;
        FileInputStream fis;
        DBCallback callback;
        BSONDecoder decoder;
        String fields;
        String[] filter;
        Iterator iterator;

        public ExportFileInputStream() throws FileNotFoundException, IOException {
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
                fis = new FileInputStream(file);
                callback = new DefaultDBCallback(null);
                decoder = new BasicBSONDecoder();
            }
        }

        public ExportFileIterator iterator() {
            return new ExportFileIterator();
        }

        public DBObject readObject() throws IOException {
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
                    obj = new BasicDBObject();
                    for (int i = 0; i < filter.length; ++i) {
                        String val = values[i];
                        obj.put(filter[i], JSON.parse(val));
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
                    decoder.decode(fis, callback);
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
            if (fis != null) {
                fis.close();
            }

        }
    }
}
