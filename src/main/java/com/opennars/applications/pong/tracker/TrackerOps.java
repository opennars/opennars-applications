package com.opennars.applications.pong.tracker;

import java.util.Random;

public class TrackerOps {
    public Tracker tracker;

    private Random rng = new Random();

    public void up() {
        System.out.println("[a ] TrackerOps.up");

        tracker.posY -= 2.0;

        tracker.timeoutForOps = -50;
    }

    public void down() {
        System.out.println("[a ] TrackerOps.down");

        tracker.posY += 2.0;

        tracker.timeoutForOps = -50;
    }

    public void left() {
        System.out.println("[a ] TrackerOps.left");

        tracker.posX -= 2.0;

        tracker.timeoutForOps = -50;
    }

    public void right() {
        System.out.println("[a ] TrackerOps.right");

        tracker.posX += 2.0;

        tracker.timeoutForOps = -50;
    }

    public void teleport() {
        return; // teleport is disabled

        /*

        System.out.println("[a ] TrackerOps.teleport");

        tracker.posX = rng.nextInt(8) * 10.0;
        tracker.posY = rng.nextInt(7) * 10.0;
        */
    }
}
