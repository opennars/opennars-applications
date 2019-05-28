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

public class FloodFill {
    public static Map2dGeneric<Integer> fill(Map2dGeneric<Boolean> inputMap) {
        Map2dGeneric<Integer> res = new Map2dGeneric<>(inputMap.retHeight(), inputMap.retWidth());
        // we need to init it
        for(int iy=0;iy<res.retHeight();iy++) {
            for(int ix=0;ix<res.retWidth();ix++) {
                res.writeAtSafe(iy,ix,-1); // -1 is not set
            }
        }

        // transfer input map as value 0 (which means not filled spot)
        for(int iy=0;iy<res.retHeight();iy++) {
            for(int ix=0;ix<res.retWidth();ix++) {
                if (inputMap.readAtSafe(iy,ix)) {
                    res.writeAtSafe(iy,ix,0); // 0 is set but not yet filled
                }
            }
        }

        int regionIdCounter = 1; // counter for the regions
        // scan for not assigned colors and fill it with a new id
        for(int iy=0;iy<res.retHeight();iy++) {
            for(int ix=0;ix<res.retWidth();ix++) {
                if (res.readAtSafe(iy,ix) == 0) { // 0 means it's not assigned with a color yet
                    floodFillRec(iy,ix, regionIdCounter, res);
                    regionIdCounter++;
                }
            }
        }

        return res;
    }

    private static void floodFillRec(int iy, int ix, int regionId, Map2dGeneric<Integer> map) {
        if (iy < 0 || iy >= map.retHeight()) {
            return;
        }
        if (ix < 0 || ix >= map.retWidth()) {
            return;
        }
        if (map.readAtSafe(iy,ix) == -1) { // in empty black pixel
            return; // because we are done and can't fill
        }

        if (map.readAtSafe(iy,ix) == regionId) {
            return; // because we were here before, so we avoid looping
        }

        if (map.readAtSafe(iy,ix) == 0) { // is not filled with a color
            map.writeAtSafe(iy,ix,regionId);
        }

        // call recursivly to fill neightbor cells
        floodFillRec(iy,ix-1,regionId,map);
        floodFillRec(iy,ix+1,regionId,map);
        floodFillRec(iy-1,ix,regionId,map);
        floodFillRec(iy+1,ix,regionId,map);
    }
}
