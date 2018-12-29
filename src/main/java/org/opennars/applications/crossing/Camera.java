/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opennars.applications.crossing;

import java.util.List;
import org.opennars.main.Nar;
import processing.core.PApplet;

/**
 *
 * @author patha
 */
public class Camera {
    int posX;
    int posY;
    int radius=60;
    int minX;
    int minY;
    public Camera(int posX, int posY) {
        this.posX = posX;
        this.posY = posY;
        this.minX = posX-radius*2;
        this.minY = posY-radius*2;
    }
    
    InformNARS informer = new InformNARS();
    boolean see(Nar nar, List<Entity> entities, List<TrafficLight> trafficLights, final boolean force) {
        //InformNARS.informAboutEntity(nar, chosen);
        for (Entity ent : entities) {
            if (Util.distance(posX, posY, ent.posX, ent.posY) < radius) {
                informer.informAboutEntity(nar, ent, minX, minY);
            }
        }
        for (TrafficLight ent : trafficLights) {
            if (Util.distance(posX, posY, ent.posX, ent.posY) < radius) {
                informer.informAboutTrafficLight(nar, ent, minX, minY);
                break;
            }
        }
        return informer.Input(nar, force);
    }
    
    public void draw(PApplet applet) {
        applet.fill(0,0,255,20);
        applet.ellipse(posX,posY,radius*2,radius*2);
    }
}
