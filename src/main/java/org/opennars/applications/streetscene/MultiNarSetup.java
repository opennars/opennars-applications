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
package org.opennars.applications.streetscene;

import org.opennars.applications.streetscene.Entities.Entity;
import org.opennars.applications.streetscene.Encoders.InformPredictionNar;
import org.opennars.applications.streetscene.Encoders.InformQaNar;
import org.opennars.applications.streetscene.Encoders.InformLocationNar;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opennars.io.events.Events;
import org.opennars.io.events.OutputHandler;
import org.opennars.main.Nar;
import org.opennars.operator.Operator;

/**
 *
 * @author tc
 */
public class MultiNarSetup {
    
    //prediction nar
    public Nar predictionNar;
    public List<Prediction> predictions = new ArrayList<>();
    public List<Prediction> disappointments = new ArrayList<>();
    //nar dedicated to user questions
    public Nar qanar;
    //nar dedicated to labelling locations
    public Nar locationNar;
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
    
    public MultiNarSetup(Operator infoOp, List<Entity> entities) {
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
            predictionNar.narParameters.DURATION*=10;
            NarListener listener = new NarListener(predictionNar, predictions, disappointments);
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
                    Logger.getLogger(MultiNarSetup.class.getName()).log(Level.SEVERE, null, ex);
                }
                lock.unlock();
                informQaNar.inform(qanar, entities, informLocationNar.locationToLabel);
            }
        }
    }

    public void perceiveScene(int t, int perceptionUpdate, int questionUpdate, int relationUpdate) {
        if(t % relationUpdate == 0) {
            //System.out.println("TICK spatial");
            lock.lock();
            doWork.signal();
            lock.unlock();
        }
        if (t % perceptionUpdate == 0) {
            this.informLocationNar.askForLabels(locationNar, t, perceptionUpdate, questionUpdate, entities);
            informer.informAboutEntities(predictionNar, entities);
            informer.Input(predictionNar);
        }
    }
    
    public static int successes = 0;
    public static int failures = 0;
    public void removeOutdatedPredictions(List<Prediction> predictions) {
        List<Prediction> toDelete = new ArrayList<Prediction>();
        for(Prediction pred : predictions) {
            if(pred.time <= predictionNar.time()) {
                toDelete.add(pred);
                if(!pred.confirmed) {
                    failures++;
                }
            }
        }
        predictions.removeAll(toDelete);
    }
}
