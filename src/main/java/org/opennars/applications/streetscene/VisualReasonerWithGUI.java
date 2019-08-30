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
package org.opennars.applications.streetscene;

import org.opennars.applications.streetscene.Entities.Entity;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opennars.applications.crossing.OperatorPanel;
import org.opennars.applications.streetscene.Entities.Bike;
import org.opennars.applications.streetscene.Entities.Car;
import org.opennars.applications.streetscene.Entities.Pedestrian;
import static org.opennars.applications.streetscene.VisualReasonerHeadless.cleanupAllAnomalies;
import static org.opennars.applications.streetscene.VisualReasonerHeadless.crosswalkers;
import static org.opennars.applications.streetscene.VisualReasonerHeadless.entities;
import static org.opennars.applications.streetscene.VisualReasonerHeadless.i;
import static org.opennars.applications.streetscene.VisualReasonerHeadless.indangers;
import static org.opennars.applications.streetscene.VisualReasonerHeadless.initVisualReasoner;
import static org.opennars.applications.streetscene.VisualReasonerHeadless.jaywalkers;
import static org.opennars.applications.streetscene.VisualReasonerHeadless.movementThresholdBike;
import static org.opennars.applications.streetscene.VisualReasonerHeadless.movementThresholdCar;
import static org.opennars.applications.streetscene.VisualReasonerHeadless.movementThresholdPedestrian;
import static org.opennars.applications.streetscene.VisualReasonerHeadless.perceptionUpdate;
import static org.opennars.applications.streetscene.VisualReasonerHeadless.questionUpdate;
import static org.opennars.applications.streetscene.VisualReasonerHeadless.relationUpdate;
import static org.opennars.applications.streetscene.VisualReasonerHeadless.trafficMultiNar;
import processing.core.PApplet;
import processing.core.PImage;

public class VisualReasonerWithGUI extends PApplet {

    public static String videopath="/mnt/sda1/Users/patha/Downloads/Test/Test/Test003/";
    public static String trackletpath = "/home/tc/Dateien/CROSSING/Test003/";
    boolean drawPlaces = false;
    OperatorPanel panel = null;
    
    public void setup() {
        size(1280, 720);
        this.setSize(1280, 720);
        initVisualReasoner("./StreetScene/AnomalyOntology.nal");
        if(i == 0) {
            i = 4200;
        }
        final int fps = 10;
        panel = new OperatorPanel(trafficMultiNar.qanar);
        panel.show();
        frameRate(fps);
    }

    public void draw() {
        frame.setTitle("RealCrossing (frame=" + i +")");
        cleanupAllAnomalies();
        synchronized(entities) {
            entities.clear(); //refresh detections
            AddEntitiesFromTracklets();
        }
        trafficMultiNar.perceiveScene(i, perceptionUpdate, questionUpdate, relationUpdate);
        trafficMultiNar.reason();
        for(Entity e : entities) {
            if(e instanceof Car) {
                new org.opennars.applications.crossing.Car(e.angle, e.posX, e.posY, e.speed, 0, e.id).draw(this, null, 0);
            }
            if(e instanceof Pedestrian) {
                new org.opennars.applications.crossing.Pedestrian(e.angle, e.posX, e.posY, e.speed, 0, e.id).draw(this, null, 0);
            }
            if(e instanceof Bike) {
                new org.opennars.applications.crossing.Bike(e.angle, e.posX, e.posY, e.speed, 0, e.id).draw(this, null, 0);
            }
        }
        for (Prediction pred : trafficMultiNar.predictions) {
            Entity e = pred.ent;
            pushMatrix();
            translate(Util.discretization/2,Util.discretization/2);
            long dt = pred.time - trafficMultiNar.predictionNar.time();
            if("car".equals(pred.type)) {
                new org.opennars.applications.crossing.Car(e.angle, e.posX, e.posY, e.speed, 0, e.id).draw(this, pred.truth, dt);
            }
            if("pedestrian".equals(pred.type)) {
                new org.opennars.applications.crossing.Pedestrian(e.angle, e.posX, e.posY, e.speed, 0, e.id).draw(this, pred.truth, dt);
            }
            if("bike".equals(pred.type)) {
                new org.opennars.applications.crossing.Bike(e.angle, e.posX, e.posY, e.speed, 0, e.id).draw(this, pred.truth, dt);
            }
            popMatrix();
        }
        synchronized(indangers) {
            for(String ent : indangers.keySet()) {
                String s = "in_danger "+ent;
                panel.jTextArea5.setText(s + " (frame=" + i + ")" + "\n" + panel.jTextArea5.getText());
            }
        }
        synchronized(jaywalkers) {
            for(String ent : jaywalkers.keySet()) {
                String s = "is_jwaylking "+ent;
                panel.jTextArea5.setText(s + " (frame=" + i + ")" + "\n" + panel.jTextArea5.getText());
            }
        }
        synchronized(crosswalkers) {
            for(String ent : crosswalkers.keySet()) {
                String s = "is_crosswalking "+ent;
                panel.jTextArea5.setText(s + " (frame=" + i + ")" + "\n" + panel.jTextArea5.getText());
            }
        }
        synchronized(trafficMultiNar.informQaNar.relatedLeft) {
            for(int k=0; k<trafficMultiNar.informQaNar.relatedLeft.size(); k++) {
                Entity left = trafficMultiNar.informQaNar.relatedLeft.get(k);
                Entity right = trafficMultiNar.informQaNar.relatedRight.get(k);
                stroke(200,200,0);
                line((float)left.posX, (float)left.posY, (float)right.posX, (float)right.posY);
            }
        }
        if(drawPlaces) {
            synchronized(trafficMultiNar.informLocationNar.locationToLabel) {
                for(String key : trafficMultiNar.informLocationNar.locationToLabel.keySet()) {
                    String choice = trafficMultiNar.informLocationNar.locationToLabel.get(key).choice();
                    int X = Integer.valueOf(key.split("_")[0]);
                    int Y = Integer.valueOf(key.split("_")[1]);
                    stroke(0,0,0,0);
                    int alpha = 50;
                    if(choice.equals("street")) {
                        fill(128,0,0,alpha);
                    }
                    if(choice.equals("sidewalk")) {
                        fill(0,128,0,alpha);
                    }
                    if(choice.equals("crosswalk")) {
                        fill(0,0,128,alpha);
                    }
                    rect(X*Util.discretization,Y*Util.discretization,Util.discretization,Util.discretization);
                }
            }
        }
        i++;
    }
    
