/*
 * The MIT License
 *
 * Copyright 2019 OpenNARS.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opennars.applications.crossing.Camera;
import org.opennars.applications.crossing.Entity;
import org.opennars.applications.crossing.NarListener;
import org.opennars.applications.crossing.TrafficLight;
import org.opennars.entity.Sentence;
import org.opennars.entity.TruthValue;
import org.opennars.inference.TruthFunctions;
import org.opennars.io.Parser;
import org.opennars.io.events.AnswerHandler;
import org.opennars.io.events.Events;
import org.opennars.io.events.OutputHandler;
import org.opennars.language.Inheritance;
import org.opennars.main.Nar;
import org.opennars.operator.Operator;

/**
 *
 * @author tc
 */
public class TrafficMultiNar {
    
    //prediction nar
    public Nar nar;
    public List<NarListener.Prediction> predictions = new ArrayList<>();
    public List<NarListener.Prediction> disappointments = new ArrayList<>();
    //nar dedicated to user questions
    public Nar qanar;
    //nar dedicated to labelling locations
    static Nar locationNar;
      
    //saving away parameters
    public int SEQUENCE_BAG_ATTEMPTS = 0;
    
    //objects the traffic multinar needs to be aware of
    List<Entity> entities;
    Camera camera;
    
    public InformNARS informNARS = new InformNARS();
    
    public void reason() {
        qanar.cycles(10); //for now its a seperate nar but can be merged potentially
                    //but this way we make sure predictions don't get worse when
                    //questions are given and vice versa
        nar.cycles(10); //only doing prediction so no need
        removeOutdatedPredictions(predictions);
        removeOutdatedPredictions(disappointments);
    }
    
    
    public TrafficMultiNar(Operator infoOp, List<Entity> entities, Camera camera) {
        this.entities = entities;
        this.camera = camera;
        QANarThread qaThread = new QANarThread();
        qaThread.start();
        try {
            qanar = new Nar();
            qanar.addPlugin(infoOp);
            qanar.narParameters.VOLUME = 0;
            locationNar = new Nar();
            nar = new Nar();
            
            SEQUENCE_BAG_ATTEMPTS = nar.narParameters.SEQUENCE_BAG_ATTEMPTS;
            locationNar.narParameters.SEQUENCE_BAG_ATTEMPTS=0;
            //nar.narParameters.SEQUENCE_BAG_ATTEMPTS=0;
            //qanar.narParameters.SEQUENCE_BAG_ATTEMPTS *= 2;
            
            nar.narParameters.VOLUME = 0;
            nar.narParameters.DURATION*=10;
            NarListener listener = new NarListener(camera, nar, predictions, disappointments, entities);
            nar.on(Events.TaskAdd.class, listener);
            nar.on(OutputHandler.DISAPPOINT.class, listener);
        } catch (Exception ex) {
            System.out.println(ex);
            System.exit(1);
        }
        
    }
    
    public Boolean QAWork = false; 
    public class QANarThread extends Thread {

        public void run(){
            while(true) {
                boolean DoWork = false;
                synchronized(QAWork) {
                    if(QAWork) {
                        QAWork = false;
                        DoWork = true;
                    }
                }
                if(DoWork) {
                    informNARS.informNARSForQA(false, qanar, entities, locationToLabel);
                }
            }
        }
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
    public void askForSemanticLabel(int t, int perception_update) {
        if(t > 0 && t % (5*perception_update) == 0) {
            locationNar.reset();
            informNARS.informNARSForQA(true, qanar, entities, locationToLabel); //input locations
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
    
    public void perceiveScene(int t, int perception_update) {
        if(t > 0 && t % (5*perception_update) == 0) {
            System.out.println("TICK spatial");
            synchronized(QAWork) {
                QAWork = true;
            }
        }

        if (t % perception_update == 0) {
            askForSemanticLabel(t, perception_update);
            camera.see(nar, entities, new ArrayList<TrafficLight>(), false);
        }
    }
    
    public void removeOutdatedPredictions(List<NarListener.Prediction> predictions) {
        List<NarListener.Prediction> toDelete = new ArrayList<NarListener.Prediction>();
        for(NarListener.Prediction pred : predictions) {
            if(pred.time <= nar.time()) {
                toDelete.add(pred);
            }
        }
        predictions.removeAll(toDelete);
    }
}
