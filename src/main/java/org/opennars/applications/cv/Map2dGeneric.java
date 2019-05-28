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

final public class Map2dGeneric<Type> {
    public Type[] arr;
    private int width;

    public Map2dGeneric(int height, int width) {
        arr = (Type[])new Object[height*width];
        this.width = width;
    }

    public int retHeight() {
        return arr.length / width;
    }

    public int retWidth() {
        return width;
    }

    public Type readAtSafe(int y, int x) {
        if (y < 0 || y >= retHeight() || x < 0 || x >= retWidth()) {
            return null;
        }
        return arr[y*retWidth() + x];
    }

    public void writeAtSafe(int y, int x, Type value) {
        if (y < 0 || y >= retHeight() || x < 0 || x >= retWidth()) {
            return;
        }
        arr[y*retWidth() + x] = value;
    }

    public void writeAtUnsafe(int y, int x, Type value) {
        arr[y*retWidth() + x] = value;
    }

    public Type readAtUnsafe(int y, int x) {
        return arr[y*retWidth() + x];
    }
}
