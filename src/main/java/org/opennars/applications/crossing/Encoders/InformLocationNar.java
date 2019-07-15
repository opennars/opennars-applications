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
package org.opennars.applications.crossing.Encoders;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.opennars.applications.crossing.Entities.Entity;
import org.opennars.applications.crossing.VisualReasoner;
import org.opennars.applications.crossing.Util;
import org.opennars.entity.Sentence;
import org.opennars.entity.TruthValue;
import org.opennars.inference.TruthFunctions;
import org.opennars.io.Parser;
import org.opennars.io.events.AnswerHandler;
import org.opennars.language.Inheritance;
import org.opennars.main.Nar;

public class InformLocationNar {

    //user-given background knowledge
    public String ontology = "";
    //Location labels estimated by reasoning
    public Map<String,MapEvidence> locationToLabel = new HashMap<String,MapEvidence>();
    
    void inform(Nar locationNar, List<Entity> entities) {
        List<Entity> sortedEntX = entities.stream().sorted(Comparator.comparing(Entity::getPosX)).collect(Collectors.toList());
        for(Entity ent : sortedEntX) {
            String typeInfo = EntityToNarsese.informType(ent)+". :|:";
            ArrayList<String> info = new ArrayList<String>();
            info.add(typeInfo);
            locationNar.addInput(typeInfo);
            //also give info about position at labelled locations
            String locationnarInput = "<(*,"+EntityToNarsese.name(ent)+","+Util.positionToTerm((int)ent.posX,(int)ent.posY)+") --> at>. :|:";
            locationNar.addInput(locationnarInput);
            System.out.println("location nar input: " + locationnarInput);
        }
    }
    
    public void askForLabels(Nar locationNar, int t, int perception_update, List<Entity> entities) {
        if(t > 0 && t % (5*perception_update) == 0) {
            locationNar.reset();
            inform(locationNar, entities); //input locations
            try {
                for(String type : new String[] {"street","sidewalk","bikelane","crosswalk"}) {
                    locationNar.askNow("<?what --> ["+type+"]>", new AnswerHandler() {
                        @Override
                        public void onSolution(Sentence belief) {
                            //eternal or outdated
                            if(belief.isEternal() || locationNar.time()-belief.getOccurenceTime() > 100000) {
                                return;
                            }
                            String subj = ((Inheritance) belief.getTerm()).getSubject().toString();
                            if(subj.contains("_")) {
                                if(!locationToLabel.containsKey(subj)) {
                                    locationToLabel.put(subj, new MapEvidence(locationNar));
                                }
                                MapEvidence mapval = locationToLabel.get(subj);
                                mapval.collect(locationNar, type, belief.truth);
                            }
                        }
                    });
                }
            } catch (Parser.InvalidInputException ex) {
                Logger.getLogger(VisualReasoner.class.getName()).log(Level.SEVERE, null, ex);
            }
            locationNar.addInput(ontology);
        }
        if(t > 0 && t % (1*perception_update) == 0) {
            locationNar.cycles(300);
        }
    }
}
