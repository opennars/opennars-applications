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

import java.util.*;

/**
 * segments particles by motion
 */
public class MotionSegmentationService {
    public double motionQuanization = 5.0; // config

    public int motionParticleGridCellsize = 32; // config

    public double particleSegmentationMaxFuseDistance = 25.0; // config - max distance of particles to fuse into one segmentation

    public List<SegmentatedMotionParticlesInBoundingHull> segmentate(List<MotionParticle> motionParticles, int imageWidth, int imageHeight) {
        // we put the motion particles into a grid to accelerate the whole algorithm to an accaptable level
        Map2dGeneric<List<MotionParticle>> gridOfMotionParticles = new Map2dGeneric<>(imageHeight / motionParticleGridCellsize + 1, imageWidth / motionParticleGridCellsize + 1);
        for(int iy=0;iy<gridOfMotionParticles.retHeight();iy++) {
            for(int ix=0;ix<gridOfMotionParticles.retWidth();ix++) {
                gridOfMotionParticles.writeAtSafe(iy,ix, new ArrayList<>());
            }
        }

        for(MotionParticle iParticle : motionParticles) {
            int cellX = (int)(iParticle.posX / motionParticleGridCellsize);
            int cellY = (int)(iParticle.posY / motionParticleGridCellsize);

            boolean isMotionValid = iParticle.retVelX() != Double.NaN && iParticle.retVelY() != Double.NaN;
            if (isMotionValid) { // filter for invalid motion
                gridOfMotionParticles.readAtSafe(cellY,cellX).add(iParticle);
            }
        }


        if (motionParticles.size() > 0) {
            int here = 42;
        }


        // and now we iterate over the cells and try to merge the particles in them to the segmentations
        List<SegmentatedMotionParticlesInBoundingHull> unfusedSegmentations = new ArrayList<>();
        for(int iCellY=0;iCellY<gridOfMotionParticles.retHeight();iCellY++) {
            for(int iCellX=0;iCellX<gridOfMotionParticles.retWidth();iCellX++) {

                List<MotionParticle> motionparticlesOfCell = gridOfMotionParticles.readAtUnsafe(iCellY,iCellX);

                // quantize motion
                Map<Vec2Long, List<MotionParticle>> motionParticlesByQuanizedMotion = new HashMap<>();
                quantizeMotionOfParticles(motionparticlesOfCell, motionParticlesByQuanizedMotion);

                for(Map.Entry<Vec2Long, List<MotionParticle>> iEntry : motionParticlesByQuanizedMotion.entrySet()) {
                    SegmentatedMotionParticlesInBoundingHull createdSegmentation = new SegmentatedMotionParticlesInBoundingHull();
                    createdSegmentation.quanizedMotion = iEntry.getKey();
                    createdSegmentation.motionParticles = iEntry.getValue();
                    unfusedSegmentations.add(createdSegmentation);
                }
            }
        }

        // we need to update the hulls
        for(SegmentatedMotionParticlesInBoundingHull iSegmenentation : unfusedSegmentations) {
            iSegmenentation.updateHull();
        }

        // last step is a try to merge the segmentations if possible
        // merging is possible if the distance between the boundaries (in the direction) is not above the threshold
        return fuseSegmentations(unfusedSegmentations, particleSegmentationMaxFuseDistance);
    }

    public static List<SegmentatedMotionParticlesInBoundingHull> fuseSegmentations(List<SegmentatedMotionParticlesInBoundingHull> unfusedSegmentations, double particleSegmentationMaxFuseDistance) {
        List<SegmentatedMotionParticlesInBoundingHull> fusedSegmentations = new ArrayList<>();
        fusedSegmentations.addAll(unfusedSegmentations); // copy


        for(;;) {
            boolean wasFused = false;

            loop:
            for(int idxOuter=0;idxOuter<fusedSegmentations.size();idxOuter++) {
                for(int idxInner = idxOuter+1;idxInner<fusedSegmentations.size();idxInner++) {
                    SegmentatedMotionParticlesInBoundingHull inner = fusedSegmentations.get(idxInner);
                    SegmentatedMotionParticlesInBoundingHull outer = fusedSegmentations.get(idxOuter);

                    boolean sameQuantizedVelocity = inner.quanizedMotion.equals(outer.quanizedMotion);

                    if (sameQuantizedVelocity) {
                        int here = 5;
                    }

                    if (sameQuantizedVelocity && checkSegmentationsFusable(inner, outer, particleSegmentationMaxFuseDistance)) {
                        // fuse
                        outer.motionParticles.addAll(inner.motionParticles);
                        outer.updateHull();
                        fusedSegmentations.remove(idxInner);
                        wasFused = true;
                        break loop; // repeat search for fusable segmentations
                    }
                }
            }

            if (!wasFused) {
                break; // done when we are here
            }
        }

        return fusedSegmentations;
    }

