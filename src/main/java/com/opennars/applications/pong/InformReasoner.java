package com.opennars.applications.pong;

import com.opennars.applications.componentbased.Entity;
import com.opennars.applications.pong.components.MappedPositionInformer;
import org.opennars.interfaces.NarseseConsumer;

import java.util.ArrayList;
import java.util.List;

public class InformReasoner {
    String lastInput = "";
    String input = "";
    List<String> inputs = new ArrayList<>();

    //minX and minY define the lower end of the relative coordinate system
    public void informAboutEntities(List<Entity> entities) {
        for (final Entity iEntity : entities) {
            final MappedPositionInformer informer = (MappedPositionInformer)iEntity.retComponentByName("MappedPositionInformer");
            if (informer == null) {
                continue;
            }

            inputs.add(informer.informAboutEntity(iEntity));
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
