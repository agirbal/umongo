/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.edgytech.jmongobrowser;

import com.edgytech.swingfast.ComboBox;
import com.edgytech.swingfast.ConfirmDialog;
import com.edgytech.swingfast.EnumListener;
import com.edgytech.swingfast.FormDialog;
import com.edgytech.swingfast.InfoDialog;
import com.edgytech.swingfast.MenuItem;
import com.edgytech.swingfast.TreeNodeLabel;
import com.edgytech.swingfast.XmlComponentUnit;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.CommandResult;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.GroupCommand;
import com.mongodb.LazyDBDecoder;
import com.mongodb.MapReduceCommand;
import com.mongodb.MapReduceCommand.OutputType;
import com.mongodb.MapReduceOutput;
import com.mongodb.Mongo;
import com.mongodb.MongoException.DuplicateKey;
import com.mongodb.WriteConcern;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import javax.swing.JPanel;
import com.edgytech.jmongobrowser.CollectionPanel.Item;

/**
 *
 * @author antoine
 */
public class CollectionPanel extends BasePanel implements EnumListener<Item> {

    enum Item {

        icon,
        name,
        fullName,
        queryOptions,
        writeConcern,
        stats,
        statsCmd,
        refresh,
        dropCollection,
        rename,
        newName,
        dropTarget,
        doImport,
        export,
        find,
        findQuery,
        findFields,
        findSort,
        findSkip,
        findLimit,
        findBatchSize,
        findExplain,
        findExport,
        findHint,
        findOne,
        foQuery,
        foFields,
        count,
        countQuery,
        countSkip,
        countLimit,
        remove,
        removeQuery,
        mapReduce,
        mrMap,
        mrReduce,
        mrFinalize,
        mrQuery,
        mrSort,
        mrType,
        mrOut,
        mrOutDB,
        mrOutSharded,
        mrNonAtomic,
        mrLimit,
        mrJSMode,
        group,
        grpKeys,
        grpQuery,
        grpInitialValue,
        grpReduce,
        grpFinalize,
        options,
        insert,
        insertDoc,
        insertCount,
        insertBulk,
        ensureIndex,
        eiKeys,
        eiUnique,
        eiName,
        eiDropDups,
        eiSparse,
        eiBackground,
        findAndModify,
        famQuery,
        famFields,
        famSort,
        famUpdate,
        famRemove,
        famReturnNew,
        famUpsert,
        update,
        upQuery,
        upUpdate,
        upUpsert,
        upMulti,
        upSafe,
        save,
        saveDoc,
        shardingInfo,
        shardCollection,
        shardKeyCombo,
        shardCustomKey,
        shardUniqueIndex,
        findChunks,
        validate,
        validateFull,
        compact,
        compactForce,
        reIndex,
        moveChunk,
        mvckQuery,
        mvckToShard,
        splitChunk,
        spckQuery,
        spckOnValue,
        geoNear,
        gnOrigin,
        gnNum,
        gnMaxDistance,
        gnDistanceMultiplier,
        gnQuery,
        gnSpherical,
        gnSearch,
        lazyDecoding,
        fixCollection,
        fcDialog,
        fcSrcMongo,
        fcUpsert
    }

    public CollectionPanel() {
        setEnumBinding(Item.values(), this);
    }

    public CollectionNode getCollectionNode() {
        return (CollectionNode) getNode();
    }

    @Override
    protected void updateComponentCustom(JPanel old) {
        try {
            DBCollection collection = getCollectionNode().getCollection();
            setStringFieldValue(Item.name, collection.getName());
            setStringFieldValue(Item.fullName, collection.getFullName());
            setStringFieldValue(Item.queryOptions, MongoUtils.queryOptionsToString(collection.getOptions()));
            ((DocField) getBoundUnit(Item.writeConcern)).setDoc(collection.getWriteConcern().getCommand());
            ((CmdField) getBoundUnit(Item.stats)).updateFromCmd(collection);
        } catch (Exception e) {
            JMongoBrowser.instance.showError(this.getClass().getSimpleName() + " update", e);
        }
    }

    @Override
    public void actionPerformed(Item enm, XmlComponentUnit unit, Object src) {
        if (enm == Item.lazyDecoding) {
            boolean lazy = getBooleanFieldValue(Item.lazyDecoding);
            DBCollection col = getCollectionNode().getCollection();
            if (lazy)
                col.setDBDecoderFactory(LazyDBDecoder.FACTORY);
            else
                col.setDBDecoderFactory(null);
        }
    }

