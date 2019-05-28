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
