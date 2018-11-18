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

import com.opennars.applications.crossing.NarListener.Prediction;
import com.opennars.sgui.NarSimpleGUI;
import java.util.ArrayList;
import java.util.List;
import org.opennars.io.events.Events;
import org.opennars.io.events.OutputHandler.DISAPPOINT;
import org.opennars.main.Nar;
import processing.core.PApplet;
import processing.event.MouseEvent;

public class Crossing extends PApplet {
    Nar nar;
    int entityID = 1;
    
    List<Prediction> predictions = new ArrayList<Prediction>();
    List<Prediction> disappointments = new ArrayList<Prediction>();

    Heatmap heatmap = new Heatmap();
    int heatmapTimer = 0;

    final int streetWidth = 40;
    final int fps = 50;
    @Override
    public void setup() {
        // 100x100 because the world has a size of 1000 and is divided by cellsize=10
        heatmap.allocate(100, 100);
        heatmap.cooldownFactor = 0.98;
        heatmap.heatadditive = 1.0; // 1.0 because we don't care about the frequency of the first visiting object

        cameras.add(new Camera(this,500+streetWidth/2, 500+streetWidth/2));
        try {
            nar = new Nar();
            nar.narParameters.VOLUME = 0;
            nar.narParameters.DURATION*=10;
            NarListener listener = new NarListener(cameras.get(0), nar, predictions, disappointments);
            nar.on(Events.TaskAdd.class, listener);
            nar.on(DISAPPOINT.class, listener);
        } catch (Exception ex) {
            System.out.println(ex);
            System.exit(1);
        }
        int trafficLightRadius = 25;
        streets.add(new Street(false, 0, 500, 1000, 500 + streetWidth));
        streets.add(new Street(true, 500, 0, 500 + streetWidth, 1000));
        int trafficLightID = 1;
        trafficLights.add(new TrafficLight(trafficLightID++, trafficLightRadius, 500 + streetWidth + trafficLightRadius, 500 + streetWidth/2, 0));
        trafficLights.add(new TrafficLight(trafficLightID++, trafficLightRadius, 500 - trafficLightRadius, 500 + streetWidth/2, 0));
        trafficLights.add(new TrafficLight(trafficLightID++, trafficLightRadius/2, 500 + streetWidth, 500 + streetWidth + trafficLightRadius, 1));
        trafficLights.add(new TrafficLight(trafficLightID++, trafficLightRadius/2, 500, 500 - trafficLightRadius, 1));
        int cars = 4; //cars and pedestrians
        for (float i = 0; i < cars/2; i += 1.05) {
            entities.add(new Car(entityID++, 500 + streetWidth - Util.discretization+1, 900 - i * 100, 0.3, -PI / 2));
            entities.add(new Car(entityID++, 500 + Util.discretization, 900 - i * 100, 0.3, PI / 2));
        }
        int pedestrians = 4;//4;
        for (float i = 0; i < pedestrians/2; i += 1.05) {
            entities.add(new Pedestrian(entityID++, 900 - i * 100, 500 + streetWidth - Util.discretization, 0.3, 0));
            entities.add(new Pedestrian(entityID++, 900 - i * 100, 500 + Util.discretization, 0.3, -PI));
        }
        /*for (TrafficLight l : trafficLights) { //it can't move anyway, so why would the coordinates matter to NARS?
            String pos = Util.positionToTerm(l.posX, l.posY);
            String narsese = "<(*,{" + l.id + "}," + pos + ") --> at>.";
            nar.addInput(narsese);
        }*/
        
        size(1000, 1000);
        frameRate(fps);
        new NarSimpleGUI(nar);
    }

    List<Street> streets = new ArrayList<Street>();
    List<TrafficLight> trafficLights = new ArrayList<TrafficLight>();
    List<Entity> entities = new ArrayList<Entity>();
    List<Camera> cameras = new ArrayList<Camera>();
    int t = 0;
    public static boolean showAnomalies = false;

    String questions = "<trafficLight --> [?whatColor]>? :|:";
    int perception_update = 1;
    @Override
    public void draw() {
        heatmapTimer++;

        if ((heatmapTimer % 60) == 0) {
            heatmap.cooldown();
        }




        viewport.Transform();
        background(64,128,64);
        fill(0);
        for (Street s : streets) {
            s.draw(this);
        }
        if (t % perception_update == 0) {
            boolean hadInput = false;
            for(Camera c : cameras) {
                hadInput = hadInput || c.see(nar, entities, trafficLights);
            }
            if(hadInput) {
                nar.addInput(questions);
            }
        }
        for (int i = 0; i < 1000; i += Util.discretization) {
            stroke(128);
            line(0, i, 1000, i);
            line(i, 0, i, 1000);
        }

        // draw heatmap

        /*
        this.pushMatrix();
        this.translate(0, 0);
        this.stroke(0,0,0, 0.0f);
        this.fill(255, 0, 0, 128.0f);//(float)heat*255.0f);
        this.rect(0, 0, 300, 300);
        this.popMatrix();

        this.stroke(0);
        */


        for (int y=0;y<heatmap.map.length;y++) {
            for (int x=0;x<heatmap.map[y].length;x++) {
                final double heat = heatmap.map[y][x];

                this.pushMatrix();
                this.translate(x*10, y*10);
                this.stroke(0,0,0, 0.0f);
                this.fill(255, 0, 0, (float)heat*255.0f);
                this.rect(0, 0, 10, 10);
                this.popMatrix();

                this.stroke(0);

            }
        }

        for (Entity e : entities) {
            e.draw(this, streets, trafficLights, entities, null, 0);
        }
        for (TrafficLight tl : trafficLights) {
            tl.draw(this, t);
        }
        t++;
        nar.cycles(10);
        removeOutdatedPredictions(predictions);
        removeOutdatedPredictions(disappointments);
        for (Prediction pred : predictions) {
            Entity e = pred.ent;
            e.draw(this, streets, trafficLights, entities, pred.truth, pred.time - nar.time());
        }
        if(showAnomalies) {
            for (Prediction pred : disappointments) {
                Entity e = pred.ent;
                if(e instanceof Car) {
                    fill(255,0,0);
                }
                if(e instanceof Pedestrian) {
                    fill(0,0,255);
                }
                this.text("ANOMALY", (float)e.posX, (float)e.posY);
                e.draw(this, streets, trafficLights, entities, pred.truth, pred.time - nar.time());
            }
        }
        for(Camera c : cameras) {
            c.draw(this);
        }
    }

    public void removeOutdatedPredictions(List<Prediction> predictions) {
        List<Prediction> toDelete = new ArrayList<Prediction>();
        for(Prediction pred : predictions) {
            if(pred.time <= nar.time()) {
                toDelete.add(pred);
            }
        }
        predictions.removeAll(toDelete);
    }
    
    float mouseScroll = 0;
    Viewport viewport = new Viewport(this);
    public void mouseWheel(MouseEvent event) {
        mouseScroll = -event.getCount();
        viewport.mouseScrolled(mouseScroll);
    }
    @Override
    public void keyPressed() {
        viewport.keyPressed();
    }
    @Override
    public void mousePressed() {
        viewport.mousePressed();
    }
    @Override
    public void mouseReleased() {
        viewport.mouseReleased();
    }
    @Override
    public void mouseDragged() {
        viewport.mouseDragged();
    }

    public static void main(String[] args) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(NarSimpleGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>
        String[] args2 = {"Crossing"};
        Crossing mp = new Crossing();
        new IncidentSimulator().show();
        PApplet.runSketch(args2, mp);
    }
}
