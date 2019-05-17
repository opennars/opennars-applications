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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.opennars.applications.crossing.NarListener.Prediction;
import org.opennars.applications.gui.NarSimpleGUI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.opennars.io.events.Events;
import org.opennars.io.events.OutputHandler.DISAPPOINT;
import org.opennars.main.Nar;
import processing.core.PApplet;
import processing.core.PImage;
import processing.event.MouseEvent;

public class RealCrossing extends PApplet {
    static Nar nar;
    static Nar qanar;
    int entityID = 1;
    
    List<Prediction> predictions = new ArrayList<Prediction>();
    List<Prediction> disappointments = new ArrayList<Prediction>();
    final int streetWidth = 40;
    final int fps = 20;
    @Override
    public void setup() {
        Camera cam = new Camera(500+streetWidth/2, 500+streetWidth/2);
        cam.radius = 600;
        cameras.add(cam);
        try {
            qanar = new Nar();
            qanar.narParameters.VOLUME = 0;
            nar = new Nar();
            nar.narParameters.VOLUME = 0;
            nar.narParameters.DURATION*=10;
            NarListener listener = new NarListener(cameras.get(0), nar, predictions, disappointments, entities);
            nar.on(Events.TaskAdd.class, listener);
            nar.on(DISAPPOINT.class, listener);
        } catch (Exception ex) {
            System.out.println(ex);
            System.exit(1);
        }
        new OperatorPanel(qanar).show();
        //int trafficLightRadius = 25;
        streets.add(new Street(false, 0, 500, 1000, 500 + streetWidth));
        streets.add(new Street(true, 500, 0, 500 + streetWidth, 1000));
        int trafficLightID = 1;
        //trafficLights.add(new TrafficLight(trafficLightID++, trafficLightRadius, 500 + streetWidth + trafficLightRadius, 500 + streetWidth/2, 0));
        //trafficLights.add(new TrafficLight(trafficLightID++, trafficLightRadius, 500 - trafficLightRadius, 500 + streetWidth/2, 0));
        //trafficLights.add(new TrafficLight(trafficLightID++, trafficLightRadius/2, 500 + streetWidth, 500 + streetWidth + trafficLightRadius, 1));
        //trafficLights.add(new TrafficLight(trafficLightID++, trafficLightRadius/2, 500, 500 - trafficLightRadius, 1));
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
        
        size(1920, 1080);
        frameRate(fps);
        new NarSimpleGUI(nar);
    }

    List<Street> streets = new ArrayList<Street>();
    List<TrafficLight> trafficLights = new ArrayList<TrafficLight>();
    List<Entity> entities = new ArrayList<Entity>();
    List<Camera> cameras = new ArrayList<Camera>();
    int t = 0;
    public static boolean showAnomalies = false;

    public static String questions = "(&|,<?1 --> pedestrian>,<?2 --> car>,<(*,?1,?2) --> leftOf>)? :|:";
    int perception_update = 1;
    int i = 2;
    
    public String unwrap(String s) {
        return s.replace("[", "").replace("]", "");
    }
    
    public static String videopath="/mnt/sda1/Users/patha/Downloads/Test/Test/Test001/";
    public static String trackletpath = "/home/tc/Dateien/CROSSING/Test001/";
    public static double movementThreshold = 10;
    
    @Override
    public void draw() {
        viewport.Transform();
        background(64,128,64);
        fill(0);
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
            
            if(Math.sqrt((X-X2)*(X-X2) + (Y - Y2)*(Y - Y2)) < ((double)Util.discretization)/((double)movementThreshold)) {
                continue;
            }
            
            if(props[0].equals("0")) { //person or vehicle for now, TODO make car motorcycle distinction
                entities.add(new Car(id, X, Y, 0, 0, label));
            } else {
                entities.add(new Pedestrian(id, X, Y, 0, 0, label));
            }
        }
        
        i++;
        
        if(t % (10*perception_update) == 0) {
            System.out.println("TICK spatial");
            informNARSForQA();
            if(!"".equals(questions)) {
                qanar.addInput(questions);
            }
        }
        
        if (t % perception_update == 0) {
            
            boolean hadInput = false;
            for(Camera c : cameras) {
                final boolean force = false; // not required HACK
                hadInput = hadInput || c.see(nar, entities, trafficLights, force);
            }
            if(hadInput) {
                //qanar.addInput(questions);
            }
        }
        
