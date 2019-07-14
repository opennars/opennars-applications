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

import com.jsoniter.JsonIterator;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
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
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;


public class RealCrossing {

    int entityID = 1;
    final int streetWidth = 40;
    
    public static HashMap<String,Integer> jaywalkers = new HashMap<String,Integer>();
    public static HashMap<String,Integer> indangers = new HashMap<String,Integer>();
    public static HashMap<String,Integer> relations = new HashMap<String,Integer>();
    public void cleanupAnomalies() {
        List<String> cleanupJaywalkers = new ArrayList<String>();
        for(String key : jaywalkers.keySet()) {
            if(i - jaywalkers.get(key) > anomalyRetrieveDuration) {
                cleanupJaywalkers.add(key);
            }
        }
        for(String key : cleanupJaywalkers) {
            jaywalkers.remove(key);
        }
        List<String> cleanupIndangers = new ArrayList<String>();
        for(String key : indangers.keySet()) {
            if(i - indangers.get(key) > anomalyRetrieveDuration) {
                cleanupIndangers.add(key);
            }
        }
        for(String key : cleanupIndangers) {
            indangers.remove(key);
        }
        List<String> cleanupRelations = new ArrayList<String>();
        for(String key : relations.keySet()) {
            if(i - relations.get(key) > anomalyRetrieveDuration) {
                cleanupRelations.add(key);
            }
        }
        for(String key : cleanupRelations) {
            relations.remove(key);
        }
    }
    
