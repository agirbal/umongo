/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.edgytech.jmongobrowser;

import com.edgytech.swingfast.EnumListener;
import com.edgytech.swingfast.Menu;
import com.edgytech.swingfast.MenuItem;
import com.edgytech.swingfast.PopUpMenu;
import com.edgytech.swingfast.TabInterface;
import com.edgytech.swingfast.TabbedDiv;
import com.edgytech.swingfast.Tree;
import com.edgytech.swingfast.TreeNodeLabel;
import com.edgytech.swingfast.XmlComponentUnit;
import com.edgytech.swingfast.Zone;
import com.mongodb.BasicDBObject;
import com.mongodb.CommandResult;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import java.awt.Component;
import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Level;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

/**
 *
 * @author antoine
 */
public class DocView extends Zone implements EnumListener, TabInterface, Runnable {

    enum Item {

        docTree,
        tabDiv,
        tabTitle,
        tabClose,
        refresh,
        append,
        export,
        cursor,
        getMore,
        getAll,
        tools,
        startAutoUpdate,
        stopAutoUpdate,
        expandAll,
        collapseAll
    }
    DB db;
    DBObject cmd;
    Iterator<DBObject> iterator;
    DBCursor dbcursor;
    boolean busy = false;
    TabbedDiv tabbedDiv;
    Thread updateThread;
    String updateType;
    int updateInterval;
    int updateCount;
    boolean running;
    BasePanel panel;

