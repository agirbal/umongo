/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mongo.jmongob;

import com.edgytech.swingfast.Application;
import com.edgytech.swingfast.Frame;
import com.edgytech.swingfast.Scroller;
import com.edgytech.swingfast.TabbedDiv;
import com.edgytech.swingfast.Tree;
import com.edgytech.swingfast.XmlJComponentUnit;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoException;
import com.mongodb.MongoURI;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.xml.sax.SAXException;

/**
 *
 * @author antoine
 */
public class JMongoBrowser extends Application implements Runnable {

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
        jobBar
    }
    public final static JMongoBrowser instance = new JMongoBrowser();
    private ArrayList<MongoNode> mongos = new ArrayList<MongoNode>();
    boolean stopped = false;

    public JMongoBrowser() {
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

    AppPreferences getPreferences() {
        return getMainMenu().getPreferences();
    }

    MainToolBar getMainToolBar() {
        return (MainToolBar) getBoundUnit(Item.mainToolBar);
    }

    public void load() throws IOException, SAXException {
        xmlLoad(Resource.getXmlDir(), Resource.File.JMongoBrowser, null);
    }

    public void loadSettings() throws IOException, SAXException {
        try {
            xmlLoad(Resource.getConfDir(), Resource.File.JMongoBrowser, null);
        } catch (FileNotFoundException e) {
            // means no custom setting
        }
    }

    public void saveSettings() {
        try {
            xmlSave(Resource.getConfDir(), Resource.File.JMongoBrowser, null);
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public void loadDefaultImageSettings() {
//        xmlLoad(Resource.getXmlDir(), Resource.File.JMongoBrowserConnect, "Image", XmlUnit.State.NORMAL);
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
    }

    public void stop() {
        stopped = true;
    }

    public static void main(String[] args) {
        instance.launch();
    }

    GlobalStore getGlobalStore() {
        return (GlobalStore) getBoundUnit(Item.globalStore);
    }

    void addMongo(Mongo mongo, List<String> dbs) throws MongoException, UnknownHostException {
        MongoNode node = new MongoNode(mongo, dbs);
        getTree().addChild(node);
        mongos.add(node);
        getTree().structureComponent();
        getTree().expandNode(node);
        getTree().selectNode(node);
    }

    void disconnect(MongoNode node) {
        mongos.remove(node);
        if (mongos.size() > 0) {
            displayNode(mongos.get(0));
        } else {
            displayElement(null);
        }

        node.removeNode();
        Mongo mongo = ((MongoNode) node).getMongo();
        mongo.close();
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

    public void displayNode(BaseTreeNode node) {
        BasePanel panel = null;
        if (node instanceof MongoNode)
            panel = getGlobalStore().getMongoPanel();
        else if(node instanceof DbNode)
            panel = getGlobalStore().getDbPanel();
        else if(node instanceof CollectionNode)
            panel = getGlobalStore().getCollectionPanel();
        else if(node instanceof IndexNode)
            panel = getGlobalStore().getIndexPanel();
        else if(node instanceof ServerNode)
            panel = getGlobalStore().getServerPanel();
        else if(node instanceof RouterNode)
            panel = getGlobalStore().getRouterPanel();
        else if(node instanceof ReplSetNode)
            panel = getGlobalStore().getReplSetPanel();

        panel.setNode(node);
        displayElement(panel);
    }

    public void runJob(DbJob job) {
        ((JobBar) getBoundUnit(Item.jobBar)).addJob(job);
        job.start();
    }

    public void removeJob(DbJob job) {
        ((JobBar) getBoundUnit(Item.jobBar)).removeJob(job);
    }

    long _nextTreeUpdate = System.currentTimeMillis();

    public void run() {
        while (!stopped) {
            try {
                long now = System.currentTimeMillis();
                int treeRate = getPreferences().getTreeUpdateRate();
                if (treeRate > 0 && _nextTreeUpdate < now) {
                    _nextTreeUpdate = now + treeRate;
                    SwingUtilities.invokeAndWait(new Runnable() {
                        public void run() {
                           getTree().updateComponent();
//                           getTree().structureComponent();
                        }
                    });
                }
            } catch (Exception ex) {
                getLogger().log(Level.SEVERE, null, ex);
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                getLogger().log(Level.SEVERE, null, ex);
            }
        }
    }
}
