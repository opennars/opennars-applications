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

public class Map2d {
    public float[][] arr;

    public Map2d(int height, int width) {
        arr = new float[height][width];
        for(int i=0;i<arr.length;i++) {
            for(int j=0;j<arr[i].length;j++) {
                arr[i][j] = 0;
            }
        }
    }

    public int retHeight() {
        return arr.length;
    }

    public int retWidth() {
        return arr[0].length;
    }

    public float readAtUnbound(int y, int x) {
        if (y < 0 || y >= arr.length || x < 0 || x >= arr[0].length) {
            return 0.0f;
        }
        return arr[y][x];
    }
}
