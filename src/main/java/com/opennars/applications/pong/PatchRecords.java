package com.opennars.applications.pong;

import java.util.ArrayList;
import java.util.List;

/**
 * Keeps track of patches - which are small rectangular snippets of the screen
 */
public class PatchRecords {
    public static class Patch {
        public boolean arr[][];

        public long id;

        // used to decide between foreground and background
        //public long noMovementScore = 0; // incremented when the patch is not moving at all
        //public long movementScore = 0; // incremented when the patch is moving

        public Patch(int patchSize, long id) {
            arr = new boolean[patchSize][patchSize];
            this.id = id;
        }

        public boolean[] retSdr() {
            boolean[] r = new boolean[arr.length * arr[0].length * 2];
            int idx=0;
            for( int y = 0; y < arr.length; y++ ) for( int x = 0; x < arr[0].length; x++ )
                r[idx++] = arr[y][x];

            for( int y = 0; y < arr.length; y++ ) for( int x = 0; x < arr[0].length; x++ )
                r[idx++] = !arr[y][x];
            return r;
        }
    }

    public List<Patch> patches = new ArrayList<>();

    public double resultSimilarity = 0.0; // used to return the similarity of the last query

    public Patch querySdrMostSimiliarPatch(final Patch patch) {
        Patch bestPatch = null;
        resultSimilarity = 0.0;

        if (patches.size() == 0) {
            return bestPatch;
        }

        bestPatch = patches.get(0);

        for(final Patch other : patches) {
            if (sdrSimSym(patch.retSdr(), other.retSdr()) > resultSimilarity) {
                resultSimilarity = sdrSimSym(patch.retSdr(), other.retSdr());
                bestPatch = other;
            }
        }

        return bestPatch;
    }

    public void addPatch(final Patch patch) {
        patches.add(patch);
    }

    // SDR symetric similarity
    public static double sdrSimSym(final boolean[] a, final boolean[] b) {
        return (double)sdrAnd(a, b) / a.length;
    }

    private static int sdrAnd(final boolean[] a, final boolean[] b) {
        int c=0;
        for(int i=0;i<a.length;i++) {
            c += (a[i]&b[i]?1:0);
        }
        return c;
    }
}
