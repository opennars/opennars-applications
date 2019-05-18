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

/**
 * Prototype which stores for each color channel of a pixel a central distribution
 */
public class MultichannelCentralDistPrototype {
    public Map2dGeneric<IncrementalCentralDistribution>[] channels; // color channels

    public long class_;

    public MultichannelCentralDistPrototype(long class_, Map2dGeneric<IncrementalCentralDistribution>[] channels) {
        this.class_ = class_;
        this.channels = channels;
    }

    /**
     * revises all distributions with the new evidence
     * @param inputChannels
     */
    public void revise(Map2d[] inputChannels) {
        for(int iChannelIdx=0;iChannelIdx<channels.length;iChannelIdx++) {
            Map2dGeneric<IncrementalCentralDistribution> channelOfPrototype = this.channels[iChannelIdx];
            Map2d channelOfInput = inputChannels[iChannelIdx];

            for(int iy=0;iy<channelOfPrototype.retHeight();iy++) {
                for(int ix=0;ix<channelOfPrototype.retWidth();ix++) {
                    float val = channelOfInput.readAtSafe(iy,ix);
                    channelOfPrototype.readAtSafe(iy,ix).next(val);
                }
            }
        }
    }

    /**
     * calculate distance
     * returns inf if it is out of bounds
     * @param posX
     * @param posY
     * @param img
     * @return
     */
    public float calcDist(int posX, int posY, int stepsize, PImage img) {
        int prototypeWidth = channels[0].retWidth();
        int prototypeHeight = channels[0].retHeight();

        if (posX <= prototypeWidth/2 || posX >= img.width-prototypeWidth/2) {
            return Float.POSITIVE_INFINITY;
        }

        if (posY <= prototypeHeight/2 || posY >= img.height-prototypeHeight/2) {
            return Float.POSITIVE_INFINITY;
        }

        float dist = 0;

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
                    Math.abs(r - (float)channels[0].readAtUnsafe(iy, ix).mean) +
                    Math.abs(g - (float)channels[1].readAtUnsafe(iy, ix).mean) +
                    Math.abs(b - (float)channels[2].readAtUnsafe(iy, ix).mean);

                dist+=(diff * stepsize*stepsize); // we need to compute the error based on the covered area to get roughtly the same result with different stepsizes
            }
        }

        dist /= (img.width*img.height); // normalize
        return dist;
    }
}
