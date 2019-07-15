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
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opennars.applications.crossing.Entity;
import org.opennars.applications.crossing.InformPredictionNar;
import org.opennars.applications.crossing.NarListener;
import org.opennars.applications.crossing.Prediction;
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
    public Nar predictionNar;
    public List<Prediction> predictions = new ArrayList<>();
    public List<Prediction> disappointments = new ArrayList<>();
    //nar dedicated to user questions
    public Nar qanar;
    //nar dedicated to labelling locations
    public static Nar locationNar;
    //objects the traffic multinar needs to be aware of
    List<Entity> entities;
    //Encoder classes mapping entities to Narsese for the Nar instances
    public InformQaNar informQaNar = new InformQaNar();
    public InformLocationNar informLocationNar = new InformLocationNar();
    public InformPredictionNar informer = new InformPredictionNar();
    //Lock&Condition for QANar thread
    final Lock lock = new ReentrantLock();
    final Condition doWork  = lock.newCondition(); 
    
    public void reason() {
        qanar.cycles(10);
        predictionNar.cycles(10);
        removeOutdatedPredictions(predictions);
        removeOutdatedPredictions(disappointments);
    }
    
    public TrafficMultiNar(Operator infoOp, List<Entity> entities) {
        this.entities = entities;
        QANarThread qaThread = new QANarThread();
        qaThread.start();
        try {
            qanar = new Nar();
            qanar.addPlugin(infoOp);
            qanar.narParameters.VOLUME = 0;
            locationNar = new Nar();
            locationNar.narParameters.SEQUENCE_BAG_ATTEMPTS=0;
            predictionNar = new Nar();
            NarListener listener = new NarListener(predictionNar, predictions, disappointments, entities);
            predictionNar.on(Events.TaskAdd.class, listener);
            predictionNar.on(OutputHandler.DISAPPOINT.class, listener);
        } catch (Exception ex) {
            System.out.println(ex);
            System.exit(1);
        }
    }
    
    public class QANarThread extends Thread {
        public void run(){
            while(true) {
                lock.lock();
                try {
                    doWork.await();
                } catch (InterruptedException ex) {
                    Logger.getLogger(TrafficMultiNar.class.getName()).log(Level.SEVERE, null, ex);
                }
                lock.unlock();
                informQaNar.inform(qanar, entities, informLocationNar.locationToLabel);
            }
        }
    }

    public void perceiveScene(int t, int perception_update, int relation_update) {
        if(t % relation_update == 0) {
            System.out.println("TICK spatial");
            lock.lock();
            doWork.signal();
            lock.unlock();
        }
        if (t % perception_update == 0) {
            this.informLocationNar.askForLabels(t, perception_update, entities);
            for (Entity ent : entities) {
                informer.informAboutEntity(predictionNar, ent);
            }
            informer.Input(predictionNar);
        }
    }
    
    public void removeOutdatedPredictions(List<Prediction> predictions) {
        List<Prediction> toDelete = new ArrayList<Prediction>();
        for(Prediction pred : predictions) {
            if(pred.time <= predictionNar.time()) {
                toDelete.add(pred);
            }
        }
        predictions.removeAll(toDelete);
    }
}
