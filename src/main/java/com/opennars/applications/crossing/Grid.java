package com.opennars.applications.crossing;

public class Grid {
    public static int retSizeOfGridType(final EnumGridSize gridSize) {
        if (gridSize == EnumGridSize.COARSE) {
            return 10;
        }
        return 5; // for EnumGridSize.FINE
    }

    enum EnumGridSize {
        COARSE,
        FINE
    }
}
