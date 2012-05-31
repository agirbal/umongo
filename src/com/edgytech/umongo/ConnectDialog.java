/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.edgytech.umongo;

import com.edgytech.swingfast.FormDialog;
import com.mongodb.MongoOptions;
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

        uri,
        servers,
        databases,
        connectionsPerHost,
        blockingThreadMultiplier,
        maxWaitTime,
        socketType,
        connectTimeout,
        socketTimeout,
        autoConnectRetry,
        safe,
    }

    public ConnectDialog() {
        setEnumBinding(Item.values(), null);
    }

//    void update(MongoOptions moptions) {
//        setIntFieldValue(Item.connectionsPerHost, moptions.connectionsPerHost);
//        setIntFieldValue(Item.blockingThreadMultiplier, moptions.threadsAllowedToBlockForConnectionMultiplier);
//        setIntFieldValue(Item.maxWaitTime, moptions.maxWaitTime);
//        setIntFieldValue(Item.connectTimeout, moptions.connectTimeout);
//        setIntFieldValue(Item.socketTimeout, moptions.socketTimeout);
//        setBooleanFieldValue(Item.autoConnectRetry, moptions.autoConnectRetry);
//        setBooleanFieldValue(Item.safe, moptions.safe);
//    }

    MongoOptions getMongoOptions() {
        MongoOptions moptions = new MongoOptions();
        moptions.connectionsPerHost = getIntFieldValue(Item.connectionsPerHost);
        moptions.threadsAllowedToBlockForConnectionMultiplier = getIntFieldValue(Item.blockingThreadMultiplier);
        moptions.maxWaitTime = getIntFieldValue(Item.maxWaitTime);
        moptions.connectTimeout = getIntFieldValue(Item.connectTimeout);
        moptions.socketTimeout = getIntFieldValue(Item.socketTimeout);
        moptions.autoConnectRetry = getBooleanFieldValue(Item.autoConnectRetry);
        moptions.safe = getBooleanFieldValue(Item.safe);
        
        int stype = getIntFieldValue(Item.socketType);
        if (stype == 1) {
            moptions.socketFactory = SSLSocketFactory.getDefault();
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
                moptions.socketFactory = sc.getSocketFactory();
            } catch (Exception e) {
            }
        }
        return moptions;
    }

}
