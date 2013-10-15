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

import com.mongodb.DBObject;

/**
 *
 * @author antoine
 */
public class EditAggMatchDialog extends EditAggOpDialog {

    enum Item {
        parameters
    }

    public EditAggMatchDialog() {
        setEnumBinding(Item.values(), null);
    }
    
    @Override
    public Object getParameters() {
        return ((DocBuilderField)getBoundUnit(Item.parameters)).getDBObject();
    }

    @Override
    public void setParameters(Object value) {
        ((DocBuilderField)getBoundUnit(Item.parameters)).setDBObject((DBObject) value);
    }
    
}
