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
import java.util.Random;

public class Sdr {
    public boolean[] arr;

    public static Sdr makeNull(int size) {
        Sdr sdr = new Sdr();
        sdr.arr = new boolean[size];
        return sdr;
    }

    public Sdr clone() {
        Sdr res = makeNull(arr.length);
        for(int idx=0;idx<res.arr.length;idx++) {
            res.arr[idx] = arr[idx];
        }
        return res;
    }

    /**
     * symetric similarity
     * @return
     */
    public static float symSimilarity(Sdr a, Sdr b) {
        float sim=0;
        for(int idx=0;idx<a.arr.length;idx++) {
            if (a.arr[idx] == b.arr[idx]) {
                sim+=1;
            }
        }
        return sim / a.arr.length;
    }

    public Sdr permutate(int[] permutation) {
        Sdr res = makeNull(arr.length);
        for(int resIdx=0;resIdx<res.arr.length;resIdx++) {
            res.arr[resIdx] = arr[ permutation[resIdx] ];
        }
        return res;
    }

    public static Sdr union(Sdr a, Sdr b) {
        Sdr res = Sdr.makeNull(a.arr.length);
        for(int i=0;i<res.arr.length;i++) {
            res.arr[i] = a.arr[i] || b.arr[i];
        }
        return res;
    }

    // helper to compute random permutation
    public static int[] createRandomPermutation(int size, Random rng) {
        List<Integer> unused = new ArrayList<>();
        for(int i=0;i<size;i++) {
            unused.add(i);
        }

        List<Integer> resList = new ArrayList<>();
        while(unused.size() > 0) {
            int idx = rng.nextInt(unused.size());
            int val = unused.get(idx);
            unused.remove((int)idx);
            resList.add(val);
        }

        // copy to arr
        int[] resArr = new int[size];
        for(int idx=0;idx<size;idx++) {
            resArr[idx] = resList.get(idx);
        }
        return resArr;
    }
}
