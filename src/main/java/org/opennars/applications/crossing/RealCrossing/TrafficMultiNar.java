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
import java.util.List;
import org.opennars.applications.crossing.Camera;
import org.opennars.applications.crossing.Entity;
import org.opennars.applications.crossing.NarListener;
import org.opennars.applications.crossing.TrafficLight;
import org.opennars.entity.TruthValue;
import org.opennars.io.events.Events;
import org.opennars.io.events.OutputHandler;
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
    
    public InformQaNar informQaNar = new InformQaNar();
    public InformLocationNar informLocationNar = new InformLocationNar();
    
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
                    informQaNar.inform(qanar, entities, informLocationNar.locationToLabel);
                }
            }
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
            this.informLocationNar.askForLabels(t, perception_update, entities);
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
