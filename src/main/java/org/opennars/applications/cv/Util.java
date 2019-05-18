package org.opennars.applications.cv;

import processing.core.PImage;

public class Util {
    public static Map2d[] subimage(int posX, int posY, int width, int height, PImage img) {
        Map2d[] maps = new Map2d[3];
        maps[0] = new Map2d(height, width);
        maps[1] = new Map2d(height, width);
        maps[2] = new Map2d(height, width);

        if (posX <= width/2 || posX >= img.width-width/2) {
            return null; // not valid
        }

        if (posY <= height/2 || posY >= img.height-height/2) {
            return null; // not valid
        }

        for(int iy=0;iy<height;iy++) {
            for(int ix=0;ix<width;ix++){
                int dx = ix-width/2;
                int dy = iy-height/2;

                int x = posX + dx;
                int y = posY + dy;

                int colorcode =  img.pixels[y*img.width+x];
                //TODO check if the rgb is extracted correctly
                float r = ((colorcode >> 0) & 0xFF) / 255.0f;
                float g = ((colorcode >> 8*1) & 0xFF) / 255.0f;
                float b = ((colorcode >> 8*2) & 0xFF) / 255.0f;

                maps[0].writeAtSafe(iy,ix,r);
                maps[1].writeAtSafe(iy,ix,g);
                maps[2].writeAtSafe(iy,ix,b);
            }
        }

        return maps;
    }
}
