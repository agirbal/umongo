/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.mongo.jmongob;

import com.edgytech.swingfast.ConfirmDialog;
import com.edgytech.swingfast.XmlUnit;

/**
 *
 * @author antoine
 */
public class GlobalStore extends XmlUnit<XmlUnit> {

    enum Item {
        mongoPanel,
        dbPanel,
        collectionPanel,
        indexPanel,
        serverPanel,
        routerPanel,
        replSetPanel,
        optionDialog,
        errorDialog,
        exportDialog,
        importDialog,
        autoUpdateDialog,
        docBuilderDialog,
        lockingOperationDialog
    }

    public GlobalStore() {
        setEnumBinding(Item.values(), null);
    }

    public MongoPanel getMongoPanel() {
        return (MongoPanel) getBoundUnit(Item.mongoPanel);
    }

    public DbPanel getDbPanel() {
        return (DbPanel) getBoundUnit(Item.dbPanel);
    }

    public CollectionPanel getCollectionPanel() {
        return (CollectionPanel) getBoundUnit(Item.collectionPanel);
    }

    public IndexPanel getIndexPanel() {
        return (IndexPanel) getBoundUnit(Item.indexPanel);
    }

    public ServerPanel getServerPanel() {
        return (ServerPanel) getBoundUnit(Item.serverPanel);
    }

    public RouterPanel getRouterPanel() {
        return (RouterPanel) getBoundUnit(Item.routerPanel);
    }

    public ReplSetPanel getReplSetPanel() {
        return (ReplSetPanel) getBoundUnit(Item.replSetPanel);
    }

    OptionDialog getOptionDialog() {
        return (OptionDialog) getBoundUnit(Item.optionDialog);
    }

    ErrorDialog getErrorDialog() {
        return (ErrorDialog) getBoundUnit(Item.errorDialog);
    }

    ExportDialog getExportDialog() {
        return (ExportDialog) getBoundUnit(Item.exportDialog);
    }

    ImportDialog getImportDialog() {
        return (ImportDialog) getBoundUnit(Item.importDialog);
    }

    AutoUpdateDialog getAutoUpdateDialog() {
        return (AutoUpdateDialog) getBoundUnit(Item.autoUpdateDialog);
    }

    DocBuilderDialog getDocBuilderDialog() {
        return (DocBuilderDialog) getBoundUnit(Item.docBuilderDialog);
    }

    ConfirmDialog getLockingOperationDialog() {
        return (ConfirmDialog) getBoundUnit(Item.lockingOperationDialog);
    }

    boolean confirmLockingOperation() {
        if (!getLockingOperationDialog().show())
            return false;
        return true;
    }
}
