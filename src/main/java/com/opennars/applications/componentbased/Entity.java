/*
 * The MIT License
 *
 * Copyright 2018 The OpenNARS authors.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.opennars.applications.componentbased;

import processing.core.PApplet;

import java.util.ArrayList;
import java.util.List;

public class Entity {
    public BehaviourComponent behaviour;
    public RenderableComponent renderable;

    public List<Component> components = new ArrayList<>();

    public double posX, posY;
    public double velocityX = 0, velocityY = 0;

    public double directionVelocity;
    public double directionAngle;
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
        this.directionVelocity = velocity;
        this.directionAngle = angle;
    }

    public void render(final PApplet applet) {
        renderable.render(this, applet);
    }

    public Component retComponentByName(String name) {
        for (final Component iComponent : components) {
            if (iComponent.retName().equals(name)) {
                return iComponent;
            }
        }
        return null;
    }
}
