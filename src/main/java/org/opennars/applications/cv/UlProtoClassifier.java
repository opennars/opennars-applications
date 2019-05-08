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

        //System.out.println("[d ] UlProtoClassifier   best classification distance = " + Float.toString(bestClassificationDist));

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
