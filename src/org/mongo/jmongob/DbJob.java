/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mongo.jmongob;

import com.edgytech.swingfast.Div;
import com.edgytech.swingfast.EnumListener;
import com.edgytech.swingfast.ProgressBar;
import com.edgytech.swingfast.ProgressBarWorker;
import com.edgytech.swingfast.XmlComponentUnit;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;
import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.mongo.jmongob.DbJob.Item;
import org.xml.sax.SAXException;

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

    public DbJob() {
        try {
            xmlLoad(Resource.getXmlDir(), Resource.File.dbJob, null);
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, null, ex);
        }
        setEnumBinding(Item.values(), this);
        setStringFieldValue(Item.jobName, getTitle());
    }

    public void start() {
        start(false);
    }

    public void start(boolean determinate) {
        _progress = (ProgressBar) getBoundUnit(Item.progressBar);
        _progress.determinate = isDeterminate();
        _pbw = new ProgressBarWorker(_progress) {

            @Override
            protected Object doInBackground() throws Exception {
                startTime = System.currentTimeMillis();
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
                    JMongoBrowser.instance.showError(getTitle(), (Exception) res);
                }

                try {
                    wrapUp(res);
                } catch (Exception ex) {
                    JMongoBrowser.instance.showError(getTitle(), (Exception) ex);
                }
            }
        };
        _pbw.start();
    }

    public abstract Object doRun() throws Exception;

    public abstract String getNS();

    public abstract String getShortName();

    public Object getRoot(Object result) {
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
        JMongoBrowser.instance.removeJob(this);
        if (res == null) {
            return;
        }

        String title = getTitle();
        Object root = getRoot(res);
        String sroot = title;
        if (root != null) {
            sroot += ": " + root;
        }

        if (res instanceof Iterator) {
            new DocView(null, title, (Iterator) res, sroot).addToTabbedDiv();
        } else if (res instanceof DBObject) {
            new DocView(null, title, (DBObject) res, sroot, this).addToTabbedDiv();
        } else if (res instanceof WriteResult) {
            WriteResult wres = (WriteResult) res;
            DBObject lasterr = wres.getCachedLastError();
            if (lasterr != null) {
                new DocView(null, title, lasterr, sroot, this).addToTabbedDiv();
            }
        } else if (res instanceof Exception) {
            JMongoBrowser.instance.showError(title, (Exception) res);
        } else {
            DBObject obj = new BasicDBObject("Result", res.toString());
            new DocView(null, title, obj, sroot, this).addToTabbedDiv();
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
        JMongoBrowser.instance.runJob(this);
    }

    long getRunTime() {
        return endTime - startTime;
    }
}
