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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import org.opennars.applications.crossing.Car;
import org.opennars.applications.crossing.Entity;
import org.opennars.applications.crossing.Pedestrian;
import static org.opennars.applications.crossing.RealCrossing.EntityToNarsese.informType;
import static org.opennars.applications.crossing.RealCrossing.EntityToNarsese.name;
import org.opennars.applications.crossing.Util;
import org.opennars.main.Nar;

/**
 *
 * @author tc
 */
public class InformQaNar {
    
    //knowledge, user questions, motivations
    public String ontology = ""; //currently loaded from operator panel
    
    //allow relative location relations
    public boolean RELATIVE_LOCATION_RELATIONS = true;
    

    public double nearnessThreshold = 399; //3 times the discretization + 1 tolerance for the cell width
    private boolean near(Entity a, Entity b) {
        if(Math.sqrt(Math.pow(a.posX - b.posX, 2)+Math.pow(a.posY - b.posY, 2)) < nearnessThreshold) {
            return true;
        }
        return false;
    }
    
    public double veryClosenessThreshold = 169; //1 times the discretization + 1 tolerance for the cell width
    private boolean veryClose(Entity a, Entity b) {
        if(Math.sqrt(Math.pow(a.posX - b.posX, 2)+Math.pow(a.posY - b.posY, 2)) < veryClosenessThreshold) {
            return true;
        }
        return false;
    }
    
    ArrayList<ArrayList<String>> QAinformation = new ArrayList<>();
    List<Entity> relatedLeft = new ArrayList<>(); //just to visualize the entities that have been spatially related
    List<Entity> relatedRight = new ArrayList<>(); //just to visualize the entities that have been spatially related
    Random rnd = new Random(1337);
    public void inform(Nar qanar, List<Entity> entities, Map<String,MapEvidence> locationToLabel) {
        QAinformation.clear();
        relatedLeft.clear();
        relatedRight.clear();
        qanar.reset();
        //inform NARS about the spatial relationships between objects and which categories they belong to according to the Tracker
        List<Entity> sortedEntX = entities.stream().sorted(Comparator.comparing(Entity::getPosX)).collect(Collectors.toList());
        for(Entity ent : sortedEntX) {
            if(RELATIVE_LOCATION_RELATIONS) {
                for(Entity entity : entities) {
                    if(ent != entity && near(ent, entity)) {
                        ArrayList<String> QAInfo = new ArrayList<String>();
                        boolean enable_leftOf_aboveOf = false;
                        boolean relate_pedestrians = false;
                        if(relate_pedestrians || !(entity instanceof Pedestrian && ent instanceof Pedestrian)) {
                            if(ent.posX < entity.posX) {
                                if(enable_leftOf_aboveOf) {
                                    QAInfo.add("<(*," + name(ent) + "," + name(entity) + ") --> leftOf>. :|:");
                                }
                                QAInfo.add("(&|," + informType(ent) + "," + informType(entity)+"). :|:");
                                if(veryClose(ent, entity)) {
                                    if(ent instanceof Car) {
                                        QAInfo.add("<(*," + name(entity) + "," + name(ent) + ") --> closeTo>. :|:");
                                    } else {
                                        QAInfo.add("<(*," + name(ent) + "," + name(entity) + ") --> closeTo>. :|:");
                                    }
                                }
                                QAinformation.add(QAInfo);
                                relatedLeft.add(ent);
                                relatedRight.add(entity);
                            }
                            if(ent.posY < entity.posY) {
                                if(enable_leftOf_aboveOf) {
                                    QAInfo.add("<(*," + name(ent) + "," + name(entity) + ") --> aboveOf>. :|:");
                                }
                                QAInfo.add("(&|," + informType(ent) + "," + informType(entity)+"). :|:");
                                if(veryClose(ent, entity)) {
                                    if(ent instanceof Car) {
                                        QAInfo.add("<(*," + name(entity) + "," + name(ent) + ") --> closeTo>. :|:");
                                    } else {
                                        QAInfo.add("<(*," + name(ent) + "," + name(entity) + ") --> closeTo>. :|:");
                                    }
                                }
                                QAinformation.add(QAInfo);
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
            String subj = X + "_" + Y;
            if(locationToLabel.containsKey(subj)) {
                System.out.println("QA INFO: <(*,"+name(ent)+","+locationToLabel.get(subj).choice()+") --> at>. :|:");
                ArrayList<String> Atinfo = new ArrayList<String>();
                Atinfo.add(typeInfo);
                Atinfo.add("<(*,"+name(ent)+","+locationToLabel.get(subj).choice()+") --> at>. :|:");
                QAinformation.add(Atinfo);
            }
        }
        
        Collections.shuffle(QAinformation, rnd);
        int take_k = 8;
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
