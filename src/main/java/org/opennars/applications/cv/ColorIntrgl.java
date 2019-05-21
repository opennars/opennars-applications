/*
            DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
                    Version 2, December 2004
 Copyright (C) 2019 Robert Wünsche <rt09@protonmail.com>
 Everyone is permitted to copy and distribute verbatim or modified
 copies of this license document, and changing it is allowed as long
 as the name is changed.
            DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
   TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION
  0. You just DO WHAT THE FUCK YOU WANT TO.
 */

package org.opennars.applications.cv;

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
