package com.opennars.applications.pong;

public class PixelScreen {
    public PixelScreen(int width, int height) {
        arr = new boolean[height][width];
    }

    public PatchRecords.Patch genPatchAt(int y, int x, int width, int height, long id) {
        PatchRecords.Patch resPatch = new PatchRecords.Patch(width, height, id);

        int py, px;

        for(py=0;py<height;py++) {
            for(px=0;px<width;px++) {
                int dy= py - height/2;
                int dx= px - width/2;


                int absX = x + dx;
                int absY = y + dy;

                if(absX >= retWidth() || absX < 0 || absY >= retHeight() || absY < 0) {
                    continue;
                }

                boolean readPixel = arr[absY][absX];
                resPatch.arr[py][px] = readPixel;
            }
        }

        return resPatch;
    }

    public void clear() {
        for(int y=0;y<retHeight();y++) {
            for(int x=0;x<retWidth();x++) {
                arr[y][x] = false;
            }
        }
    }

    public void drawDot(final int x, final int y) {
        if (x < 0 || x >= retWidth() || y < 0 || y >= retHeight()) {
            return;
        }
        arr[y][x] = true;
    }

    public boolean readAt(int y, int x) {
        if(x < 0 || y < 0 || x >= retWidth() || y >= retHeight()) {
            return false;
        }
        return arr[y][x];
    }

    public int retWidth() {
        return arr[0].length;
    }

    public int retHeight() {
        return arr.length;
    }

    public boolean arr[][];
}