    public void find() {
        DBCollection col = getCollectionNode().getCollection();
        DBObject query = ((DocBuilderField) getBoundUnit(Item.findQuery)).getDBObject();
        DBObject fields = ((DocBuilderField) getBoundUnit(Item.findFields)).getDBObject();
        DBObject sort = ((DocBuilderField) getBoundUnit(Item.findSort)).getDBObject();
        DBObject hint = ((DocBuilderField) getBoundUnit(Item.findHint)).getDBObject();
        int skip = getIntFieldValue(Item.findSkip);
        int limit = getIntFieldValue(Item.findLimit);
        int bs = getIntFieldValue(Item.findBatchSize);
        boolean explain = getBooleanFieldValue(Item.findExplain);
        boolean export = getBooleanFieldValue(Item.findExport);
        if (export) {
            exportToFile(col, query, fields, sort, skip, limit, bs);
        } else {
            doFind(col, query, fields, sort, skip, limit, bs, explain, hint);
        }
    }

    public void findOne() {
        DBCollection col = getCollectionNode().getCollection();
        DBObject query = ((DocBuilderField) getBoundUnit(Item.foQuery)).getDBObject();
        DBObject fields = ((DocBuilderField) getBoundUnit(Item.foFields)).getDBObject();
        doFindOne(col, query, fields);
    }

    public void rename() {
        final CollectionNode colNode = getCollectionNode();
        final DBCollection col = colNode.getCollection();
        // select parent since this is getting renamed
        JMongoBrowser.instance.displayNode(colNode.getDbNode());

        final String name = getStringFieldValue(Item.newName);
        final boolean dropTarget = getBooleanFieldValue(Item.dropTarget);

        new DbJob() {

            @Override
            public Object doRun() {
                col.rename(name, dropTarget);
                return null;
            }

            @Override
            public String getNS() {
                return col.getFullName();
            }

            @Override
            public String getShortName() {
                return "Rename";
            }

            @Override
            public Object getRoot(Object result) {
                return col.getName() + " to " + name;
            }

            @Override
            public void wrapUp(Object res) {
                super.wrapUp(res);
                TreeNodeLabel node = colNode.getParentNode();
                if (node != null) {
                    node.structureComponent();
                }
            }
        }.addJob();
    }

    public void group() {
        final DBCollection col = getCollectionNode().getCollection();
        DBObject keys = ((DocBuilderField) getBoundUnit(Item.grpKeys)).getDBObject();
        DBObject initial = ((DocBuilderField) getBoundUnit(Item.grpInitialValue)).getDBObject();
        DBObject query = ((DocBuilderField) getBoundUnit(Item.grpQuery)).getDBObject();
        String reduce = getStringFieldValue(Item.grpReduce);
        String finalize = getStringFieldValue(Item.grpFinalize);
        final GroupCommand cmd = new GroupCommand(col, keys, query, initial, reduce, finalize);
//        new DocView(null, "Group", col.getDB(), cmd.toDBObject()).addToTabbedDiv();
        new DbJob() {

            @Override
            public Object doRun() {
                return col.group(cmd);
            }

            @Override
            public String getNS() {
                return col.getFullName();
            }

            @Override
            public String getShortName() {
                return "Group";
            }

            @Override
            public Object getRoot(Object result) {
                return cmd.toDBObject();
            }
        }.addJob();
    }

