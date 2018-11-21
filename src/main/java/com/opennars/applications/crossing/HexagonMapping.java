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
    public double width; // width of hexagon
    public double height; // height of hexagon

    public HexagonMapping(final double width, final double height) {
        this.width = width;
        this.height = height;
    }

    public double[] calcPositionOfHexagon(final int x, final int y) {
        final double offsetX = (y % 2) == 0 ? 0.0 : -0.5*width;

        double resultX = (x + 1) * width + offsetX;
        double resultY = 0.5*height + 0.75*height*y;
        return new double[]{resultX, resultY};
    }

    public Vec2Int map(final double x, final double y) {
        final int ix = (int)(x / (width*2.0));
        final int iy = (int)(y / (height*1.5));

        final double relX = x - ix * (width*2.0);
        final double relY = y - iy * (height*1.5);

        final Vec2Int relativeHexagonIndices = mapGroupToRelCell(relX, relY);
        return new Vec2Int(relativeHexagonIndices.x + ix*2, relativeHexagonIndices.y + iy*2);
    }

    private Vec2Int mapGroupToRelCell(final double x, final double y)  {
        // see https://www.redblobgames.com/grids/hexagons/ for illustration

        double[][] positions = new double[][]{
                {0.5 * width, -0.25 * height},
                {1.5 * width, -0.25 * height},

                {0.0, 0.5 * height},
                {width, 0.5 * height},
                {width + width, 0.5 * height},

                {0.5 * width, 1.25 * height},
                {1.5 * width, 1.25 * height},
        };

        double[] distances = new double[positions.length];

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

        /*

        final int topRowResult = mapXRowOfHexagons(x + width * 0.5, y + height * 0.75);
        if (topRowResult != 2) {
            return new Vec2Int(topRowResult, -1);
        }

        final int centerRowResult = mapXRowOfHexagons(x, y);
        if (centerRowResult != 2) {
            return new Vec2Int(centerRowResult, 0);
        }

        final int bottomRowResult = mapXRowOfHexagons(x + width * 0.5, y - height * 0.75);
        if (bottomRowResult != 2) {
            return new Vec2Int(bottomRowResult, 1);
        }
        */

        //System.out.println(x);
        //System.out.println(y);

        // must never happen
        //throw new InternalError();
    }

    // maps the coordinate to the x index of the cell
    // see top row of illustration
    // -1 if the cell before the center cell
    // 0 if it is the center cell
    // 1 if it is after the center cell
    // 2 if it not a cell before or after or the center cell
    private int mapXRowOfHexagons(final double x, final double y) {
        // see https://www.redblobgames.com/grids/hexagons/ for illustration

        // map coordinates to relative hexagon coordinates

        // absolute to hexagon pairs
        final double hx = x / (width*2.0);
        final double hy = y / (height*1.0);

        if( isInSingleCell(mapTo11((hx) * 0.5), mapTo11(hy)) ) {
            return -1;
        }
        if( isInSingleCell(mapTo11((hx - 0.5) * 0.5), mapTo11(hy)) ) {
            return 0;
        }
        else if( isInSingleCell(mapTo11((hx - 1.0) * 0.5), mapTo11(hy)) ) {
            return 1;
        }
        else {
            return 2;
        }
    }

    // /param rx relative x to center - -1.0 to 1.0
    // /param ry relative y to center - -1.0 to 1.0
    private boolean isInSingleCell(final double rx, final double ry) {
        final double absRelYCenter = Math.abs(ry);
        final double absRelXCenter = Math.abs(rx);
        // is not inside it if outside of -1.0 to 1.0 range
        if (absRelXCenter > 1.0) {
            return false;
        }

        // compute coordinate system relative to the edge
        final double rrx = absRelXCenter + 2.0;
        final double rry = absRelYCenter;

        // is in hexagon if it is inside the cut area
        return rrx <= rry;
    }

    // maps 0.0 - 1.0 to -1.0 - 1.0
    private static double mapTo11(final double v) {
        return -1.0 + v * 2.0;
    }
}
