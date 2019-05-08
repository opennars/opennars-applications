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

import java.util.ArrayList;
import java.util.List;

/**
 * Unsupervised SDR based prototype classifier
 *
 * Input are "images" where each pixel is a SDR
 * Output is a classification as an id
 */
public class UlSdrProtoClassifier {
    public List<Prototype> prototypes = new ArrayList<>();

    public long idCounter = 0;

    // TODO< TUNE >
    public float minDistance = 5.0f;//3.0f;

    public long classify(Sdr sdr) {


        long bestClassification = -1;
        float bestClassificationDist = Float.MAX_VALUE;

        for(Prototype iPrototype : prototypes) {
            float dist = Prototype.dist(iPrototype, new Prototype(sdr, -1));
            if (dist < bestClassificationDist) {
                bestClassificationDist = dist;
                bestClassification = iPrototype.id;
            }
        }

        System.out.println("[d ] UlSdrProtoClassifier   best classification distance = " + Float.toString(bestClassificationDist));

        if (bestClassificationDist > minDistance) {
            /*
            Sdr arrCopied[][] = new Sdr[sdrImage.length][sdrImage[0].length];
            // copy all SDR's
            for(int j=0;j<sdrImage.length;j++) {
                for(int i=0;i<sdrImage[0].length;i++) {
                    arrCopied[j][i] = sdrImage[i][j].clone();
                }
            }
            */

            long resId = idCounter;
            prototypes.add(new Prototype(sdr.clone(), idCounter));
            idCounter++;
            return resId;
        }
        else {
            return bestClassification;
        }
    }


    static class Prototype {
        public Sdr sdr;
        public long id; // classification id

        public Prototype(Sdr sdr, long id) {
            this.sdr = sdr;
            this.id = id;
        }

        public static float dist(Prototype a, Prototype b) {
            return Sdr.symSimilarity(a.sdr, b.sdr);

            /*
            for(int j=0;j<a.arr.length;j++) {
                for(int i=0;i<a.arr.length;i++) {
                    dist += Sdr.symSimilarity(a.arr[i][j], b.arr[i][j]);
                }
            }

            return dist;
            */
        }
    }
}
