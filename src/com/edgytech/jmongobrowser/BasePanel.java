/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.edgytech.jmongobrowser;

import com.edgytech.swingfast.Text;
import com.edgytech.swingfast.Zone;

/**
 *
 * @author antoine
 */
public class BasePanel extends Zone {
    BaseTreeNode node;

    public BaseTreeNode getNode() {
        return node;
    }

    public void setNode(BaseTreeNode node) {
        this.node = node;
    }

    public void refresh() {
        if (node == null)
            return;
        node.structureComponent();
        updateComponent();
    }

}
