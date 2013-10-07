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

import com.edgytech.swingfast.FormDialog;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 *
 * @author antoine
 */
public class EnsureIndexDialog extends FormDialog {

    enum Item {
        keys,
        unique,
        name,
        dropDuplicates,
        sparse,
        expireDocuments,
        expireAfterSeconds,
        background,
        weights,
        defaultLanguage,
        languageOverride,
        extra
    }

    public EnsureIndexDialog() {
        setEnumBinding(Item.values(), null);
    }
    
    DBObject getKeys() {
        return ((DocBuilderField) getBoundUnit(Item.keys)).getDBObject();
    }
    
    DBObject getOptions() {
        final DBObject opts = new BasicDBObject();
        final String name = getStringFieldValue(Item.name);
        if (name != null && !name.trim().isEmpty()) {
            opts.put("name", name);
        }
        if (getBooleanFieldValue(Item.unique)) {
            opts.put("unique", true);
        }
        if (getBooleanFieldValue(Item.dropDuplicates)) {
            opts.put("dropDups", true);
        }
        if (getBooleanFieldValue(Item.sparse)) {
            opts.put("sparse", true);
        }
        if (getBooleanFieldValue(Item.expireDocuments)) {
            opts.put("expireAfterSeconds", getIntFieldValue(Item.expireAfterSeconds));
        }
        if (getBooleanFieldValue(Item.background)) {
            opts.put("background", true);
        }
        
        DBObject weights = ((DocBuilderField) getBoundUnit(Item.weights)).getDBObject();
        if (weights != null) {
            opts.put("weights", weights);
        }
        String defaultLanguage = getStringFieldValue(Item.defaultLanguage);
        if (!defaultLanguage.trim().isEmpty()) {
            opts.put("default_language", defaultLanguage);
        }
        String languageOverride = getStringFieldValue(Item.languageOverride);
        if (!languageOverride.trim().isEmpty()) {
            opts.put("language_override", languageOverride);
        }

        DBObject extra = ((DocBuilderField) getBoundUnit(Item.extra)).getDBObject();
        if (extra != null) {
            opts.putAll(extra);
        }
        return opts;
    }
}
