/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.edgytech.umongo;

import com.mongodb.BasicDBObject;
import com.mongodb.CommandResult;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.Mongo;

/**
 *
 * @author antoine
 */
public class CmdField extends DocField {
    @Serial
    public String cmd;
    @Serial
    public String db;

    DB theDb;
    DBObject theCmd;

    public CmdField() {
        db = "admin";
    }

    @Override
    public void addView() {
        new DocView(null, getLabelToDisplay(), theDb, theCmd, getDoc(), null, null).addToTabbedDiv();
    }

    void updateFromCmd(DB db) {
        theDb = db;
        if (cmd == null) {
            return;
        }
        updateFromCmd(db, new BasicDBObject(cmd, 1));
    }

    void updateFromCmd(DB db, DBObject cmd) {
        theDb = db;
        theCmd = cmd;
        if (theDb == null || theCmd == null) {
            return;
        }
        CommandResult res = db.command(cmd, db.getOptions());
        setDoc(res);
    }

    void updateFromCmd(Mongo mongo) {
        if (db == null) {
            return;
        }
        updateFromCmd(mongo.getDB(db));
    }

    void updateFromCmd(DBCollection col) {
        updateFromCmd(col.getDB(), new BasicDBObject(cmd, col.getName()));
    }

    public String getCmd() {
        return cmd;
    }

    public String getDB() {
        return db;
    }

}
