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
