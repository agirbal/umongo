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

import com.edgytech.swingfast.FormDialog;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientOptions;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import java.io.IOException;
import java.net.*;
import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 *
 * @author antoine
 */
public class ConnectDialog extends FormDialog {

    enum Item {
        name,
        uri,
        servers,
        connectionMode,
        databases,
        user,
        password,
        connectionsPerHost,
        blockingThreadMultiplier,
        maxWaitTime,
        socketType,
        connectTimeout,
        socketTimeout,
        safeWrites,
        secondaryReads,
        proxyType,
        proxyHost,
        proxyPort,
        proxyUser,
        proxyPassword
    }

    public ConnectDialog() {
        setEnumBinding(Item.values(), null);
    }

    MongoClientOptions getMongoClientOptions() {
        MongoClientOptions.Builder builder = MongoClientOptions.builder();
//        moptions.connectionsPerHost = getIntFieldValue(Item.connectionsPerHost);
//        moptions.threadsAllowedToBlockForConnectionMultiplier = getIntFieldValue(Item.blockingThreadMultiplier);
//        moptions.maxWaitTime = getIntFieldValue(Item.maxWaitTime);
        builder.connectTimeout(getIntFieldValue(Item.connectTimeout));
        builder.socketTimeout(getIntFieldValue(Item.socketTimeout));
//        moptions.autoConnectRetry = getBooleanFieldValue(Item.autoConnectRetry);
        if (!getBooleanFieldValue(Item.safeWrites)) {
            builder.writeConcern(WriteConcern.NONE);
        }
        
//        moptions.slaveOk = getBooleanFieldValue(Item.secondaryReads);
        if (getBooleanFieldValue(Item.secondaryReads)) {
            builder.readPreference(ReadPreference.secondaryPreferred());
        }
        
        int stype = getIntFieldValue(Item.socketType);
        int proxy = getIntFieldValue(Item.proxyType);
        if (proxy == 1) {
            // SOCKS proxy
            final String host = getStringFieldValue(Item.proxyHost);
            final int port = getIntFieldValue(Item.proxyPort);
            builder.socketFactory(new SocketFactory() {

                @Override
                public Socket createSocket() throws IOException {
                    SocketAddress addr = new InetSocketAddress(host, port);
                    Proxy proxy = new Proxy(Proxy.Type.SOCKS, addr);
                    Socket socket = new Socket(proxy);
                    return socket;
                }

                @Override
                public Socket createSocket(String string, int i) throws IOException, UnknownHostException {
                    SocketAddress addr = new InetSocketAddress(host, port);
                    Proxy proxy = new Proxy(Proxy.Type.SOCKS, addr);
                    Socket socket = new Socket(proxy);
                    InetSocketAddress dest = new InetSocketAddress(string, i);
                    socket.connect(dest);
                    return socket;
                }

                @Override
                public Socket createSocket(String string, int i, InetAddress ia, int i1) throws IOException, UnknownHostException {
                    throw new UnsupportedOperationException("Not supported yet.");
                }

                @Override
                public Socket createSocket(InetAddress ia, int i) throws IOException {
                    SocketAddress addr = new InetSocketAddress(host, port);
                    Proxy proxy = new Proxy(Proxy.Type.SOCKS, addr);
                    Socket socket = new Socket(proxy);
                    InetSocketAddress dest = new InetSocketAddress(ia, i);
                    socket.connect(dest);
                    return socket;
                }

                @Override
                public Socket createSocket(InetAddress ia, int i, InetAddress ia1, int i1) throws IOException {
                    throw new UnsupportedOperationException("Not supported yet.");
                }
            });

//            // authentication.. only supports 1 global for all proxies :(
//            final String user = getStringFieldValue(Item.proxyUser);
//            final String pwd = getStringFieldValue(Item.proxyPassword);
//            if (!user.isEmpty()) {
//                Authenticator.setDefault(new Authenticator() {
//                    @Override
//                    protected PasswordAuthentication getPasswordAuthentication() {
//                        PasswordAuthentication p = new PasswordAuthentication(user, pwd.toCharArray());
//                        return p;
//                    }
//                });
//            }
        }
        
        if (stype == 1) {
            builder.socketFactory(SSLSocketFactory.getDefault());
        } else if (stype == 2) {
            // Create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {

                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }
                }
            };
            try {
                SSLContext sc = SSLContext.getInstance("SSL");
                sc.init(null, trustAllCerts, new java.security.SecureRandom());
                builder.socketFactory(sc.getSocketFactory());
            } catch (Exception e) {
            }
        }

        return builder.build();
    }
    
    void setName(String name) {
        setStringFieldValue(Item.name, name);
    }
    
    String getName() {
        return getStringFieldValue(Item.name);
    }
}
