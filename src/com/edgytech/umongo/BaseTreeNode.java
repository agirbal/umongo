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

import com.edgytech.swingfast.SwingFast;
import com.edgytech.swingfast.TreeNodeLabel;
import com.mongodb.MongoException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 *
 * @author antoine
 */
public abstract class BaseTreeNode extends TreeNodeLabel {
    ImageIcon cachedIcon;

    @Override
    protected void structureComponentCustom(JComponent old) {
        removeAllChildren();
        try {
            populateChildren();
        } catch (Exception e) {
            getLogger().log(Level.WARNING, e.getMessage(), e);
        }
        super.structureComponentCustom(old);
        getTree().expandNode(this);
    }

    @Override
    protected void updateComponentCustom(JComponent comp) {
        cachedIcon = SwingFast.createIcon(icon, iconGroup);
        List<ImageIcon> overlays = new ArrayList<ImageIcon>();
        try {
            long start = System.currentTimeMillis();
            updateNode(overlays);
            getLogger().log(Level.FINE, "Node " + toString() + " took " + (System.currentTimeMillis() - start));

        } catch (MongoException e) {
            if (e.getCode() == 10057 || e.getMessage().startsWith("unauthorized"))
                overlays.add(SwingFast.createIcon("overlay/lock.png", iconGroup));
            else
                overlays.add(SwingFast.createIcon("overlay/error.png", iconGroup));
            getLogger().log(Level.FINE, null, e);
        } catch (Exception e) {
            overlays.add(SwingFast.createIcon("overlay/error.png", iconGroup));
            getLogger().log(Level.FINE, null, e);
        }
        if (overlays.size() > 0)
            cachedIcon = SwingFast.createOverlayIcon(cachedIcon, overlays);
        super.updateComponentCustom(comp);
    }

    public TreeNodeLabel getParentNode() {
        return (TreeNodeLabel) ((DefaultMutableTreeNode) getTreeNode().getParent()).getUserObject();
    }

    protected abstract void populateChildren();

    public void removeNode() {
        TreeNodeLabel parent = getParentNode();
        parent.removeChild(this);
        parent.structureComponent();
    }

    @Override
    public void handleNodeSelection() {
        UMongo.instance.displayNode(this);
    }

    @Override
    public Icon getIcon() {
        return cachedIcon;
    }

    protected abstract void updateNode(List<ImageIcon> overlays);
}
