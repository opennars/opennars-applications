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