    public void mapReduce() {
        final DBCollection col = getCollectionNode().getCollection();
        String map = getStringFieldValue(Item.mrMap);
        String reduce = getStringFieldValue(Item.mrReduce);
        String finalize = getStringFieldValue(Item.mrFinalize);
        String stype = getStringFieldValue(Item.mrType);
        final OutputType type = OutputType.valueOf(stype.toUpperCase());
        String out = getStringFieldValue(Item.mrOut);
        if (type != OutputType.INLINE && (out.isEmpty())) {
            new InfoDialog(id, null, null, "Output collection cannot be empty if type is not inline.").show();
            return;
        }

        String outDB = getStringFieldValue(Item.mrOutDB);
        DBObject query = ((DocBuilderField) getBoundUnit(Item.mrQuery)).getDBObject();
        int limit = getIntFieldValue(Item.mrLimit);
        final MapReduceCommand cmd = new MapReduceCommand(col, map, reduce, out, type, query);
        DBObject sort = ((DocBuilderField) getBoundUnit(Item.mrSort)).getDBObject();
        if (sort != null)
            cmd.setSort(sort);
        if (!outDB.isEmpty()) {
            cmd.setOutputDB(outDB);
        }
        if (!finalize.isEmpty()) {
            cmd.setFinalize(finalize);
        }
        if (limit > 0) {
            cmd.setLimit(limit);
        }

        if (getBooleanFieldValue(Item.mrJSMode)) {
            cmd.addExtraOption("jsMode", true);
        }
        
        final BasicDBObject cmdobj = (BasicDBObject) cmd.toDBObject();        
        if (getBooleanFieldValue(Item.mrOutSharded)) {
            ((BasicDBObject)cmdobj.get("out")).put("sharded", true);
        }
        if (getBooleanFieldValue(Item.mrNonAtomic)) {
            ((BasicDBObject)cmdobj.get("out")).put("nonAtomic", true);
        }

        new DbJob() {

            MapReduceOutput output;

            @Override
            public Object doRun() {
//                output = col.mapReduce(cmd);

                // if type in inline, then query options like slaveOk is fine
                CommandResult res = null;
                if (type == MapReduceCommand.OutputType.INLINE) {
                    res = col.getDB().command( cmdobj, col.getOptions() );
                    return res;
                }

                res = col.getDB().command( cmdobj );
                res.throwOnError();
                output = new MapReduceOutput( col , cmdobj, res );
                return output;
            }

            @Override
            public void wrapUp(Object res) {
                if (output != null) {
                    if (cmd.getOutputType() == OutputType.INLINE) {
                        res = output.results();
                    } else {
                        // spawn a find
                        doFind(output.getOutputCollection(), null);
                        res = output.getRaw();
                    }
                }
                super.wrapUp(res);
            }

            @Override
            public String getNS() {
                return col.getFullName();
            }

            @Override
            public String getShortName() {
                return "M/R";
            }

            @Override
            public Object getRoot(Object result) {
                return cmdobj;
            }
        }.addJob();
    }

    static void doFind(final DBCollection col, final DBObject query) {
        doFind(col, query, null, null, 0, 0, 0, false, null);
    }
    
    static void doFind(final DBCollection col, final DBObject query, final DBObject fields, final DBObject sort, final int skip, final int limit, final int batchSize, final boolean explain, final DBObject hint) {
        new DbJob() {

            @Override
            public Object doRun() {
                // this does not actually block, may not need dbjob
                DBCursor cur = col.find(query, fields, skip, batchSize);
                if (sort != null) {
                    cur.sort(sort);
                }
                if (limit > 0) {
                    cur.limit(limit);
                }
                if (hint != null) {
                    cur.hint(hint);
                }
                if (explain) {
                    return cur.explain();
                }
                return cur;
            }

            @Override
            public String getNS() {
                return col.getFullName();
            }

            @Override
            public String getShortName() {
                return "Find";
            }

            @Override
            public Object getRoot(Object result) {
                if (result == null || !(result instanceof DBCursor)) {
                    return null;
                }
                return ((DBCursor) result).toString();
            }
        }.addJob();
    }

    private void exportToFile(final DBCollection col, final DBObject query, final DBObject fields, final DBObject sort, final int skip, final int limit, final int batchSize) {
        ExportDialog dia = JMongoBrowser.instance.getGlobalStore().getExportDialog();
        if (!dia.show()) {
            return;
        }
        final ExportFile.ExportFileOutputStream os = dia.getOutputStream();
        final boolean continueOnError = dia.getBooleanFieldValue(ExportDialog.Item.continueOnError);
        new DbJob() {

            @Override
            public Object doRun() throws Exception {
                try {
                    try {
                        DBCursor cur = col.find(query, fields);
                        if (skip > 0)
                            cur.skip(skip);
                        if (batchSize != 0)
                            cur.batchSize(batchSize);
                        if (sort != null) {
                            cur.sort(sort);
                        }
                        if (limit > 0) {
                            cur.limit(limit);
                        }
                        while (cur.hasNext() && !stopped) {
                            os.writeObject(cur.next());
                        }
                    } catch (Exception e) {
                        if (continueOnError) {
                            getLogger().log(Level.WARNING, null, e);
                        } else {
                            throw e;
                        }
                    }
                } finally {
                    os.close();
                }
                return null;
            }

            @Override
            public String getNS() {
                return col.getFullName();
            }

            @Override
            public String getShortName() {
                return "Export";
            }
        }.addJob();
    }

