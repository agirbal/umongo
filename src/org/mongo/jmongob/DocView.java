/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mongo.jmongob;

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
        document,
        remove,
        duplicate,
        dupCount,
        update,
        upUpdate,
        upMulti,
        popUp,
        popUpdate,
        popDuplicate,
        popRemove,
        tools,
        startAutoUpdate,
        stopAutoUpdate,
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
            addDocument(doc, job);
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
            ((Menu) getBoundUnit(Item.document)).enabled = true;
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

    public void close() {
        if (dbcursor != null) {
            dbcursor.close();
            dbcursor = null;
        }
        tabbedDiv.removeChild(this);
        tabbedDiv.structureComponent();
    }

    void addToTabbedDiv() {
        tabbedDiv = JMongoBrowser.instance.getTabbedResult();
        tabbedDiv.addChild(this);
        tabbedDiv.structureComponent();
        tabbedDiv.selectLastTab();

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
                } else if (obj instanceof DBObjectWrapper) {
                    doc = ((DBObjectWrapper) obj).getDBObject();
                } else if (obj instanceof TreeNodeDBObject) {
                    doc = ((TreeNodeDBObject) obj).getDBObject();
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
                    getTree().expandNode(getTree().getTreeNode());

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
        return (DefaultMutableTreeNode) path.getPathComponent(1);
    }

    public DBObject getSelectedDocument() {
        TreePath path = getTree().getSelectionPath();
        if (path == null || path.getPathCount() < 2) {
            return null;
        }
        DefaultMutableTreeNode node = getSelectedNode();
        Object obj = node.getUserObject();
        if (obj instanceof DBObjectWrapper) {
            return ((DBObjectWrapper) obj).getDBObject();
        } else if (obj instanceof DBObject) {
            return (DBObject) obj;
        } else if (obj instanceof TreeNodeDBObject) {
            return ((TreeNodeDBObject) obj).getDBObject();
        }
        return null;
    }

    public void remove() {
        DBObject doc = getSelectedDocument();
        if (doc == null) {
            return;
        }

        if (!((MenuItem) getBoundUnit(Item.remove)).getDialog().show()) {
            return;
        }

        if (dbcursor == null) {
            getTree().removeChild((TreeNodeDBObject) getSelectedNode().getUserObject());
            getTree().structureComponent();
            getTree().expandNode(getTree().getTreeNode());
            return;
        }

        // go by _id if possible
        final DBObject query = doc.containsField("_id") ? new BasicDBObject("_id", doc.get("_id")) : doc;
        new DbJob() {

            @Override
            public Object doRun() throws IOException {
                return dbcursor.getCollection().remove(query);
            }

            @Override
            public String getNS() {
                return dbcursor.getCollection().getFullName();
            }

            @Override
            public String getShortName() {
                return "Remove";
            }

            @Override
            public void wrapUp(Object res) {
                super.wrapUp(res);
                updateCursor();
            }

            @Override
            public Object getRoot(Object result) {
                return query;
            }
        }.addJob();
    }

//    public void duplicate() {
//        final DBObject doc = getSelectedDocument();
//        if (doc == null) {
//            return;
//        }
//        final int num = getIntFieldValue(Item.dupCount);
//
//        if (dbcursor == null) {
//            for (int i = 0; i < num; ++i) {
//                DBObject newdoc = new BasicDBObject(doc.toMap());
//                newdoc.removeField("_id");
//                addDocument(newdoc);
//            }
//            getTree().structureComponent();
//            getTree().expandNode(getTree().getTreeNode());
//            return;
//        }
//
//        new DbJob() {
//
//            @Override
//            public Object doRun() throws IOException {
//                List list = new ArrayList(num);
//                for (int i = 0; i < num; ++i) {
//                    DBObject newdoc = new BasicDBObject(doc.toMap());
//                    newdoc.removeField("_id");
//                    list.add(newdoc);
//                }
//                return dbcursor.getCollection().insert(list);
//            }
//
//            @Override
//            public String getNS() {
//                return dbcursor.getCollection().getFullName();
//            }
//
//            @Override
//            public String getShortName() {
//                return "Duplicate";
//            }
//
//            @Override
//            public void wrapUp(Object res) {
//                super.wrapUp(res);
//                updateCursor();
//            }
//
//            @Override
//            public Object getRoot(Object result) {
//                return doc;
//            }
//        }.addJob();
//    }

    private void updateCursor() {
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

    public void update() {
        final DBObject doc = getSelectedDocument();
        if (doc == null) {
            return;
        }

        ((DocBuilderField) getBoundUnit(Item.upUpdate)).setDBObject((BasicDBObject) doc);
        if (!((MenuItem) getBoundUnit(Item.update)).getDialog().show()) {
            return;
        }

        final DBObject query = doc.containsField("_id") ? new BasicDBObject("_id", doc.get("_id")) : doc;
        final DBObject update = ((DocBuilderField) getBoundUnit(Item.upUpdate)).getDBObject();
        final boolean multi = getBooleanFieldValue(Item.upMulti);

        if (dbcursor == null) {
            getTree().removeChild((TreeNodeDBObject) getSelectedNode().getUserObject());
            addDocument(update, null);
            getTree().structureComponent();
            getTree().expandNode(getTree().getTreeNode());
            return;
        }

        final DBCollection col = dbcursor.getCollection();
        new DbJob() {

            @Override
            public Object doRun() {
                return col.update(query, update, false, multi);
            }

            @Override
            public String getNS() {
                return col.getFullName();
            }

            @Override
            public String getShortName() {
                return "Update";
            }

            @Override
            public Object getRoot(Object result) {
                StringBuilder sb = new StringBuilder();
                sb.append("query=").append(query);
                sb.append(", update=").append(update);
                sb.append(", multi=").append(multi);
                return sb.toString();
            }

            @Override
            public void wrapUp(Object res) {
                super.wrapUp(res);
                updateCursor();
            }
        }.addJob();
    }

    private void addDocument(DBObject doc, DbJob job) {
        TreeNodeLabel node = new TreeNodeDBObject(doc, job);
        final PopUpMenu popUp = (PopUpMenu) getBoundUnit(Item.popUp);
        node.setPopUpMenu(popUp);
        getTree().addChild(node);
    }

//    protected void appendDoc(DBObject doc) {
//        TreeNodeLabel node = new TreeNodeLabel();
//        node.forceTreeNode(MongoUtils.dbObjectToTreeNode(doc));
//        getTree().addChild(node);
//    }
}
