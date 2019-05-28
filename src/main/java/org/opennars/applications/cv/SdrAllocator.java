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

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * allocates SDR's by id
 */
public class SdrAllocator {
    public Map<Long, Sdr> idToSdr = new HashMap<>();

    public Random rng = new Random();

    public int sdrSize = 0;
    public int sdrUsedBits = 0;

    public Sdr retSdrById(long id) {
        if (!idToSdr.containsKey(id)) {
            // we need to generate a new SDR, store and return it

            Sdr sdr = Sdr.makeNull(sdrSize);
            for(int i=0;i<sdrUsedBits;i++) {
                sdr.arr[rng.nextInt(sdr.arr.length)] = true;
            }

            idToSdr.put(id, sdr);
        }

        return idToSdr.get(id);
    }

}
