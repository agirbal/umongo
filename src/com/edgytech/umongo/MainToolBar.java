/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
