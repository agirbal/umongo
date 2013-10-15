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

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 *
 * @author antoine
 */
public class PluginClassLoader extends ClassLoader {

    /**
     * This is the directory from which the classes will be loaded
     */
    File directory;

    /**
     * The constructor. Just initialize the directory
     */
    public PluginClassLoader(File dir) {
        directory = dir;
    }

    /**
     * A convenience method that calls the 2-argument form of this method
     */
    public Class loadClass(String name) throws ClassNotFoundException {
        return loadClass(name, true);
    }

    /**
     * This is one abstract method of ClassLoader that all subclasses must
     * define. Its job is to load an array of bytes from somewhere and to pass
     * them to defineClass(). If the resolve argument is true, it must also call
     * resolveClass(), which will do things like verify the presence of the
     * superclass. Because of this second step, this method may be called to
     * load superclasses that are system classes, and it must take this into
     * account.
     */
    public Class loadClass(String classname, boolean resolve) throws ClassNotFoundException {
        try {
            // Our ClassLoader superclass has a built-in cache of classes it has
            // already loaded. So, first check the cache.
            Class c = findLoadedClass(classname);

            // After this method loads a class, it will be called again to
            // load the superclasses. Since these may be system classes, we've
            // got to be able to load those too. So try to load the class as
            // a system class (i.e. from the CLASSPATH) and ignore any errors
            if (c == null) {
                try {
                    c = findSystemClass(classname);
                } catch (Exception ex) {
                }
            }

            // If the class wasn't found by either of the above attempts, then
            // try to load it from a file in (or beneath) the directory
            // specified when this ClassLoader object was created. Form the
            // filename by replacing all dots in the class name with
            // (platform-independent) file separators and by adding the ".class" extension.
            if (c == null) {
                // Figure out the filename
                String filename = classname.replace('.', File.separatorChar) + ".class";

                // Create a File object. Interpret the filename relative to the
                // directory specified for this ClassLoader.
                File f = new File(directory, filename);

                // Get the length of the class file, allocate an array of bytes for
                // it, and read it in all at once.
                int length = (int) f.length();
                byte[] classbytes = new byte[length];
                DataInputStream in = new DataInputStream(new FileInputStream(f));
                in.readFully(classbytes);
                in.close();

                // Now call an inherited method to convert those bytes into a Class
                c = defineClass(classname, classbytes, 0, length);
            }

            // If the resolve argument is true, call the inherited resolveClass method.
            if (resolve) {
                resolveClass(c);
            }

            // And we're done. Return the Class object we've loaded.
            return c;
        } // If anything goes wrong, throw a ClassNotFoundException error
        catch (Exception ex) {
            throw new ClassNotFoundException(ex.toString());
        }
    }

    public List<Class> loadClasses(JarFile jar) throws ClassNotFoundException, IOException {
        List<Class> classes = new ArrayList<Class>();
        for (Enumeration<JarEntry> e = jar.entries(); e.hasMoreElements();) {
            JarEntry entry = e.nextElement();
//                            System.out.println(entry.getName());
            String name = entry.getName();
            if (name.endsWith(".class")) {
                ZipEntry zip = jar.getEntry(entry.getName());
                int len = (int) zip.getSize();
                byte[] bytes = new byte[len];
                DataInputStream is = new DataInputStream(jar.getInputStream(zip));
                is.readFully(bytes);
                Class c = defineClass(name.substring(0, name.indexOf(".")), bytes, 0, len);

                boolean resolve = true;
                if (resolve) {
                    resolveClass(c);
                }
                classes.add(c);
            }
        }
        return classes;
    }
}
