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
package org.opennars.applications.crossing;

import processing.core.PApplet;

public class TrafficLight {

    public int GREEN = 0, RED = 1;
    public int posX, posY;
    public int id;
    public int colour = 0;
    public int radius = 0;

    public TrafficLight(int id, int radius, int positionX, int positionY, int colour) {
        this.radius = radius;
        this.id = id;
        this.posX = positionX;
        this.posY = positionY;
        this.colour = colour;
    }

    public void draw(PApplet applet, int t) {
        int g = colour == 0 ? 255 : 0;
        int r = colour == 1 ? 255 : 0;
        applet.fill(r, g, 0);
        applet.ellipse(posX, posY, 10, 10);
        if (t % 200 == 0) {
            colour = (colour + 1) % 2;
        }
    }
}