    public DocView() {
        try {
            xmlLoad(Resource.getXmlDir(), Resource.File.docView, null);
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, null, ex);
        }
        setEnumBinding(Item.values(), this);
    }

    public DocView(String id, String label, DBObject doc, Object root, DbJob job) {
        this();
        setId(id);
        setLabel(label);

        setStringFieldValue(Item.tabTitle, label);
        getTree().label = root.toString();
        if (doc != null) {
            addDocument(doc, job, true);
        }
    }

    public DocView(String id, String label, Iterator<DBObject> iterator, Object root) {
        this();
        setId(id);
        setLabel(label);

        setStringFieldValue(Item.tabTitle, label);
        getTree().label = root.toString();
        if (iterator instanceof DBCursor) {
            this.dbcursor = (DBCursor) iterator;
            this.iterator = dbcursor;
            ((MenuItem) getBoundUnit(Item.refresh)).enabled = true;
            ((MenuItem) getBoundUnit(Item.startAutoUpdate)).enabled = true;
        } else {
            this.iterator = iterator;
        }
        getMore();
    }

    public DocView(String id, String label, DB db, String cmd) {
        this(id, label, db, new BasicDBObject(cmd, 1));
    }

    public DocView(String id, String label, DBCollection col, String cmd) {
        this(id, label, col.getDB(), new BasicDBObject(cmd, col.getName()));
    }

    public DocView(String id, String label, DB db, DBObject cmd) {
        this(id, label, db, cmd, null, null, null);
    }

    public DocView(String id, String label, DB db, DBObject cmd, DBObject result, BasePanel panel, DbJob job) {
        this(id, label, result, db.getName() + ": " + cmd, job);
        this.db = db;
        this.cmd = cmd;
        this.panel = panel;
        ((MenuItem) getBoundUnit(Item.startAutoUpdate)).enabled = true;
        ((MenuItem) getBoundUnit(Item.refresh)).enabled = true;
        ((MenuItem) getBoundUnit(Item.append)).enabled = true;
        if (result == null) {
            refresh();
        }
    }

    Tree getTree() {
        return (Tree) getBoundUnit(Item.docTree);
    }

    public DBCursor getDBCursor() {
        return dbcursor;
    }
    
    public void close() {
        if (dbcursor != null) {
            dbcursor.close();
            dbcursor = null;
        }
        tabbedDiv.removeTab(this);
    }

    void addToTabbedDiv() {
        tabbedDiv = JMongoBrowser.instance.getTabbedResult();
        tabbedDiv.addTab(this, true);        

        getTree().expandNode(getTree().getTreeNode());
    }

    public void actionPerformed(Enum enm, XmlComponentUnit unit, Object src) {
    }

    public void export() throws IOException {
        // export should be run in thread, to prevent concurrent mods
        ExportDialog dia = JMongoBrowser.instance.getGlobalStore().getExportDialog();
        if (!dia.show()) {
            return;
        }
        final ExportFile.ExportFileOutputStream os = dia.getOutputStream();
        try {
            DefaultMutableTreeNode root = getTree().getTreeNode();
            for (int i = 0; i < root.getChildCount(); ++i) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) root.getChildAt(i);
                Object obj = child.getUserObject();
                DBObject doc = null;
                if (obj instanceof DBObject) {
                    doc = (DBObject) obj;
                } else if (obj instanceof TreeNodeDocumentField) {
                    doc = (DBObject) ((TreeNodeDocumentField) obj).getValue();
                } else if (obj instanceof TreeNodeDocument) {
                    doc = ((TreeNodeDocument) obj).getDBObject();
                }
                if (doc != null) {
                    os.writeObject(doc);
                }
            }
        } finally {
            os.close();
        }
    }

    public Component getTabComponent() {
        return getComponentBoundUnit("tabDiv").getComponent();
    }

    public void startAutoUpdate() {
        AutoUpdateDialog dia = JMongoBrowser.instance.getGlobalStore().getAutoUpdateDialog();
        if (!dia.show()) {
            return;
        }

        if (updateThread != null) {
            stopAutoUpdate();
        }

        updateThread = new Thread(this);
        updateType = dia.getComponentStringFieldValue(AutoUpdateDialog.Item.autoType);
        updateInterval = dia.getComponentIntFieldValue(AutoUpdateDialog.Item.autoInterval);
        updateCount = dia.getComponentIntFieldValue(AutoUpdateDialog.Item.autoCount);
        running = true;
        updateThread.start();

        getComponentBoundUnit(Item.stopAutoUpdate).enabled = true;
        getComponentBoundUnit(Item.stopAutoUpdate).updateComponent();
    }

    public void stopAutoUpdate() {
        running = false;
        try {
            updateThread.interrupt();
            updateThread.join();
        } catch (InterruptedException ex) {
        }
        updateThread = null;

        getComponentBoundUnit(Item.stopAutoUpdate).enabled = false;
        getComponentBoundUnit(Item.stopAutoUpdate).updateComponent();
    }

    public void run() {
        int i = 0;
        while (running) {
            try {
                if ("Refresh".equals(updateType) || dbcursor != null) {
                    refresh();
                } else if ("Append".equals(updateType)) {
                    append();
                }

                if (updateCount > 0 && ++i >= updateCount) {
                    break;
                }
                Thread.sleep(updateInterval);
            } catch (Exception ex) {
                getLogger().log(Level.SEVERE, null, ex);
            }
        }
        getLogger().log(Level.INFO, "Ran " + i + " updates");
    }

    public void refresh() {
        if (dbcursor != null) {
            updateCursor();
        } else if (cmd != null) {
            refreshCmd(false);
        }
    }

    public void append() {
        if (cmd != null) {
            refreshCmd(true);
        }
    }

    public void refreshCmd(final boolean append) {
        if (db == null || cmd == null) {
            return;
        }
        if (busy) {
            return;
        }
        busy = true;
        new DbJob() {

            CommandResult result;

            @Override
            public Object doRun() {
                CommandResult res = db.command(cmd);
                res.throwOnError();
                result = res;
                return null;
            }

            @Override
            public String getNS() {
                return db.getName();
            }

            @Override
            public String getShortName() {
                return "Refresh";
            }

            @Override
            public void wrapUp(Object res) {
                busy = false;
                super.wrapUp(res);
                if (res == null && result != null) {
                    if (!append) {
                        getTree().removeAllChildren();
                    }
                    addDocument(result, this);
                    getTree().structureComponent();
                    // result of command should be fully expanded
                    getTree().expandAll();

                    // panel info may need to be refreshed
                    if (panel != null)
                        panel.refresh();
                }
            }
        }.addJob();
    }

    public void getMore(final int max) {
        if (busy) {
            return;
        }
        busy = true;
        new DbJob() {

            @Override
            public Object doRun() throws IOException {
                int i = 0;
                while (iterator.hasNext() && (i++ < max || max <= 0)) {
                    DBObject obj = iterator.next();
                    if (obj == null)
                        break;
                    addDocument(obj, null);
                }
                return null;
            }

            @Override
            public String getNS() {
                if (dbcursor != null) {
                    return dbcursor.getCollection().getFullName();
                }
                return getLabel();
            }

            @Override
            public String getShortName() {
                return "Get More";
            }

            @Override
            public void wrapUp(Object res) {
                busy = false;
                super.wrapUp(res);
                if (res == null) {
                    // res should be null
                    // should have a cursor id now
                    if (dbcursor != null) {
                        getTree().label = dbcursor.toString();
                    }
                    getTree().structureComponent();
                    getTree().expandNode(getTree().getTreeNode());

                    DocView.this.updateButtons();
                }
            }
        }.addJob();
    }

    public void getMore() {
        getMore(JMongoBrowser.instance.getPreferences().getGetMoreSize());
    }

    public void getAll() {
        getMore(0);
    }

    public DefaultMutableTreeNode getSelectedNode() {
        TreePath path = getTree().getSelectionPath();
        if (path == null || path.getPathCount() < 2) {
            return null;
        }
        return (DefaultMutableTreeNode) path.getLastPathComponent();
    }

    public DBObject getSelectedDocument() {
        TreePath path = getTree().getSelectionPath();
        if (path == null || path.getPathCount() < 2) {
            return null;
        }
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getPathComponent(1);
        Object obj = node.getUserObject();
        if (obj instanceof TreeNodeDocumentField) {
            return (DBObject) ((TreeNodeDocumentField) obj).getValue();
        } else if (obj instanceof DBObject) {
            return (DBObject) obj;
        } else if (obj instanceof TreeNodeDocument) {
            return ((TreeNodeDocument) obj).getDBObject();
        }
        return null;
    }
    
    String getSelectedDocumentPath() {
        TreePath path = getTree().getSelectionPath();
        String pathStr = "";
        if (path.getPathCount() < 2)
            return null;
        for (int i = 2; i < path.getPathCount(); ++i) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getPathComponent(i);
            String key = ((TreeNodeDocumentField) node.getUserObject()).getKey();
            pathStr += "." + key;
        }
        return pathStr.substring(1);
    }

    public void updateCursor() {
        if (dbcursor == null) {
            return;
        }
        if (busy) {
            return;
        }
        busy = true;
//        final int count = getTree().getChildren().size();
        new DbJob() {

            @Override
            public Object doRun() throws IOException {
                dbcursor = dbcursor.getCollection().find(dbcursor.getQuery(), dbcursor.getKeysWanted());
                return null;
            }

            @Override
            public String getNS() {
                return dbcursor.getCollection().getFullName();
            }

            @Override
            public String getShortName() {
                return "Update Cursor";
            }

            @Override
            public void wrapUp(Object res) {
                busy = false;
                super.wrapUp(res);
                if (res == null) {
                    iterator = dbcursor;
                    getTree().removeAllChildren();
                    getMore();
                }
            }
        }.addJob();
    }

    private void updateButtons() {
        boolean canGetMore = false;
        if (dbcursor != null) {
            // can always get more from tailable cursor
            canGetMore = iterator.hasNext() || dbcursor.getCursorId() > 0;
        } else {
            canGetMore = iterator.hasNext();
        }
        getComponentBoundUnit(Item.getMore).enabled = canGetMore;
        getComponentBoundUnit(Item.getMore).updateComponent();
        getComponentBoundUnit(Item.getAll).enabled = canGetMore;
        getComponentBoundUnit(Item.getAll).updateComponent();
    }

    public void addDocument(DBObject doc, DbJob job) {
        addDocument(doc, job, false);
    }

    public void addDocument(DBObject doc, DbJob job, boolean expand) {
        TreeNodeLabel node = new TreeNodeDocument(doc, job);
        getTree().addChild(node);
        if (expand)
            getTree().expandNode(node);
    }

//    protected void appendDoc(DBObject doc) {
//        TreeNodeLabel node = new TreeNodeLabel();
//        node.forceTreeNode(MongoUtils.dbObjectToTreeNode(doc));
//        getTree().addChild(node);
//    }
    
    public void collapseAll() {
        getTree().collapseAll();
        // need to reexpand root
        getTree().expandNode(getTree().getTreeNode());
    }

    public void expandAll() {
        getTree().expandAll();
    }
}
