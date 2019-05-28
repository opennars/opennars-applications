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

import java.util.ArrayList;
import java.util.List;

/**
 * cellular automata to fill gaps
 */
public class CaFill {
    private List<boolean[][]> fillMasks;

    // query if the center bit has to be set to get filled
    public boolean queryFill(boolean[][] map) {
        if (map[1][1]) {
            return true; // must be filled if it is already filled
        }

        // check if a fillmap applies and return true if so
        for(boolean[][] iFillMask:fillMasks) {
            boolean allRequiredBitsSet = true;

            int requiredBitCount = 0; // required bits
            int setBitCount = 0; // really set bits

            for(int iy=0;iy<3;iy++) {
                for(int ix=0;ix<3;ix++) {
                    if (ix==1&&iy==1) {
                        continue; // ignore middle
                    }

                    if (iFillMask[iy][ix]) {
                        // bit must be true!

                        allRequiredBitsSet &= map[iy][ix];
                        requiredBitCount++;

                        if (map[iy][ix]) {
                            setBitCount++;
                        }
                    }
                }
            }

            if (allRequiredBitsSet && requiredBitCount > 0) {
                return true;
            }
        }

        return false;
    }

    public CaFill() {
        init();
    }

    private void init() {
        // fill direct gap
        // .x.
        // ...
        // .x.
        // must be filled
        boolean[][] mapDirectGap0 = new boolean[][]{
                {false, true, false},
                {false, false, false},
                {false, true, false},
        };
        // x..
        // ...
        // ..x
        // must be filled
        boolean[][] mapDirectGap1 = new boolean[][]{
                {true, false, false},
                {false, false, false},
                {false, false, true},
        };

        // fill indirect gap
        // x.x
        // ...
        // .x.
        // must be filled
        boolean[][] mapIndirectGap0 = new boolean[][]{
                {true, false, true},
                {false, false, false},
                {false, true, false},
        };

        fillMasks = new ArrayList<>();
        fillMasks.addAll(calcAllMirroredAndFlippedVersions(mapDirectGap0));
        fillMasks.addAll(calcAllMirroredAndFlippedVersions(mapDirectGap1));
        fillMasks.addAll(calcAllMirroredAndFlippedVersions(mapIndirectGap0));
    }

    private static List<boolean[][]> calcAllMirroredAndFlippedVersions(boolean[][] map) {
        List<boolean[][]> result = new ArrayList<>();

        for (boolean enableMirrorX : new boolean[]{false, true}) {
            for (boolean enableMirrorY : new boolean[]{false, true}) {
                for (boolean rotate90: new boolean[]{false, true}) {
                    boolean[][] res = map;
                    if (enableMirrorX) {
                        res = mirrorX(res);
                    }
                    if (enableMirrorY) {
                        res = mirrorY(res);
                    }
                    if (rotate90) {
                        res = rotate90(res);
                    }
                    result.add(res);
                }
            }
        }

        return result;
    }

    private static boolean[][] mirrorX(boolean[][] map) {
        return new boolean[][]{
                {map[0][2], map[0][1], map[0][0]},
                {map[1][2], map[1][1], map[1][0]},
                {map[2][2], map[2][1], map[2][0]}
        };
    }

    private static boolean[][] mirrorY(boolean[][] map) {
        return new boolean[][]{
                {map[2][0], map[2][1], map[2][2]},
                {map[1][0], map[1][1], map[1][2]},
                {map[0][0], map[0][1], map[0][2]}
        };
    }

    // flip x and y
    private static boolean[][] rotate90(boolean[][] map) {
        return new boolean[][]{
                {map[0][0], map[1][0], map[2][0]},
                {map[0][1], map[1][1], map[2][1]},
                {map[0][2], map[1][2], map[2][2]}
        };
    }
}
