/**
 *      Copyright (C) 2010 EdgyTech LLC.
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
        new DbJobCmd(theDb, theCmd).addJob();
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
