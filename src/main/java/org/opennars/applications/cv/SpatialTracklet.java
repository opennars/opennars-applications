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

public class SpatialTracklet {
    public double posX;
    public double posY;
    public long id;
    public long idletime = 0;
    public long notTrainedSince = Integer.MAX_VALUE;

    // meta data - collected training samples of this class
    public List<float[]> trainingDataOfThisClass = new ArrayList<>();

    public SpatialTracklet(double posX, double posY, long id) {
        this.posX = posX;
        this.posY = posY;
        this.id = id;
    }
}
