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

import org.opennars.entity.TruthValue;
import org.opennars.inference.TruthFunctions;
import org.opennars.main.Nar;

public class MapEvidence {

    public TruthValue street;
    public TruthValue sidewalk;
    public TruthValue bikelane;
    public TruthValue crosswalk;
    
    public MapEvidence(Nar locationNar) {
        street = new TruthValue(1.0f, 0.001f,locationNar.narParameters);
        sidewalk = new TruthValue(1.0f, 0.001f,locationNar.narParameters);
        bikelane = new TruthValue(1.0f, 0.001f,locationNar.narParameters);
        crosswalk = new TruthValue(1.0f, 0.001f,locationNar.narParameters);
    }

    public String choice() {
        if(bikelane.getExpectation() > sidewalk.getExpectation() && bikelane.getExpectation() > street.getExpectation() && bikelane.getExpectation() > crosswalk.getExpectation()) {
            return "bikelane"; //TODO bikelane
        }
        if(sidewalk.getExpectation() > bikelane.getExpectation() && sidewalk.getExpectation() > street.getExpectation() && sidewalk.getExpectation() > crosswalk.getExpectation()) {
            return "sidewalk";
        }
        if(crosswalk.getExpectation() > bikelane.getExpectation() && crosswalk.getExpectation() > street.getExpectation() && crosswalk.getExpectation() > sidewalk.getExpectation()) {
            return "crosswalk";
        }
        return "street";
    }
    
    public void collect(Nar locationNar, String type, TruthValue beliefTruth) {
        TruthValue neg = TruthFunctions.negation(beliefTruth, locationNar.narParameters);
        if(type.equals("street")) {
            street = TruthFunctions.revision(street, beliefTruth, locationNar.narParameters);
        }
        if(type.equals("sidewalk")) {
            sidewalk = TruthFunctions.revision(sidewalk, beliefTruth, locationNar.narParameters);
        }
        if(type.equals("bikelane")) {
            bikelane = TruthFunctions.revision(bikelane, beliefTruth, locationNar.narParameters);
        }
        if(type.equals("crosswalk")) {
            crosswalk = TruthFunctions.revision(crosswalk, beliefTruth, locationNar.narParameters);
        }
    }
}
