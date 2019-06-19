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

import processing.core.PImage;

public class MultichannelPrototype {
    public Map2d[] channels; // color channels

    public long class_;

    public MultichannelPrototype(long class_, Map2d[] channels) {
        this.class_ = class_;
        this.channels = channels;
    }

    /**
     * calculate distance
     * returns inf if it is out of bounds
     * @param posX
     * @param posY
     * @param img
     * @return
     */
    public Dists calcDist(double posX, double posY, double widht, double height, int stepsize, PImage img) {
        ImgDistTools.Dists dists2 = ImgDistTools.calcDist(channels, posX, posY, widht, height, stepsize, img);
        Dists dists = new Dists();
        dists.dist = dists2.dist;
        dists.mse = dists2.mse;
        return dists;
    }

    public static class Dists {
        float dist = Float.POSITIVE_INFINITY;
        float mse = Float.POSITIVE_INFINITY;
    }
}
