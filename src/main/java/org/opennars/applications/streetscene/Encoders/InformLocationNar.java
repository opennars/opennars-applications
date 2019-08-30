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
package org.opennars.applications.streetscene.Encoders;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.opennars.applications.streetscene.Entities.Car;
import org.opennars.applications.streetscene.Entities.Entity;
import org.opennars.applications.streetscene.Entities.Pedestrian;
import org.opennars.applications.streetscene.VisualReasonerWithGUI;
import org.opennars.applications.Util;
import org.opennars.applications.streetscene.VisualReasonerHeadless;
import org.opennars.entity.Sentence;
import org.opennars.entity.TruthValue;
import org.opennars.io.Parser;
import org.opennars.io.events.AnswerHandler;
import org.opennars.language.Inheritance;
import org.opennars.main.Nar;

public class InformLocationNar {

    //user-given background knowledge
    public String ontology = "";
    //Location labels estimated by reasoning
    public final Map<String,MapEvidence> locationToLabel = new HashMap<String,MapEvidence>();
    //Angles on locations estimated by reasoning
    public final Map<String,MapEvidence> locationToCarAngle = new HashMap<String,MapEvidence>();
    
    void inform(Nar locationNar, List<Entity> entities) {
        List<Entity> sortedEntX = null;
        synchronized(entities) {
            sortedEntX = entities.stream().sorted(Comparator.comparing(Entity::getPosX)).collect(Collectors.toList());
        }
        for(Entity ent : sortedEntX) {
            String typeInfo = EntityToNarsese.informType(ent);
            //also give info about position at labelled locations
            String position = Util.positionToTerm((int)ent.posX,(int)ent.posY, VisualReasonerHeadless.discretization);
            locationNar.addInput("(&|,<(*,"+EntityToNarsese.name(ent)+","+position+") --> at>,"+typeInfo+"). :|:");
            String locationNarInput = "<" + EntityToNarsese.name(ent) + " --> [aligned]>. :|:";
            synchronized(locationToCarAngle) {
                if (ent instanceof Pedestrian && locationToCarAngle.containsKey(position)) {
                    String carAngle = locationToCarAngle.get(position).choice();
                    boolean orthogonal = (ent.angle == 0 && "10".equals(carAngle)
                            || ent.angle == 10 && "0".equals(carAngle)
                            || ent.angle == 0 && "1".equals(carAngle)
                            || ent.angle == 1 && "0".equals(carAngle)
                            || ent.angle == 1 && "11".equals(carAngle)
                            || ent.angle == 11 && "1".equals(carAngle)
                            || ent.angle == 10 && "11".equals(carAngle)
                            || ent.angle == 11 && "10".equals(carAngle));
                    if (carAngle != null && orthogonal) {
                        locationNarInput = "<" + EntityToNarsese.name(ent) + " --> [crossing]>. :|:";
                    }
                }
            }
            locationNar.addInput(locationNarInput);
            synchronized(locationToCarAngle) {
                if(ent instanceof Car) {
                    String[] labels = new String[] {"0","1","11","10"};
                    if(!locationToCarAngle.containsKey(position)) {
                        locationToCarAngle.put(position, new MapEvidence(locationNar, labels));
                    }
                    locationToCarAngle.get(position).collect(locationNar, String.valueOf(ent.angle), new TruthValue(1.0f, 0.9f, locationNar.narParameters));
                }
            }
        }
    }
    
    public void askForLabels(Nar locationNar, int t, int perception_update, int question_update, List<Entity> entities) {
        if(t > 0 && t % (question_update) == 0) {
            locationNar.reset();
            inform(locationNar, entities); //input locations
            try {
                String[] labels = new String[] {"sidewalk","street","bikelane","crosswalk"};
                for(String type : labels) {
                    locationNar.askNow("<?what --> ["+type+"]>", new AnswerHandler() {
                        @Override
                        public void onSolution(Sentence belief) {
                            //eternal or outdated
                            if(belief.isEternal() || locationNar.time()-belief.getOccurenceTime() > 100000) {
                                return;
                            }
                            String subj = ((Inheritance) belief.getTerm()).getSubject().toString();
                            synchronized(locationToLabel) {
                                if(subj.contains("_")) {
                                    if(!locationToLabel.containsKey(subj)) {
                                        locationToLabel.put(subj, new MapEvidence(locationNar, labels));
                                    }
                                    locationToLabel.get(subj).collect(locationNar, type, belief.truth);
                                }
                            }
                        }
                    });
                }
            } catch (Parser.InvalidInputException ex) {
                Logger.getLogger(VisualReasonerWithGUI.class.getName()).log(Level.SEVERE, null, ex);
            }
            locationNar.addInput(ontology);
        }
        if(t > 0 && t % (1*perception_update) == 0) {
            locationNar.cycles(300);
        }
    }
}
