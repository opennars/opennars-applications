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

import java.util.Random;

/**
 * drawing to SDR images
 */
public class SdrDraw {
    /**
     * draws a circle with less and less strength to the border
     * @param img
     * @param paintedSdr
     * @param posX
     * @param posY
     * @param radius
     */
    public static void drawConeCircle(Sdr[][] img, Sdr paintedSdr, int posX, int posY, int radius,   Random rng) {

        for(int iy=posY-radius;iy<posY+radius;iy++) {
            for(int ix=posX-radius;ix<posX+radius;ix++) {
                int diffX = ix - posX;
                int diffY = iy - posY;
                float dist = (float)Math.sqrt(diffX*diffX+diffY*diffY);
                if(dist > radius) {
                    continue;
                }

                float strength = 1.0f - dist / radius; // strength of the drawn SDR pixel

                Sdr drawnSdr = sampleSdr(paintedSdr, strength, rng);
                unionAtSafe(iy, ix, img, drawnSdr);
            }
        }
    }

    public static void unionAtSafe(int posY, int posX, Sdr[][] img, Sdr color) {
        if (posX < 0 || posX >= img[0].length) {
            return;
        }
        if (posY < 0 || posY >= img.length) {
            return;
        }

        img[posY][posX] = Sdr.union(img[posY][posX], color);
    }

    // helper
    // samples a SDR so only a x amount of bits are set
    public static Sdr sampleSdr(Sdr sdr, float ratio,   Random rng) {
        Sdr res = Sdr.makeNull(sdr.arr.length);

        for(int idx=0;idx<sdr.arr.length;idx++) {
            if (sdr.arr[idx] && rng.nextFloat() < ratio) { // sample bit by chance
                res.arr[idx] = true;
            }
        }

        return res;
    }
}
