package com.opennars.applications.pong;

import java.util.ArrayList;
import java.util.List;

import org.opennars.interfaces.NarseseConsumer;

/**
 * informs the Narsese consumer about global changes (when it changes)
 */
// TODO< maintain a list of entities and pull information from entities on tick() >
public class StaticInformer {

    public NarseseConsumer consumer;

    private String lastInput = "";
    private String input = "";
    private List<String> inputs = new ArrayList<>();
    public TemporalQa temporalQa;

    public StaticInformer(final NarseseConsumer consumer) {
        this.consumer = consumer;
    }

    public void informAboutReinforcment(double reward) {
        informAboutReinforcment(reward, 0.8);
    }

    public void informAboutReinforcment(double reward, double conf) {
        String narsese;
        if (reward > 0.0) {
            narsese = "<{SELF1} --> [good]>. :|: %" + Double.toString(reward) + ";" + Double.toString(conf) + "%";
        }
        else {
            narsese = "(--, <{SELF1} --> [good]>). :|: %" + Double.toString(-reward) + ";" + Double.toString(conf) + "%";
        }
        inputs.add(narsese);
        input += inputs.get(inputs.size()-1);
    }

    public void addNarsese(final String narsese) {
        inputs.add(narsese + ". :|: %1.0;0.99%");
        input += inputs.get(inputs.size()-1);
    }

    public boolean informWhenNecessary(final boolean force) {
        boolean hadInput = false;
        if(!input.equals(lastInput)||force) {
            //System.out.println(">>" + input);
            //System.out.println("#>" + lastInput);


            for(String inp : inputs) {
                String narsese = inp;

                System.out.println(narsese);

                consumer.addInput(narsese);
                temporalQa.inputEventAsNarsese(inp);
                hadInput = true;
            }
            lastInput = input;
        }
        input = "";
        inputs.clear();
        return hadInput;
    }
}
