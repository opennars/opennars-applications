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

import processing.core.PImage;

public class ImgDistTools {
    public static Dists calcDist(Map2d[] channels, int posX, int posY, int stepsize, PImage img) {
        int prototypeWidth = channels[0].retWidth();
        int prototypeHeight = channels[0].retHeight();

        if (posX <= prototypeWidth/2 || posX >= img.width-prototypeWidth/2) {
            return new Dists();
        }

        if (posY <= prototypeHeight/2 || posY >= img.height-prototypeHeight/2) {
            return new Dists();
        }

        float dist = 0;
        float mse = 0;

        for(int iy=0;iy<prototypeHeight;iy+=stepsize) {
            for(int ix=0;ix<prototypeWidth;ix+=stepsize){
                int dx = ix-prototypeWidth/2;
                int dy = iy-prototypeHeight/2;

                int x = posX + dx;
                int y = posY + dy;

                int colorcode =  img.pixels[y*img.width+x];
                //TODO check if the rgb is extracted correctly
                float r = (colorcode & 0xff) / 255.0f;
                float g = ((colorcode >> 8) & 0xFF) / 255.0f;
                float b = ((colorcode >> 8*2) & 0xFF) / 255.0f;

                float diff =
                        Math.abs(r - channels[0].readAtUnsafe(iy, ix)) +
                                Math.abs(g - channels[1].readAtUnsafe(iy, ix)) +
                                Math.abs(b - channels[2].readAtUnsafe(iy, ix));

                dist+=(diff * stepsize*stepsize); // we need to compute the error based on the covered area to get roughtly the same result with different stepsizes

                float diff2 =
                        Math.abs(r - channels[0].readAtUnsafe(iy, ix))*Math.abs(r - channels[0].readAtUnsafe(iy, ix)) +
                                Math.abs(g - channels[1].readAtUnsafe(iy, ix))*Math.abs(g - channels[1].readAtUnsafe(iy, ix)) +
                                Math.abs(b - channels[2].readAtUnsafe(iy, ix))*Math.abs(b - channels[2].readAtUnsafe(iy, ix));

                dist+=(diff * stepsize*stepsize); // we need to compute the error based on the covered area to get roughtly the same result with different stepsizes
                mse += diff2;
            }
        }

        dist /= (img.width*img.height); // normalize

        Dists dists = new Dists();
        dists.dist = dist;
        dists.mse = mse;
        return dists;
    }

    public static class Dists {
        float dist = Float.POSITIVE_INFINITY;
        float mse = Float.POSITIVE_INFINITY;
    }
}
