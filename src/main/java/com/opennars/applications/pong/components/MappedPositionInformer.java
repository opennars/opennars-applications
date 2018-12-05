/*
 * The MIT License
 *
 * Copyright 2018 The OpenNARS authors.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.opennars.applications.pong.components;

import com.opennars.applications.componentbased.Entity;
import com.opennars.applications.componentbased.InformReasonerComponent;
import com.opennars.applications.pong.GridMapper;

public class MappedPositionInformer implements InformReasonerComponent {
    public GridMapper mapper;

    // used to override name (and id) of the mapped object
    public String nameOverride = "";

    public MappedPositionInformer(final GridMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public String informAboutEntity(Entity entity) {
        String id = String.valueOf(entity.id);
        boolean useMultipleIDs = true;
        if(!useMultipleIDs) {
            id = "0";
        }

        // TODO< we need to invoke the mapper here >
        final String posAsString = mapper.mapPositionOfEntityToString(entity);

        final String objectNameAndId = nameOverride.isEmpty() ? entity.tag + id : nameOverride;

        //return "<(*," + objectNameAndId + ","+ posAsString + ") --> at>. :|:";
        return "<" + posAsString + " --> [at]>. :|:";
    }

    @Override
    public String retName() {
        return "MappedPositionInformer";
    }
}
