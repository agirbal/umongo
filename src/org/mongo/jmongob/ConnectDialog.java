/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mongo.jmongob;

import com.edgytech.swingfast.FormDialog;
import com.mongodb.MongoOptions;
import javax.net.ssl.SSLSocketFactory;

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
        if (stype == 1)
            moptions.socketFactory = SSLSocketFactory.getDefault();
        return moptions;
    }

}
