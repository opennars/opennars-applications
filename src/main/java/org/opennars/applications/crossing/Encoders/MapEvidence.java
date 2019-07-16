/*
 * The MIT License
 *
 * Copyright 2019 OpenNARS.
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

import java.util.HashMap;
import java.util.Map;
import org.opennars.entity.TruthValue;
import org.opennars.inference.TruthFunctions;
import org.opennars.main.Nar;

public class MapEvidence {

    //The labels
    private String[] labels;
    //The mapping from label to truth
    private Map<String,TruthValue> ev = new HashMap<>();
    //Acceptance evidence
    public float minExpectation = 0.55f;
    
    public MapEvidence(Nar locationNar, String[] labels) {
        this.labels = labels;
        for(String s : labels) {
            ev.put(s, new TruthValue(1.0f, 0.001f,locationNar.narParameters));
        }
    }

    public String choice() {
        String maxKey = labels[0];
        double maxExpectation = 0;
        for(String s : ev.keySet()) {
            double curExpectation = ev.get(s).getExpectation();
            if(curExpectation >= maxExpectation) {
                maxKey = s;
                maxExpectation = curExpectation;
            }
        }
        if(maxExpectation < minExpectation) {
            return null;
        }
        return maxKey;
    }
    
    public void collect(Nar locationNar, String type, TruthValue beliefTruth) {
        ev.put(type, TruthFunctions.revision(ev.get(type), beliefTruth, locationNar.narParameters));
    }
}
