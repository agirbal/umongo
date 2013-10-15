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

/**
 *
 * @author antoine
 */
public class PluginSecurityManager extends SecurityManager {

    private String pluginDir = null;

    PluginSecurityManager(String dir) {
        pluginDir = dir;
    }

    /**
     * This is the basic method that tests whether there is a class loaded by a
     * ClassLoader anywhere on the stack. If so, it means that that untrusted
     * code is trying to perform some kind of sensitive operation. We prevent it
     * from performing that operation by throwing an exception. trusted() is
     * called by most of the check...() methods below.
     */
    protected void trusted() {
        if (inClassLoader()) {
            throw new SecurityException();
        }
    }

    /**
     * These are all the specific checks that a security manager can perform.
     * They all just call one of the methods above and throw a SecurityException
     * if the operation is not allowed. This SecurityManager subclass is perhaps
     * a little too restrictive. For example, it doesn't allow loaded code to
     * read *any* system properties, even though some of them are quite
     * harmless.
     */
    public void checkCreateClassLoader() {
        trusted();
    }

    public void checkAccess(Thread g) {
        trusted();
    }

    public void checkAccess(ThreadGroup g) {
        trusted();
    }

    public void checkExit(int status) {
        trusted();
    }

    public void checkExec(String cmd) {
        trusted();
    }

    public void checkLink(String lib) {
        trusted();
    }

    public void checkRead(java.io.FileDescriptor fd) {
        trusted();
    }

    public void checkRead(String file) {
//		String path = new File(file).getParentFile().getAbsolutePath();
//		if (! path.endsWith(pluginDir))
        trusted();
    }

    public void checkRead(String file, Object context) {
        trusted();
    }

    public void checkWrite(java.io.FileDescriptor fd) {
        trusted();
    }

    public void checkWrite(String file) {
        trusted();
    }

    public void checkDelete(String file) {
        trusted();
    }

    public void checkConnect(String host, int port) {
        trusted();
    }

    public void checkConnect(String host, int port, Object context) {
        trusted();
    }

    public void checkListen(int port) {
        trusted();
    }

    public void checkAccept(String host, int port) {
        trusted();
    }

    public void checkMulticast(java.net.InetAddress maddr) {
        trusted();
    }

    public void checkMulticast(java.net.InetAddress maddr, byte ttl) {
        trusted();
    }

    public void checkPropertiesAccess() {
        trusted();
    }

    public void checkPropertyAccess(String key) {
//		if (! key.equals("user.dir"))
        trusted();
    }

    public void checkPrintJobAccess() {
        trusted();
    }

    public void checkSystemClipboardAccess() {
        trusted();
    }

    public void checkAwtEventQueueAccess() {
        trusted();
    }

    public void checkSetFactory() {
        trusted();
    }

    public void checkMemberAccess(Class clazz, int which) {
        trusted();
    }

    public void checkSecurityAccess(String provider) {
        trusted();
    }

    /**
     * Loaded code can only load classes from java.* packages
     */
    public void checkPackageAccess(String pkg) {
        if (inClassLoader() && !pkg.startsWith("java.") && !pkg.startsWith("javax.")) {
            throw new SecurityException();
        }
    }

    /**
     * Loaded code can't define classes in java.* or sun.* packages
     */
    public void checkPackageDefinition(String pkg) {
        if (inClassLoader() && ((pkg.startsWith("java.") || pkg.startsWith("javax.") || pkg.startsWith("sun.")))) {
            throw new SecurityException();
        }
    }

    /**
     * This is the one SecurityManager method that is different from the others.
     * It indicates whether a top-level window should display an "untrusted"
     * warning. The window is always allowed to be created, so this method is
     * not normally meant to throw an exception. It should return true if the
     * window does not need to display the warning, and false if it does. In
     * this example, however, our text-based Service classes should never need
     * to create windows, so we will actually throw an exception to prevent any
     * windows from being opened.
	 *
     */
    public boolean checkTopLevelWindow(Object window) {
        trusted();
        return true;
    }
}
