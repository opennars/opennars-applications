package com.opennars.applications.componentbased;

import processing.core.PApplet;

public class Entity {
    public BehaviourComponent behaviour;
    public RenderableComponent renderable;

    public double posX, posY;
    public double velocity;
    public double angle;
    public int id;
    public float scale = 1.0f;

    public double normalness = 0.0;

    public boolean isPredicted = false;

    public String tag;

    //public double lastPosX = 0;
    //public double lastPosY = 0;

    public Entity(final int id, final double posX, final double posY, final double velocity, final double angle, final String tag) {
        this.id = id;
        this.tag = tag;

        this.posX = posX;
        this.posY = posY;
        this.velocity = velocity;
        this.angle = angle;
    }

    public void render(final PApplet applet) {
        renderable.render(this, applet);
    }
}
