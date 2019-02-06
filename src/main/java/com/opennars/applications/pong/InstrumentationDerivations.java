package com.opennars.applications.pong;

import org.opennars.entity.Task;
import org.opennars.io.events.EventEmitter;
import org.opennars.io.events.Events;
import org.opennars.main.Nar;
import org.opennars.plugin.Plugin;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Used to instrument derivations
 *
 * May be used for debugging, analysis, tracing, etc.
 */
public class InstrumentationDerivations implements Plugin, EventEmitter.EventObserver {
    public String instanceName = "A"; // name of the instance of the Instrumentation or Reasoner

    public PrintWriter out;

    public InstrumentationDerivations() {

        FileWriter fw = null;
        try {
            fw = new FileWriter("reasoner-instrumentation.log", true);
        } catch (IOException e) {
            {}
        }
        BufferedWriter bw = new BufferedWriter(fw);
        out = new PrintWriter(bw);
    }

    @Override
    public boolean setEnabled(final Nar n, final boolean enabled) {
        //n.memory.event.set(this, enabled, Events.TaskDerive.class);
        //n.memory.event.set(this, enabled, Events.TaskAdd.class);
        return true;
    }

    @Override
    public void event(Class event, Object[] args) {
        if(event.equals(Events.TaskDerive.class)) {
            Task task = (Task)args[0];
            boolean revised = (boolean)args[1];
            boolean single = (boolean)args[2];

            //out.println("[TaskDerive]" + instanceName + ": " + task.toStringLong());
        }
        else if(event.equals(Events.TaskAdd.class)) {
            Task task = (Task)args[0];
            String reason = (String)args[1];

            if (task.sentence.truth == null || task.sentence.truth.getConfidence() > 0.3/* && reason.equals("Perceived")*/) {
                out.println("[TaskAdd]" + instanceName + ": " + reason + "   " + task.toStringLong());
            }
        }
    }
}
