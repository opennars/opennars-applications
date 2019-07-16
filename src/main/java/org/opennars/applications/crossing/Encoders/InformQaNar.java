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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import org.opennars.applications.crossing.Entities.Car;
import org.opennars.applications.crossing.Entities.Entity;
import org.opennars.applications.crossing.Entities.Pedestrian;
import static org.opennars.applications.crossing.Encoders.EntityToNarsese.informType;
import static org.opennars.applications.crossing.Encoders.EntityToNarsese.name;
import org.opennars.applications.crossing.Util;
import static org.opennars.applications.crossing.VisualReasoner.fastThreshold;
import org.opennars.main.Nar;

public class InformQaNar {
    
    //knowledge, user questions, motivations
    public String ontology = "";
    //how close things must be to trigger closeTo relation (2x times it will trigger leftOf, aboveOf)
    public static double veryClosenessThreshold = 169;
    //Batches of information, take_k pieces randomly sampled if too much, misses may be catched up in the next frame
    Random rnd = new Random(1337);
    ArrayList<ArrayList<String>> QAinformation = new ArrayList<>();
    int take_k = 8;
    //just to visualize the entities that have been spatially related
    public final List<Entity> relatedLeft = new ArrayList<>(); 
    public final List<Entity> relatedRight = new ArrayList<>();
    //Whether leftOf and aboveOf is enabled
    boolean enable_leftOf_aboveOf = false;
    //Whether pedestrians are also related to each other
    boolean relate_pedestrians = false;

    private boolean near(Entity a, Entity b) {
        if(Math.sqrt(Math.pow(a.posX - b.posX, 2)+Math.pow(a.posY - b.posY, 2)) < veryClosenessThreshold*2) {
            return true;
        }
        return false;
    }
    
    private boolean veryClose(Entity a, Entity b) {
        if(Math.sqrt(Math.pow(a.posX - b.posX, 2)+Math.pow(a.posY - b.posY, 2)) < veryClosenessThreshold) {
            return true;
        }
        return false;
    }
    
    public void inform(Nar qanar, List<Entity> entities, Map<String,MapEvidence> locationToLabel) {
        QAinformation.clear();
        synchronized(relatedLeft) {
            relatedLeft.clear();
            relatedRight.clear();
        }
        qanar.reset();
        //inform NARS about the spatial relationships between objects and which categories they belong to according to the Tracker
        List<Entity> sortedEntX = entities.stream().sorted(Comparator.comparing(Entity::getPosX)).collect(Collectors.toList());
        for(Entity ent : sortedEntX) {
            for(Entity entity : entities) {
                if(ent != entity && near(ent, entity)) {
                    ArrayList<String> QAInfo = new ArrayList<String>();
                    if(relate_pedestrians || !(entity instanceof Pedestrian && ent instanceof Pedestrian)) {
                        if(ent.posX < entity.posX) {
                            if(enable_leftOf_aboveOf) {
                                QAInfo.add("<(*," + name(ent) + "," + name(entity) + ") --> leftOf>. :|:");
                            }
                            QAInfo.add("(&|," + informType(ent) + "," + informType(entity)+"). :|:");
                            if(veryClose(ent, entity)) {
                                if(ent instanceof Car) {
                                    if(((Car) ent).speed > fastThreshold) {
                                        QAInfo.add("(&|,<(*," + name(entity) + "," + name(ent) + ") --> closeTo>,"+"<"+name(ent) + " --> [fast]>). :|:");
                                    } else {
                                        QAInfo.add("<(*," + name(entity) + "," + name(ent) + ") --> closeTo>. :|:");
                                    }
                                } else {
                                    QAInfo.add("<(*," + name(ent) + "," + name(entity) + ") --> closeTo>. :|:");
                                }
                            }
                            QAinformation.add(QAInfo);
                            synchronized(relatedLeft) {
                                relatedLeft.add(ent);
                                relatedRight.add(entity);
                            }
                        }
                        if(ent.posY < entity.posY) {
                            if(enable_leftOf_aboveOf) {
                                QAInfo.add("<(*," + name(ent) + "," + name(entity) + ") --> aboveOf>. :|:");
                            }
                            QAInfo.add("(&|," + informType(ent) + "," + informType(entity)+"). :|:");
                            if(veryClose(ent, entity)) {
                                if(ent instanceof Car) {
                                    if(((Car) ent).speed > fastThreshold) {
                                        QAInfo.add("(&|,<(*," + name(entity) + "," + name(ent) + ") --> closeTo>,"+"<"+name(ent) + " --> [fast]>). :|:");
                                    } else {
                                        QAInfo.add("<(*," + name(entity) + "," + name(ent) + ") --> closeTo>. :|:");
                                    }
                                } else {
                                    QAInfo.add("<(*," + name(ent) + "," + name(entity) + ") --> closeTo>. :|:");
                                }
                            }
                            QAinformation.add(QAInfo);
                            synchronized(relatedLeft) {
                                relatedLeft.add(ent);
                                relatedRight.add(entity);
                            }
                        }
                    }
                }
            }
            String typeInfo = informType(ent)+". :|:";
            ArrayList<String> info = new ArrayList<String>();
            info.add(typeInfo);
            //also give info about position at labelled locations
            int X = (int) (ent.posX / Util.discretization);
            int Y = (int) (ent.posY / Util.discretization);
            String position = X + "_" + Y;
            if(locationToLabel.containsKey(position)) {
                String label = locationToLabel.get(position).choice();
                if(label != null) {
                    System.out.println("QA INFO: <(*,"+name(ent)+","+label+") --> at>. :|:");
                    ArrayList<String> Atinfo = new ArrayList<>();
                    Atinfo.add(typeInfo);
                    Atinfo.add("<(*,"+name(ent)+","+locationToLabel.get(position).choice()+") --> at>. :|:");
                    QAinformation.add(Atinfo);
                }
            }
        }
        Collections.shuffle(QAinformation, rnd);
        int k = 0;
        for(ArrayList<String> info : QAinformation) {
            for(String s : info) {
                System.out.println(s);
                qanar.addInput(s);
                k++;
                if(k >= take_k) {
                    break;
                }
            }
            if(k >= take_k) {
                break;
            }
        }
        inputOntology(qanar);
    }
    
    private void inputOntology(Nar qanar) {
        if(!"".equals(ontology)) {
            qanar.addInput(ontology);
        }
    }
}
