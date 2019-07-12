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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.opennars.applications.crossing.NarListener.Prediction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opennars.applications.crossing.Bike;
import org.opennars.applications.crossing.Camera;
import org.opennars.applications.crossing.Car;
import org.opennars.applications.crossing.Entity;
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

public class RealCrossing {

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
            if(outputFolder != null) {
                try {
                    String fname = String.format("%05d", i);
                    String st = s;
                    if(labelToLocation.containsKey(args[1].toString())) {
                        st += " " + labelToLocation.get(args[1].toString());
                    }
                    FileOutputStream fs = new FileOutputStream(outputFolder+fname+".txt",true);
                    fs.write(st.getBytes("UTF8"));
                    fs.write("\n".getBytes("UTF8"));
                    fs.close();
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(RealCrossing.class.getName()).log(Level.SEVERE, null, ex);
                } catch (UnsupportedEncodingException ex) {
                    Logger.getLogger(RealCrossing.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(RealCrossing.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            return null;
        }
    }
    
        
    public void loadOntology() throws URISyntaxException, IOException {
        //String file = new String(Files.readAllBytes(Paths.get(new URI("./StreetScene/AnomalyOntology.nal"))));
        String ontPath = customOntologyPath == null ? "./StreetScene/AnomalyOntology.nal" : customOntologyPath;
        String content = new String(Files.readAllBytes(new File(ontPath).toPath()),Charset.forName("UTF-8"));
        String qapart = content.split(">>QANar:")[1].split(">>LocationNar:")[0].trim();
        String locpart = content.split(">>LocationNar:")[1].split(">>General information:")[0].trim();
        trafficMultiNar.informLocationNar.ontology = locpart;
        trafficMultiNar.informQaNar.ontology = qapart;
    }
    
    public static TrafficMultiNar trafficMultiNar = null;
    public static boolean running = false;

    public void setup() {
        running = true;
                   
        Camera cam = new Camera(500+streetWidth/2, 500+streetWidth/2);
        cam.radius = 600;
        cameras.add(cam);

        trafficMultiNar = new TrafficMultiNar(new Say(), entities, cam);
                
        try {
            //Load in ontology
            loadOntology();
        } catch (URISyntaxException ex) {
            Logger.getLogger(RealCrossing.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(RealCrossing.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    List<Street> streets = new ArrayList<Street>();
    List<TrafficLight> trafficLights = new ArrayList<TrafficLight>();
    List<Entity> entities = new ArrayList<Entity>();
    List<Camera> cameras = new ArrayList<Camera>();
    int t = 0;

    
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
    
    class Point {
        int X;
        int Y;
        public Point(int X, int Y) {
            this.X = X;
            this.Y = Y;
        }
        @Override
        public String toString() {
            return "(location " + X + " " + Y + ")";
        }
    }
    HashMap<String,Point> labelToLocation = new HashMap<String,Point>();
    
    public void step() {
        cleanupMarkers();
        
        String nr = "";
        if(liveVideo) { //or when debugging
            try {
                Object[] paths = Files.list(Paths.get(trackletpath)).sorted().toArray();
                String extractNumber = paths[paths.length-2].toString();
                String number = extractNumber.split("/TKL")[1].split(".txt")[0];
                i = Integer.valueOf(number);

                //System.out.println(paths[paths.length-1]);

                        } catch (IOException ex) {
                Logger.getLogger(RealCrossing.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        nr = String.format("%05d", i);
        
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
            if(s.trim().isEmpty()) {
                continue;
            }
            //ClassID TrackID : [X5c Y5c W5 H5] 
            String[] props = s.split(" ");
            String label = props[1];
            int id = 0; //treat them as same for now, but distinguished by type!
            if(unwrap(props[3]).equals("")) {
                System.out.println(s);
            }
            Integer X = Integer.valueOf(unwrap(props[3]));
            Integer Y = Integer.valueOf(unwrap(props[4]));
            
            Integer width = Integer.valueOf(unwrap(props[5]));
            Integer height = Integer.valueOf(unwrap(props[6]));

            Integer X2 = Integer.valueOf(unwrap(props[19])); //7 6
            Integer Y2 = Integer.valueOf(unwrap(props[20]));

            //use an id according to movement direction
            if(X < X2) {
                id += 10;
            }
            if(Y < Y2) {
                id += 1;
            }
            
            //double angle = Math.atan2(Y - Y2, X - X2);
            
            double movement = Math.sqrt((X-X2)*(X-X2) + (Y - Y2)*(Y - Y2));
            if(props[0].equals("0")) { //person or vehicle for now
                if(movement < (double)movementThresholdPedestrian) {
                    continue;
                }
                labelToLocation.put("pedestrian"+label, new Point(X2,Y2));
                Pedestrian toAdd = new Pedestrian(id, X2, Y2, 0, 0, label);
                toAdd.width = width;
                toAdd.height = height;
                //toAdd.angle = angle;
                entities.add(toAdd);
            } else {
                if(!props[0].equals("1")) {
                    if(movement < (double)movementThresholdCar) {
                        continue;
                    }
                    labelToLocation.put("car"+label, new Point(X2,Y2));
                    Car toAdd = new Car(id, X2, Y2, 0, 0, label);
                    toAdd.width = width;
                    toAdd.height = height;
                    //toAdd.angle = angle;
                    entities.add(toAdd);
                } else {
                    if(movement < (double)movementThresholdBike) {
                        continue;
                    }
                    labelToLocation.put("bike"+label, new Point(X2,Y2));
                    Bike toAdd = new Bike(id, X2, Y2, 0, 0, label);
                    toAdd.width = width;
                    toAdd.height = height;
                    //toAdd.angle = angle;
                    entities.add(toAdd);
                }
            }
        }

        i++;
        
        trafficMultiNar.perceiveScene(t, perception_update);
        t++;
        trafficMultiNar.reason();
        for (Prediction pred : trafficMultiNar.predictions) {
            Entity e = pred.ent;
            //return predictions too?
        }
        for(Camera c : cameras) {
            //c.draw(this);
        }
        for(int i=0; i<trafficMultiNar.informQaNar.relatedLeft.size(); i++) {
            Entity left = trafficMultiNar.informQaNar.relatedLeft.get(i);
            Entity right = trafficMultiNar.informQaNar.relatedRight.get(i);
            //return spatially related entities?
        }
        //System.out.println("Concepts: " + trafficMultiNar.nar.memory.concepts.size());
    }
    public static boolean liveVideo = false;
    
    public static int resX = 1280;
    public static int resY = 720;
    public static String outputFolder = null;
    public static String customOntologyPath = null;
    
    public static void main(String[] args) throws InterruptedException {
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
            java.util.logging.Logger.getLogger(RealCrossing.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>
        System.out.println("args: videopath trackletpath [discretization movementThreshold useLiveVideo resX resY outputFile veryClosenessThreshold]");
        System.out.println("example: java -cp \"*\" org.opennars.applications.crossing.RealCrossing /home/tc/video/ /home/tc/tracklets/ 80 30 True 1280 720 /home/tc/output/ /home/tc/Dateien/CROSSING/StreetScene/AnomalyOntology.nal 169");
        Util.discretization = 80;
        if(args.length >= 2) {
            RealCrossing.videopath = args[0];
            RealCrossing.trackletpath = args[1];
        }
        if(args.length >= 4) {
            Util.discretization = Integer.valueOf(args[2]);
            RealCrossing.movementThresholdCar = Integer.valueOf(args[3]); 
        }
        if(args.length >= 5) {
            if(args[4].equals("True")) {
                liveVideo = true;
            }
        }
        if(args.length >= 7) {
            resX = Integer.valueOf(args[5]);
            resY = Integer.valueOf(args[6]);
        }
        if(args.length >= 8) {
            outputFolder = args[7];
        }
        if(args.length >= 9) {
            customOntologyPath = args[8];
        }
        if(args.length >= 10) {
            InformQaNar.veryClosenessThreshold = Integer.valueOf(args[9]);
        }
        //new FileOutputStream(saveFile, true);
        String[] args2 = {"Street Scene"};
        RealCrossing mp = new RealCrossing();
        mp.setup();
        while(true) {
            mp.step();
            Thread.sleep(1000/mp.fps);
        }
    }
}
