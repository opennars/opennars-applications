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
