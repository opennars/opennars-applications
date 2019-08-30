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

import org.opennars.applications.crossing.Entities.Car;
import org.opennars.applications.crossing.Entities.Entity;
import org.opennars.applications.crossing.Entities.Pedestrian;
import java.util.List;
import org.opennars.applications.Util;
import org.opennars.entity.Sentence;
import org.opennars.entity.Stamp;
import org.opennars.entity.Task;
import org.opennars.entity.TruthValue;
import org.opennars.io.Symbols;
import org.opennars.io.events.EventEmitter;
import org.opennars.io.events.Events;
import org.opennars.io.events.OutputHandler;
import org.opennars.language.Inheritance;
import org.opennars.language.Product;
import org.opennars.language.Term;
import org.opennars.main.Nar;

public class NarListener implements EventEmitter.EventObserver {
    public class Prediction
    {
        public Entity ent;
        public long time;
        public TruthValue truth;
        public String type;
        public Prediction(Entity ent, TruthValue truth, long time, String type) {
            this.ent = ent;
            this.time = time;
            this.truth = truth;
            this.type = type;
        }
    }

    List<Entity> entities;

    List<NarListener.Prediction> predictions;
    List<NarListener.Prediction> disappointments;
    Nar nar;
    Camera camera;
    public NarListener(Camera camera, Nar nar, List<NarListener.Prediction> predictions, List<NarListener.Prediction> disappointments, List<Entity> entities) {
        this.predictions = predictions;
        this.disappointments = disappointments;
        this.nar = nar;
        this.camera = camera;
        this.entities = entities;
    }
    Term pedestrian = Term.get("pedestrian");
    Term car = Term.get("car");
    Term at = Term.get("at");
    @Override
    public void event(Class event, Object[] args) {
        if(event == OutputHandler.DISAPPOINT.class) {
            Term term = (Term) args[0];
            Sentence s = new Sentence(term, Symbols.JUDGMENT_MARK, new TruthValue(0.0f,0.9f, nar.narParameters), new Stamp(nar, nar.memory));
            Task t = new Task(s, null, Task.EnumType.DERIVED);
            Prediction result = predictionFromTask(t);
            if(result != null) {
                int showFor = 100; //how long the disappointment should be displayed in the GUI
                result.time = nar.time() + showFor;
                disappointments.add(result);
            }
        }
        if (event == Events.TaskAdd.class) {
            Task t = (Task) args[0];
            if(t.sentence.isJudgment() && t.sentence.truth.getExpectation() > nar.narParameters.DEFAULT_CONFIRMATION_EXPECTATION && t.sentence.getOccurenceTime()-nar.time() < 200 &&
                    t.sentence.getOccurenceTime() >= nar.time()) {
                if (t.sentence.getOccurenceTime() >= nar.time() && t.sentence.isJudgment()) {
                    Prediction result = predictionFromTask(t);
                    if(result != null) {
                        predictions.add(result);

                        broadcastPrediction(result);
                    }
                }
            }
        }
    }

    public Prediction predictionFromTask(Task t) {
        Prediction prediction = null;
        //format: "<(*,car,50_82) --> at>. %0.45;0.26%";
        if(t.sentence.term instanceof Inheritance) {
            Inheritance positionInh = (Inheritance) t.sentence.term;
            if(positionInh.getSubject() instanceof Product) {
                Product prod = (Product) positionInh.getSubject();
                if(prod.size() == 2) {
                    Term type = prod.term[0];
                    String position = prod.term[1].toString();
                    if(position.contains("_")) {
                        try {
                            int posX = camera.minX + Crossing.discretization * Integer.valueOf(position.split("_")[0]);
                            int posY = camera.minY + Crossing.discretization * Integer.valueOf(position.split("_")[1]);
                            //int id = 0; //Integer.valueOf(idStr.toString()); often a dep var
                            Entity pred;
                            if(type.toString().startsWith(car.toString())) {
                                String id = type.toString().substring(car.toString().length(), type.toString().length());
                                pred = new Car(Integer.valueOf(id), posX, posY, 0, 0);
                                pred.isPredicted = true;
                                prediction = new Prediction(pred, t.sentence.truth, t.sentence.getOccurenceTime(), "car");
                            }
                            else  
                            if(type.toString().startsWith(pedestrian.toString())) {
                                String id = type.toString().substring(pedestrian.toString().length(), type.toString().length());
                                pred = new Pedestrian(Integer.valueOf(id), posX, posY, 0, 0);
                                pred.isPredicted = true;
                                prediction = new Prediction(pred, t.sentence.truth, t.sentence.getOccurenceTime(), "pedestrian");
                            }
                        } catch(Exception ex) {} //wrong format, it's not such a type of prediction but something else
                    }
                }
            }
        }
        return prediction;
    }

    public void broadcastPrediction(Prediction prediction) {
        for (final Entity iEntity: entities) {
            if (prediction.ent.id != iEntity.id) {
                continue;
            }

            // distance of prediction
            final double dist = Util.distance(iEntity.posX, iEntity.posY, prediction.ent.posX, prediction.ent.posY);
            if (dist > 17) {
                continue;
            }

            iEntity.normalness += 0.03;

            // we found one - done
            return;
        }
    }
}