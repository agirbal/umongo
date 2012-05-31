/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.edgytech.umongo;

import com.edgytech.swingfast.ConfirmDialog;
import com.edgytech.swingfast.EnumListener;
import com.edgytech.swingfast.MenuBar;
import com.edgytech.swingfast.MenuItem;
import com.edgytech.swingfast.TabbedDiv;
import com.edgytech.swingfast.XmlComponentUnit;
import com.mongodb.Mongo;
import com.mongodb.MongoURI;
import com.mongodb.ServerAddress;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import com.edgytech.umongo.MainMenu.Item;

/**
 *
 * @author antoine
 */
public class MainMenu extends MenuBar implements EnumListener<Item> {

    public enum Item {

        connect,
        exit,
        preferences,
        prefDialog,
        importFile
    }

    public MainMenu() {
        setEnumBinding(Item.values(), this);
    }

    AppPreferences getPreferences() {
        return (AppPreferences) getBoundUnit(Item.prefDialog);
    }

    public void actionPerformed(Item enm, XmlComponentUnit unit, Object src) {
    }

    public void start() {
    }

    public void importFile() throws IOException {
        ImportDialog dia = UMongo.instance.getGlobalStore().getImportDialog();
        if (!dia.show()) {
            return;
        }
        final boolean continueOnError = dia.getBooleanFieldValue(ImportDialog.Item.continueOnError);
        final ExportFile ef = dia.getExportFile();
        final ExportFile.ExportFileInputStream os = ef.getInputStream();

        new DbJob() {

            @Override
            public Object doRun() throws Exception {
                return os.iterator();
            }

            @Override
            public String getNS() {
                return ef.getFile().getName();
            }

            @Override
            public String getShortName() {
                return "Import";
            }
        }.addJob();
    }

    public void connect() {
        try {
            ConnectDialog dialog = (ConnectDialog) ((MenuItem) getBoundUnit(Item.connect)).getDialog();
            Mongo mongo = null;
            List<String> dbs = new ArrayList<String>();
            String uri = dialog.getStringFieldValue(ConnectDialog.Item.uri);
            if (!uri.trim().isEmpty()) {
                if (!uri.startsWith(MongoURI.MONGODB_PREFIX)) {
                    uri = MongoURI.MONGODB_PREFIX + uri;
                }
                MongoURI muri = new MongoURI(uri);
                mongo = new Mongo(muri);
                String db = muri.getDatabase();
                if (db != null && !db.trim().isEmpty()) {
                    dbs.add(db.trim());
                }
            } else {
                String servers = dialog.getStringFieldValue(ConnectDialog.Item.servers);
                if (servers.trim().isEmpty()) {
                    return;
                }
                String[] serverList = servers.split(",");
                ArrayList<ServerAddress> addrs = new ArrayList<ServerAddress>();
                for (String server : serverList) {
                    String[] tmp = server.split(":");
                    if (tmp.length > 1) {
                        addrs.add(new ServerAddress(tmp[0], Integer.valueOf(tmp[1]).intValue()));
                    } else {
                        addrs.add(new ServerAddress(tmp[0]));
                    }
                }
                mongo = new Mongo(addrs, dialog.getMongoOptions());
                String sdbs = dialog.getStringFieldValue(ConnectDialog.Item.databases);
                if (!sdbs.trim().isEmpty()) {
                    for (String db : sdbs.split(",")) {
                        dbs.add(db.trim());
                    }
                }
            }

            if (dbs.size() == 0) {
                dbs = null;
            }
            UMongo.instance.addMongo(mongo, dbs);
        } catch (Exception ex) {
            UMongo.instance.showError(id, ex);
        }
    }

    public void exit() {
        UMongo.instance.windowClosing(null);
    }
}
