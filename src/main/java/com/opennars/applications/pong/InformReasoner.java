package com.opennars.applications.pong;

import com.opennars.applications.componentbased.Entity;
import org.opennars.interfaces.NarseseConsumer;

import java.util.ArrayList;
import java.util.List;

public class InformReasoner {
    String lastInput = "";
    String input = "";
    List<String> inputs = new ArrayList<>();

    //minX and minY define the lower end of the relative coordinate system
    public void informAboutEntity(Entity entity) {
        String id = String.valueOf(entity.id);
        boolean useMultipleIDs = true;
        if(!useMultipleIDs) {
            id = "0";
        }
        String pos = Util.positionToTerm((int) entity.posX-minX, (int) entity.posY-minY);
        if (entity instanceof Car) {
            inputs.add("<(*,car" + id + ","+ pos + ") --> at>. :|:");
            input += inputs.get(inputs.size()-1);
        }
        if (entity instanceof Pedestrian) {
            inputs.add("<(*,pedestrian" + id + "," + pos + ") --> at>. :|:");
            input += inputs.get(inputs.size()-1);
        }
    }

    // /param force are the inputs forced to be fed into the reasoner
    public boolean Input(NarseseConsumer consumer, final boolean force) {
        boolean hadInput = false;
        if(!input.equals(lastInput)||force) {
            for(String inp : inputs) {
                consumer.addInput(inp);
                hadInput = true;
            }
            lastInput = input;
        }
        input = "";
        inputs.clear();
        return hadInput;
    }
}
