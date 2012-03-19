/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.edgytech.jmongobrowser;

import com.edgytech.swingfast.Common;
import com.edgytech.swingfast.Directory;

/**
 *
 * @author antoine
 */
public class Resource {

    enum File {

        JMongoBrowser,
        docView,
        cmdView,
        queryResultView,
        dbJob,
        mongoNode,
        dbNode,
        collectionNode,
        indexNode,
        serverNode,
        routerNode,
        replSetNode,
        docBuilderDialog,
        docFieldText
    }
    ///////////////////////////////////////
    // directories
    ///////////////////////////////////////
    static Directory _xmlDir;
    static Directory _confDir;

    static Directory getXmlDir() {
        if (_xmlDir == null) {
            _xmlDir = new Directory("xml", "xml", true, File.values(), Common.Ext.xml);
        }
        return _xmlDir;
    }

    static Directory getConfDir() {
        if (_confDir == null) {
            _confDir = new Directory("conf", "conf", false, File.values(), Common.Ext.xml);
        }
        return _confDir;
    }
}
