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

public class Util {
    public static Map2d[] subimage(int posX, int posY, int width, int height, PImage img) {
        Map2d[] maps = new Map2d[3];
        maps[0] = new Map2d(height, width);
        maps[1] = new Map2d(height, width);
        maps[2] = new Map2d(height, width);

        if (posX <= width/2 || posX >= img.width-width/2) {
            return null; // not valid
        }

        if (posY <= height/2 || posY >= img.height-height/2) {
            return null; // not valid
        }

        for(int iy=0;iy<height;iy++) {
            for(int ix=0;ix<width;ix++){
                int dx = ix-width/2;
                int dy = iy-height/2;

                int x = posX + dx;
                int y = posY + dy;

                int colorcode =  img.pixels[y*img.width+x];
                //TODO check if the rgb is extracted correctly
                float r = ((colorcode >> 0) & 0xFF) / 255.0f;
                float g = ((colorcode >> 8*1) & 0xFF) / 255.0f;
                float b = ((colorcode >> 8*2) & 0xFF) / 255.0f;

                maps[0].writeAtSafe(iy,ix,r);
                maps[1].writeAtSafe(iy,ix,g);
                maps[2].writeAtSafe(iy,ix,b);
            }
        }

        return maps;
    }

    public static Map2dGeneric<Boolean> subimageField(int posX, int posY, int width, int height, Map2dGeneric<Boolean> field, int cellsize) {
        Map2dGeneric<Boolean> map = new Map2dGeneric<>(height, width);
        for(int iy=0;iy<height;iy++) {
            for(int ix=0;ix<width;ix++) {
                map.writeAtUnsafe(iy,ix,false); // we need to init it
            }
        }

        if (posX <= width/2 || posX >= field.retWidth()*cellsize-width/2) {
            return null; // not valid
        }

        if (posY <= height/2 || posY >= field.retHeight()*cellsize-height/2) {
            return null; // not valid
        }

        for(int iy=0;iy<height;iy++) {
            for(int ix=0;ix<width;ix++){
                int dx = ix-width/2;
                int dy = iy-height/2;

                int x = posX + dx;
                int y = posY + dy;

                boolean fieldValue = readField(x, y, field, cellsize);

                map.writeAtSafe(iy,ix,fieldValue);
            }
        }

        return map;
    }



    // reads a field which is composed out of cells
    public static boolean readField(int absoluteX, int absoluteY, Map2dGeneric<Boolean> field, int cellsize) {
        return field.readAtSafe(absoluteY / cellsize, absoluteX / cellsize);
    }

    public static float calcDistBetweenImageAndPrototype(Map2d[] subimage, MultichannelCentralDistPrototype prototype) {
        float dist = 0;

        for(int iy=0;iy<prototype.channels[0].retHeight();iy++) {
            for(int ix=0;ix<prototype.channels[0].retWidth();ix++) {
                float subimgR = subimage[0].readAtUnsafe(iy, ix);
                float subimgG = subimage[1].readAtUnsafe(iy, ix);
                float subimgB = subimage[2].readAtUnsafe(iy, ix);

                float protoR = (float)prototype.channels[0].readAtUnsafe(iy, ix).mean;
                float protoG = (float)prototype.channels[1].readAtUnsafe(iy, ix).mean;
                float protoB = (float)prototype.channels[2].readAtUnsafe(iy, ix).mean;

                dist += Math.abs(subimgR - protoR)*Math.abs(subimgR - protoR);
                dist += Math.abs(subimgG - protoG)*Math.abs(subimgR - protoR);
                dist += Math.abs(subimgB - protoB)*Math.abs(subimgR - protoR);
            }
        }

        dist /= (prototype.channels[0].retHeight()*prototype.channels[0].retWidth()*3);

        return dist;
    }

    // calculated difference between ectual image and prototype
    public static float calcMaskedDiff(int posX, int posY, MultichannelCentralDistPrototype prototype, PImage img) {
        int prototypeWidth = prototype.channels[0].retWidth();
        int prototypeHeight = prototype.channels[0].retHeight();

        if (posX <= prototypeWidth/2 || posX >= img.width-prototypeWidth/2) {
            return Float.POSITIVE_INFINITY;
        }

        if (posY <= prototypeHeight/2 || posY >= img.height-prototypeHeight/2) {
            return Float.POSITIVE_INFINITY;
        }

        float dist = 0;

        int stepsize = 1;

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

                if (prototype.channels[0].readAtUnsafe(iy, ix).n == 0) {
                    continue; // optimization
                }

                float diff =
                    Math.abs(r - (float)prototype.channels[0].readAtUnsafe(iy, ix).mean) +
                    Math.abs(g - (float)prototype.channels[1].readAtUnsafe(iy, ix).mean) +
                    Math.abs(b - (float)prototype.channels[2].readAtUnsafe(iy, ix).mean);

                dist+=(diff * (float)prototype.channels[0].readAtUnsafe(iy, ix).n * stepsize*stepsize); // we need to compute the error based on the covered area to get roughtly the same result with different stepsizes


            }
        }

        dist /= (img.width*img.height); // normalize
        return dist;
    }
}
