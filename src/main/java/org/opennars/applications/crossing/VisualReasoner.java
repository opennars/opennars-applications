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

import org.opennars.applications.crossing.Entities.Pedestrian;
import org.opennars.applications.crossing.Entities.Entity;
import org.opennars.applications.crossing.Entities.Car;
import org.opennars.applications.crossing.Entities.Bike;
import org.opennars.applications.crossing.Encoders.InformQaNar;
import org.opennars.applications.crossing.Encoders.EntityToNarsese;
import com.jsoniter.JsonIterator;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opennars.entity.Task;
import org.opennars.interfaces.Timable;
import org.opennars.language.Term;
import org.opennars.operator.Operation;
import org.opennars.operator.Operator;
import org.opennars.storage.Memory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class VisualReasoner {

    //Detected anomalies and spatial relations
    public final static HashMap<String,Integer> jaywalkers = new HashMap<String,Integer>();
    public final static HashMap<String,Integer> indangers = new HashMap<String,Integer>();
    public final static HashMap<String,Integer> relations = new HashMap<String,Integer>();
    //The MultiNar setup
    public static MultiNarSetup trafficMultiNar = null;
    //The entities detected
    List<Entity> entities = new ArrayList<>();
    //Perception&relation update with related counter
    public static int i = 0;
    int perception_update = 1;
    int relation_update = 5;
    //Movement thresholds for the entity classes
    public static double movementThresholdCar = 30;
    public static double movementThresholdBike = 5;
    public static double movementThresholdPedestrian = 5;
    //The path to the ontology
    public static String customOntologyPath = "";
    //How long anomalies persist
    public static int anomalyRetrieveDuration = 30;
    //Instance to use Redis
    static Jedis r = null;
    //The related queue names for communication
    public static String QTrackletToNar = null;
    public static String QInfoFromNar = null;
    
    public VisualReasoner() {
        trafficMultiNar = new MultiNarSetup(new Say(), entities);    
        try {
            String content = new String(Files.readAllBytes(new File(customOntologyPath).toPath()),Charset.forName("UTF-8"));
            String qapart = content.split(">>QANar:")[1].split(">>LocationNar:")[0].trim();
            String locpart = content.split(">>LocationNar:")[1].split(">>General information:")[0].trim();
            trafficMultiNar.informLocationNar.ontology = locpart;
            trafficMultiNar.informQaNar.ontology = qapart;
        } catch (IOException ex) {
            Logger.getLogger(VisualReasoner.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void cleanupAnomalies(HashMap<String,Integer> hashmap) {
        List<String> cleanups = new ArrayList<String>();
        for(String key : hashmap.keySet()) {
            if(i - hashmap.get(key) > anomalyRetrieveDuration) {
                cleanups.add(key);
            }
        }
        for(String key : cleanups) {
            hashmap.remove(key);
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

    public void step() throws FileNotFoundException, UnsupportedEncodingException, IOException {
        cleanupAnomalies(jaywalkers);
        cleanupAnomalies(indangers);
        cleanupAnomalies(relations);
        entities.clear(); //refresh detections
        AddEntitiesFromTracklets();
        trafficMultiNar.perceiveScene(i, perception_update, relation_update);
        trafficMultiNar.reason();
        List<String> msgs = new ArrayList<>();
        for (Prediction pred : trafficMultiNar.predictions) {
            Entity e = pred.ent;
            long timeDistance = pred.time - trafficMultiNar.predictionNar.time();
            float value = pred.truth.getExpectation();
            String st = "predicted " + pred.type + " " + e.posX + " " + e.posY + " "+ e.angle + " " + value + " " + timeDistance;
            msgs.add(st);
        }
        synchronized(trafficMultiNar.informQaNar.relatedLeft) {
            for(int k=0; k<trafficMultiNar.informQaNar.relatedLeft.size(); k++) {
                Entity left = trafficMultiNar.informQaNar.relatedLeft.get(k);
                Entity right = trafficMultiNar.informQaNar.relatedRight.get(k);
                String st = "related "+EntityToNarsese.name(left) + " " + EntityToNarsese.name(right);
                relations.put(st, i);
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
        MessagesToRedis(msgs);
        msgs.clear();
        i++;
    }

    private void AddEntitiesFromTracklets() {
        /*String tkl_msg = r.blpop(0,QTrackletToNar).get(1);
        //trim everything except the latest
        long new_messages_amount = r.llen(QTrackletToNar);
        if(new_messages_amount > 0) {
            r.ltrim(QTrackletToNar,new_messages_amount-1,-1);
        }*/
        //pop the newest, blocking if empty queue
        String tkl_msg = r.brpop(0,QTrackletToNar).get(1);
        long[][] tracklets = JsonIterator.deserialize(tkl_msg, long[][].class);
        for(long[] props : tracklets) {
            //ClassID TrackID X1 Y1 W1 H1 P1 X2 Y2 W2 H2 P2 X3 Y3 W3 H3 P3 X4 Y4 W4 H4 P4 X5 Y5 W5 H5 P5
            String label = "" + props[1];
            long probability = props[6];
            if(probability == 0) {
                continue;
            }
            long X = props[2];
            long Y = props[3];
            long X2 = props[22]; //7 6
            long Y2 = props[23];
            int angle = 0; //id according to movement direction
            if(X < X2) {
                angle += 10;
            }
            if(Y < Y2) {
                angle += 1;
            }  
            double movement = Math.sqrt((X-X2)*(X-X2) + (Y - Y2)*(Y - Y2));
            if(props[0] == 0) { //person or vehicle for now
                if(movement < (double)movementThresholdPedestrian) {
                    continue;
                }
                Pedestrian toAdd = new Pedestrian(angle, X2, Y2, label);
                entities.add(toAdd);
            } else {
                if(props[0] == 2) {
                    if(movement < (double)movementThresholdCar) {
                        continue;
                    }
                    Car toAdd = new Car(angle, X2, Y2, label);
                    entities.add(toAdd);
                } else {
                    if(movement < (double)movementThresholdBike) {
                        continue;
                    }
                    Bike toAdd = new Bike(angle, X2, Y2, label);
                    entities.add(toAdd);
                }
            }
        }
    }
    
    public void MessagesToRedis(List<String> msgs) {
        String s = "";
        for(String msg : msgs) {
            s += msg + "\n";
        }
        //redis rpush
        r.rpush(QInfoFromNar, s);
        msgs.clear();
    }
    
    public static void main(String[] args) throws FileNotFoundException, IOException {
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
            java.util.logging.Logger.getLogger(VisualReasoner.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>
        System.out.println("args: discretization movementThresholdCar movementThresholdPedestrian movementThresholdBike veryClosenessThreshold OntologyNalFile AnomalyRetrieveDuration redisHost redisPort redisPassword QTrackletToNar QInfoFromNar");
        System.out.println("example: java -cp \"*\" org.opennars.applications.crossing.RealCrossing 80 30 5 5 169 /home/tc/Dateien/CROSSING/StreetScene/AnomalyOntology.nal 30 locahost 6379 pwd Q_Tracklet_To_Nar Q_Info_From_Nar");
        Util.discretization = Integer.valueOf(args[0]);
        VisualReasoner.movementThresholdCar = Integer.valueOf(args[1]); 
        VisualReasoner.movementThresholdPedestrian = Integer.valueOf(args[2]); 
        VisualReasoner.movementThresholdBike = Integer.valueOf(args[3]); 
        InformQaNar.veryClosenessThreshold = Integer.valueOf(args[4]);
        customOntologyPath = args[5];
        anomalyRetrieveDuration = Integer.valueOf(args[6]);
        String redishost = args[7];
        int redisport = Integer.valueOf(args[8]);
        String redispwd = args[9];
        QTrackletToNar = args[10];
        QInfoFromNar = args[11];
        JedisPool pool = new JedisPool(redishost, redisport);
        r = pool.getResource();
        if(!redispwd.isEmpty()) {
            try {
                r.auth(redispwd);
                r.connect();
            } catch(Exception ex) {
                System.out.println("Invalid password " + ex.toString());
                return;
            }
        }
        VisualReasoner mp = new VisualReasoner();
        while(true) {
            mp.step();
        }
    }
}
