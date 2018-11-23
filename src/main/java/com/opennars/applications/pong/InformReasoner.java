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
package com.opennars.applications.pong;

import com.opennars.applications.componentbased.Entity;
import com.opennars.applications.pong.components.MappedPositionInformer;
import org.opennars.interfaces.NarseseConsumer;

import java.util.ArrayList;
import java.util.List;

public class InformReasoner {
    String lastInput = "";
    String input = "";
    List<String> inputs = new ArrayList<>();

    //minX and minY define the lower end of the relative coordinate system
    public void informAboutEntities(List<Entity> entities) {
        for (final Entity iEntity : entities) {
            final MappedPositionInformer informer = (MappedPositionInformer)iEntity.retComponentByName("MappedPositionInformer");
            if (informer == null) {
                continue;
            }

            inputs.add(informer.informAboutEntity(iEntity));
            input += inputs.get(inputs.size()-1);
        }
    }

    // /param force are the inputs forced to be fed into the reasoner
    public boolean Input(NarseseConsumer consumer, final boolean force) {
        boolean hadInput = false;
        if(!input.equals(lastInput)||force) {
            for(String inp : inputs) {
                consumer.addInput(inp);
                hadInput = true;
            }
            lastInput = input;
        }
        input = "";
        inputs.clear();
        return hadInput;
    }
}
