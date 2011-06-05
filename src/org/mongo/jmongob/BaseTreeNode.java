/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mongo.jmongob;

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
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;

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
            getLogger().log(Level.WARNING, null, e);
        }
        super.structureComponentCustom(old);
        getTree().expandNode(this);
    }

    @Override
    protected void updateComponentCustom(JComponent comp) {
        cachedIcon = SwingFast.createIcon(icon, iconGroup);
        List<ImageIcon> overlays = new ArrayList<ImageIcon>();
        try {
            updateNode(overlays);
        } catch (MongoException e) {
            if (e.getCode() == 10057)
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
        JMongoBrowser.instance.displayNode(this);
    }

    @Override
    public Icon getIcon() {
        return cachedIcon;
    }

    protected abstract void updateNode(List<ImageIcon> overlays);
}
