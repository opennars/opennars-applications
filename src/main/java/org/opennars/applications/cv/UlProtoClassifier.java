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
 * Prototype based classifier
 * Input is a vector of real values
 * Output is a class
 */
public class UlProtoClassifier {
    public List<Prototype> prototypes = new ArrayList<>();

    public long idCounter = 0;

    public float minDistance = 70.0f;//7.0f;//5.0f;//3.0f;

    // stimulus - classify or add new
    public long classify(float[] arr) {
        long bestClassification = -1;
        float bestClassificationDist = Float.MAX_VALUE;

        for(Prototype iPrototype : prototypes) {
            float dist = Prototype.dist(iPrototype, new Prototype(arr, -1));
            if (dist < bestClassificationDist) {
                bestClassificationDist = dist;
                bestClassification = iPrototype.id;
            }
        }

        System.out.println("[d ] UlProtoClassifier   best classification distance = " + Float.toString(bestClassificationDist));

        if (bestClassificationDist > minDistance) {
            float arrCopied[] = new float[arr.length];
            System.arraycopy(arr, 0, arrCopied, 0, arr.length);

            long resId = idCounter;
            prototypes.add(new Prototype(arrCopied, idCounter));
            idCounter++;
            return resId;
        }
        else {
            return bestClassification;
        }
    }

    static class Prototype {
        public float arr[];
        public long id; // classification id

        public Prototype(float arr[], long id) {
            this.arr = arr;
            this.id = id;
        }

        public static float dist(Prototype a, Prototype b) {
            float dist = 0;

            for(int i=0;i<a.arr.length;i++) {
                dist += Math.abs(a.arr[i] - b.arr[i]);
            }

            return dist;
        }
    }
}