    static void doFindOne(final DBCollection col, final DBObject query, final DBObject fields) {
        new DbJob() {

            @Override
            public Object doRun() {
                return col.findOne(query, fields);
            }

            @Override
            public String getNS() {
                return col.getFullName();
            }

            @Override
            public String getShortName() {
                return "FindOne";
            }

            @Override
            public Object getRoot(Object result) {
                return "query=" + query + ", fields=" + fields;
            }
        }.addJob();
    }

    public void dropCollection() {
        final CollectionNode colNode = getCollectionNode();
        final DBCollection col = getCollectionNode().getCollection();
        new DbJob() {

            @Override
            public Object doRun() {
                col.drop();
                return null;
            }

            @Override
            public String getNS() {
                return col.getFullName();
            }

            @Override
            public String getShortName() {
                return "Drop";
            }

            @Override
            public void wrapUp(Object res) {
                super.wrapUp(res);
                colNode.removeNode();
            }
        }.addJob();
    }

    public void options() {
        final DBCollection col = getCollectionNode().getCollection();
        OptionDialog od = JMongoBrowser.instance.getGlobalStore().getOptionDialog();
        od.update(col.getOptions(), col.getWriteConcern(), col.getReadPreference());
        if (!od.show()) {
            return;
        }
        col.setOptions(od.getQueryOptions());
        col.setWriteConcern(od.getWriteConcern());
        col.setReadPreference(od.getReadPreference());
        refresh();
    }

    public void insert() {
        final DBCollection col = getCollectionNode().getCollection();
        final BasicDBObject doc = (BasicDBObject) ((DocBuilderField) getBoundUnit(Item.insertDoc)).getDBObject();
        final int count = getIntFieldValue(Item.insertCount);
        final boolean bulk = getBooleanFieldValue(Item.insertBulk);
        new DbJob() {

            @Override
            public Object doRun() throws IOException {
                List<DBObject> list = new ArrayList<DBObject>();
                for (int i = 0; i < count; ++i) {
                    BasicDBObject newdoc = (BasicDBObject) doc.copy();
                    handleSpecialFields(newdoc);
                    if (bulk)
                        list.add(newdoc);
                    else
                        col.insert(newdoc);
                }
                if (bulk)
                    return col.insert(list);
                return null;
            }

            @Override
            public String getNS() {
                return col.getFullName();
            }

            @Override
            public String getShortName() {
                return "Insert";
            }

            @Override
            public Object getRoot(Object result) {
                return doc.toString();
            }

            @Override
            public void wrapUp(Object res) {
                super.wrapUp(res);
                CollectionPanel.this.updateComponent();
            }
        }.addJob();
    }

    public void save() {
        final DBCollection col = getCollectionNode().getCollection();
        final BasicDBObject doc = (BasicDBObject) ((DocBuilderField) getBoundUnit(Item.saveDoc)).getDBObject();
        new DbJob() {

            @Override
            public Object doRun() throws IOException {
                return col.save((DBObject) doc.copy());
            }

            @Override
            public String getNS() {
                return col.getFullName();
            }

            @Override
            public String getShortName() {
                return "Save";
            }

            @Override
            public Object getRoot(Object result) {
                return doc.toString();
            }

            @Override
            public void wrapUp(Object res) {
                super.wrapUp(res);
                CollectionPanel.this.updateComponent();
            }
        }.addJob();
    }

    public void remove() {
        final DBCollection col = getCollectionNode().getCollection();
        final DBObject tmp = ((DocBuilderField) getBoundUnit(Item.removeQuery)).getDBObject();
        final DBObject doc = tmp != null ? tmp : new BasicDBObject();
        new DbJob() {

            @Override
            public Object doRun() throws IOException {
                return col.remove(doc);
            }

            @Override
            public String getNS() {
                return col.getFullName();
            }

            @Override
            public String getShortName() {
                return "Remove";
            }

            @Override
            public Object getRoot(Object result) {
                return doc.toString();
            }

            @Override
            public void wrapUp(Object res) {
                super.wrapUp(res);
                CollectionPanel.this.updateComponent();
            }
        }.addJob();
    }

