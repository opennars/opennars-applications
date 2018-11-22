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
    private final double[][] positions;
    private final double[] distances;

    public double width; // width of hexagon
    public double height; // height of hexagon
    
    public HexagonMapping(final double width, final double height) {
        this.width = width;
        this.height = height;

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
}