    public static boolean checkSegmentationsFusable(SegmentatedMotionParticlesInBoundingHull a, SegmentatedMotionParticlesInBoundingHull b, double maxFuseDistance) {
        for(MotionParticle iParticleA : a.motionParticles) {
            for(MotionParticle iParticleB : b.motionParticles) {
                if (iParticleA == iParticleB) {
                    continue;// not valid case, we just ignore the invalid case non the less
                }

                double diffX = iParticleA.posX - iParticleB.posX;
                double diffY = iParticleA.posY - iParticleB.posY;
                double dist = Math.sqrt(diffX*diffX+diffY*diffY);

                if (dist > maxFuseDistance) {
                    continue;
                }

                return true;
            }
        }
        return false;
    }

    private void quantizeMotionOfParticles(List<MotionParticle> motionParticles, Map<Vec2Long, List<MotionParticle>> motionParticlesByQuanizedMotion) {
        for(MotionParticle iParticle : motionParticles) {
            Vec2Long quanizedMotion = new Vec2Long((long)(iParticle.retVelX() / motionQuanization), (long)(iParticle.retVelY() / motionQuanization));

            if (quanizedMotion.x != 0 || quanizedMotion.y != 0) {
                // debug
            }


            if (!motionParticlesByQuanizedMotion.containsKey(quanizedMotion)) {
                motionParticlesByQuanizedMotion.put(quanizedMotion, new ArrayList<>());
            }
            motionParticlesByQuanizedMotion.get(quanizedMotion).add(iParticle);
        }
    }

    public static class Vec2Long {
        public long x,y;
        public Vec2Long(long x, long y) {
            this.x=x;
            this.y=y;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Vec2Long vec2Long = (Vec2Long) o;
            return x == vec2Long.x &&
                    y == vec2Long.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
    }

    public static class SegmentatedMotionParticlesInBoundingHull {
        public List<MotionParticle> motionParticles = new ArrayList<>();

        public BoundingHull hull;

        public Vec2Long quanizedMotion;

        public void updateHull() {
            hull = new BoundingHull();
            for(MotionParticle iParticle : motionParticles) {
                hull.union(iParticle.posX, iParticle.posY);
            }
        }
    }

    /**
     * BoundingHull hull in x, y, and diagonal direction(s)
     */
    public static class BoundingHull {
        public DirAndMinMax[] dirs;

        public BoundingHull() {
            dirs = new DirAndMinMax[3];
            dirs[0] = new DirAndMinMax(new Vec2(1.0, 0.0));
            dirs[1] = new DirAndMinMax(new Vec2(0.0, 1.0));
            dirs[2] = new DirAndMinMax(new Vec2(1.0/Math.sqrt(2), 1.0/Math.sqrt(2)));
        }

        public void union(double x, double y) {
            for(DirAndMinMax iDirMinAndMax : dirs) {
                double dotResult = x*iDirMinAndMax.dir.x+y*iDirMinAndMax.dir.y;

                iDirMinAndMax.max = Math.max(iDirMinAndMax.max, dotResult);
                iDirMinAndMax.min = Math.min(iDirMinAndMax.min, dotResult);
            }
        }

        public static class DirAndMinMax {
            public Vec2 dir;
            public double min = Double.MAX_VALUE, max = Double.MIN_VALUE;

            public DirAndMinMax(Vec2 dir) {
                this.dir = dir;
            }
        }

        public static class Vec2 {
            public double x,y;
            public Vec2(double x, double y) {
                this.x = x;
                this.y = y;
            }
        }
    }
}
