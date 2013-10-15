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

import com.edgytech.swingfast.IconGroup;
import com.edgytech.swingfast.SwingFast;
import com.edgytech.swingfast.Tree;
import com.edgytech.swingfast.TreeNodeLabel;
import com.edgytech.swingfast.XmlUnit;
import com.mongodb.MongoException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    boolean hasExpanded = false;
    private Set<String> overlays = new HashSet<String>();

    @Override
    protected void structureComponentCustom(JComponent old) {
        long time = System.currentTimeMillis();

        removeAllChildren();
        try {
            XmlUnit parent = getParent();
            if (parent instanceof Tree || ((BaseTreeNode) parent).hasExpanded) {
                UMongo.instance.addNodeToRefresh(this);
                populateChildren();
            }
        } catch (Exception e) {
            getLogger().log(Level.WARNING, e.getMessage(), e);
        }
        super.structureComponentCustom(old);
        // node shouldnt be expanded always, otherwise can't use lazy load
//        getTree().expandNode(this);

//        System.out.println("Called structure for " + this.getClass().getName() + " " + this.hashCode() + " " + (System.currentTimeMillis() - time));
    }

    @Override
    protected void updateComponentCustom(JComponent comp) {
        updateNode();

        cachedIcon = SwingFast.createIcon(icon, iconGroup);
        if (overlays.size() > 0) {
            List<ImageIcon> icons = new ArrayList<ImageIcon>();
            for (String str : overlays) {
                icons.add(SwingFast.createIcon(str, iconGroup));
            }
            cachedIcon = SwingFast.createOverlayIcon(cachedIcon, icons);
        }

        long time = System.currentTimeMillis();
        super.updateComponentCustom(comp);
//        System.out.println("Called update for " + this.getClass().getName() + " " + this.hashCode() + " " + (System.currentTimeMillis() - time));
    }

    void refresh() {
        clearOverlays();

        try {
            refreshNode();
        } catch (MongoException e) {
//            System.out.println(e.getMessage());
            if (e.getCode() == 10057 || e.getMessage().contains("unauthorized") || e.getMessage().contains("not authorized")) {
                addOverlay("overlay/lock_tiny.png");
            } else {
                addOverlay("overlay/error.png");
            }
            getLogger().log(Level.FINE, null, e);
        } catch (Exception e) {
            addOverlay("overlay/error.png");
            getLogger().log(Level.FINE, null, e);
        }
    }

    public TreeNodeLabel getParentNode() {
        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) getTreeNode().getParent();
        if (parent == null) {
            return null;
        }
        return (TreeNodeLabel) parent.getUserObject();
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

    protected abstract void updateNode();

    protected abstract void refreshNode();

    void clearOverlays() {
        overlays.clear();
    }

    void addOverlay(String str) {
        overlays.add(str);
    }

    @Override
    public void handleWillExpand() {
        if (!hasExpanded) {
            hasExpanded = true;
            // the children need to restructure
            // ideally would call structure directly on children
            structureComponent();
        }
    }
}
