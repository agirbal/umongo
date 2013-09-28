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

import com.edgytech.swingfast.*;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;
import java.util.Iterator;
import java.util.logging.Level;
import com.edgytech.umongo.DbJob.Item;
import com.mongodb.DB;
import com.mongodb.DBCursor;
import java.awt.Component;
import java.util.concurrent.ExecutionException;
import javax.swing.AbstractButton;
import javax.swing.JMenuItem;
import org.bson.types.ObjectId;

/**
 *
 * @author antoine
 */
public abstract class DbJob extends Div implements EnumListener<Item> {

    enum Item {

        jobName,
        progressBar,
        close
    }
    long startTime, endTime;
    boolean stopped = false;
    ProgressBar _progress;
    ProgressBarWorker _pbw;
    BaseTreeNode node;

    public DbJob() {
        try {
            xmlLoad(Resource.getXmlDir(), Resource.File.dbJob, null);
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, null, ex);
        }
        setEnumBinding(Item.values(), this);
    }

    public void start() {
        start(false);
    }

    public void start(boolean determinate) {
        setComponentStringFieldValue(Item.jobName, getTitle());

        // if dialog, save current state
        ButtonBase button = getButton();
        if (button != null) {
            xmlSaveLocalCopy(button, null, null);
        }
        
        _progress = (ProgressBar) getBoundUnit(Item.progressBar);
        _progress.determinate = isDeterminate();
        _pbw = new ProgressBarWorker(_progress) {

            @Override
            protected Object doInBackground() throws Exception {
//                Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
                
                startTime = System.currentTimeMillis();
//                Thread.yield();
                try {
                    Object res = doRun();
                    return res;
                } catch (Exception e) {
                    getLogger().log(Level.SEVERE, null, e);
                    return e;
                } finally {
                    endTime = System.currentTimeMillis();
                }
            }

            @Override
            protected void done() {
                Object res = null;
                try {
                    res = get();
                } catch (Exception ex) {
                    UMongo.instance.showError(getTitle(), (Exception) res);
                }

                try {
                    wrapUp(res);
                } catch (Exception ex) {
                    UMongo.instance.showError(getTitle(), (Exception) ex);
                }
            }
        };
        _pbw.start();
    }

    public abstract Object doRun() throws Exception;

    public abstract String getNS();

    public abstract String getShortName();

    public DBObject getRoot(Object result) {
        return null;
    }

    public ButtonBase getButton() {
        return null;
    }

    public boolean isCancelled() {
        if (_pbw != null) {
            return _pbw.isCancelled();
        }
        return false;
    }

    public void cancel() {
        if (_pbw != null) {
            _pbw.cancel(true);
        }
    }

    public void setProgress(int progress) {
        if (_pbw != null) {
            _pbw.updateProgress(progress);
        }
    }

    public void wrapUp(Object res) {
        UMongo.instance.removeJob(this);
        
        if (node != null)
            UMongo.instance.addNodeToRefresh(node);
        
        if (res == null) {
            return;
        }

        String title = getTitle();
        
        boolean log = UMongo.instance.isLoggingOn();
        boolean logRes = UMongo.instance.isLoggingFirstResultOn();

        BasicDBObject sroot = new BasicDBObject();
        sroot.put("ns", getNS());
        sroot.put("name", getShortName());
        sroot.put("details", getRoot(res));
        
        BasicDBObject logObj = null;
        if (log) {
            logObj = new BasicDBObject("_id", new ObjectId());
            logObj.put("ns", getNS());
            logObj.put("name", getShortName());
            logObj.put("details", getRoot(res));
        }

        if (res instanceof Iterator) {
            new DocView(null, title, this, sroot, (Iterator) res).addToTabbedDiv();
            if (logRes && res instanceof DBCursor) {
                logObj.put("firstResult", ((DBCursor)res).curr());
            }
        } else if (res instanceof DBObject) {
            new DocView(null, title, this, sroot, (DBObject) res).addToTabbedDiv();
            if (logRes) {
                logObj.put("firstResult", res);
            }
        } else if (res instanceof String) {
            new TextView(null, title, this, (String) res).addToTabbedDiv();
            // string may be large
            if (logRes) {
                logObj.put("firstResult", MongoUtils.limitString((String)res, 0));
            }
        } else if (res instanceof WriteResult) {
            WriteResult wres = (WriteResult) res;
            DBObject lasterr = wres.getCachedLastError();
            if (lasterr != null) {
                new DocView(null, title, this, sroot, lasterr).addToTabbedDiv();
            }
            if (logRes) {
                logObj.put("firstResult", res);
            }
        } else if (res instanceof Exception) {
            UMongo.instance.showError(title, (Exception) res);
            if (logRes) {
                logObj.put("firstResult", res.toString());
            }
        } else {
            DBObject obj = new BasicDBObject("Result", res.toString());
            new DocView(null, title, this, sroot, obj).addToTabbedDiv();
            if (logRes) {
                logObj.put("firstResult", res.toString());
            }
        }
        
        if (log) {
            UMongo.instance.logActivity(logObj);
        }
        
        _progress = null;
        _pbw = null;
    }

    public String getTitle() {
        String title = "";
        if (getNS() != null) {
            title += getNS() + " / ";
        }
        if (getShortName() != null) {
            title += getShortName();
        }
        return title;
    }

    public void actionPerformed(Item enm, XmlComponentUnit unit, Object src) {
        if (enm == Item.close) {
            // cancel job
        }
    }

    public boolean isDeterminate() {
        return false;
    }

    public void addJob() {
        node = UMongo.instance.getNode();
        UMongo.instance.runJob(this);
    }

    long getRunTime() {
        return endTime - startTime;
    }
    
    void spawnDialog() {
        if (node == null)
            return;
        UMongo.instance.getTree().selectNode(node);
//        UMongo.instance.displayNode(node);
        
        ButtonBase button = getButton();
        if (button == null)
            return;
        xmlLoadLocalCopy(button, null, null);
        Component comp = ((ButtonBase) button).getComponent();
        if (comp != null)
            ((AbstractButton) comp).doClick();
    }

    DB getDB() {
        return null;
    }
    
    DBObject getCommand() {
        return null;
    }
    
    BasePanel getPanel() {
        return null;
    }

    void join() throws InterruptedException, ExecutionException {
        if (_pbw != null)
            _pbw.get();
    }
}