    public void ensureIndex() {
        final CollectionNode node = getCollectionNode();
        final DBCollection col = getCollectionNode().getCollection();
        final DBObject keys = ((DocBuilderField) getBoundUnit(Item.eiKeys)).getDBObject();

        final DBObject opts = new BasicDBObject();
        final String name = getStringFieldValue(Item.eiName);
        if (name != null && !name.trim().isEmpty())
            opts.put("name", name);
        if (getBooleanFieldValue(Item.eiUnique))
            opts.put("unique", true);
        if (getBooleanFieldValue(Item.eiDropDups))
            opts.put("dropDups", true);
        if (getBooleanFieldValue(Item.eiSparse))
            opts.put("sparse", true);
        if (getBooleanFieldValue(Item.eiBackground))
            opts.put("background", true);

        if (!JMongoBrowser.instance.getGlobalStore().confirmLockingOperation())
            return;
        
        new DbJob() {

            @Override
            public Object doRun() throws IOException {
                col.ensureIndex(keys, opts);
                return null;
            }

            @Override
            public String getNS() {
                return col.getFullName();
            }

            @Override
            public String getShortName() {
                return "Ensure Index";
            }

            @Override
            public void wrapUp(Object res) {
                super.wrapUp(res);
                node.structureComponent();
            }
        }.addJob();
    }

    public void count() {
        final DBCollection col = getCollectionNode().getCollection();
        final DBObject query = ((DocBuilderField) getBoundUnit(Item.countQuery)).getDBObject();
        final int skip = getIntFieldValue(Item.countSkip);
        final int limit = getIntFieldValue(Item.countLimit);

        new DbJob() {

            @Override
            public Object doRun() throws IOException {
                return col.getCount(query, null, limit, skip);
            }

            @Override
            public String getNS() {
                return col.getFullName();
            }

            @Override
            public String getShortName() {
                return "Count";
            }
        }.addJob();
    }

    public void findAndModify() {
        final DBCollection col = getCollectionNode().getCollection();
        final DBObject query = ((DocBuilderField) getBoundUnit(Item.famQuery)).getDBObject();
        final DBObject fields = ((DocBuilderField) getBoundUnit(Item.famFields)).getDBObject();
        final DBObject sort = ((DocBuilderField) getBoundUnit(Item.famSort)).getDBObject();
        final BasicDBObject update = (BasicDBObject) ((DocBuilderField) getBoundUnit(Item.famUpdate)).getDBObject();
        final boolean remove = getBooleanFieldValue(Item.famRemove);
        final boolean returnNew = getBooleanFieldValue(Item.famReturnNew);
        final boolean upsert = getBooleanFieldValue(Item.famUpsert);

        new DbJob() {

            @Override
            public Object doRun() {
                return col.findAndModify(query, fields, sort, remove, (DBObject) update.copy(), returnNew, upsert);
            }

            @Override
            public String getNS() {
                return col.getFullName();
            }

            @Override
            public String getShortName() {
                return "FindAndMod";
            }

            @Override
            public Object getRoot(Object result) {
                StringBuilder sb = new StringBuilder();
                sb.append("query=").append(query);
                sb.append(", fields=").append(fields);
                sb.append(", sort=").append(sort);
                sb.append(", update=").append(update);
                sb.append(", remove=").append(remove);
                sb.append(", returnNew=").append(returnNew);
                sb.append(", upsert=").append(upsert);
                return sb.toString();
            }

            @Override
            public void wrapUp(Object res) {
                super.wrapUp(res);
                CollectionPanel.this.updateComponent();
            }
        }.addJob();
    }

    public void update() {
        final DBCollection col = getCollectionNode().getCollection();
        final DBObject query = ((DocBuilderField) getBoundUnit(Item.upQuery)).getDBObject();
        final BasicDBObject update = (BasicDBObject) ((DocBuilderField) getBoundUnit(Item.upUpdate)).getDBObject();
        final boolean upsert = getBooleanFieldValue(Item.upUpsert);
        final boolean multi = getBooleanFieldValue(Item.upMulti);
        final boolean safe = getBooleanFieldValue(Item.upSafe);
        col.setWriteConcern(WriteConcern.SAFE);

        new DbJob() {

            @Override
            public Object doRun() {
                if (safe) {
                    long count = col.getCount(query);
                    long toupdate = count > 0 ? 1 : 0;
                    if (multi) {
                        toupdate = count;
                    }
                    String text = "Proceed with updating " + toupdate + " of " + count + " documents?";
                    ConfirmDialog confirm = new ConfirmDialog(null, "Confirm Update", null, text);
                    if (!confirm.show()) {
                        return null;
                    }
                }
                return col.update(query, (DBObject) update.copy(), upsert, multi);
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
                sb.append(", upsert=").append(upsert);
                sb.append(", multi=").append(multi);
                return sb.toString();
            }

            @Override
            public void wrapUp(Object res) {
                super.wrapUp(res);
                CollectionPanel.this.updateComponent();
            }
        }.addJob();
    }

