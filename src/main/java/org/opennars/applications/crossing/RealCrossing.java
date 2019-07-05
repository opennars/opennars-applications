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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.opennars.applications.crossing.NarListener.Prediction;
import org.opennars.applications.gui.NarSimpleGUI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.JOptionPane;
import org.opennars.entity.Sentence;
import org.opennars.entity.Task;
import org.opennars.entity.TruthValue;
import org.opennars.inference.TruthFunctions;
import org.opennars.interfaces.Timable;
import org.opennars.io.Parser;
import org.opennars.io.events.AnswerHandler;
import org.opennars.io.events.Events;
import org.opennars.io.events.OutputHandler.DISAPPOINT;
import org.opennars.language.Inheritance;
import org.opennars.language.Term;
import org.opennars.main.Nar;
import org.opennars.operator.Operation;
import org.opennars.operator.Operator;
import org.opennars.storage.Memory;
import processing.core.PApplet;
import processing.core.PImage;
import processing.event.MouseEvent;

public class RealCrossing extends PApplet {
    public static Nar nar;
    static Nar qanar;
    static Nar locationNar;
    int entityID = 1;
    
    List<Prediction> predictions = new ArrayList<Prediction>();
    List<Prediction> disappointments = new ArrayList<Prediction>();
    final int streetWidth = 40;
    final int fps = 20;
    String[][] names = new String[30][30]; //make larger if needed :)
    
    public class Say extends Operator {
        public Say() {
            super("^say");
        }
        @Override
        public List<Task> execute(Operation operation, Term[] args, Memory memory, Timable time) {
            String s = "";
            for(int i=1;i<args.length;i++) {
                s+=args[i].toString() + " ";
            }
            //JOptionPane.showMessageDialog(null, "Operator information: "+s);
            panel.jTextArea5.setText(s + "\n" + panel.jTextArea5.getText() + " (frame=" + i + ")");
            return null;
        }
    }
    
