package com.opennars.applications.pong;

import com.opennars.applications.crossing.*;
import org.opennars.main.Nar;

import java.util.ArrayList;
import java.util.List;

public class InformReasoner {
    String lastInput = "";
    String input = "";
    List<String> inputs = new ArrayList<>();

    //minX and minY define the lower end of the relative coordinate system
    public void informAboutEntity(Nar nar, Entity ent, int minX, int minY) {
        String id = String.valueOf(ent.id);
        boolean useMultipleIDs = true;
        if(!useMultipleIDs) {
            id = "0";
        }
        String pos = Util.positionToTerm((int) ent.posX-minX, (int) ent.posY-minY);
        if (ent instanceof Car) {
            inputs.add("<(*,car" + id + ","+ pos + ") --> at>. :|:");
            input += inputs.get(inputs.size()-1);
        }
        if (ent instanceof Pedestrian) {
            inputs.add("<(*,pedestrian" + id + "," + pos + ") --> at>. :|:");
            input += inputs.get(inputs.size()-1);
        }
    }

    public void informAboutTrafficLight(Nar nar, TrafficLight light, int minX, int minY) {
        //String id = String.valueOf(light.id);
        String colour = light.colour == 0 ? "green" : "red";
        String narsese = "<trafficLight --> ["+colour+"]>. :|:";
        inputs.add(narsese);
        input+=narsese;
    }

    // /param force are the inputs forced to be fed into the reasoner
    public boolean Input(Nar nar, final boolean force) {
        boolean hadInput = false;
        if(!input.equals(lastInput)||force) {
            for(String inp : inputs) {
                nar.addInput(inp);
                hadInput = true;
            }
            lastInput = input;
        }
        input = "";
        inputs.clear();
        return hadInput;
    }
}
