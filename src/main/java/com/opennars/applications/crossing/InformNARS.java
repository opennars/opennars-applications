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
package com.opennars.applications.crossing;

import java.util.ArrayList;
import java.util.List;

import org.opennars.interfaces.pub.Reasoner;
import org.opennars.main.Nar;

public class InformNARS {
    String lastInput = "";
    String input = "";
    List<String> inputs = new ArrayList<String>();
    //minX and minY define the lower end of the relative coordinate system
    public void informAboutEntity(Crossing crossing, TrafficLight light, Nar nar, Entity ent, int minX, int minY) {
        String lightState = light.colour == 0 ? "green" : "red";

        String id = String.valueOf(ent.id);
        boolean useMultipleIDs = true;
        if(!useMultipleIDs) {
            id = "0";
        }
        String pos = Util.positionToTerm((int) ent.posX-minX, (int) ent.posY-minY);
        if (ent instanceof Car) {
            // is a anomaly if the system doesn't know about it
            final boolean isAnomaly = !((new Qa()).check(nar, lightState, "car", id, pos));
            if (isAnomaly) {
                Anomaly anomaly = new Anomaly(ent.posX, ent.posY);
                anomaly.remainingFramesToDisplay = 100;
                crossing.anomalies.add(anomaly);
            }

            inputs.add("<(*,car" + id + ","+ pos + ", [" + lightState + "]) --> at>. :|:");
            input += inputs.get(inputs.size()-1);
        }
        if (ent instanceof Pedestrian) {
            // is a anomaly if the system doesn't know about it
            final boolean isAnomaly = !((new Qa()).check(nar, lightState, "pedestrian", id, pos));
            if (isAnomaly) {
                Anomaly anomaly = new Anomaly(ent.posX, ent.posY);
                anomaly.remainingFramesToDisplay = 100;
                crossing.anomalies.add(anomaly);
            }

            inputs.add("<(*,pedestrian" + id + "," + pos + ", [" + lightState + "]) --> at>. :|:");
            input += inputs.get(inputs.size()-1);
        }
    }

    public void informAboutTrafficLight(Nar nar, TrafficLight light, int minX, int minY) {
        //String id = String.valueOf(light.id);
        String colour = light.colour == 0 ? "green" : "red";
        String narsese = "<trafficLight --> ["+colour+"]>. :|:";
        inputs.add(narsese);
        input+=narsese;
    }
    
    public boolean Input(Nar nar) {
        boolean hadInput = false;
        if(!input.equals(lastInput)) {
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
