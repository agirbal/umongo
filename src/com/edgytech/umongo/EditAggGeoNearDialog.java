/**
 * Copyright (C) 2010 EdgyTech Inc.
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

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 *
 * @author antoine
 */
public class EditAggGeoNearDialog extends EditAggOpDialog {

    enum Item {
        near,
        distanceField,
        limit,
        maxDistance,
        distanceMultiplier,
        query,
        spherical,
        includeLocs,
        uniqueDocs
    }

    public EditAggGeoNearDialog() {
        setEnumBinding(Item.values(), null);
    }
    
    @Override
    public Object getParameters() {
        DBObject cmd = new BasicDBObject();
        cmd.put("near", ((DocBuilderField) getBoundUnit(Item.near)).getDBObject());
        cmd.put("distanceField", getStringFieldValue(Item.distanceField));
        cmd.put("maxDistance", getDoubleFieldValue(Item.maxDistance));
        double distanceMult = getDoubleFieldValue(Item.distanceMultiplier);
        if (distanceMult > 0) {
            cmd.put("distanceMultiplier", distanceMult);
        }
        DBObject query = ((DocBuilderField) getBoundUnit(Item.query)).getDBObject();
        if (query != null) {
            cmd.put("query", query);
        }
        boolean spherical = getBooleanFieldValue(Item.spherical);
        if (spherical) {
            cmd.put("spherical", spherical);
        }
        DBObject search = ((DocBuilderField) getBoundUnit(Item.query)).getDBObject();
        if (search != null) {
            cmd.put("query", search);
        }
        String includeLocs = getStringFieldValue(Item.includeLocs);
        if (includeLocs != null && !includeLocs.isEmpty()) {
            cmd.put("includeLocs", includeLocs);
        }
        boolean unique = getBooleanFieldValue(Item.uniqueDocs);
        if (unique) {
            cmd.put("uniqueDocs", unique);
        }

        return cmd;
    }

    @Override
    public void setParameters(Object value) {
        BasicDBObject cmd = (BasicDBObject) value;
        ((DocBuilderField) getBoundUnit(Item.near)).setDBObject((DBObject) cmd.get("near"));
        setStringFieldValue(Item.distanceField, cmd.getString("distanceField"));
        setDoubleFieldValue(Item.maxDistance, cmd.getDouble("maxDistance"));
        if (cmd.containsField("distanceMultiplier")) {
            setDoubleFieldValue(Item.distanceMultiplier, cmd.getDouble("distanceMultiplier"));
        }
        if (cmd.containsField("query")) {
            ((DocBuilderField) getBoundUnit(Item.query)).setDBObject((DBObject) cmd.get("query"));
        }
        if (cmd.containsField("spherical")) {
            setBooleanFieldValue(Item.spherical, cmd.getBoolean("spherical"));
        }
        
        if (cmd.containsField("query")) {
            ((DocBuilderField) getBoundUnit(Item.query)).setDBObject((DBObject) cmd.get("query"));
        }
        
        if (cmd.containsField("includeLocs")) {
            setStringFieldValue(Item.includeLocs, cmd.getString("includeLocs"));
        }
        
        if (cmd.containsField("uniqueDocs")) {
            setBooleanFieldValue(Item.uniqueDocs, cmd.getBoolean("uniqueDocs"));
        }        
    }
    
}
