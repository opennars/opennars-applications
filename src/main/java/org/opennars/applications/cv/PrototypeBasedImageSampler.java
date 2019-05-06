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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// samples a image and stores the prototypes
public class PrototypeBasedImageSampler {
    public UnsupervisedPrototypeClassifier classifier = new UnsupervisedPrototypeClassifier();

    public Map2d heatmap = null; // heatmap used for stochastic sampling
    public int heatmapCellsize = 32;

    public Random rng = new Random();

    public int numberOfSamples = 8;

    public List<Classification> sample(PImage img) {
        List<Classification> classifications = new ArrayList<>();

        float integralOfHeatmap = 0;
        if (heatmap != null) {
            integralOfHeatmap = integralOfHeatmap();
        }


        for(int iSample=0;iSample<numberOfSamples;iSample++) {
            int posX = 0, posY = 0;

            if (heatmap != null) { // sample by heatmap
                float chosenIntgrlVal = rng.nextFloat() * integralOfHeatmap;

                float currentIntrl = 0;
                boolean intrlDone = false;
                for(int iy=0;iy<heatmap.retHeight();iy++) {
                    for(int ix=0;ix<heatmap.retWidth();ix++) {
                        currentIntrl += heatmap.readAtUnbound(iy,ix);

                        if(currentIntrl >= chosenIntgrlVal) { // integration finished, we found the position of this sample
                            posX = ix * heatmapCellsize;
                            posY = iy * heatmapCellsize;

                            intrlDone = true;
                            break;
                        }
                    }
                    if(intrlDone) {
                        break;
                    }
                }
            }
            else { // sample uniformly
                posX = rng.nextInt(img.width);
                posY = rng.nextInt(img.height);
            }

            int prototypeSize = 24; // size of the prototype
            int stride = 4;
            float[] convResult = Conv.convAt(img, posX, posY, prototypeSize, stride);

            long classification = classifier.classify(convResult);

            System.out.println("[d ] classification = " + Long.toString(classification));

            int here = 0;

            // put classification into result together with the coordinate
            Classification cl = new Classification();
            cl.posX = posX;
            cl.posY = posY;
            cl.class_ = classification;
            classifications.add(cl);
        }

        return classifications;
    }

    private float integralOfHeatmap() {
        float integral = 0;
        for(int iy=0;iy<heatmap.retHeight();iy++) {
            for(int ix=0;ix<heatmap.retWidth();ix++) {
                integral += heatmap.readAtUnbound(iy, ix);
            }
        }
        return integral;
    }

    public static class Classification {
        public int posX, posY;
        public long class_;
    }
}
