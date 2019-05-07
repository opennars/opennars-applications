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

public class AttentionField {
    public Map2d map;

    public float decayFactor = 0.0f;

    public AttentionField(int height, int width) {
        map = new Map2d(height, width);
    }

    public int retHeight() {
        return map.retHeight();
    }

    public int retWidth() {
        return map.retWidth();
    }

    public void decay() {
        for(int i=0;i<map.arr.length;i++) {
            map.arr[i] *= decayFactor;
        }
    }

    public void addAt(int y, int x, float value) {
        map.arr[y*map.retWidth() + x] += value;
        map.arr[y*map.retWidth() + x] = Math.min(map.arr[y*map.retWidth() + x], 1.0f);
    }

    public float readAtUnbound(int y, int x) {
        return map.readAtSafe(y, x);
    }


    public void blur() {
        float[] newField = new float[map.arr.length];
        System.arraycopy(map.arr, 0, newField, 0, map.arr.length);

        for(int i=0;i<map.retHeight();i++) {
            for(int j=0;j<map.retWidth();j++) {
                newField[i*map.retWidth()+ j] += readAtUnbound(i+1, j) * 0.125f;
                newField[i*map.retWidth()+ j] += readAtUnbound(i-1, j) * 0.125f;
                newField[i*map.retWidth()+ j] += readAtUnbound(i, j-1) * 0.125f;
                newField[i*map.retWidth()+ j] += readAtUnbound(i, j+1) * 0.125f;
                newField[i*map.retWidth()+ j] += readAtUnbound(i, j) * 0.5f;
                //newField[i][j] /= (0.5f*4+1.0f);
            }
        }

        map.arr = newField;
    }
}
