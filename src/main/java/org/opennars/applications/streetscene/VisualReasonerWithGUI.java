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

import org.opennars.applications.Util;
import org.opennars.applications.streetscene.Entities.Entity;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.opennars.applications.Util.distanceSum;
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
import org.opennars.entity.TruthValue;
import processing.core.PApplet;
import processing.core.PImage;

public class VisualReasonerWithGUI extends PApplet {

    public static String videopath="/mnt/sda1/Users/patha/Downloads/Test/Test/Test003/";
    public static String trackletpath = "/home/tc/Dateien/CROSSING/Test003/";
    boolean drawPlaces = false;
    OperatorPanel panel = null;
    public static int discretization = 80;
    
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
    
    public void drawEntity(PApplet applet, Entity e, TruthValue truth, long time) {
        boolean isPredicted = truth != null;
        float scale = 1.0f;
        applet.pushMatrix();
        //float posXDiscrete = (((int) this.posX)/Util.discretization * Util.discretization);
        //float posYDiscrete = (((int) this.posY)/Util.discretization * Util.discretization);
        applet.translate((float) e.posX, (float) e.posY);
        //name by type
        String name = "";
        {
            if(e instanceof Bike) {
                name = "bike"+e.id;
            }
            else
            if(e instanceof Pedestrian) {
                name = "pedestrian"+e.id;
            }
            else { //this instanceof Bike
                name = "car"+e.id;
            }
        }
        //stroke color by type
        {
            if(e instanceof Car) {
                applet.stroke(200,0,200);
            }
            if(e instanceof Pedestrian) {
                applet.stroke(0,200,0);
            }
            if(e instanceof Bike) {
                applet.stroke(0,0,200);
            }
        }
        //fill color by anomaly
        if(VisualReasonerHeadless.indangers.containsKey(name)) {
            applet.fill(255,0,0,128);
        } else
        if(VisualReasonerHeadless.jaywalkers.containsKey(name)) {
            applet.fill(255,255,0,128);
        }
        else {
            applet.fill(128,0,0,0);
        }
        //it's not a prediction, draw ellipse
        if(!isPredicted) {
            applet.ellipse(0.0f, 0.0f, VisualReasonerHeadless.discretization*scale, VisualReasonerHeadless.discretization*scale);
        }
        //arrow stroke color by type modulated by truth
        float mul = isPredicted ? Util.truthToValue(truth) * Util.timeToValue(time) : 1.0f;
        int alpha = (int) (mul * 255);
        if(e instanceof Car) {
            applet.stroke(255,0,255, alpha);
        }
        if(e instanceof Pedestrian) {
            applet.stroke(0,255,0, alpha);
        }
        if(e instanceof Bike) {
            applet.stroke(0,0,255, alpha);
        }
        applet.fill(128,0,0,0);

        //Draw arrow
        {
            //applet.stroke(255,0,0);
            //applet.fill(255,0,0);
            applet.pushMatrix();
            applet.strokeWeight(5.0f);
            applet.scale(0.3f);
            if(e.angle == 0) {
                applet.rotate((float) (-PI/4.0f- PI/2.0f));
                applet.line(0, 0,   100, 0);
                applet.line(70, 30, 100, 0);
                applet.line(70,-30, 100, 0);
            }
            else
            if(e.angle == 11) {
                applet.rotate((float) (-PI/4.0f + PI/2.0f));
                applet.line(0, 0,   100, 0);
                applet.line(70, 30, 100, 0);
                applet.line(70,-30, 100, 0);
            }
            else
            if(e.angle == 10) {
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
        }
        applet.popMatrix();
        //draw instance ID
        applet.fill(0,255,255);
        if(applet instanceof VisualReasonerWithGUI) {
            applet.textSize(20);
        }
        if(e.id.isEmpty()) {
            if(!isPredicted) {
                applet.text(String.valueOf(e.angle), (float)e.posX, (float)e.posY - VisualReasonerHeadless.discretization/2);
            }
        } else {
            if(applet instanceof VisualReasonerWithGUI) {
                applet.text(name, (float)e.posX- VisualReasonerHeadless.discretization/2, (float)e.posY - VisualReasonerHeadless.discretization/2);
            } else {
                applet.text(name, (float)e.posX, (float)e.posY);
            }
        }
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
            drawEntity(this, e, null, 0);
        }
        for (Prediction pred : trafficMultiNar.predictions) {
            Entity e = pred.ent;
            pushMatrix();
            translate(VisualReasonerHeadless.discretization/2,VisualReasonerHeadless.discretization/2);
            long dt = pred.time - trafficMultiNar.predictionNar.time();
            drawEntity(this, e, pred.truth, dt);
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
                String s = "is_jaywalking "+ent;
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
                    rect(X*VisualReasonerHeadless.discretization,Y*VisualReasonerHeadless.discretization,VisualReasonerHeadless.discretization,VisualReasonerHeadless.discretization);
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
            //speed calc:
            double[] XAll = new double[]{ X, Integer.valueOf(unwrap(props[7])), Integer.valueOf(unwrap(props[11])), Integer.valueOf(unwrap(props[15])), X2 };
            double[] YAll = new double[]{ Y, Integer.valueOf(unwrap(props[8])), Integer.valueOf(unwrap(props[12])), Integer.valueOf(unwrap(props[16])), Y2 };
            double speed = distanceSum(XAll, YAll);
            Entity added = null;
            if(props[0].equals("0")) { //person or vehicle for now
                if(speed < (double)movementThresholdPedestrian) {
                    continue;
                }
                Pedestrian toAdd = new Pedestrian(angle, X2, Y2, label);
                entities.add(toAdd);
                added = toAdd;
            } else {
                if(!props[0].equals("1")) {
                    if(speed < (double)movementThresholdCar) {
                        continue;
                    }
                    Car toAdd = new Car(angle, X2, Y2, label);
                    entities.add(toAdd);
                    added = toAdd;
                } else {
                    if(speed < (double)movementThresholdBike) {
                        continue;
                    }
                    Bike toAdd = new Bike(angle, X2, Y2, label);
                    entities.add(toAdd);
                    added = toAdd;
                }
            }
            added.speed = speed;
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
