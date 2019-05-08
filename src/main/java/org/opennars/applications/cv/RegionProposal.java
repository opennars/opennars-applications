package org.opennars.applications.cv;

public class RegionProposal {
    public int minX=0,minY=0,maxX=0,maxY=0;

    public static boolean checkOverlap(RegionProposal a, RegionProposal b) {
        //boolean xOverlap = a.minX <= b.minX && b.minX < a.maxX;
        //xOverlap = xOverlap || (a.minX <= b.maxX && b.maxX < a.maxX);


        boolean xNoOverlap = a.maxX < b.minX || a.minX > b.maxX;
        boolean yNoOverlap = a.maxY < b.minY || a.minY > b.maxY;

        return !xNoOverlap && !yNoOverlap;
    }

    public void merge(RegionProposal other) {
        minX = Math.min(minX, other.minX);
        minY = Math.min(minY, other.minY);
        maxX = Math.max(maxX, other.maxX);
        maxY = Math.max(maxY, other.maxY);
    }

    public RegionProposal clone() {
        RegionProposal cloned = new RegionProposal();
        cloned.minX = minX;
        cloned.minY = minY;
        cloned.maxX = maxX;
        cloned.maxY = maxY;
        return cloned;
    }
}