    public void doImport() throws IOException {
        ImportDialog dia = JMongoBrowser.instance.getGlobalStore().getImportDialog();
        if (!dia.show()) {
            return;
        }
        final boolean dropCollection = dia.getBooleanFieldValue(ImportDialog.Item.dropCollection);
        final boolean continueOnError = dia.getBooleanFieldValue(ImportDialog.Item.continueOnError);
        final boolean upsert = dia.getBooleanFieldValue(ImportDialog.Item.upsert);
        final boolean bulk = dia.getBooleanFieldValue(ImportDialog.Item.bulk);
        String supsertFields = dia.getStringFieldValue(ImportDialog.Item.upsertFields);
        final String[] upsertFields = supsertFields != null ? supsertFields.split(",") : null;
        if (upsertFields != null) {
            for (int i = 0; i < upsertFields.length; ++i) {
                upsertFields[i] = upsertFields[i].trim();
            }
        }
        final ExportFile ef = dia.getExportFile();
        final ExportFile.ExportFileInputStream os = ef.getInputStream();
        final DBCollection col = getCollectionNode().getCollection();

        new DbJob() {

            @Override
            public Object doRun() throws Exception {
                try {
                    if (dropCollection) {
                        col.drop();
                    }
                    DBObject obj = null;
                    List<DBObject> batch = new ArrayList<DBObject>();
                    while ((obj = os.readObject()) != null) {
                        try {
                            if (upsert) {
                                if (upsertFields == null) {
                                    col.save(obj);
                                } else {
                                    BasicDBObject query = new BasicDBObject();
                                    for (int i = 0; i < upsertFields.length; ++i) {
                                        String field = upsertFields[i];
                                        if (!obj.containsField(field))
                                            throw new Exception("Upsert field " + field + " not present in object " + obj.toString());
                                        query.put(field, obj.get(field));
                                    }
                                    col.update(query, obj, true, false);
                                }
                            } else {
                                if (bulk) {
                                    batch.add(obj);
                                } else {
                                    col.insert(obj);
                                }
                            }
                        } catch (Exception e) {
                            if (continueOnError) {
                                getLogger().log(Level.WARNING, null, e);
                            } else {
                                throw e;
                            }
                        }
                    }

                    if (!batch.isEmpty()) {
                        col.insert(batch);
                    }

                } finally {
                    os.close();
                }
                return null;
            }

            @Override
            public String getNS() {
                return col.getFullName();
            }

            @Override
            public String getShortName() {
                return "Import";
            }

            @Override
            public void wrapUp(Object res) {
                super.wrapUp(res);
                CollectionPanel.this.updateComponent();
            }
        }.addJob();
    }

    public void export() {
        exportToFile(getCollectionNode().getCollection(), null, null, null, 0, 0, 0);
    }

    public void stats() {
        new DocView(null, "Collection Stats", getCollectionNode().getCollection(), "collstats").addToTabbedDiv();
    }

    public void validate() {
        BasicDBObject cmd = new BasicDBObject("validate", getCollectionNode().getCollection().getName());
        if (getBooleanFieldValue(Item.validateFull))
            cmd.put("full", true);
        new DocView(null, "Validate", getCollectionNode().getDbNode().getDb(), cmd).addToTabbedDiv();
    }
    
    public void compact() {
        BasicDBObject cmd = new BasicDBObject("compact", getCollectionNode().getCollection().getName());
        if (getBooleanFieldValue(Item.compactForce))
            cmd.put("force", true);
        if (!JMongoBrowser.instance.getGlobalStore().confirmLockingOperation())
            return;
        new DocView(null, "Compact", getCollectionNode().getDbNode().getDb(), cmd).addToTabbedDiv();
    }
    
    public void reIndex() {
        BasicDBObject cmd = new BasicDBObject("reIndex", getCollectionNode().getCollection().getName());
        if (!JMongoBrowser.instance.getGlobalStore().confirmLockingOperation())
            return;
        new DocView(null, "Re-Index", getCollectionNode().getDbNode().getDb(), cmd).addToTabbedDiv();
    }
    
