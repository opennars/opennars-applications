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

import processing.core.PImage;

public class MultichannelPrototype {
    public Map2d[] channels; // color channels

    public long class_;

    public MultichannelPrototype(long class_, Map2d[] channels) {
        this.class_ = class_;
        this.channels = channels;
    }

    /**
     * calculate distance
     * returns inf if it is out of bounds
     * @param posX
     * @param posY
     * @param img
     * @return
     */
    public float calcDist(int posX, int posY, PImage img) {
        int prototypeWidth = channels[0].retWidth();
        int prototypeHeight = channels[0].retHeight();

        if (posX <= prototypeWidth/2 || posX >= img.width-prototypeWidth/2) {
            return Float.POSITIVE_INFINITY;
        }

        if (posY <= prototypeHeight/2 || posY >= img.height-prototypeHeight/2) {
            return Float.POSITIVE_INFINITY;
        }

        float dist = 0;

        for(int iy=0;iy<prototypeHeight;iy++) {
            for(int ix=0;ix<prototypeWidth;ix++){
                int dx = ix-prototypeWidth/2;
                int dy = iy-prototypeHeight/2;

                int x = posX + dx;
                int y = posY + dy;

                int colorcode =  img.pixels[y*img.width+x];
                //TODO check if the rgb is extracted correctly
                float r = (colorcode & 0xff) / 255.0f;
                float g = ((colorcode >> 8) & 0xFF) / 255.0f;
                float b = ((colorcode >> 8*2) & 0xFF) / 255.0f;

                dist += Math.abs(r - channels[0].readAtUnsafe(iy, ix));
                dist += Math.abs(g - channels[1].readAtUnsafe(iy, ix));
                dist += Math.abs(b - channels[2].readAtUnsafe(iy, ix));
            }
        }

        dist /= (img.width*img.height); // normalize
        return dist;
    }
}
