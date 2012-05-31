/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.edgytech.umongo;

import com.edgytech.swingfast.BoxPanel;
import com.edgytech.swingfast.Common.Axis;
import com.edgytech.swingfast.XmlJComponentUnit;
import com.edgytech.swingfast.XmlUnit;
import java.awt.Component;
import javax.swing.JLabel;

/**
 *
 * @author antoine
 */
public class Workspace extends XmlJComponentUnit<XmlUnit, BoxPanel> {

    private XmlJComponentUnit content;

    public Workspace() {
    }

    public XmlJComponentUnit getContent() {
        return content;
    }

    public void setContent(XmlJComponentUnit content) {
        this.content = content;
        structureComponent();
        if (content != null) {
            content.updateComponent();
        }
    }

    ////////////////////////////////////////////////////////////////////////
    // Component
    ////////////////////////////////////////////////////////////////////////
    @Override
    protected BoxPanel createComponent() {
        BoxPanel panel = new BoxPanel(Axis.Y, false);
        return panel;
    }

    @Override
    protected void structureComponentCustom(BoxPanel comp) {
        Component view = null;
        if (content != null) {
            view = content.getComponent();
        } else {
            view = new JLabel("Click on a Tree node to view");
        }
        comp.removeAll();
        comp.add(view);
    }

    @Override
    protected void updateComponentCustom(BoxPanel comp) {
    }

    @Override
    protected void commitComponentCustom(BoxPanel comp) {
    }
}
