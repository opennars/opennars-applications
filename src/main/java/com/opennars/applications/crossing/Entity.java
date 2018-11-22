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
package com.opennars.applications.crossing;

import java.util.List;
import org.opennars.entity.TruthValue;
import processing.core.PApplet;

public class Entity {

    public static int entityID = 0;
    public double posX, posY;
    public double velocity;
    public double angle;
    public int id;
    public float scale = 1.0f;
    public float maxSpeed = 2.0f;
    public static boolean pedestrianIgnoreTrafficLight = false;
    public static boolean carIgnoreTrafficLight = false;

    public double normalness = 0.0;

    public boolean isPredicted = false;

    public double lastPosX = 0;
    public double lastPosY = 0;

    public Entity() {
    }

    public Entity(int id, double posX, double posY, double velocity, double angle) {
        this.id = id;
        this.posX = posX;
        this.posY = posY;
        this.velocity = velocity;
        this.angle = angle;
    }

    public void tick() {
        // decay normalness

        // 0.96 is to slow
        //normalness *= 0.8; is to fast
        normalness *= 0.91;
    }

    boolean hasMoved() {
        double dist = velocity;
        return dist >= 0.1;
    }

    public boolean isAnomaly() {
        // exception: is not a anomaly if it hasn't moved
        return normalness < 0.3 && hasMoved();
    }

    public void draw(PApplet applet, List<Street> streets, List<TrafficLight> trafficLights, List<Entity> entities, TruthValue truth, long time) {
        applet.pushMatrix();
        //float posXDiscrete = (((int) this.posX)/Util.discretization * Util.discretization);
        //float posYDiscrete = (((int) this.posY)/Util.discretization * Util.discretization);
        applet.translate((float) posX, (float) posY);
        applet.rotate((float) angle);

        if(truth == null) {
            final float width = Util.discretization/2*scale;
            applet.rect(0.0f*Util.discretization*scale, -0.5f*width, Util.discretization*scale, width);
        }
        applet.ellipse(0, 0, Util.discretization*scale, Util.discretization*scale);

        applet.popMatrix();
        applet.fill(0);
        applet.text(String.valueOf(id), (float)posX, (float)posY);

        // used for debugging the "real position"
        applet.rect((float)posX, (float)posY, 3, 3);


        if(truth != null) {
            return;
        }

        boolean accelerate = true;
        for (TrafficLight l : trafficLights) {
            if (Util.distance(posX, posY, l.posX, l.posY) < l.radius) {
                if (l.colour == l.RED) {
                    if (Util.rnd.nextFloat() > 0.3 && ((this instanceof Car && !carIgnoreTrafficLight) || (this instanceof Pedestrian && !pedestrianIgnoreTrafficLight))) {
                        velocity *= 0.5;
                        accelerate = false;
                    }
                }
            }
        }
        for (Entity e : entities) {
            boolean collidable = !(this instanceof Pedestrian && e instanceof Pedestrian);
            if (e != this && collidable) {
                double nearEnough = 10;
                for (double k = 0; k < nearEnough; k += 0.1) {
                    double pXNew = posX + k * Math.cos(angle);
                    double pYNew = posY + k * Math.sin(angle);
                    if (Util.distance(pXNew, pYNew, e.posX, e.posY) < nearEnough) {
                        velocity *= 0.8;
                        accelerate = false;
                    }
                }
            }
        }

        if (accelerate && velocity < maxSpeed) {
            velocity += 0.02;
        }

        double aX = Math.cos(angle);
        double aY = Math.sin(angle);
        posX += aX * velocity;
        posY += aY * velocity;

        double epsilon = 1;
        if (posY < 0) {
            posY = 1000 - epsilon;
            //this.id = entityID++;
        }
        if (posY > 1000) {
            posY = epsilon;
            //this.id = entityID++;
        }
        if (posX < 0) {
            posX = 1000 - epsilon;
            //this.id = entityID++;
        }
        if (posX > 1000) {
            posX = epsilon;
            //this.id = entityID++;
        }
    }
}
