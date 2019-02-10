package com.opennars.applications.pong;

import org.opennars.entity.Sentence;
import org.opennars.entity.Task;
import org.opennars.io.events.EventEmitter;
import org.opennars.io.events.Events;
import org.opennars.main.Nar;
import org.opennars.plugin.Plugin;

public class InstrumentForgetting implements Plugin, EventEmitter.EventObserver {
    // Events.EnactableExplainationRemove.class


    @Override
    public boolean setEnabled(final Nar n, final boolean enabled) {
        n.memory.event.set(this, enabled, Events.EnactableExplainationRemove.class);
        return true;
    }

    @Override
    public void event(Class event, Object[] args) {
        if(event.equals(Events.EnactableExplainationRemove.class)) {
            Sentence removed = (Sentence)args[1];

            //if (removed.toString().contains("good")) { // filter by reward
                System.out.println("forgot precond=  " + removed.toString());
            //}


            //Task task = (Task)args[0];
            //boolean revised = (boolean)args[1];
            //boolean single = (boolean)args[2];

            //out.println("[TaskDerive]" + instanceName + ": " + task.toStringLong());
        }
    }
}
