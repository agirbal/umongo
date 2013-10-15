/**
 *      Copyright (C) 2010 EdgyTech LLC.
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

import com.edgytech.swingfast.Common;
import com.edgytech.swingfast.Directory;

/**
 *
 * @author antoine
 */
public class Resource {

    enum File {

        umongo,
        docView,
        textView,
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
        docBuilderField,
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
