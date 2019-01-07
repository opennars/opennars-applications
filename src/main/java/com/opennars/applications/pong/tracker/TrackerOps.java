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

    public void upLeft() {
        System.out.println("[a ] TrackerOps.upleft");

        tracker.posY -= 2.0;
        tracker.posX -= 2.0;

        tracker.timeoutForOps = -50;
    }

    public void downLeft() {
        System.out.println("[a ] TrackerOps.downleft");

        tracker.posY += 2.0;
        tracker.posX -= 2.0;

        tracker.timeoutForOps = -50;
    }

    public void upRight() {
        System.out.println("[a ] TrackerOps.upright");

        tracker.posY -= 2.0;
        tracker.posX += 2.0;

        tracker.timeoutForOps = -50;
    }

    public void downRight() {
        System.out.println("[a ] TrackerOps.downright");

        tracker.posY += 2.0;
        tracker.posX += 2.0;

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
        System.out.println("[a ] TrackerOps.teleport");

        tracker.posX = rng.nextInt(8) * 10.0;
        tracker.posY = rng.nextInt(7) * 10.0;
    }
}
