/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.edgytech.umongo;

import com.edgytech.swingfast.ButtonBase;
import com.mongodb.*;

/**
 *
 * @author antoine
 */
public class DbJobCmd extends DbJob {

    DB db;
    DBObject cmd;
    BasePanel panel;
    ButtonBase button;

    public DbJobCmd(DB db, DBObject cmd, BasePanel panel, ButtonBase button) {
        this.id = null;
        this.label = cmd.keySet().iterator().next();
        this.db = db;
        this.cmd = cmd;
        this.panel = panel;
        this.button = button;
    }

    public DbJobCmd(DB db, DBObject cmd) {
        this(db, cmd, null, null);
    }

    public DbJobCmd(DB db, String cmdStr) {
        this(db, new BasicDBObject(cmdStr, 1), null, null);
    }

    public DbJobCmd(DBCollection col, String cmdStr) {
        this(col.getDB(), new BasicDBObject(cmdStr, col.getName()), null, null);
    }

    @Override
    public Object doRun() {
        CommandResult res = db.command(cmd);
        res.throwOnError();
        return res;
    }

    @Override
    public String getNS() {
        return db.getName();
    }

    @Override
    public String getShortName() {
        return cmd.keySet().iterator().next();
    }

    @Override
    public Object getRoot(Object result) {
        return cmd.toString();
    }
    
    @Override
    public ButtonBase getButton() {
        return button;
    }

    @Override
    DB getDB() {
        return super.getDB();
    }

    @Override
    DBObject getCommand() {
        return cmd;
    }

    @Override
    BasePanel getPanel() {
        return panel;
    }

    @Override
    public void wrapUp(Object res) {
        super.wrapUp(res);
        
        // panel info may need to be refreshed
        if (panel != null) {
            panel.refresh();
        }
    }
}
