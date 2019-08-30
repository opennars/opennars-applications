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
package org.opennars.applications.crossing.Encoders;

import org.opennars.applications.crossing.Entities.Entity;
import org.opennars.applications.crossing.Entities.Car;
import org.opennars.applications.crossing.Entities.Bike;
import java.util.ArrayList;
import java.util.List;
import org.opennars.applications.Util;
import org.opennars.applications.crossing.Crossing;
import org.opennars.applications.crossing.TrafficLight;
import org.opennars.main.Nar;

public class InformPredictionNar {
    String lastInput = "";
    String input = "";
    List<String> inputs = new ArrayList<String>();
    //minX and minY define the lower end of the relative coordinate system
    public void informAboutEntity(Nar nar, Entity ent, int minX, int minY) {
        String id = String.valueOf(ent.id);
        boolean useMultipleIDs = true;
        if(!useMultipleIDs) {
            id = "0";
        }
        String pos = Util.positionToTerm((int) ent.posX-minX, (int) ent.posY-minY, Crossing.discretization);
        if(ent instanceof Bike) {
            inputs.add("<(*,bike" + id + ","+ pos + ") --> at>. :|:");
            input += inputs.get(inputs.size()-1);
        } else
        if (ent instanceof Car) {
            inputs.add("<(*,car" + id + ","+ pos + ") --> at>. :|:");
            input += inputs.get(inputs.size()-1);
        }
        //prediction nar doesn't receive pedestrians for now as they behave too unpredictably
        //if (ent instanceof Pedestrian) {
        //    inputs.add("<(*,pedestrian" + id + "," + pos + ") --> at>. :|:");
        //    input += inputs.get(inputs.size()-1);
        //}
    }

    public void informAboutTrafficLight(Nar nar, TrafficLight light, int minX, int minY) {
        //String id = String.valueOf(light.id);
        String colour = light.colour == 0 ? "green" : "red";
        String narsese = "<trafficLight --> ["+colour+"]>. :|:";
        inputs.add(narsese);
        input+=narsese;
    }

    // /param force are the inputs forced to be fed into the reasoner
    public boolean Input(Nar nar, final boolean force) {
        boolean hadInput = false;
        if(!input.equals(lastInput)||force) {
            for(String inp : inputs) {
                nar.addInput(inp);
                hadInput = true;
            }
            lastInput = input;
        }
        input = "";
        inputs.clear();
        return hadInput;
    }
}