    public static int SEQUENCE_BAG_ATTEMPTS = 0;
    public static boolean RELATIVE_LOCATION_RELATIONS = false;
    OperatorPanel panel = null;
    public static boolean running = false;
    @Override
    public void setup() {
        running = true;
        Camera cam = new Camera(500+streetWidth/2, 500+streetWidth/2);
        cam.radius = 600;
        cameras.add(cam);
        try {
            qanar = new Nar();
            qanar.addPlugin(new Say());
            qanar.narParameters.VOLUME = 0;
            locationNar = new Nar();
            nar = new Nar();
            
            SEQUENCE_BAG_ATTEMPTS = nar.narParameters.SEQUENCE_BAG_ATTEMPTS;
            locationNar.narParameters.SEQUENCE_BAG_ATTEMPTS=0;
            nar.narParameters.SEQUENCE_BAG_ATTEMPTS=0;
            
            //nar.narParameters.THREADS_AMOUNT = 2;
            //qanar.narParameters.THREADS_AMOUNT = 2;
            locationNar.narParameters.THREADS_AMOUNT = 4;
            
            nar.narParameters.VOLUME = 0;
            nar.narParameters.DURATION*=10;
            NarListener listener = new NarListener(cameras.get(0), nar, predictions, disappointments, entities);
            nar.on(Events.TaskAdd.class, listener);
            nar.on(DISAPPOINT.class, listener);
        } catch (Exception ex) {
            System.out.println(ex);
            System.exit(1);
        }
        panel = new OperatorPanel(qanar);
        panel.show();
        
        size(1280, 720);
        frameRate(fps);
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

    public static String questionsAndKnowledge = "(&|,<?1 --> pedestrian>,<?2 --> car>,<(*,?1,?2) --> leftOf>)? :|:";
    int perception_update = 1;
    public static int i = 4100; //2
    
    public String unwrap(String s) {
        return s.replace("[", "").replace("]", "");
    }
    
    public static String videopath="/mnt/sda1/Users/patha/Downloads/Test/Test/Test003/";
    public static String trackletpath = "/home/tc/Dateien/CROSSING/Test003/";
    public static double movementThresholdCar = 23;
    public static double movementThresholdBike = 5;
    public static double movementThresholdPedestrian = 5;
    public int skipFrames = 1; //skip frames for processing (not visualization)
    
    @Override
    public void draw() {
        frame.setTitle("RealCrossing (frame=" + i +")");
        //viewport.Transform();
        background(64,128,64);
        fill(0);
        for (Street s : streets) {
            s.draw(this);
        }
        String nr = String.format("%05d", i);
        PImage img = loadImage(videopath+nr+".jpg"); //1 2 3 7
        image(img, 0, 0);
        
        if(i % skipFrames == 0) {
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
        }
        
        i++;
        
        if(i % skipFrames == 0) {
            if(t > 0 && t % (5*perception_update) == 0) {

                System.out.println("TICK spatial");
                informNARSForQA(false);
                if(!"".equals(questionsAndKnowledge)) {
                    qanar.addInput(questionsAndKnowledge);
                }
            }

            if (t % perception_update == 0) {
                askForSemanticLabel();
                boolean hadInput = false;
                for(Camera c : cameras) {
                    final boolean force = false; // not required HACK
                    hadInput = hadInput || c.see(nar, entities, trafficLights, force);
                }
                if(hadInput) {
                    //qanar.addInput(questions);
                }
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
        if(i % skipFrames == 0) {
            qanar.cycles(1000); //for now its a seperate nar but can be merged potentially
                                //but this way we make sure predictions don't get worse when
                                //questions are given and vice versa
            nar.cycles(10); //only doing prediction so no need
            removeOutdatedPredictions(predictions);
            removeOutdatedPredictions(disappointments);
        }
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
    
    public class MapEvidence {
        public TruthValue car = new TruthValue(1.0f, 0.001f,locationNar.narParameters);
        public TruthValue pedestrian = new TruthValue(1.0f, 0.001f,locationNar.narParameters);
        public TruthValue bike = new TruthValue(1.0f, 0.001f,locationNar.narParameters);
        public String choice() {
            if(bike.getExpectation() > pedestrian.getExpectation() && bike.getExpectation() > car.getExpectation()) {
                return "street"; //TODO bikelane
            }
            if(pedestrian.getExpectation() > bike.getExpectation() && pedestrian.getExpectation() > car.getExpectation()) {
                return "sidewalk";
            }
            return "street";
        }
    }
    Map<String,MapEvidence> locationToLabel = new HashMap<String,MapEvidence>();
    private void askForSemanticLabel() {
        if(t > 0 && t % (5*perception_update) == 0) {
            locationNar.reset();
            informNARSForQA(true); //input locations
            try {
                for(String s : new String[] {"street","sidewalk","bikelane"}) {
                    locationNar.askNow("<?what --> ["+s+"]>", new AnswerHandler() {
                        @Override
                        public void onSolution(Sentence belief) {
                            //eternal or outdated
                            if(belief.isEternal() || locationNar.time()-belief.getOccurenceTime() > 100000) {
                                return;
                            }
                            String subj = ((Inheritance) belief.getTerm()).getSubject().toString();
                            if(subj.contains("_")) {
                                if(!locationToLabel.containsKey(subj)) {
                                    locationToLabel.put(subj, new MapEvidence());
                                }
                                MapEvidence mapval = locationToLabel.get(subj);
                                if(s.equals("street")) {
                                   TruthValue truth = mapval.car;
                                   TruthValue revised = TruthFunctions.revision(belief.truth, truth, locationNar.narParameters);
                                   mapval.car = revised;
                                }
                                if(s.equals("sidewalk")) {
                                    TruthValue truth = mapval.pedestrian;
                                    TruthValue revised = TruthFunctions.revision(belief.truth, truth, locationNar.narParameters);
                                    mapval.pedestrian = revised;
                                }
                                if(s.equals("bikelane")) {
                                    TruthValue truth = mapval.bike;
                                    TruthValue revised = TruthFunctions.revision(belief.truth, truth, locationNar.narParameters);
                                    mapval.bike = revised;
                                }
                            }
                        }
                    });
                }
            } catch (Parser.InvalidInputException ex) {
                Logger.getLogger(RealCrossing.class.getName()).log(Level.SEVERE, null, ex);
            }
            locationNar.addInput("<(&|,<#1 --> pedestrian>,<(*,#1,$location) --> at>) =|> <$location --> [sidewalk]>>.");
            locationNar.addInput("<(&|,<#1 --> car>,<(*,#1,$location) --> at>) =|> <$location --> [street]>>.");
            locationNar.addInput("<(&|,<#1 --> bike>,<(*,#1,$location) --> at>) =|> <$location --> [street]>>.");
            //locationNar.addInput("<(&|,<#1 --> bike>,<(*,#1,$location) --> at>) =|> <$location --> [bikelane]>>.");
        }
        if(t > 0 && t % (1*perception_update) == 0) {
            locationNar.cycles(300);
        }
    }
    
    ArrayList<ArrayList<String>> QAinformation = new ArrayList<ArrayList<String>>();
    public String informType(Entity entity) {
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
    
    public String name(Entity entity) { //TODO put in class
        if(entity instanceof Bike) {
            return "bike" + entity.label;
        }
        if(entity instanceof Car) {
            return "car" + entity.label;
        }
        if(entity instanceof Pedestrian) {
            return "pedestrian" + entity.label;
        }
        return "entity";
    }

    public double nearnessThreshold = 399; //3 times the discretization + 1 tolerance for the cell width
    public boolean near(Entity a, Entity b) {
        if(Math.sqrt(Math.pow(a.posX - b.posX, 2)+Math.pow(a.posY - b.posY, 2)) < nearnessThreshold) {
            return true;
        }
        return false;
    }
    
    public double veryClosenessThreshold = 199; //1 times the discretization + 1 tolerance for the cell width
    public boolean veryClose(Entity a, Entity b) {
        if(Math.sqrt(Math.pow(a.posX - b.posX, 2)+Math.pow(a.posY - b.posY, 2)) < veryClosenessThreshold) {
            return true;
        }
        return false;
    }
    
    public static boolean showPredictions = true;
    List<Entity> relatedLeft = new ArrayList<Entity>(); //just to visualize the entities that have been spatially related
    List<Entity> relatedRight = new ArrayList<Entity>(); //just to visualize the entities that have been spatially related
    Random rnd = new Random(1337);
    private void informNARSForQA(boolean updateLocationNar) {
        if(!updateLocationNar) {
            QAinformation.clear();
            relatedLeft.clear();
            relatedRight.clear();
            qanar.reset();
        }
        //inform NARS about the spatial relationships between objects and which categories they belong to according to the Tracker
        List<Entity> sortedEntX = entities.stream().sorted(Comparator.comparing(Entity::getPosX)).collect(Collectors.toList());
        for(Entity ent : sortedEntX) {
            if(!updateLocationNar && RealCrossing.RELATIVE_LOCATION_RELATIONS) {
                for(Entity entity : entities) {
                    if(ent != entity && near(ent, entity)) {
                        ArrayList<String> QAInfo = new ArrayList<String>();
                        if(ent.posX < entity.posX) {
                            QAInfo.add("<(*," + name(ent) + "," + name(entity) + ") --> leftOf>. :|:");
                            QAInfo.add("(&|," + informType(ent) + "," + informType(entity)+"). :|:");
                            if(veryClose(ent, entity)) {
                                QAInfo.add("<(*," + name(ent) + "," + name(entity) + ") --> closeTo>. :|:");
                            }
                            QAinformation.add(QAInfo);
                            relatedLeft.add(ent);
                            relatedRight.add(entity);
                        }
                        if(ent.posY < entity.posY) {
                            QAInfo.add("<(*," + name(ent) + "," + name(entity) + ") --> aboveOf>. :|:");
                            QAInfo.add("(&|," + informType(ent) + "," + informType(entity)+"). :|:");
                            if(veryClose(ent, entity)) {
                                QAInfo.add("<(*," + name(ent) + "," + name(entity) + ") --> closeTo>. :|:");
                            }
                            QAinformation.add(QAInfo);
                            relatedLeft.add(ent);
                            relatedRight.add(entity);
                        }
                    }
                }
            }
            String typeInfo = informType(ent)+". :|:";
            ArrayList<String> info = new ArrayList<String>();
            info.add(typeInfo);
            if(!updateLocationNar) {
                //QAinformation.add(info); //already below
            }
            if(updateLocationNar) {
                locationNar.addInput(typeInfo);
            }
            //also give info about position at labelled locations
            int X = (int) (ent.posX / Util.discretization);
            int Y = (int) (ent.posY / Util.discretization);
            String subj = X + "_" + Y;
            if(!updateLocationNar && locationToLabel.containsKey(subj)) {
                System.out.println("QA INFO: <(*,"+name(ent)+","+locationToLabel.get(subj).choice()+") --> at>. :|:");
                ArrayList<String> Atinfo = new ArrayList<String>();
                Atinfo.add(typeInfo);
                Atinfo.add("<(*,"+name(ent)+","+locationToLabel.get(subj).choice()+") --> at>. :|:");
                QAinformation.add(Atinfo);
            }
            if(updateLocationNar) {
                String locationnarInput = "<(*,"+name(ent)+","+Util.positionToTerm((int)ent.posX,(int)ent.posY)+") --> at>. :|:";
                locationNar.addInput(locationnarInput);
                System.out.println("location nar input: " + locationnarInput);
            }
        }
        if(updateLocationNar) {
            return;
        }
        
        Collections.shuffle(QAinformation);
        int take_k = 16;
        int k = 0;
        for(ArrayList<String> info : QAinformation) {
            for(String s : info) {
                System.out.println(s);
                qanar.addInput(s);
                k++;
                if(k >= take_k) {
                    break;
                }
            }
            if(k >= take_k) {
                break;
            }
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
            RealCrossing.movementThresholdCar = Integer.valueOf(args[3]);
            
        }
        String[] args2 = {"Street Scene"};
        RealCrossing mp = new RealCrossing();
        PApplet.runSketch(args2, mp);
    }
}
