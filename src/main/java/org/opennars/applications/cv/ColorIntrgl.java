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

import java.util.ArrayList;
import java.util.List;

/**
 * Color integration of image
 */
public class ColorIntrgl {
    public double centerX;
    
    public static Color integrate(int gridX, int gridY, int gridsize, PImage img) {
        float resR=0, resG=0, resB=0;
        int cnt=0;
        
        int stride = 2;
        for(int iy=0;iy<gridsize;iy+=stride) {
            for(int ix=0;ix<gridsize;ix+=stride) {
                int colorcode =  img.pixels[iy*img.width+ix];
                //TODO check if the rgb is extracted correctly
                float r = (colorcode & 0xff) / 255.0f;
                float g = ((colorcode >> 8) & 0xFF) / 255.0f;
                float b = ((colorcode >> 8*2) & 0xFF) / 255.0f;
                
                resR += r;
                resG += g;
                resB += b;
                
                cnt++;
            }
        }
        
        return new Color(resR/cnt,resG/cnt,resB/cnt);
    }
    
    public static class Color {
        public float r,g,b;
        
        public Color(float r, float g, float b) {
            this.r=r;
            this.g=g;
            this.b=b;
        }
    }
}
