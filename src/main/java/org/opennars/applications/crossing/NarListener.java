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

import java.util.List;
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

    Term pedestrian = Term.get("pedestrian");
    Term bike = Term.get("bike");
    Term car = Term.get("car");
    Term at = Term.get("at");
    List<Entity> entities;
    List<Prediction> predictions;
    List<Prediction> disappointments;
    Nar nar;
    
    public NarListener(Nar nar, List<Prediction> predictions, List<Prediction> disappointments, List<Entity> entities) {
        this.predictions = predictions;
        this.disappointments = disappointments;
        this.nar = nar;
        this.entities = entities;
    }
    
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
                            int posX = Util.discretization * Integer.valueOf(position.split("_")[0]);
                            int posY = Util.discretization * Integer.valueOf(position.split("_")[1]);
                            if(type.toString().startsWith(car.toString())) {
                                String id = type.toString().substring(car.toString().length(), type.toString().length());
                                Entity pred = new Car(Integer.valueOf(id), posX, posY, "");
                                prediction = new Prediction(pred, t.sentence.truth, t.sentence.getOccurenceTime(), "car");
                            }
                            else  
                            if(type.toString().startsWith(pedestrian.toString())) {
                                String id = type.toString().substring(pedestrian.toString().length(), type.toString().length());
                                Entity pred = new Pedestrian(Integer.valueOf(id), posX, posY, "");
                                prediction = new Prediction(pred, t.sentence.truth, t.sentence.getOccurenceTime(), "pedestrian");
                            }
                            else {
                                String id = type.toString().substring(bike.toString().length(), type.toString().length());
                                Entity pred = new Bike(Integer.valueOf(id), posX, posY, "");
                                prediction = new Prediction(pred, t.sentence.truth, t.sentence.getOccurenceTime(), "bike");
                            } 
                        } catch(Exception ex) {} //wrong format, it's not such a type of prediction but something else
                    }
                }
            }
        }
        return prediction;
    }
}