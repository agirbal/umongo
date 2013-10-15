/**
 * Copyright (C) 2010 EdgyTech LLC.
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
import com.mongodb.Mongo;
import com.mongodb.MongoURI;
import com.mongodb.ServerAddress;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import com.edgytech.umongo.MainMenu.Item;
import com.mongodb.MongoClient;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map.Entry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 *
 * @author antoine
 */
public class MainMenu extends MenuBar implements EnumListener<Item> {

    public enum Item {

        connect,
        connectProgressDialog,
        exit,
        preferences,
        prefDialog,
        importFile,
        about,
        aboutTextArea
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

    public void importFile(ButtonBase button) throws IOException {
        ImportDialog dia = UMongo.instance.getGlobalStore().getImportDialog();
        if (!dia.show()) {
            return;
        }
        final boolean continueOnError = dia.getBooleanFieldValue(ImportDialog.Item.continueOnError);
        final DocumentDeserializer dd = dia.getDocumentDeserializer();

        new DbJob() {
            @Override
            public Object doRun() throws Exception {
                return dd.iterator();
            }

            @Override
            public String getNS() {
                return dd.getFile().getName();
            }

            @Override
            public String getShortName() {
                return "Import";
            }
        }.addJob();
    }

    public void connect(ButtonBase button) {
        try {
            ConnectDialog dialog = (ConnectDialog) ((MenuItem) getBoundUnit(Item.connect)).getDialog();
            ProgressDialog progress = (ProgressDialog) getBoundUnit(Item.connectProgressDialog);
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
//                mongo = new MongoClient(addrs, dialog.getMongoOptions());
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

            String user = dialog.getStringFieldValue(ConnectDialog.Item.user).trim();
            String password = dialog.getStringFieldValue(ConnectDialog.Item.password);
            if (!user.isEmpty()) {
                // authenticate against all dbs
                if (dbs != null) {
                    for (String db : dbs) {
                        mongo.getDB(db).authenticate(user, password.toCharArray());
                    }
                } else {
                    mongo.getDB("admin").authenticate(user, password.toCharArray());
                }
            }

            final Mongo fmongo = mongo;
            final List<String> fdbs = dbs;
            // doing in background can mean concurrent modification, but dialog is modal so unlikely
            progress.show(new ProgressDialogWorker(progress) {
                @Override
                protected void finished() {
                }

                @Override
                protected Object doInBackground() throws Exception {
                    UMongo.instance.addMongo(fmongo, fdbs);
                    return null;
                }
            });

        } catch (Exception ex) {
            UMongo.instance.showError(id, ex);
        }
    }

    public void exit(ButtonBase button) {
        UMongo.instance.windowClosing(null);
    }

    public void about(ButtonBase button) throws IOException {
        FormDialog dia = (FormDialog) button.getDialog();

        StringBuilder text = new StringBuilder();
        text.append("<html>");

        Enumeration<URL> urls = Thread.currentThread().getContextClassLoader().getResources("META-INF/MANIFEST.MF");
        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            if (!url.getPath().contains("umongo")) {
                continue;
            }
            
            String jar = url.getPath();
            int end = jar.indexOf(".jar");
            if (end < 0)
                continue;
            jar = jar.substring(0, end + 4);
            text.append("<b>").append(jar).append("</b>").append(":<br/>");
//        text.append(url.getPath()).append("<br/>");
//        InputStream is = getClass().getClassLoader().getResourceAsStream("META-INF/MANIFEST.MF");
//        BufferedReader d = new BufferedReader(new InputStreamReader(is));
            BufferedReader d = new BufferedReader(new InputStreamReader(url.openStream()));
            String str;
            while ((str = d.readLine()) != null) {
                text.append(str).append("<br/>");
            }
            text.append("<br/>");
//        for (Entry e : man.getMainAttributes().entrySet()) {
//            text.append(e.getKey()).append(": ").append(e.getValue()).append("<br/>");
//        }
        }
        text.append("</html>");
        setStringFieldValue(Item.aboutTextArea, text.toString());
//        is.close();
        dia.show();
    }
}