    public void shardingInfo() {
        final DB config = getCollectionNode().getCollection().getDB().getSisterDB("config");
        final DBCollection col = config.getCollection("collections");
        CollectionPanel.doFind(col, new BasicDBObject("_id", getCollectionNode().getCollection().getFullName()));
    }
    
    public void findChunks() {
        final DB config = getCollectionNode().getCollection().getDB().getSisterDB("config");
        final DBCollection col = config.getCollection("chunks");
        CollectionPanel.doFind(col, new BasicDBObject("ns", getCollectionNode().getCollection().getFullName()));
    }
    
    public void moveChunk() {
        FormDialog dialog = (FormDialog) ((MenuItem) getBoundUnit(Item.moveChunk)).getDialog();
        ComboBox combo = (ComboBox) getBoundUnit(Item.mvckToShard);
        combo.value = 0;
        BasicDBList shards = getCollectionNode().getDbNode().getMongoNode().getShards();
        if (!shards.isEmpty()) {
            String[] items = new String[shards.size()];
            for (int i = 0; i < shards.size(); ++i) {
                DBObject shard = (DBObject) shards.get(i);
                items[i] = (shard.get("_id")).toString();
            }
            combo.items = items;
            combo.structureComponent();
        }

        if (!dialog.show())
            return;
        
        BasicDBObject cmd = new BasicDBObject("moveChunk", getCollectionNode().getCollection().getFullName());
        DBObject query = ((DocBuilderField) getBoundUnit(Item.mvckQuery)).getDBObject();
        cmd.append("find", query);
        cmd.append("to", getStringFieldValue(Item.mvckToShard));
        new DocView(null, "Move Chunk", getCollectionNode().getDbNode().getDb().getSisterDB("admin"), cmd).addToTabbedDiv();
    }
    
    public void splitChunk() {
        BasicDBObject cmd = new BasicDBObject("split", getCollectionNode().getCollection().getFullName());
        DBObject query = ((DocBuilderField) getBoundUnit(Item.spckQuery)).getDBObject();
        if (getBooleanFieldValue(Item.spckOnValue))
            cmd.append("middle", query);
        else
            cmd.append("find", query);
        new DocView(null, "Split Chunk", getCollectionNode().getDbNode().getDb().getSisterDB("admin"), cmd).addToTabbedDiv();
    }

    public void shardCollection() {
        FormDialog dialog = (FormDialog) ((MenuItem) getBoundUnit(Item.shardCollection)).getDialog();
        ComboBox combo = (ComboBox) getBoundUnit(Item.shardKeyCombo);
        combo.value = 0;
        List<DBObject> indices = getCollectionNode().getCollection().getIndexInfo();
        String[] items = new String[indices.size() + 1];
        items[0] = "None";
        int i = 1;
        for (DBObject index : indices) {
            items[i++] = ((DBObject) index.get("key")).toString();
        }
        combo.items = items;
        combo.structureComponent();

        if (!dialog.show())
            return;

        DBObject key = null;
        int index = combo.getComponentIntValue();
        if (index > 0)
            key = (DBObject) indices.get(index - 1).get("key");
        else
            key = ((DocBuilderField)getBoundUnit(Item.shardCustomKey)).getDBObject();

        if (key == null) {
            new InfoDialog(null, "Empty key", null, "You must select a shard key").show();
            return;
        }
        if (!new ConfirmDialog(null, "Confirm shard key", null, "About to shard collection with key " + key + ", is that correct?").show())
            return;

        boolean unique = getBooleanFieldValue(Item.shardUniqueIndex);
        DB admin = getCollectionNode().getDbNode().getDb().getSisterDB("admin");
        DBObject cmd = new BasicDBObject("shardCollection", getCollectionNode().getCollection().getFullName());
        cmd.put("key", key);
        if (unique)
            cmd.put("unique", unique);
        new DocView(null, "Shard Collection", admin, cmd).addToTabbedDiv();
    }