    public class Say extends Operator {
        public Say() {
            super("^say");
        }
        @Override
        public List<Task> execute(Operation operation, Term[] args, Memory memory, Timable time) {
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
    
    int perception_update = 1;
    public static int i = 0;
    
    public String unwrap(String s) {
        return s.replace("[", "").replace("]", "");
    }
    
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
    
    public void step() throws FileNotFoundException, UnsupportedEncodingException, IOException {
        cleanupAnomalies();
        entities.clear(); //refresh detections
        //pop the newest, blocking if empty queue
        /*String tkl_msg = r.blpop(0,QTrackletToNar).get(1);
        //trim everything except the latest
        long new_messages_amount = r.llen(QTrackletToNar);
        if(new_messages_amount > 0) {
            r.ltrim(QTrackletToNar,new_messages_amount-1,-1);
        }*/
        String tkl_msg = r.brpop(0,QTrackletToNar).get(1);
        long[][] tracklets = JsonIterator.deserialize(tkl_msg, long[][].class);
        
        //process (TODO get rid of format)
        for(long[] props : tracklets) {
            //ClassID TrackID X1 Y1 W1 H1 P1 X2 Y2 W2 H2 P2 X3 Y3 W3 H3 P3 X4 Y4 W4 H4 P4 X5 Y5 W5 H5 P5
            String label = "" + props[1];
            int id = 0; //treat them as same for now, but distinguished by type!
            long probability = props[6];
            if(probability == 0) {
                continue;
            }
            long X = props[2];
            long Y = props[3];
            
            long width = props[4];
            long height = props[5];

            long X2 = props[22]; //7 6
            long Y2 = props[23];

            //use an id according to movement direction
            if(X < X2) {
                id += 10;
            }
            if(Y < Y2) {
                id += 1;
            }  
            double movement = Math.sqrt((X-X2)*(X-X2) + (Y - Y2)*(Y - Y2));
            if(props[0] == 0) { //person or vehicle for now
                if(movement < (double)movementThresholdPedestrian) {
                    continue;
                }
                Pedestrian toAdd = new Pedestrian(id, X2, Y2, 0, 0, label);
                toAdd.width = width;
                toAdd.height = height;
                //toAdd.angle = angle;
                entities.add(toAdd);
            } else {
                if(props[0] == 2) {
                    if(movement < (double)movementThresholdCar) {
                        continue;
                    }
                    Car toAdd = new Car(id, X2, Y2, 0, 0, label);
                    toAdd.width = width;
                    toAdd.height = height;
                    //toAdd.angle = angle;
                    entities.add(toAdd);
                } else {
                    if(movement < (double)movementThresholdBike) {
                        continue;
                    }
                    Bike toAdd = new Bike(id, X2, Y2, 0, 0, label);
                    toAdd.width = width;
                    toAdd.height = height;
                    //toAdd.angle = angle;
                    entities.add(toAdd);
                }
            }
        }
        
        trafficMultiNar.perceiveScene(i, perception_update);
        trafficMultiNar.reason();
        for (Prediction pred : trafficMultiNar.predictions) {
            Entity e = pred.ent;
            float value = Util.truthToValue(pred.truth);
            long predictionTime = pred.time;
            //return predictions too?
            String st = "predicted " + pred.type + " " + e.posX + " " + e.posY + " "+ e.id + " " + value + " " + predictionTime;
            //System.out.println(st);
            msgs.add(st);
        }
        synchronized(trafficMultiNar.informQaNar.relatedLeft) {
            for(int k=0; k<trafficMultiNar.informQaNar.relatedLeft.size(); k++) {
                Entity left = trafficMultiNar.informQaNar.relatedLeft.get(k);
                Entity right = trafficMultiNar.informQaNar.relatedRight.get(k);
                //return spatially related entities?
                String st = "related "+EntityToNarsese.name(left) + " " + EntityToNarsese.name(right);
                relations.put(st, i);
                //System.out.println(st);
                //msgs.add(st);
            }
        }
        for(String ent : relations.keySet()) {
            msgs.add(ent);
        }
        synchronized(indangers) {
            for(String ent : indangers.keySet()) {
                msgs.add("in_danger "+ent);
            }
        }
        synchronized(jaywalkers) {
            for(String ent : jaywalkers.keySet()) {
                msgs.add("is_jaywalking "+ent);
            }
        }
        //send info packet to redis
        MessagesToScript();
        i++;
    }
    
    public void MessagesToScript() {
        String s = "";
        for(String msg : msgs) {
            s += msg + "\n";
        }
        //redis rpush
        r.rpush(QInfoFromNar, s);
        msgs.clear();
    }

    List<String> msgs = new ArrayList<String>();
    
    public static int resX = 1280;
    public static int resY = 720;
    public static String customOntologyPath = null;
    public static int anomalyRetrieveDuration = 30;
    static Jedis r = null;
    public static String QTrackletToNar = null;
    public static String QInfoFromNar = null;
    
    public static void main(String[] args) throws InterruptedException, FileNotFoundException, IOException {
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
        System.out.println("args: discretization movementThresholdCar movementThresholdPedestrian movementThresholdBike veryClosenessThreshold resX resY OntologyNalFile AnomalyRetrieveDuration redisHost redisPort redisPassword QTrackletToNar QInfoFromNar");
        System.out.println("example: java -cp \"*\" org.opennars.applications.crossing.RealCrossing 80 30 5 5 169 1280 720 /home/tc/Dateien/CROSSING/StreetScene/AnomalyOntology.nal 30 locahost 6379 pwd Q_Tracklet_To_Nar Q_Info_From_Nar");
        Util.discretization = Integer.valueOf(args[0]);
        RealCrossing.movementThresholdCar = Integer.valueOf(args[1]); 
        RealCrossing.movementThresholdPedestrian = Integer.valueOf(args[2]); 
        RealCrossing.movementThresholdBike = Integer.valueOf(args[3]); 
        InformQaNar.veryClosenessThreshold = Integer.valueOf(args[4]);
        resX = Integer.valueOf(args[5]);
        resY = Integer.valueOf(args[6]);
        customOntologyPath = args[7];
        anomalyRetrieveDuration = Integer.valueOf(args[8]);
        String redishost = args[9];
        int redisport = Integer.valueOf(args[10]);
        String redispwd = args[11];
        QTrackletToNar = args[12];
        QInfoFromNar = args[13];
        r = new JedisPool(redishost, redisport).getResource();
        RealCrossing mp = new RealCrossing();
        mp.setup();
        while(true) {
            mp.step();
            Thread.sleep(0);
        }
    }
}
