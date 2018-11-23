package com.opennars.applications.componentbased;

public class Entity {
    public Behaviour behaviour;

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

    public Entity() {
    }

    public Entity(final int id, final double posX, final double posY, final double velocity, final double angle, final String tag, final Behaviour behaviour) {
        this.id = id;
        this.tag = tag;

        this.posX = posX;
        this.posY = posY;
        this.velocity = velocity;
        this.angle = angle;
        this.behaviour = behaviour;
    }
}
