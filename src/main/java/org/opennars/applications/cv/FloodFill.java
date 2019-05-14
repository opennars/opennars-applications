/*
            DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
                    Version 2, December 2004

 Copyright (C) 2019 Robert Wünsche <rt09@protonmail.com>

 Everyone is permitted to copy and distribute verbatim or modified
 copies of this license document, and changing it is allowed as long
 as the name is changed.

            DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
   TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION

  0. You just DO WHAT THE FUCK YOU WANT TO.
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
