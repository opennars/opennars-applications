package com.opennars.applications.pong;

import com.opennars.applications.componentbased.Entity;
import com.opennars.applications.crossing.Util;
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

public class NarListener implements EventEmitter.EventObserver {

    List<Entity> entities;

    List<Prediction> predictions;
    List<Prediction> disappointments;
    Reasoner reasoner;
    public NarListener(Reasoner reasoner, List<Prediction> predictions, List<Prediction> disappointments, List<Entity> entities) {
        this.predictions = predictions;
        this.disappointments = disappointments;
        this.reasoner = reasoner;
        this.entities = entities;
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
                            final int unmappedX = Integer.valueOf(position.split("_")[0];
                            final int unmappedY = Integer.valueOf(position.split("_")[0];

                            int posX = unmappedX;
                            int posY = unmappedY;
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