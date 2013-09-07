/**
 * Copyright (C) 2010 EdgyTech Inc.
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

import com.edgytech.swingfast.*;
import java.awt.Component;
import java.awt.Rectangle;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import javax.swing.SwingUtilities;

/**
 *
 * @author antoine
 */
public class TextView extends Zone implements EnumListener, TabInterface, Runnable {

    enum Item {

        textArea,
        tabDiv,
        tabTitle,
        tabClose,
        refresh,
        export,
        tools,
        startAutoUpdate,
        stopAutoUpdate
    }

    TabbedDiv tabbedDiv;
    Thread updateThread;
    int updateInterval;
    int updateCount;
    boolean running;
    DbJob job;

    public TextView(String id, String label, DbJob job) {
        try {
            xmlLoad(Resource.getXmlDir(), Resource.File.textView, null);
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, null, ex);
        }
        setEnumBinding(Item.values(), this);

        setId(id);
        setLabel(label);
        this.job = job;
        setStringFieldValue(Item.tabTitle, label);

        ((MenuItem) getBoundUnit(Item.refresh)).enabled = true;
        ((MenuItem) getBoundUnit(Item.startAutoUpdate)).enabled = true;
    }

    /**
     * create a doc view with static document
     *
     * @param id
     * @param label
     * @param job
     * @param root
     * @param doc
     */
    public TextView(String id, String label, DbJob job, String text) {
        this(id, label, job);

        if (text != null) {
            setContent(text);
        }
    }

    public void close(ButtonBase button) {
        tabbedDiv.removeTab(this);
    }

    void addToTabbedDiv() {
        tabbedDiv = UMongo.instance.getTabbedResult();
        tabbedDiv.addTab(this, true);
    }

    public void actionPerformed(Enum enm, XmlComponentUnit unit, Object src) {
    }

//    public void export(ButtonBase button) throws IOException {
//        // export should be run in thread, to prevent concurrent mods
//        ExportDialog dia = UMongo.instance.getGlobalStore().getExportDialog();
//        if (!dia.show()) {
//            return;
//        }
//        final DocumentSerializer ds = dia.getDocumentSerializer();
//        try {
//            DefaultMutableTreeNode root = getTree().getTreeNode();
//            for (int i = 0; i < root.getChildCount(); ++i) {
//                DefaultMutableTreeNode child = (DefaultMutableTreeNode) root.getChildAt(i);
//                Object obj = child.getUserObject();
//                DBObject doc = null;
//                if (obj instanceof DBObject) {
//                    doc = (DBObject) obj;
//                } else if (obj instanceof TreeNodeDocumentField) {
//                    doc = (DBObject) ((TreeNodeDocumentField) obj).getValue();
//                } else if (obj instanceof TreeNodeDocument) {
//                    doc = ((TreeNodeDocument) obj).getDBObject();
//                }
//                if (doc != null) {
//                    ds.writeObject(doc);
//                }
//            }
//        } finally {
//            ds.close();
//        }
//    }

    public Component getTabComponent() {
        return getComponentBoundUnit("tabDiv").getComponent();
    }

    TextArea getTextArea() {
        return (TextArea) getBoundUnit(Item.textArea);
    }
    
    public void startAutoUpdate(ButtonBase button) {
        AutoUpdateDialog dia = UMongo.instance.getGlobalStore().getAutoUpdateDialog();
        if (!dia.show()) {
            return;
        }

        if (updateThread != null) {
            stopAutoUpdate(null);
        }

        updateThread = new Thread(this);
        updateInterval = dia.getComponentIntFieldValue(AutoUpdateDialog.Item.autoInterval);
        updateCount = dia.getComponentIntFieldValue(AutoUpdateDialog.Item.autoCount);
        running = true;
        updateThread.start();

        getComponentBoundUnit(Item.stopAutoUpdate).enabled = true;
        getComponentBoundUnit(Item.stopAutoUpdate).updateComponent();
    }

    public void stopAutoUpdate(ButtonBase button) {
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
                DbJob job = getRefreshJob();
                final DbJob fjob = job;

                SwingUtilities.invokeAndWait(new Runnable() {

                    @Override
                    public void run() {
                        fjob.addJob();
                    }
                });

                try {
                    fjob.join();
                } catch (InterruptedException ex) {
                    getLogger().log(Level.WARNING, null, ex);
                } catch (ExecutionException ex) {
                    getLogger().log(Level.WARNING, null, ex);
                }

                if (updateCount > 0 && ++i >= updateCount) {
                    break;
                }
                
                Thread.sleep(updateInterval * 1000);

            } catch (Exception ex) {
                getLogger().log(Level.SEVERE, null, ex);
            }
        }
        getLogger().log(Level.INFO, "Ran " + i + " updates");
    }

    public void refresh(ButtonBase button) {
        getRefreshJob().addJob();
    }
        
    public DbJob getRefreshJob() {
        if (job == null) {
            return null;
        }

        DbJob newJob = new DbJob() {

            String result;

            @Override
            public Object doRun() throws Exception {
                result = (String) job.doRun();
                return null;
            }

            @Override
            public String getNS() {
                return job.getNS();
            }

            @Override
            public String getShortName() {
                return job.getShortName();
            }

            @Override
            public void wrapUp(Object res) {
                super.wrapUp(res);
                if (res == null && result != null) {
//                    setContent(result);
//                    getTextArea().updateComponent();
                    appendContent(result);
                    
                    // panel info may need to be refreshed
                    if (job.getPanel() != null) {
                        job.getPanel().refresh();
                    }
                }
            }
        };
        
        return newJob;
    }

    public void setContent(String text) {
        setStringFieldValue(Item.textArea, text);
    }
    
    /**
     * Following method doesnt really work because the log buffer is a short moving window
     */
    public void swapContent(String text) {
        TextArea ta = (TextArea) getBoundUnit(Item.textArea);
//        int pos = ta.getTextArea().getCaretPosition();
        int val = ta.getComponent().getVerticalScrollBar().getValue();
        int max = ta.getComponent().getVerticalScrollBar().getMaximum();
        int vis = ta.getComponent().getVerticalScrollBar().getVisibleAmount();
        
        setStringFieldValue(Item.textArea, text);
        updateComponent();

        // scroll resets, set it back if not to close to the end
        System.out.println(val + " " + max + " " + vis);
        if (val < max - vis)
            ta.getComponent().getVerticalScrollBar().setValue(val);
    }
    
    public void appendContent(String text) {
        final TextArea ta = (TextArea) getBoundUnit(Item.textArea);
        // since logs are moving window, need to find the spot within new log
        String old = ta.getComponentStringValue();
        int last = old.lastIndexOf("\n", old.length() - 5);
        String lastLine = old.substring(last + 1);
        int pos = text.indexOf(lastLine);
        if (pos >= 0)
            text = text.substring(pos + lastLine.length());

        final int val = ta.getComponent().getVerticalScrollBar().getValue();
        int max = ta.getComponent().getVerticalScrollBar().getMaximum();
        int vis = ta.getComponent().getVerticalScrollBar().getVisibleAmount();
        Rectangle rect = ta.getComponent().getVisibleRect();
            
        ta.getTextArea().append(text);

        // scroll resets, set it back if not to close to the end, needs to be done at later time in EVT
        if (val < max - 2 * vis) {
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    ta.getComponent().getVerticalScrollBar().setValue(val);
                }
            });

        }
        
    }

}
