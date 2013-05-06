/**
 *      Copyright (C) 2010 EdgyTech Inc.
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
            view = new JLabel("");
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
