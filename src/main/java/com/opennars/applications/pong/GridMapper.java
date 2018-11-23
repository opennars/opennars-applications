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
package com.opennars.applications.pong;

import com.opennars.applications.common.Vec2Int;
import com.opennars.applications.componentbased.Entity;

public class GridMapper {
    public int cellsize = 10;

    public String mapPositionOfEntityToString(final Entity entity) {
        int posX = (int)(entity.posX / cellsize);
        int posY = (int)(entity.posY / cellsize);
        return posX + "_" + posY;
    }

    public Vec2Int mapStringToPosition(final String encoding) {
        double posX = Integer.valueOf(encoding.split("_")[0]);
        double posY = Integer.valueOf(encoding.split("_")[1]);

        posX += 0.5;
        posY += 0.5;

        posX *= cellsize;
        posY *= cellsize;

        return new Vec2Int((int)posX, (int)posY);
    }
}
