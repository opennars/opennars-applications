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

import org.opennars.applications.crossing.UnrealCrossing;
import processing.core.PImage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import static processing.core.PConstants.RGB;

public class ClassDatabase {
    public boolean isDirty = false; // used to decide when to upload something

    public Map<Long, Class> classesByClassId = new HashMap<>();

    // counter for the unique id of new classes
    public long classIdCounter = Integer.MAX_VALUE; // set to high value to guard against interfering with other class id's (just for debugging)

    public Class createNewClass(int imageHeight, int imageWidth) {
        Class createdClass = new Class(classIdCounter, imageHeight, imageWidth);
        classesByClassId.put(classIdCounter, createdClass);
        classIdCounter++;

        isDirty = true; // is dirty because we added a new class

        return createdClass;
    }

    public Class retrieveById(long class_) {
        return classesByClassId.get(class_);
    }

    public static class Class {
        public MultichannelCentralDistPrototype prototype;

        // all retained samples (under AIKR)
        // TODO< use real database to store the information on disk and retrieve or remove from database when necessary >
        // items are Map2d's with the channel information
        public List<Map2d[]> samples = new ArrayList<>();

        public long class_;

        // for debugging
        public PImage protoMeanImg; // image of the mean of the prototype

        public boolean isDirty;

        public long revisionCount = 0;

        public Class(long class_, int imageHeight, int imageWidth) {
            this.class_ = class_;

            Map2dGeneric<IncrementalCentralDistribution>[] channels = new Map2dGeneric[3];
            for(int iChannelIdx=0;iChannelIdx<3;iChannelIdx++) {
                channels[iChannelIdx] = new Map2dGeneric<>(imageHeight, imageWidth);

                for(int iy=0;iy<channels[iChannelIdx].retHeight();iy++) {
                    for(int ix=0;ix<channels[iChannelIdx].retWidth();ix++) {
                        channels[iChannelIdx].writeAtSafe(iy, ix, new IncrementalCentralDistribution());
                    }
                }
            }
            prototype = new MultichannelCentralDistPrototype(class_, channels);
        }

        /* commented because not used
        public void revise(Map2d[] imageColors, PApplet applet) {
            prototype.revise(imageColors);

            // update image for debugging purposes
            protoMeanImg = applet.createImage(128,128, RGB); // image of prototype


            for(int y=0;y<prototype.channels[0].retHeight();y++) {
                for(int x=0;x<prototype.channels[0].retWidth();x++) {
                    double r = prototype.channels[0].readAtSafe(y,x).mean;
                    double g = prototype.channels[1].readAtSafe(y,x).mean;
                    double b = prototype.channels[2].readAtSafe(y,x).mean;

                    protoMeanImg.pixels[y*protoMeanImg.width + x] = applet.color((int)(r*255),(int)(g*255),(int)(b*255));
                }
            }
        }
         */

        public void reviseByMask(Map2d[] imageColors, Map2dGeneric<Boolean> mask, UnrealCrossing applet) {
            if (revisionCount >= 1) {
                return; // FOR DEBUGGING
            }

            revisionCount++;


            isDirty = true;

            prototype.reviseByMask(imageColors, mask);

            // update image for debugging purposes
            protoMeanImg = applet.createImage(128,128, RGB); // image of prototype


            for(int y=0;y<prototype.channels[0].retHeight();y++) {
                for(int x=0;x<prototype.channels[0].retWidth();x++) {
                    double r = prototype.channels[0].readAtSafe(y,x).mean;
                    double g = prototype.channels[1].readAtSafe(y,x).mean;
                    double b = prototype.channels[2].readAtSafe(y,x).mean;

                    protoMeanImg.pixels[y*protoMeanImg.width + x] = applet.color((int)(r*255),(int)(g*255),(int)(b*255));
                }
            }
        }

        /**
         * @return human readable class id, is NOT the real class id!
         */
        public long retHumanReadableClass() {
            return class_ - Integer.MAX_VALUE; // because we start counting classes with Integer.MAX_VALUE
        }
    }
}
