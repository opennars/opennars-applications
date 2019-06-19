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

import java.util.ArrayList;
import java.util.List;

public class MultichannelProtoClassifier {
    public List<MultichannelPrototype> prototypes = new ArrayList<>();

    public long classCounter = 1;

    public float maxDistance = Float.POSITIVE_INFINITY;//100000000000.0f;

    public float classificationLastDistance = Float.POSITIVE_INFINITY;
    public float classificationLastDistanceMse = Float.POSITIVE_INFINITY;

    public boolean hasPrototypes() {
        return prototypes.size() > 0;
    }

    public int retPrototypeWidth() {
        return prototypes.get(0).channels[0].retWidth();
    }
    public int retPrototypeHeight() {
        return prototypes.get(0).channels[0].retHeight();
    }

    // position is center of prototype to classify or add
    public long classifyAt(double posX, double posY, double width, double height, int stepsize, PImage img) {
        classificationLastDistance = Float.POSITIVE_INFINITY;
        classificationLastDistanceMse = Float.POSITIVE_INFINITY;

        long class_ = -1;

        for(MultichannelPrototype iPrototype : prototypes) {
            MultichannelPrototype.Dists dists = iPrototype.calcDist(posX, posY, width, height, stepsize, img);
            float currentDistance = dists.mse;
            if (currentDistance < classificationLastDistance) {
                classificationLastDistance = currentDistance;
                classificationLastDistanceMse = currentDistance;
                class_ = iPrototype.class_;
            }
        }

        if(classificationLastDistance > maxDistance) {
            class_ = -1;
        }

        return class_;
    }

    // position is center of prototype to add
    private MultichannelPrototype createNewPrototypeAt(double posX, double posY, double width, double height, int prototypeWidth, int prototypeHeight, PImage img) {
        if (posX <= width/2 || posX >= img.width-width/2) {
            return null;
        }

        if (posY <= height/2 || posY >= img.height-height/2) {
            return null;
        }

        Map2d[] channels = new Map2d[3];
        channels[0] = new Map2d(prototypeHeight, prototypeWidth);
        channels[1] = new Map2d(prototypeHeight, prototypeWidth);
        channels[2] = new Map2d(prototypeHeight, prototypeWidth);

        MultichannelPrototype created = new MultichannelPrototype(classCounter, channels);
        classCounter++;

        for(int iy=0;iy<prototypeHeight;iy++) {
            for(int ix=0;ix<prototypeWidth;ix++){
                double relX = (double)ix / prototypeWidth;
                double relY = (double)iy / prototypeHeight;

                double dx = (relX - 0.5) * width;
                double dy = (relY - 0.5) * height;

                int x = (int)(posX + dx);
                int y = (int)(posY + dy);

                int colorcode =  img.pixels[y*img.width+x];
                //TODO check if the rgb is extracted correctly
                float r = (colorcode & 0xff) / 255.0f;
                float g = ((colorcode >> 8) & 0xFF) / 255.0f;
                float b = ((colorcode >> 8*2) & 0xFF) / 255.0f;

                channels[0].writeAtSafe(iy,ix,r);
                channels[1].writeAtSafe(iy,ix,g);
                channels[2].writeAtSafe(iy,ix,b);
            }
        }

        return created;
    }

    public long forceAddPrototype(double centerX, double centerY, double width, double height,  int prototypeWidth, int prototypeHeight, PImage img) {
        long class_ = -1;
        classificationLastDistance = Float.POSITIVE_INFINITY;

        MultichannelPrototype createdPrototype = createNewPrototypeAt(centerX, centerY, width, height,  prototypeWidth, prototypeHeight, img);

        if (createdPrototype != null) {
            class_ = createdPrototype.class_;
            prototypes.add(createdPrototype);
        }

        return class_;
    }
}
