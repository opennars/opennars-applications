package com.opennars.applications.pong;

import java.util.ArrayList;
import java.util.List;

/**
 * simotaniously tries to track many patches
 */
public class PatchTracker {
    public List<TrackingRecord> trackingRecords = new ArrayList<>();

    public void frame(final PixelScreen screen) {
        for (final TrackingRecord iTrackingRecord : trackingRecords) {
            // try to "reaquire" the tracked patch as good as possible by sampling the neightborhood as close as possible while increasing the distance

            final PatchRecords.Patch patchAtCurrentPosition = screen.genPatchAt(iTrackingRecord.lastPosY, iTrackingRecord.lastPosX, -1);
            final double currentSimilarity = PatchRecords.sdrSimSym(iTrackingRecord.patch.retSdr(), patchAtCurrentPosition.retSdr());

            tryRetrace(screen, iTrackingRecord, currentSimilarity);
        }


        // timer
        for (final TrackingRecord iTrackingRecord : trackingRecords) {
            iTrackingRecord.timeSinceLastMove++;
        }

        // forgetting
        for(int idx=trackingRecords.size()-1;idx>=0;idx--) {
            if (trackingRecords.get(idx).timeSinceLastMove >= 0) {
                trackingRecords.remove(idx);
            }
        }
    }

    /**
     *
     * @param trackingRecord
     * @param minimalSimilarity threshold of the similarity for a successful match
     */
    private void tryRetrace(final PixelScreen screen, final TrackingRecord trackingRecord, double minimalSimilarity) {
        // we just search in it's immediate neightborhood


        int bestPositionX = trackingRecord.lastPosX;
        int bestPositionY = trackingRecord.lastPosY;

        // TODO< loose tracking if it fails to find a new one with a minimalSimilarity of threshold >

        // TODO< search more distance ? >
        for(int searchDist=0;searchDist<=2;searchDist++) {
            for(int dy=-searchDist;dy<=searchDist;dy++) {
                for(int dx=-searchDist;dx<=searchDist;dx++) {
                    final PatchRecords.Patch patchAtCurrentPosition = screen.genPatchAt(bestPositionY+dy, bestPositionX+dx, -1);
                    final double currentSimilarity = PatchRecords.sdrSimSym(trackingRecord.patch.retSdr(), patchAtCurrentPosition.retSdr());

                    if (currentSimilarity > minimalSimilarity) {
                        minimalSimilarity = currentSimilarity;
                        bestPositionX = bestPositionX+dx;
                        bestPositionY = bestPositionY+dy;
                    }
                }
            }
        }

        if ( trackingRecord.lastPosX != bestPositionX || trackingRecord.lastPosY != bestPositionY) {
            if (false)   System.out.println("retrace: last=<" + trackingRecord.lastPosX + "," + trackingRecord.lastPosY + ">  best=<" + bestPositionX + "," + bestPositionY + ">");

            // update
            trackingRecord.lastPosX = bestPositionX;
            trackingRecord.lastPosY = bestPositionY;

            // set timer so it can't get forgotten for a relativly long time
            // attention< we set it here higher because it was moving indeed >
            trackingRecord.timeSinceLastMove = -500;

            // it was moving
            trackingRecord.wasMoving = true;
        }

    }

    // structure to store which patch is where tracked
    public static class TrackingRecord {
        public PatchRecords.Patch patch;

        // last known position
        public int lastPosX;
        public int lastPosY;

        // timer used to decide when a patch is inactive
        public long timeSinceLastMove = -100;

        public boolean wasMoving = false;


        public boolean forget; // record will be forgotten if this flag is set
    }
}