    private Object handleSpecialFields(DBObject doc) {
        for (String field : doc.keySet()) {
            if (field.equals("__rand")) {
                String type = (String) doc.get(field);
                if (type.equals("int")) {
                    int min = (Integer)doc.get("min");
                    int max = (Integer)doc.get("max");
                    return min + (int)(Math.random() * ((max - min) + 1));
                } else if(type.equals("str")) {
                    int len = (Integer)doc.get("len");
                    StringBuilder sb = new StringBuilder(len);
                    byte min = 0x61;
                    byte max = 0x7a;
                    for (int i = 0; i < len; ++i) {
                        char c = (char) (min + (byte) (Math.random() * ((max - min) + 1)));
                        sb.append(c);
                    }
                    return sb.toString();
                }
            }
            Object val = doc.get(field);
            if (val instanceof BasicDBObject) {
                BasicDBObject subdoc = (BasicDBObject) val;
                Object res = handleSpecialFields(subdoc);
                if (res != null)
                    doc.put(field, res);
            } else if (val instanceof BasicDBList) {
                BasicDBList sublist = (BasicDBList) val;
                handleSpecialFields(sublist);
            }
        }
        return null;
    }

    public void geoNear() {
        DBObject cmd = new BasicDBObject("geoNear", getCollectionNode().getCollection().getName());
        DBObject origin = ((DocBuilderField) getBoundUnit(Item.gnOrigin)).getDBObject();
        cmd.put("near", origin);
        int distance = getIntFieldValue(Item.gnMaxDistance);
        cmd.put("maxDistance", distance);
        double distanceMult = getDoubleFieldValue(Item.gnDistanceMultiplier);
        if (distanceMult > 0)
            cmd.put("distanceMultiplier", distanceMult);
        DBObject query = ((DocBuilderField) getBoundUnit(Item.gnQuery)).getDBObject();
        if (query != null)
            cmd.put("query", query);
        boolean spherical = getBooleanFieldValue(Item.gnSpherical);
        if (spherical)
            cmd.put("spherical", true);
        DBObject search = ((DocBuilderField) getBoundUnit(Item.gnSearch)).getDBObject();
        if (search != null)
            cmd.put("search", search);

        new DocView(null, "Geo Near", getCollectionNode().getDbNode().getDb(), cmd).addToTabbedDiv();
    }

    public void fixCollection() {
        final Mongo m = getCollectionNode().getDbNode().getMongoNode().getMongo();
        ArrayList<MongoNode> mongoNodes = JMongoBrowser.instance.getMongos();
        String[] mongonames = new String[mongoNodes.size() - 1];
        Mongo[] mongos = new Mongo[mongonames.length];
        int i = 0;
        for (MongoNode node : mongoNodes) {
            Mongo m2 = node.getMongo();
            if (m == m2) {
                continue;
            }
            mongonames[i] = m2.toString();
            mongos[i] = m2;
            ++i;
        }
        ComboBox src = (ComboBox) getBoundUnit(Item.fcSrcMongo);
        src.items = mongonames;
        src.structureComponent();
        FormDialog dialog = (FormDialog) getBoundUnit(Item.fcDialog);
        if (!dialog.show()) {
            return;
        }

        final DBCollection dstCol = getCollectionNode().getCollection();
        final Mongo srcMongo = mongos[src.getIntValue()];
        final boolean upsert = getBooleanFieldValue(Item.fcUpsert);

        final String dbname = dstCol.getDB().getName();
        final String colname = dstCol.getName();
        final DBCollection srcCol = srcMongo.getDB(dbname).getCollection(colname);
        String txt = "About to copy from ";
        txt += srcMongo.getConnectPoint() + "(" + srcCol.count() + ")";
        txt += " to ";
        txt += m.getConnectPoint() + "(" + dstCol.count() + ")";
        if (!new ConfirmDialog(null, "Confirm Fix Collection", null, txt).show())
            return;

        new DbJob() {
            @Override
            public Object doRun() {
                DBCursor cur = srcCol.find();
                int count = 0;
                int dup = 0;
                while (cur.hasNext()) {
                    DBObject obj = cur.next();
                    if (upsert) {
                        BasicDBObject id = new BasicDBObject("_id", obj.get("_id"));
                        dstCol.update(id, obj, true, false);
                    } else {
                        try {
                            dstCol.insert(obj);
                        } catch (DuplicateKey e) {
                            // dup keys are expected here
                            ++dup;
                        }
                    }
                    ++count;
                }
                DBObject res = new BasicDBObject("count", count);
                res.put("dups", dup);
                return res;
            }

            @Override
            public String getNS() {
                return "*";
            }

            @Override
            public String getShortName() {
                return "Fix Collection";
            }
        }.addJob();
    }
}
