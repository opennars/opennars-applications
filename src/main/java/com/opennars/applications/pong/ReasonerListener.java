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
package com.opennars.applications.pong;

import com.opennars.applications.common.Vec2Int;
import com.opennars.applications.componentbased.Entity;
import com.opennars.applications.crossing.Util;
import com.opennars.applications.pong.components.BallRenderComponent;
import org.opennars.entity.Sentence;
import org.opennars.entity.Stamp;
import org.opennars.entity.Task;
import org.opennars.entity.TruthValue;
import org.opennars.interfaces.pub.Reasoner;
import org.opennars.io.Symbols;
import org.opennars.io.events.EventEmitter;
import org.opennars.io.events.Events;
import org.opennars.io.events.OutputHandler;
import org.opennars.language.Inheritance;
import org.opennars.language.Product;
import org.opennars.language.Term;
import org.opennars.main.Nar;

import java.util.List;

public class ReasonerListener implements EventEmitter.EventObserver {

    public GridMapper mapper;
    List<Entity> entities;

    List<Prediction> predictions;
    List<Prediction> disappointments;
    Reasoner reasoner;
    public ReasonerListener(Reasoner reasoner, List<Prediction> predictions, List<Prediction> disappointments, List<Entity> entities, GridMapper mapper) {
        this.predictions = predictions;
        this.disappointments = disappointments;
        this.reasoner = reasoner;
        this.entities = entities;
        this.mapper = mapper;
    }

    Term pedestrian = Term.get("pedestrian");
    Term car = Term.get("car");
    Term at = Term.get("at");
    @Override
    public void event(Class event, Object[] args) {
        if(event == OutputHandler.DISAPPOINT.class) {
            Term term = (Term) args[0];
            Sentence s = new Sentence(term, Symbols.JUDGMENT_MARK, new TruthValue(0.0f,0.9f, ((Nar)reasoner).narParameters), new Stamp(reasoner, ((Nar)reasoner).memory));
            Task t = new Task(s, null, Task.EnumType.DERIVED);
            Prediction result = predictionFromTask(t);
            if(result != null) {
                int showFor = 100; //how long the disappointment should be displayed in the GUI
                result.time = reasoner.time() + showFor;
                disappointments.add(result);
            }
        }
        if (event == Events.TaskAdd.class) {
            Task t = (Task) args[0];
            if (/*t.sentence.getOccurenceTime() > reasoner.time() && */t.sentence.isJudgment() && t.sentence.getTruth().getExpectation() >= ((Nar)reasoner).narParameters.DEFAULT_CONFIRMATION_EXPECTATION) {
                Prediction result = predictionFromTask(t);
                if(result != null) {
                    predictions.add(result);

                    broadcastPrediction(result);
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
                            final String tag = "ball"; // HACK

                            String id = type.toString().substring(car.toString().length(), type.toString().length());
                            id = "0"; // HACK

                            final Vec2Int mappedPosition = mapper.mapStringToPosition(position);

                            // add prediction
                            Entity predictedEntity = new Entity(Integer.valueOf(id), mappedPosition.x, mappedPosition.y, 0, 0, tag);
                            predictedEntity.isPredicted = true;

                            BallRenderComponent ballRenderComponent = new BallRenderComponent();
                            predictedEntity.renderable = ballRenderComponent;

                            prediction = new Prediction(predictedEntity, t.sentence.truth, t.sentence.getOccurenceTime(), tag);
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