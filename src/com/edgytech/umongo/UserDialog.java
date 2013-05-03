/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.edgytech.umongo;

import com.edgytech.swingfast.FormDialog;
import com.edgytech.swingfast.PasswordField;
import com.edgytech.swingfast.TextField;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import java.io.ByteArrayOutputStream;

import com.mongodb.util.Util;
import java.io.IOException;
import org.bson.BasicBSONObject;

/**
 *
 * @author antoine
 */
public class UserDialog extends FormDialog {

    enum Item {

        user,
        password,
        userSource,
        read,
        readWrite,
        dbAdmin,
        userAdmin,
        clusterAdmin,
        readAnyDatabase,
        readWriteAnyDatabase,
        dbAdminAnyDatabase,
        userAdminAnyDatabase,
        version22
    }
    
    enum Role {
        read(Item.read),
        readWrite(Item.readWrite),
        dbAdmin(Item.dbAdmin),
        userAdmin(Item.userAdmin),
        clusterAdmin(Item.clusterAdmin),
        readAnyDatabase(Item.readAnyDatabase),
        readWriteAnyDatabase(Item.readWriteAnyDatabase),
        dbAdminAnyDatabase(Item.dbAdminAnyDatabase),
        userAdminAnyDatabase(Item.userAdminAnyDatabase);
        
        Role(Item item) {
            this.item = item;
        }
        
        Item item;
    }

    public UserDialog() {
        setEnumBinding(Item.values(), null);
    }
    
    String _hash( String username , char[] passwd ){
        ByteArrayOutputStream bout = new ByteArrayOutputStream( username.length() + 20 + passwd.length );
        try {
            bout.write( username.getBytes() );
            bout.write( ":mongo:".getBytes() );
            for ( int i=0; i<passwd.length; i++ ){
                if ( passwd[i] >= 128 )
                    throw new IllegalArgumentException( "can't handle non-ascii passwords yet" );
                bout.write( (byte)passwd[i] );
            }
        }
        catch ( IOException ioe ){
            throw new RuntimeException( "impossible" , ioe );
        }
        return Util.hexMD5( bout.toByteArray() );
    }

    void resetForEdit(BasicDBObject user) {
        xmlLoadCheckpoint();
        
        setStringFieldValue(Item.user, user.getString(Item.user.name()));
        ((TextField)getBoundJComponentUnit(Item.user)).editable = false;

        ((PasswordField)getBoundJComponentUnit(Item.password)).nonEmpty = false;
        setStringFieldValue(Item.userSource, user.getString(Item.userSource.name()));

        BasicDBList roles = (BasicDBList) user.get("roles");
        if (roles != null) {
            for (Role role : Role.values()) {
            setBooleanFieldValue(role.item, roles.contains(role.name()));
            }
        } else {
            boolean ro = user.getBoolean("readOnly");
            if (ro)
                setBooleanFieldValue(Item.readWrite, true);
            else
                setBooleanFieldValue(Item.read, true);
        }

        updateComponent();
    }

    void resetForNew() {
        xmlLoadCheckpoint();
    }

    BasicDBObject getUser(BasicDBObject userObj) {
        final String user = getStringFieldValue(Item.user);
        if (userObj == null)
            userObj = new BasicDBObject("user", user);

        // do not overwrite password if not set
        final String pass = getStringFieldValue(Item.password);
        if (!pass.isEmpty())
            userObj.put("pwd", _hash(user, pass.toCharArray()));

        String userSrc = getStringFieldValue(Item.userSource);
        if (!userSrc.trim().isEmpty()) {
            userObj.put(Item.userSource.name(), userSrc);
            // cant have pwd
            userObj.removeField("pwd");
        }
        
        if (!getBooleanFieldValue(Item.version22)) {
            // format from 2.4
            BasicDBList roles = new BasicDBList();
            for (Role role : Role.values()) {
            if (getBooleanFieldValue(role.item))
                roles.add(role.name());
            }
            userObj.put("roles", roles);
            
            // readOnly flag must be dropped
            userObj.removeField("readOnly");
        } else {
            // keep it simple: if readWrite is not checked, then readOnly
            if (!getBooleanFieldValue(Item.readWrite))
                userObj.put("readOnly", true);
            
            // remove roles
            userObj.removeField("roles");
            
            // all other flags should still be accepted
        }

        return userObj;
    }
}
