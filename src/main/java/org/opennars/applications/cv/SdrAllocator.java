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
