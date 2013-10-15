/**
 *      Copyright (C) 2010 EdgyTech LLC.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.edgytech.umongo;

import com.edgytech.swingfast.EnumListener;
import com.edgytech.swingfast.XmlComponentUnit;
import com.mongodb.DBRef;
import com.mongodb.DBRefBase;
import javax.swing.JDialog;
import org.bson.types.ObjectId;
import com.edgytech.umongo.EditObjectIdDialog.Item;

/**
 *
 * @author antoine
 */
public class EditObjectIdDialog extends EditFieldDialog implements EnumListener<Item> {

    enum Item {

        string,
        time,
        machine,
        inc,
        currentTime,
        currentMachine,
        currentInc,
        namespace
    }

    public EditObjectIdDialog() {
        setEnumBinding(Item.values(), this);
    }

    @Override
    public Object getValue() {
        String str = getStringFieldValue(Item.string);
        ObjectId id = null;
        if (!str.isEmpty()) {
            id = new ObjectId(str);
        } else {
            int time = getIntFieldValue(Item.time);
            int machine = getIntFieldValue(Item.machine);
            int inc = getIntFieldValue(Item.inc);
            id = new ObjectId(time, machine, inc);
        }
        String ns = getStringFieldValue(Item.namespace);
        if (ns.trim().isEmpty()) {
            return id;
        }
        return new DBRef(null, ns, id);
    }

    @Override
    public void setValue(Object value) {
        ObjectId id = null;
        if (value instanceof DBRefBase) {
            DBRefBase ref = (DBRefBase) value;
            setStringFieldValue(Item.namespace, ref.getRef());
            id = (ObjectId) ref.getId();
        } else {
            id = (ObjectId) value;
        }
        setStringFieldValue(Item.string, id.toString());
        setIntFieldValue(Item.time, id._time());
        setIntFieldValue(Item.machine, id.getMachine());
        setIntFieldValue(Item.inc, id.getInc());
    }

    @Override
    protected void updateComponentCustom(JDialog old) {
        super.updateComponentCustom(old);
        setStringFieldValue(Item.currentTime, String.valueOf(System.currentTimeMillis() / 1000));
        setStringFieldValue(Item.currentMachine, String.valueOf(ObjectId.getGenMachineId()));
        setStringFieldValue(Item.currentInc, String.valueOf(ObjectId.getCurrentInc()));
    }

    public void actionPerformed(Item enm, XmlComponentUnit unit, Object src) {
        switch (enm) {
            case string:
                String str = getComponentStringFieldValue(Item.string);
                if (!str.isEmpty()) {
                    ObjectId oid = new ObjectId(str);
                    setComponentIntFieldValue(Item.time, oid.getTimeSecond());
                    setComponentIntFieldValue(Item.machine, oid.getMachine());
                    setComponentIntFieldValue(Item.inc, oid.getInc());
                }
                break;
            case time:
            case machine:
            case inc:
                int time = getComponentIntFieldValue(Item.time);
                int machine = getComponentIntFieldValue(Item.machine);
                int inc = getComponentIntFieldValue(Item.inc);
                ObjectId oid = new ObjectId(time, machine, inc);
                setComponentStringFieldValue(Item.string, oid.toString());
                break;
        }
    }
}
