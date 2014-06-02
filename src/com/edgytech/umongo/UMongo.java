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

import com.edgytech.swingfast.Application;
import com.edgytech.swingfast.ConfirmDialog;
import com.edgytech.swingfast.Frame;
import com.edgytech.swingfast.MenuItem;
import com.edgytech.swingfast.Scroller;
import com.edgytech.swingfast.TabbedDiv;
import com.edgytech.swingfast.Tree;
import com.edgytech.swingfast.XmlJComponentUnit;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoException;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.jar.JarFile;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.xml.sax.SAXException;

/**
 *
 * @author antoine
 */
public class UMongo extends Application implements Runnable {

    enum Item {

        workspace,
        workspaceScroll,
        tree,
        treeScroll,
        mainMenu,
        mainToolBar,
        frame,
        globalStore,
        docViewDialog,
        docTree,
        tabbedResult,
        jobBar,
    }
    public final static UMongo instance = new UMongo();
    private ArrayList<MongoNode> mongos = new ArrayList<MongoNode>();
    boolean stopped = false;
    BaseTreeNode node = null;
    FileWriter activityLogWriter = null;
    boolean activityLogFirstResult = false;
    Handler applicationLogHandler = null;
    String pluginFolder = null;
    BinaryDecoder binaryDecoder = null;

    public UMongo() {
        super(true);
        setEnumBinding(Item.values(), null);
    }

    Frame getFrame() {
        return (Frame) getBoundUnit(Item.frame);
    }

    Workspace getWorkspace() {
        return (Workspace) getBoundUnit(Item.workspace);
    }

    Scroller getWorkspaceScroll() {
        return (Scroller) getBoundUnit(Item.workspaceScroll);
    }

    Tree getTree() {
        return (Tree) getBoundUnit(Item.tree);
    }

    MainMenu getMainMenu() {
        return (MainMenu) getBoundUnit(Item.mainMenu);
    }

    PreferencesDialog getPreferences() {
        return getMainMenu().getPreferences();
    }

    MainToolBar getMainToolBar() {
        return (MainToolBar) getBoundUnit(Item.mainToolBar);
    }

    public void load() throws IOException, SAXException {
        xmlLoad(Resource.getXmlDir(), Resource.File.umongo, null);
    }

    public void loadSettings() throws IOException, SAXException {
        try {
            xmlLoad(Resource.getConfDir(), Resource.File.umongo, null);
        } catch (FileNotFoundException e) {
            // means no custom setting
        }
    }