    public String unwrap(String s) {
        return s.replace("[", "").replace("]", "");
    }

    private void AddEntitiesFromTracklets() {
        String tracklets = "";
        String nr = String.format("%05d", i);
        PImage img = loadImage(videopath+nr+".jpg"); //1 2 3 7
        image(img, 0, 0);
        try {
            ///home/tc/Dateien/CROSSING/Test001/TKL00342.txt
            tracklets = new String(Files.readAllBytes(Paths.get(trackletpath+"TKL"+nr+".txt")));
        } catch (IOException ex) {
            Logger.getLogger(VisualReasonerWithGUI.class.getName()).log(Level.SEVERE, null, ex);
        }
        String[] lines = tracklets.replace("[ ","[").replace(" ]","]").replace("  "," ").replace("  "," ").split("\n");
        for(String s : lines) {
            //ClassID TrackID : [X5c Y5c W5 H5] 
            String[] props = s.split(" ");
            String label = props[1];
            if(unwrap(props[3]).equals("")) {
                System.out.println(s);
            }
            Integer X = Integer.valueOf(unwrap(props[3]));
            Integer Y = Integer.valueOf(unwrap(props[4]));
            Integer X2 = Integer.valueOf(unwrap(props[19])); //7 6
            Integer Y2 = Integer.valueOf(unwrap(props[20]));
            //use an id according to movement direction
            int angle = 0; //id according to movement direction
            if(X < X2) {
                angle += 10;
            }
            if(Y < Y2) {
                angle += 1;
            }
            
            double movement = Math.sqrt((X-X2)*(X-X2) + (Y - Y2)*(Y - Y2)); //TODO like in LiveVideo2
            Entity added = null;
            if(props[0].equals("0")) { //person or vehicle for now
                if(movement < (double)movementThresholdPedestrian) {
                    continue;
                }
                Pedestrian toAdd = new Pedestrian(angle, X2, Y2, label);
                entities.add(toAdd);
                added = toAdd;
            } else {
                if(!props[0].equals("1")) {
                    if(movement < (double)movementThresholdCar) {
                        continue;
                    }
                    Car toAdd = new Car(angle, X2, Y2, label);
                    entities.add(toAdd);
                    added = toAdd;
                } else {
                    if(movement < (double)movementThresholdBike) {
                        continue;
                    }
                    Bike toAdd = new Bike(angle, X2, Y2, label);
                    entities.add(toAdd);
                    added = toAdd;
                }
            }
            added.speed = movement;
        }
        i++;
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
            java.util.logging.Logger.getLogger(VisualReasonerWithGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>
        String[] args2 = {"Street Scene"};
        VisualReasonerWithGUI mp = new VisualReasonerWithGUI();
        PApplet.runSketch(args2, mp);
    }
}
