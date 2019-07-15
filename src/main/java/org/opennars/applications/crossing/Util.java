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
package org.opennars.applications.crossing;

import org.opennars.entity.TruthValue;

public class Util {

    public static int discretization = 10;
    
    public static double distance(double posX, double posY, double posX2, double posY2) {
        double dx = posX - posX2;
        double dy = posY - posY2;
        return Math.sqrt(dx * dx + dy * dy);
    }
    
    public static String positionToTerm(int X, int Y) {
        int posX = X / discretization;
        int posY = Y / discretization;
        return posX + "_" + posY;
    }
    
    public static float truthToValue(TruthValue truth) {
        if(truth == null) {
            return 1.0f;
        }
        return truth.getExpectation();
    }
    
    public static float timeToValue(long time) {
        return 10f/(1f+((float)time));
    }
}
