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

public class Pedestrian extends Entity {
    public final static float pedestrianScale = 0.75f;
    public Pedestrian(int id, double posX, double posY, double velocity, double angle) {
        super(id, posX, posY, velocity, angle, new BehaviourComponent(BehaviourComponent.EnumType.PEDESTRIAN));
        behaviour.initialAngle = angle;
        behaviour.maxSpeed = 1;
        scale = pedestrianScale;
    }

    public void draw(PApplet applet, List<Street> streets, List<TrafficLight> trafficLights, List<Entity> entities, TruthValue truth, long time) {
        float mul = Util.truthToValue(truth) * Util.timeToValue(time);
        applet.fill(0, 255, 255, mul*255.0f);
        super.draw(applet, streets, trafficLights, entities, truth, time);
    }
}
