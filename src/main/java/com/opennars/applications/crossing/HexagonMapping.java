/*
 * The MIT License
 *
 * Copyright 2018 The OpenNARS authors.
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
package com.opennars.applications.crossing;

public class HexagonMapping {
    //private final double[][] positions;
    //private final double[] distances;
    public final double[][] verticesRelative;

    public double width; // width of hexagon
    public double height; // height of hexagon

    public HexagonMapping(final double width, final double height) {
        this.width = width;
        this.height = height;

        // relative positions of the vertices of a single hexagon
        verticesRelative = new double[][]{
                {width * -0.5, height * -0.25},
                {width * -0.5, height * 0.25},

                {0.0, height * 0.5},

                {width * 0.5, height * 0.25},
                {width * 0.5, height * -0.25},

                {0.0, height * -0.5},
        };

        /*
        positions = new double[][]{
                {0.5 * width, -0.25 * height},
                {1.5 * width, -0.25 * height},

                {0.0, 0.5 * height},
                {width, 0.5 * height},
                {width + width, 0.5 * height},

                {0.5 * width, 1.25 * height},
                {1.5 * width, 1.25 * height},
        };

        distances = new double[positions.length];
        */
    }

    public double[] calcPositionOfHexagon(final int x, final int y) {
        final double offsetX = (y % 2) == 0 ? 0.0 : -0.5*width;

        double resultX = (x + 1) * width + offsetX;
        double resultY = 0.5*height + 0.75*height*y;
        return new double[]{resultX, resultY};
    }

    public Vec2Int map(final double x, final double y) {
        for(int ix=0;ix<100;ix++) {
            for(int iy=0;iy<100;iy++) {
                final double[] positionOfHexagon = calcPositionOfHexagon(ix, iy);



                boolean isInHexagon = isInHexagonRelative(x - positionOfHexagon[0], y - positionOfHexagon[1]);

                // HACK
                /*
                isInHexagon = isInPolygon(x - positionOfHexagon[0], y - positionOfHexagon[1],
                        0, 0,
                        5, 0,
                        0, 5
                        );
                */

                if (isInHexagon) {
                    return new Vec2Int(ix, iy);
                }
            }
        }

        // default!
        return new Vec2Int(5, 0);

        /*
        final int ix = (int)(x / (width*2.0));
        final int iy = (int)(y / (height*1.5));

        final double relX = x - ix * (width*2.0);
        final double relY = y - iy * (height*1.5);

        final Vec2Int relativeHexagonIndices = mapGroupToRelCell(relX, relY);
        return new Vec2Int(relativeHexagonIndices.x + ix*2, relativeHexagonIndices.y + iy*2);
        */
    }

    // public for testing
    public boolean isInHexagonRelative(final double x, final double y) {
        if (x==0 && y==0) {
            return true;
        }

        for(int i=0;i<6;i++) {
            final int iNext = (i + 1) % 6;
            final int iOtherSide = (i + 4) % 6;

            final boolean isInPolygonOfHexagon = isInPolygon(
                    x, y,
                    verticesRelative[iNext][0], verticesRelative[iNext][1],
                    verticesRelative[i][0], verticesRelative[i][1],
                    verticesRelative[iOtherSide][0], verticesRelative[iOtherSide][1]
            );
            if (isInPolygonOfHexagon) {
                return true;
            }
        }

        return false;
    }


    public static boolean isInPolygon(final double x, final double y, final double x0, final double y0, final double x1, final double y1, final double x2, final double y2) {
        final boolean side0 = side(x, y, x0, y0, x1, y1);
        final boolean side1 = side(x, y, x1, y1, x2, y2);
        final boolean side2 = side(x, y, x2, y2, x0, y0);

        return side0 && side1 && side2;
    }

    public static boolean side(final double x, final double y, final double ax, final double ay, final double bx, final double by) {
        final double dx = bx - ax;
        final double dy = by - ay;

        // perpendicular direction - which is the orientation of the edge
        final double idx = -dy;
        final double idy = dx;

        final double pdx = x - ax;
        final double pdy = y - ay;

        // side is computed by direction and difference
        return dot(idx, idy, pdx, pdy) >= 0.0;
    }

    private static double dot(final double ax, final  double ay, final double bx, final double by) {
        return ax*bx + ay*by;
    }



/*
    private Vec2Int mapGroupToRelCell(final double x, final double y)  {
        // see https://www.redblobgames.com/grids/hexagons/ for illustration



        for(int i=0;i<positions.length;i++) {
            distances[i] = Util.distance(x, y, positions[i][0], positions[i][0]);
        }

        int minDistanceIdx = 0;
        double minDistance = distances[0];
        for(int i=0;i<distances.length;i++) {
            if (distances[i] < minDistance) {
                minDistanceIdx = i;
                minDistance = distances[i];
            }
        }


        // fetch coordinate by index
        if (minDistanceIdx == 0) {
            return new Vec2Int(0, -1);
        }
        else if (minDistanceIdx == 1) {
            return new Vec2Int(1, -1);
        }

        else if (minDistanceIdx == 2) {
            return new Vec2Int(-1, 0);
        }
        else if (minDistanceIdx == 3) {
            return new Vec2Int(0, 0);
        }
        else if (minDistanceIdx == 4) {
            return new Vec2Int(1, 0);
        }

        else if (minDistanceIdx == 5) {
            return new Vec2Int(0, 1);
        }
        else {
            return new Vec2Int(1, 1);
        }
    }

    */
}
