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
