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

import org.opennars.entity.TruthValue;

import java.util.List;

/**
 * Component for the behaviour of an entity
 */
public class BehaviourComponent {
    private final EnumType type;


    public BehaviourComponent(final EnumType type) {
        this.type = type;
    }

    public void tick(Entity entity, List<Street> streets, List<TrafficLight> trafficLights, List<Entity> entities, TruthValue truth, long time) {
        if (type == EnumType.CAR) {
            carTick(entity, streets, trafficLights, entities, truth, time);
        }
        else if (type == EnumType.PEDESTRIAN) {
            carTick(entity, streets, trafficLights, entities, truth, time);



            entity.angle+=(Util.rnd.nextFloat()*0.1-0.05);
            //ok pedestrian, don't go on grass
            boolean forPedestrians = false;
            for(Street street : streets) {
                if(!street.forCarsOnly && entity.posX > street.startX && entity.posX < street.endX && entity.posY > street.startY && entity.posY < street.endY) {
                    forPedestrians = true;
                    break;
                }
            }

            /* TODO
            if(!forPedestrians) {
                entity.angle = initialAngle;
                entity.posX = prevX;
                entity.posY = prevY;
            }
             */
        }
    }


    static protected void carTick(Entity entity, List<Street> streets, List<TrafficLight> trafficLights, List<Entity> entities, TruthValue truth, long time) {
        if(truth != null) {
            return;
        }

        boolean accelerate = true;
        for (TrafficLight l : trafficLights) {
            if (Util.distance(entity.posX, entity.posY, l.posX, l.posY) < l.radius) {
                if (l.colour == l.RED) {
                    if (Util.rnd.nextFloat() > 0.3 && ((entity instanceof Car && !entity.carIgnoreTrafficLight) || (entity instanceof Pedestrian && !entity.pedestrianIgnoreTrafficLight))) {
                        entity.velocity *= 0.5;
                        accelerate = false;
                    }
                }
            }
        }
        for (Entity e : entities) {
            boolean collidable = !(entity instanceof Pedestrian && e instanceof Pedestrian);
            if (e != entity && collidable) {
                double nearEnough = 10;
                for (double k = 0; k < nearEnough; k += 0.1) {
                    double pXNew = entity.posX + k * Math.cos(entity.angle);
                    double pYNew = entity.posY + k * Math.sin(entity.angle);
                    if (Util.distance(pXNew, pYNew, e.posX, e.posY) < nearEnough) {
                        entity.velocity *= 0.8;
                        accelerate = false;
                    }
                }
            }
        }

        if (accelerate && entity.velocity < entity.maxSpeed) {
            entity.velocity += 0.02;
        }

        double aX = Math.cos(entity.angle);
        double aY = Math.sin(entity.angle);
        entity.posX += aX * entity.velocity;
        entity.posY += aY * entity.velocity;

        double epsilon = 1;
        if (entity.posY < 0) {
            entity.posY = 1000 - epsilon;
            //this.id = entityID++;
        }
        if (entity.posY > 1000) {
            entity.posY = epsilon;
            //this.id = entityID++;
        }
        if (entity.posX < 0) {
            entity.posX = 1000 - epsilon;
            //this.id = entityID++;
        }
        if (entity.posX > 1000) {
            entity.posX = epsilon;
            //this.id = entityID++;
        }
    }

    public enum EnumType {
        CAR,
        PEDESTRIAN,
    }

}
