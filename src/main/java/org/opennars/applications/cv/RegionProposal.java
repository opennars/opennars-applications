/*
 * The MIT License
 *
 * Copyright 2019 Robert Wünsche <rt09@protonmail.com>
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
package org.opennars.applications.cv;

public class RegionProposal {
    public int minX=0,minY=0,maxX=0,maxY=0;

    public static boolean checkOverlap(RegionProposal a, RegionProposal b) {
        //boolean xOverlap = a.minX <= b.minX && b.minX < a.maxX;
        //xOverlap = xOverlap || (a.minX <= b.maxX && b.maxX < a.maxX);


        boolean xNoOverlap = a.maxX <= b.minX || a.minX >= b.maxX;
        boolean yNoOverlap = a.maxY <= b.minY || a.minY >= b.maxY;

        return !xNoOverlap && !yNoOverlap;
    }

    public void merge(RegionProposal other) {
        minX = Math.min(minX, other.minX);
        minY = Math.min(minY, other.minY);
        maxX = Math.max(maxX, other.maxX);
        maxY = Math.max(maxY, other.maxY);
    }

    public RegionProposal clone() {
        RegionProposal cloned = new RegionProposal();
        cloned.minX = minX;
        cloned.minY = minY;
        cloned.maxX = maxX;
        cloned.maxY = maxY;
        return cloned;
    }
}
