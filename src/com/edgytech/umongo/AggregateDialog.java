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

import com.edgytech.swingfast.ButtonBase;
import com.edgytech.swingfast.EnumListener;
import com.edgytech.swingfast.FormDialog;
import com.edgytech.swingfast.ListArea;
import com.edgytech.swingfast.XmlComponentUnit;
import com.edgytech.umongo.AggregateDialog.Item;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JDialog;

/**
 *
 * @author antoine
 */
public class AggregateDialog extends FormDialog implements EnumListener<Item> {

    enum Item {
        operationList,
        addOperationDialog,
        valueOperationDialog,
        genericOperationDialog,
        operation,
        value,
        parameters,
        addOperation,
        removeOperation,
        editOperation,
        moveUpOperation,
        moveDownOperation,
        editAggLimitDialog,
        editAggSkipDialog,
        editAggMatchDialog,
        editAggSortDialog,
        editAggProjectDialog,
        editAggUnwindDialog,
        editAggGroupDialog,
        editAggGeoNearDialog
    }
    
    List<BasicDBObject> operationList = new ArrayList<BasicDBObject>();
    
    @SerialStar
    public String pipeline;
    
    public AggregateDialog() {
        pipeline = "";
        setEnumBinding(Item.values(), this);
    }
    
    void refreshAggList() {
        ListArea list = (ListArea) getBoundUnit(Item.operationList);
        String[] items = new String[operationList.size()];
        int i = 0;
        for (BasicDBObject op : operationList) {
            String json = MongoUtils.getJSON(op);
            items[i++] = json.substring(0, Math.min(80, json.length()));
        }
        list.items = items;
        list.structureComponent();
    }

    public void addOperation(ButtonBase button) {
        FormDialog od = (FormDialog) getBoundUnit(Item.addOperationDialog);
        if (!od.show()) {
            return;
        }
        String op = getStringFieldValue(Item.operation);
        BasicDBObject opObj = editOperation(null, "$" + op);

        if (opObj != null) {
            operationList.add(opObj);
            refreshAggList();
        }
    }

    public void removeOperation(ButtonBase button) {
        final int opId = getComponentIntFieldValue(Item.operationList);        
        operationList.remove(opId);
        refreshAggList();
    }

    public void editOperation(ButtonBase button) {
        final int opId = getComponentIntFieldValue(Item.operationList);        
        BasicDBObject opObj = operationList.get(opId);
        opObj = editOperation(opObj, null);
        if (opObj != null) {
            operationList.set(opId, opObj);
            refreshAggList();
        }
    }
    
    public BasicDBObject editOperation(BasicDBObject opObj, String op) {
        if (op == null)
            op = opObj.keySet().iterator().next();
        EditAggOpDialog od = null;
        
//        if ("$limit".equals(op) || "$skip".equals(op)) {
//            od = (FormDialog) getBoundUnit(Item.valueOperationDialog);
//            setIntFieldValue(Item.value, opObj.getInt(op));
//            if (!od.show()) {
//                return null;
//            }
//            opObj.put(op, getIntFieldValue(Item.value));
//        } else {
//            od = (FormDialog) getBoundUnit(Item.genericOperationDialog);
//            ((DocBuilderField)getBoundUnit(Item.parameters)).setDBObject((DBObject) opObj.get(op));
//            if (!od.show()) {
//                return null;
//            }
//            opObj.put(op, ((DocBuilderField)getBoundUnit(Item.parameters)).getDBObject());
//        }

        if ("$match".equals(op)) {
            od = (EditAggOpDialog) getBoundUnit(Item.editAggMatchDialog);
        } else if ("$project".equals(op)) {
            od = (EditAggOpDialog) getBoundUnit(Item.editAggProjectDialog);
        } else if ("$limit".equals(op)) {
            od = (EditAggOpDialog) getBoundUnit(Item.editAggLimitDialog);
        } else if ("$skip".equals(op)) {
            od = (EditAggOpDialog) getBoundUnit(Item.editAggSkipDialog);
        } else if ("$sort".equals(op)) {
            od = (EditAggOpDialog) getBoundUnit(Item.editAggSortDialog);
        } else if ("$unwind".equals(op)) {
            od = (EditAggOpDialog) getBoundUnit(Item.editAggUnwindDialog);
        } else if ("$group".equals(op)) {
            od = (EditAggOpDialog) getBoundUnit(Item.editAggGroupDialog);
        } else if ("$geoNear".equals(op)) {
            od = (EditAggOpDialog) getBoundUnit(Item.editAggGeoNearDialog);
        }
        
        if (opObj != null)
            od.setOperation(opObj);
        if (!od.show()) {
            return null;
        }
        opObj = od.getOperation();
        return opObj;
    }
    
    public void moveUpOperation(ButtonBase button) {
        final int opId = getComponentIntFieldValue(Item.operationList);
        if (opId > 0) {
            BasicDBObject opObj = operationList.get(opId);
            BasicDBObject other = operationList.get(opId - 1);
            operationList.set(opId - 1, opObj);
            operationList.set(opId, other);
            refreshAggList();
        }
    }
    
    public void moveDownOperation(ButtonBase button) {
        final int opId = getComponentIntFieldValue(Item.operationList);
        if (opId < operationList.size() - 1) {
            BasicDBObject opObj = operationList.get(opId);
            BasicDBObject other = operationList.get(opId + 1);
            operationList.set(opId + 1, opObj);
            operationList.set(opId, other);
            refreshAggList();
        }
    }
    
//    void resetForNew() {
//        xmlLoadCheckpoint();
//    }
//
//    void resetForEdit(BasicDBObject opObj) {
//        String op = opObj.keySet().iterator().next();
//        setStringFieldValue(Item.operation, op.substring(1));
//        ((DocBuilderField)getBoundUnit(Item.parameters)).setDBObject((DBObject) opObj.get(op));
//        updateComponent();
//    }
//    
//    BasicDBObject getOperation() {
//        String op = getStringFieldValue(Item.operation);
//        BasicDBObject param = (BasicDBObject) ((DocBuilderField)getBoundUnit(Item.parameters)).getDBObject();
//        return new BasicDBObject("$" + op, param);
//    }

    BasicDBObject getAggregateCommand(String collection) {
        BasicDBObject cmd = new BasicDBObject("aggregate", collection);
        BasicDBList list = new BasicDBList();
        cmd.put("pipeline", list);
        for (BasicDBObject op : operationList) {
            list.add(op);
        }
        return cmd;
    }

    @Override
    public void actionPerformed(Item enm, XmlComponentUnit unit, Object src) {
    }

    @Override
    protected void updateComponentCustom(JDialog old) {
        BasicDBObject cmd = (BasicDBObject) JSON.parse(pipeline);
        if (cmd != null && cmd.containsField("pipeline")) {
            BasicDBList list = (BasicDBList) cmd.get("pipeline");
            operationList.clear();
            for (Object op : list) {
                operationList.add((BasicDBObject) op);
            }
            refreshAggList();
        }
    }

    @Override
    protected void commitComponentCustom(JDialog comp) {
        pipeline = MongoUtils.getJSON(getAggregateCommand("dummy"));
    }
    
}
