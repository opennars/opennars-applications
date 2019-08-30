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
package org.opennars.applications.crossing;

import static java.lang.Math.PI;
import java.util.List;
import org.opennars.applications.streetscene.VisualReasonerHeadless;
import org.opennars.applications.streetscene.VisualReasonerWithGUI;
import org.opennars.entity.TruthValue;
import processing.core.PApplet;

public class Entity {

    public static int entityID = 0;
    public String label = "";
    public double posX, posY;
    public double width, height;
    public double getPosX() {
        return posX;
    }
    public double getPosY() {
        return posY;
    }
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

    public static boolean DrawDirection = true;
    public static boolean DrawID = true;
    public void draw(PApplet applet, TruthValue truth, long time) {
        isPredicted = applet instanceof VisualReasonerWithGUI ? truth != null : isPredicted;
        applet.pushMatrix();
        //float posXDiscrete = (((int) this.posX)/Util.discretization * Util.discretization);
        //float posYDiscrete = (((int) this.posY)/Util.discretization * Util.discretization);
        applet.translate((float) posX, (float) posY);
        applet.rotate((float) angle);
        
        String name = "";
        if(this instanceof Bike) {
            name = "bike"+label;
        }
        else
        if(this instanceof Pedestrian) {
            name = "pedestrian"+label;
        }
        else { //this instanceof Bike
            name = "car"+label;
        }
        
        if(truth == null && DrawDirection && !(applet instanceof VisualReasonerWithGUI)) {
            applet.rect(0, 0, Util.discretization*scale, Util.discretization/2*scale);
        }
        if(applet instanceof VisualReasonerWithGUI) {
            if(this instanceof Car) {
                applet.stroke(200,0,200);
            }
            if(this instanceof Pedestrian) {
                applet.stroke(0,200,0);
            }
            if(this instanceof Bike) {
                applet.stroke(0,0,200);
            }
            
            if(VisualReasonerHeadless.indangers.containsKey(name)) {
                applet.fill(255,0,0,128);
            } else
            if(VisualReasonerHeadless.jaywalkers.containsKey(name)) {
                applet.fill(255,255,0,128);
            }
            else {
                applet.fill(128,0,0,0);
            }
        }
        if(!isPredicted || !(applet instanceof VisualReasonerWithGUI)) {
            //applet.rect((float) (0.0f-width/2.0f), (float) (0.0f-height/2.0f), (float) width, (float) height);
            float mul2 = org.opennars.applications.streetscene.Util.discretization / org.opennars.applications.crossing.Util.discretization;
            if(applet instanceof VisualReasonerWithGUI) {
                applet.ellipse(0.0f, 0.0f, Util.discretization*scale*mul2, Util.discretization*scale*mul2);
            } else {
                applet.ellipse(2.5f, 2.5f, Util.discretization*scale, Util.discretization*scale);
            }
        }
        
        if(applet instanceof VisualReasonerWithGUI) {
            float mul = isPredicted ? Util.truthToValue(truth) * Util.timeToValue(time) : 1.0f;
            int alpha = (int) (mul * 255);
            
            if(this instanceof Car) {
                applet.stroke(255,0,255, alpha);
            }
            if(this instanceof Pedestrian) {
                applet.stroke(0,255,0, alpha);
            }
            if(this instanceof Bike) {
                applet.stroke(0,0,255, alpha);
            }
            applet.fill(128,0,0,0);
        }
        
        if(DrawID && applet instanceof VisualReasonerWithGUI) {
            //applet.stroke(255,0,0);
            //applet.fill(255,0,0);
            applet.pushMatrix();
            applet.strokeWeight(5.0f);
            applet.scale(0.3f);
            if(id == 0) {
                applet.rotate((float) (-PI/4.0f- PI/2.0f));
                applet.line(0, 0,   100, 0);
                applet.line(70, 30, 100, 0);
                applet.line(70,-30, 100, 0);
            }
            else
            if(id == 11) {
                applet.rotate((float) (-PI/4.0f + PI/2.0f));
                applet.line(0, 0,   100, 0);
                applet.line(70, 30, 100, 0);
                applet.line(70,-30, 100, 0);
            }
            else
            if(id == 10) {
                applet.rotate((float) (-PI/4.0f));
                applet.line(0, 0,   100, 0);
                applet.line(70, 30, 100, 0);
                applet.line(70,-30, 100, 0);
            }
            else { //1
                applet.rotate((float) (-PI/4.0f + PI));
                applet.line(0, 0,   100, 0);
                applet.line(70, 30, 100, 0);
                applet.line(70,-30, 100, 0);
            }
            applet.popMatrix();
            applet.strokeWeight(1.0f);
            //applet.text(String.valueOf(id) + " ("+label+")", (float)posX, (float)posY);
        }
        
        applet.popMatrix();
        if(!(applet instanceof VisualReasonerWithGUI)) {
            applet.fill(0,0,0);
        } else {
            applet.fill(0,255,255);
        }
        if(DrawID) {
            if(applet instanceof VisualReasonerWithGUI) {
                applet.textSize(20);
            }
            if(label.isEmpty()) {
                if(!isPredicted) {
                    applet.text(String.valueOf(id), (float)posX, (float)posY - Util.discretization/2);
                }
            } else {
                if(applet instanceof VisualReasonerWithGUI) {
                    applet.text(name, (float)posX- Util.discretization/2, (float)posY - Util.discretization/2);
                } else {
                    applet.text(name, (float)posX, (float)posY);
                }
            }
        }
    }

    public void simulate(List<TrafficLight> trafficLights, List<Entity> entities, List<Street> streets) {
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
