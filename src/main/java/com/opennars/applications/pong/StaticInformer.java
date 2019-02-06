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


    public void informAboutReinforcmentGood() {
        String narsese = "<{SELF} --> [good]>";
        inputs.add(narsese);
        input += inputs.get(inputs.size()-1);
    }

    public void addNarsese(final String narsese) {
        inputs.add(narsese);
        input += inputs.get(inputs.size()-1);
    }

    public boolean informWhenNecessary(final boolean force) {
        boolean hadInput = false;
        if(!input.equals(lastInput)||force) {
            for(String inp : inputs) {
                consumer.addInput(inp + ".:|:");
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
