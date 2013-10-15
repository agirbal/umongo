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

import com.edgytech.swingfast.EnumListener;
import com.edgytech.swingfast.MemoryBar;
import com.edgytech.swingfast.ToolBar;
import com.edgytech.swingfast.XmlComponentUnit;
import com.edgytech.umongo.MainToolBar.Item;

/**
 *
 * @author antoine
 */
public class MainToolBar extends ToolBar implements EnumListener<Item> {

    enum Item {

        memBar
    }

    public MainToolBar() {
        setEnumBinding(Item.values(), this);
    }

    public void start() {
        ((MemoryBar) getBoundUnit(Item.memBar)).start();
    }

    public void stop() {
        ((MemoryBar) getBoundUnit(Item.memBar)).stop();
    }

    public void actionPerformed(Item enm, XmlComponentUnit unit, Object src) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
