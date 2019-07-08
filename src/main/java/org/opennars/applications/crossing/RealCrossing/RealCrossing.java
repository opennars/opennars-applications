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
package org.opennars.applications.crossing.RealCrossing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.opennars.applications.crossing.NarListener.Prediction;
import org.opennars.applications.gui.NarSimpleGUI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opennars.applications.crossing.Bike;
import org.opennars.applications.crossing.Camera;
import org.opennars.applications.crossing.Car;
import org.opennars.applications.crossing.Entity;
import org.opennars.applications.crossing.OperatorPanel;
import org.opennars.applications.crossing.Pedestrian;
import org.opennars.applications.crossing.Street;
import org.opennars.applications.crossing.TrafficLight;
import org.opennars.applications.crossing.Util;
import org.opennars.entity.Task;
import org.opennars.interfaces.Timable;
import org.opennars.language.Term;
import org.opennars.operator.Operation;
import org.opennars.operator.Operator;
import org.opennars.storage.Memory;
import processing.core.PApplet;
import processing.core.PImage;

public class RealCrossing extends PApplet {

    int entityID = 1;
    

    final int streetWidth = 40;
    final int fps = 20;
    String[][] names = new String[30][30]; //make larger if needed :)
    
    public static HashMap<String,Integer> jaywalkers = new HashMap<String,Integer>();
    public static HashMap<String,Integer> indangers = new HashMap<String,Integer>();
    public void cleanupMarkers() {
        int cleanup_timeout = 20;
        List<String> cleanupJaywalkers = new ArrayList<String>();
        for(String key : jaywalkers.keySet()) {
            if(i - jaywalkers.get(key) > cleanup_timeout) {
                cleanupJaywalkers.add(key);
            }
        }
        for(String key : cleanupJaywalkers) {
            jaywalkers.remove(key);
        }
        List<String> cleanupIndangers = new ArrayList<String>();
        for(String key : indangers.keySet()) {
            if(i - indangers.get(key) > cleanup_timeout) {
                cleanupIndangers.add(key);
            }
        }
        for(String key : cleanupIndangers) {
            indangers.remove(key);
        }
    }
    
    public class Say extends Operator {
        public Say() {
            super("^say");
        }
        @Override
        public List<Task> execute(Operation operation, Term[] args, Memory memory, Timable time) {
            String s = "";
            if(args.length > 2) { //{SELF} car3 message
                if(args[2].toString().equals("is_jaywalking")) {
                    synchronized(jaywalkers) {
                        jaywalkers.put(args[1].toString(), i);
                    }
                }
                else
                if(args[2].toString().equals("is_in_danger")) {
                    synchronized(indangers) {
                        indangers.put(args[1].toString(), i);
                    }
                }
            }
            for(int i=1;i<args.length;i++) {
                s+=args[i].toString().replace("_", " ") + " ";
            }
            //JOptionPane.showMessageDialog(null, "Operator information: "+s);
            panel.jTextArea5.setText(s + " (frame=" + i + ")" + "\n" + panel.jTextArea5.getText());
            return null;
        }
    }
    
    public static TrafficMultiNar trafficMultiNar = null;
    OperatorPanel panel = null;
    public static boolean running = false;
    @Override
    public void setup() {
        size(1280, 720);
        this.setSize(1280, 720);
        running = true;
                 
        Camera cam = new Camera(500+streetWidth/2, 500+streetWidth/2);
        cam.radius = 600;
        cameras.add(cam);
        
        trafficMultiNar = new TrafficMultiNar(new Say(), entities, cam);
        //attach a panel to the Qanar:
        panel = new OperatorPanel(trafficMultiNar.qanar);
        panel.show();

        frameRate(fps);
        //optionally add simple GUI to the Nar instances of the TrafficMultiNar:
        //new NarSimpleGUI(nar);
        //new NarSimpleGUI(qanar);
        //new NarSimpleGUI(locationNar);
    }

    List<Street> streets = new ArrayList<Street>();
    List<TrafficLight> trafficLights = new ArrayList<TrafficLight>();
    List<Entity> entities = new ArrayList<Entity>();
    List<Camera> cameras = new ArrayList<Camera>();
    int t = 0;
    public static boolean showAnomalies = false;
    public static boolean showPredictions = true;

    
    int perception_update = 1;
    public static int i = 4100; //2
    
    public String unwrap(String s) {
        return s.replace("[", "").replace("]", "");
    }
    
    public static String videopath="/mnt/sda1/Users/patha/Downloads/Test/Test/Test003/";
    public static String trackletpath = "/home/tc/Dateien/CROSSING/Test003/";
    public static double movementThresholdCar = 30; //23
    public static double movementThresholdBike = 5; //5
    public static double movementThresholdPedestrian = 5; //5
    