    public void saveSettings() {
        try {
            xmlSave(Resource.getConfDir(), Resource.File.umongo, null);
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    @Override
    public void initialize() {
        try {
            load();
            loadSettings();
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, null, ex);
        }

        Thread maintenance = new Thread(this);
        maintenance.setDaemon(true);
        maintenance.start();
    }

    @Override
    public void wrapUp() {
        saveSettings();
    }

    public void start() {
        getLogger().log(Level.INFO, "UMONGO STARTING");
    }

    public void stop() {
        getLogger().log(Level.INFO, "UMONGO STOPPING");
        stopped = true;
    }

    public static void main(String[] args) {
//        LogManager.getLogManager().getLogger("").setLevel(Level.FINE);

        instance.launch();
    }

    GlobalStore getGlobalStore() {
        return (GlobalStore) getBoundUnit(Item.globalStore);
    }

    void addMongoClient(MongoClient mongo, List<String> dbs) throws MongoException, UnknownHostException {
        MongoNode node = new MongoNode(mongo, dbs);
        getTree().addChild(node);
        mongos.add(node);
        getTree().structureComponent();
        getTree().expandNode(node);
        getTree().selectNode(node);
    }

    void disconnect(MongoNode node) {
        mongos.remove(node);

        node.removeNode();
        MongoClient mongo = ((MongoNode) node).getMongoClient();
        mongo.close();

        if (mongos.size() > 0) {
            MongoNode other = mongos.get(0);
            getTree().expandNode(other);
            getTree().selectNode(other);
        } else {
            displayElement(null);
        }

    }

    public ArrayList<MongoNode> getMongos() {
        return mongos;
    }

    void displayElement(XmlJComponentUnit unit) {
        getWorkspace().setContent(unit);
    }

    void showError(String in, Exception ex) {
        ErrorDialog dia = getGlobalStore().getErrorDialog();
        dia.setException(ex, in);
        dia.show();
    }

    TabbedDiv getTabbedResult() {
        return (TabbedDiv) getBoundUnit(Item.tabbedResult);
    }
    
    void removeAllTabs() {
        TabbedDiv tabs = getTabbedResult();
        tabs.removeAllChildren();
        tabs.structureComponent();
    }

    JobBar getJobBar() {
        return (JobBar) getBoundUnit(Item.jobBar);
    }

    public void displayNode(BaseTreeNode node) {
        this.node = node;
        BasePanel panel = null;
        if (node instanceof MongoNode) {
            panel = getGlobalStore().getMongoPanel();
        } else if (node instanceof DbNode) {
            panel = getGlobalStore().getDbPanel();
        } else if (node instanceof CollectionNode) {
            panel = getGlobalStore().getCollectionPanel();
        } else if (node instanceof IndexNode) {
            panel = getGlobalStore().getIndexPanel();
        } else if (node instanceof ServerNode) {
            panel = getGlobalStore().getServerPanel();
        } else if (node instanceof RouterNode) {
            panel = getGlobalStore().getRouterPanel();
        } else if (node instanceof ReplSetNode) {
            panel = getGlobalStore().getReplSetPanel();
        }

        panel.setNode(node);
        // cant load checkpoint here, means all dialogs get reset
//        panel.xmlLoadCheckpoint();
        displayElement(panel);
    }

    public BaseTreeNode getNode() {
        return node;
    }

    public void runJob(DbJob job) {
        getJobBar().addJob(job);
        job.start();
    }

    public void removeJob(DbJob job) {
        getJobBar().removeJob(job);
    }
    
    long _nextTreeUpdate = System.currentTimeMillis();
    ConcurrentLinkedQueue<BaseTreeNode> nodesToRefresh = new ConcurrentLinkedQueue<BaseTreeNode>();

    void addNodeToRefresh(BaseTreeNode node) {
        nodesToRefresh.add(node);
    }

    public void run() {
        while (!stopped) {
            try {
                long now = System.currentTimeMillis();
                int treeRate = getPreferences().getTreeUpdateRate();
                if (treeRate > 0 && _nextTreeUpdate < now) {
                    _nextTreeUpdate = now + treeRate;
                    SwingUtilities.invokeAndWait(new Runnable() {
                        public void run() {
                            long start = System.currentTimeMillis();
                            // need structure here, to trigger refresh()
                            getTree().structureComponent();
                            getLogger().log(Level.FINE, "Tree update took " + (System.currentTimeMillis() - start));
//                           getTree().structureComponent();
                        }
                    });
                }

                BaseTreeNode node = null;
                while ((node = nodesToRefresh.poll()) != null) {
                    node.refresh();
                    final BaseTreeNode fnode = node;
                    SwingUtilities.invokeAndWait(new Runnable() {
                        public void run() {
                            fnode.updateComponent(false);
                        }
                    });
                }

            } catch (Exception ex) {
                getLogger().log(Level.SEVERE, null, ex);
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                getLogger().log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public void windowClosing(WindowEvent e) {
        if (getJobBar().hasChildren()) {
            String text = "There are jobs running, force exit?";
            ConfirmDialog confirm = new ConfirmDialog(null, "Confirm Exit", null, text);
            if (!confirm.show()) {
                return;
            }
        }
        super.windowClosing(e);
    }

    void updateLogging() {
        synchronized (this) {
            try {
                Handler handler = getPreferences().getApplicationLogHandler();
                if (handler == null) {
                    if (applicationLogHandler != null) {
                        Logger.getLogger("").removeHandler(applicationLogHandler);
                    }
                } else {
                    if (applicationLogHandler == null) {
                        Logger.getLogger("").addHandler(handler);
                    } else if (!applicationLogHandler.equals(handler)) {
                        Logger.getLogger("").removeHandler(applicationLogHandler);
                        Logger.getLogger("").addHandler(handler);
                    }
                }
                applicationLogHandler = handler;

                String logFile = getPreferences().getActivityLogFile();
                if (logFile != null) {
                    if (activityLogWriter != null) {
                        activityLogWriter.close();
                    }
                    activityLogWriter = new FileWriter(logFile, true);
                    activityLogFirstResult = getPreferences().getActivityLogFirstResult();
                } else {
                    if (activityLogWriter != null) {
                        activityLogWriter.close();
                    }
                    activityLogWriter = null;
                }
            } catch (IOException ex) {
                getLogger().log(Level.WARNING, null, ex);
            }
        }
    }

    boolean isLoggingOn() {
        return activityLogWriter != null;
    }

    boolean isLoggingFirstResultOn() {
        return activityLogFirstResult;
    }

    void logActivity(DBObject obj) {
        synchronized (this) {
            try {
                if ("Auth".equals(obj.get("name"))) {
                    // dont log auth
                    return;
                }

                activityLogWriter.write(MongoUtils.getJSON(obj));
                activityLogWriter.write("\n");
                activityLogWriter.flush();
            } catch (Exception ex) {
                getLogger().log(Level.WARNING, null, ex);
            }
        }
    }

    void updatePlugins() {
        String folder = getPreferences().getPluginFolder();
        if (folder != null && folder.equals(pluginFolder)) {
            return;
        }

        pluginFolder = folder;
        resetPlugins();
        if (folder == null) {
            return;
        }

//        System.setSecurityManager(new PluginSecurityManager(pluginFolder));
        
        //System.getProperty("user.dir")
        File dir = new File(pluginFolder);
        PluginClassLoader cl = new PluginClassLoader(dir);
        if (dir.exists() && dir.isDirectory()) {
            // we'll only load classes directly in this directory -
            // no subdirectories, and no classes in packages are recognized
            File[] files = dir.listFiles();
            for (int i = 0; i < files.length; i++) {
                try {
                    List<Class> classes = null;
                    
                    File file = files[i];
                    if (file.getPath().endsWith(".jar")) {
                        getLogger().info("Attempting to load plugin " + file.getPath());
                        JarFile jar = new JarFile(file);
                        classes = cl.loadClasses(jar);
                    }
                    
/*                    // only consider files ending in ".class"
                    if (!files[i].endsWith(".class")) {
                        continue;
                    }

                    getLogger().info("Attempting to load plugin " + files[i]);
                    Class c = cl.loadClass(files[i].substring(0, files[i].indexOf("."))); */

                    for (Class c : classes) {
                        Class[] intf = c.getInterfaces();
                        for (int j = 0; j < intf.length; j++) {
                            if (intf[j].getName().equals("com.edgytech.umongo.BinaryDecoder")) {
                                getLogger().info("Detected BinaryDecoder plugin in " + c.getCanonicalName());
                                // the following line assumes that PluginFunction has a no-argument constructor
                                BinaryDecoder bd = (BinaryDecoder) c.newInstance();
                                binaryDecoder = bd;
                            }
                        }
                    }
                } catch (Exception ex) {
                    getLogger().log(Level.WARNING, null, ex);
                }
            }
        }

    }

    void resetPlugins() {
        binaryDecoder = null;
    }

    BinaryDecoder getBinaryDecoder() {
        return binaryDecoder;
    }

    @Override
    public void handleMacAbout() {
        ((MenuItem)getMainMenu().getBoundUnit(MainMenu.Item.about)).getButton().doClick();
    }
}
