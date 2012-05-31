/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.edgytech.umongo;

import com.edgytech.swingfast.Div;

/**
 *
 * @author antoine
 */
public class JobBar extends Div {

    void addJob(DbJob job) {
        addChild(job);
        structureComponent();
    }

    void removeJob(DbJob job) {
        removeChild(job);
        structureComponent();
    }

}