    @Override
    public void draw() {
        frame.setTitle("RealCrossing (frame=" + i +")");
        //viewport.Transform();
        background(64,128,64);
        fill(0);
        cleanupMarkers();
        for (Street s : streets) {
            s.draw(this);
        }
        String nr = String.format("%05d", i);
        PImage img = loadImage(videopath+nr+".jpg"); //1 2 3 7
        image(img, 0, 0);
        
        entities.clear(); //refresh
        String tracklets = "";
        try {
            ///home/tc/Dateien/CROSSING/Test001/TKL00342.txt
            tracklets = new String(Files.readAllBytes(Paths.get(trackletpath+"TKL"+nr+".txt")));
        } catch (IOException ex) {
            Logger.getLogger(RealCrossing.class.getName()).log(Level.SEVERE, null, ex);
        }
        String[] lines = tracklets.replace("[ ","[").replace(" ]","]").replace("  "," ").replace("  "," ").split("\n");
        for(String s : lines) {
            //ClassID TrackID : [X5c Y5c W5 H5] 
            String[] props = s.split(" ");
            String label = props[1];
            int id = 0; //treat them as same for now, but distinguished by type!
            if(unwrap(props[3]).equals("")) {
                System.out.println(s);
            }
            Integer X = Integer.valueOf(unwrap(props[3]));
            Integer Y = Integer.valueOf(unwrap(props[4]));

            Integer X2 = Integer.valueOf(unwrap(props[19])); //7 6
            Integer Y2 = Integer.valueOf(unwrap(props[20]));

            //use an id according to movement direction
            if(X < X2) {
                id += 10;
            }
            if(Y < Y2) {
                id += 1;
            }

            double movement = Math.sqrt((X-X2)*(X-X2) + (Y - Y2)*(Y - Y2));
            if(props[0].equals("0")) { //person or vehicle for now
                if(movement < (double)movementThresholdPedestrian) {
                    continue;
                }
                Pedestrian toAdd = new Pedestrian(id, X, Y, 0, 0, label);
                entities.add(toAdd);
            } else {
                if(!props[0].equals("1")) {
                    if(movement < (double)movementThresholdCar) {
                        continue;
                    }
                    Car toAdd = new Car(id, X, Y, 0, 0, label);
                    entities.add(toAdd);
                } else {
                    if(movement < (double)movementThresholdBike) {
                        continue;
                    }
                    Bike toAdd = new Bike(id, X, Y, 0, 0, label);
                    entities.add(toAdd);
                }
            }
        }

        i++;
        
        trafficMultiNar.perceiveScene(t, perception_update);
        
        Entity.DrawDirection = false;
        Entity.DrawID = true;
        for (int i = 0; i < 3000; i += Util.discretization) {
            stroke(128);
            line(0, i, 3000, i);
            line(i, 0, i, 3000);
        }

        for (Entity e : entities) {
            e.draw(this, null, 0);
        }
        for (TrafficLight tl : trafficLights) {
            tl.draw(this, t);
        }

        t++;
        trafficMultiNar.reason();
        for (Prediction pred : trafficMultiNar.predictions) {
            Entity e = pred.ent;
            pushMatrix();
            translate(Util.discretization/2,Util.discretization/2);
            e.draw(this, pred.truth, pred.time - trafficMultiNar.nar.time());
            popMatrix();
        }
        if(showAnomalies) {
            for (Prediction pred : trafficMultiNar.disappointments) {
                Entity e = pred.ent;
                if(e instanceof Car) {
                    fill(255,0,0);
                }
                if(e instanceof Pedestrian) {
                    fill(0,0,255);
                }
                this.text("ANOMALY", (float)e.posX, (float)e.posY);
                e.draw(this, pred.truth, pred.time - trafficMultiNar.nar.time());
            }
        }
        for(Camera c : cameras) {
            //c.draw(this);
        }
        for(int i=0; i<trafficMultiNar.informNARS.relatedLeft.size(); i++) {
            Entity left = trafficMultiNar.informNARS.relatedLeft.get(i);
            Entity right = trafficMultiNar.informNARS.relatedRight.get(i);
            stroke(200,200,0);
            line((float)left.posX, (float)left.posY, (float)right.posX, (float)right.posY);
        }
        stroke(128);
        //System.out.println("Concepts: " + trafficMultiNar.nar.memory.concepts.size());
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
        System.out.println("args: videopath trackletpath [discretization movementThreshold]");
        System.out.println("example: java -cp \"*\" org.opennars.applications.crossing.RealCrossing /mnt/sda1/Users/patha/Downloads/Test/Test/Test001/ /home/tc/Dateien/CROSSING/Test001/ 100 10");
        Util.discretization = 100;
        if(args.length == 2) {
            RealCrossing.videopath = args[0];
            RealCrossing.trackletpath = args[1];
        }
        if(args.length == 4) {
            RealCrossing.videopath = args[0];
            RealCrossing.trackletpath = args[1];
            Util.discretization = Integer.valueOf(args[2]);
            RealCrossing.movementThresholdCar = Integer.valueOf(args[3]);
            
        }
        String[] args2 = {"Street Scene"};
        RealCrossing mp = new RealCrossing();
        PApplet.runSketch(args2, mp);
    }
}
