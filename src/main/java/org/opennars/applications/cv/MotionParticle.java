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

/**
 * Particle to track the (local) motion
 */
public class MotionParticle {
    public Map2d[] segment; // different channels of tracked segment

    public double posX = 0;
    public double posY = 0;

    public MotionParticle(double posX, double posY) {
        this.posX = posX;
        this.posY = posY;
    }
}
