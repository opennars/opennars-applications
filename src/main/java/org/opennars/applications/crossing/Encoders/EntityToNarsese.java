/*
 * The MIT License
 *
 * Copyright 2019 OpenNARS.
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
package org.opennars.applications.crossing.Encoders;

import org.opennars.applications.crossing.Bike;
import org.opennars.applications.crossing.Car;
import org.opennars.applications.crossing.Entity;
import org.opennars.applications.crossing.Pedestrian;

public class EntityToNarsese {
    
    public static String informType(Entity entity) {
        if(entity instanceof Bike) {
            return "<" + name(entity) + " --> bike>";
        }
        if(entity instanceof Car) {
            return "<" + name(entity) + " --> car>";
        }
        if(entity instanceof Pedestrian) {
            return "<" +name(entity) + " --> pedestrian>";
        }
        return "<" +name(entity) + " --> entity>";
    }
    
    public static String name(Entity entity) { //TODO put in class
        if(entity instanceof Bike) {
            return "bike" + entity.id;
        }
        if(entity instanceof Car) {
            return "car" + entity.id;
        }
        if(entity instanceof Pedestrian) {
            return "pedestrian" + entity.id;
        }
        return "entity";
    }
    
}