        Entity.DrawDirection = false;
        Entity.DrawID = true;
        for (int i = 0; i < 3000; i += Util.discretization) {
            stroke(128);
            line(0, i, 3000, i);
            line(i, 0, i, 3000);
        }

        for (Entity e : entities) {
            e.draw(this, streets, trafficLights, entities, null, 0);
        }
        for (TrafficLight tl : trafficLights) {
            tl.draw(this, t);
        }

        // tick
        for (Entity ie : entities) {
            ie.tick();
        }


        t++;
        qanar.cycles(1000); //for now its a seperate nar but can be merged potentially
                            //but this way we make sure predictions don't get worse when
                            //questions are given and vice versa
        nar.cycles(10); //only doing prediction so no need
        removeOutdatedPredictions(predictions);
        removeOutdatedPredictions(disappointments);
        for (Prediction pred : predictions) {
            Entity e = pred.ent;
            pushMatrix();
            translate(Util.discretization/2,Util.discretization/2);
            e.draw(this, streets, trafficLights, entities, pred.truth, pred.time - nar.time());
            popMatrix();
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
            //c.draw(this);
        }
        for(int i=0; i<relatedLeft.size(); i++) {
            Entity left = relatedLeft.get(i);
            Entity right = relatedRight.get(i);
            stroke(255,0,0);
            line((float)left.posX, (float)left.posY, (float)right.posX, (float)right.posY);
        }
        stroke(128);
        System.out.println("Concepts: " + nar.memory.concepts.size());
    }
    
    List<String> information = new ArrayList<String>();
    public String informType(Entity entity) {
        if(entity instanceof Car) {
            return "<" + name(entity) + " --> car>";
        }
        if(entity instanceof Pedestrian) {
            return "<" +name(entity) + " --> pedestrian>";
        }
        return "<" +name(entity) + " --> entity>";
    }
    
    public String name(Entity entity) { //TODO put in class
        if(entity instanceof Car) {
            return "car" + entity.label;
        }
        if(entity instanceof Pedestrian) {
            return "pedestrian" + entity.label;
        }
        return "entity";
    }

    public double closenessThreshold = 399; //2 times the discretization + 1 tolerance for the cell width
    public boolean near(Entity a, Entity b) {
        if(Math.sqrt(Math.pow(a.posX - b.posX, 2)+Math.pow(a.posY - b.posY, 2)) < closenessThreshold) {
            return true;
        }
        return false;
    }
    
    List<Entity> relatedLeft = new ArrayList<Entity>(); //just to visualize the entities that have been spatially related
    List<Entity> relatedRight = new ArrayList<Entity>(); //just to visualize the entities that have been spatially related
    Random rnd = new Random(1337);
    private void informNARSForQA() {
        information.clear();
        relatedLeft.clear();
        relatedRight.clear();
        qanar.reset();
        //inform NARS about the spatial relationships between objects and which categories they belong to according to the Tracker
        List<Entity> sortedEntX = entities.stream().sorted(Comparator.comparing(Entity::getPosX)).collect(Collectors.toList());
        for(Entity ent : sortedEntX) {
            for(Entity entity : entities) {
                if(ent != entity && near(ent, entity)) {
                    if(ent.posX < entity.posX) {
                        information.add("<(*," + name(ent) + "," + name(entity) + ") --> leftOf>. :|:");
                        information.add("(&|," + informType(ent) + "," + informType(entity)+"). :|:");
                        relatedLeft.add(ent);
                        relatedRight.add(entity);
                    }
                    if(ent.posY < entity.posY) {
                        information.add("<(*," + name(ent) + "," + name(entity) + ") --> aboveOf>. :|:");
                        information.add("(&|," + informType(ent) + "," + informType(entity)+"). :|:");
                        relatedLeft.add(ent);
                        relatedRight.add(entity);
                    }
                }
            }
        }
        for(String s : information) {
            System.out.println(s);
        }
        for(String s : information) {
           qanar.addInput(s);
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
            RealCrossing.movementThreshold = Integer.valueOf(args[3]);
            
        }
        String[] args2 = {"Crossing"};
        RealCrossing mp = new RealCrossing();
        PApplet.runSketch(args2, mp);
    }
}
