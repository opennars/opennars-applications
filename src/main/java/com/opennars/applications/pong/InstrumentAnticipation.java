package com.opennars.applications.pong;

import org.opennars.io.events.EventEmitter;
import org.opennars.io.events.OutputHandler;
import org.opennars.language.Term;
import org.opennars.main.Nar;
import org.opennars.plugin.Plugin;

public class InstrumentAnticipation implements Plugin, EventEmitter.EventObserver {

    @Override
    public boolean setEnabled(final Nar n, final boolean enabled) {
        n.memory.event.set(this, enabled, OutputHandler.ANTICIPATE.class);
        n.memory.event.set(this, enabled, OutputHandler.CONFIRM.class);
        n.memory.event.set(this, enabled, OutputHandler.DISAPPOINT.class);
        return true;
    }

    @Override
    public void event(Class event, Object[] args) {
        if(event.equals(OutputHandler.ANTICIPATE.class)) {
            Term term = (Term) args[0];
            System.out.println("[d] anticipate term=" + term);
        }
        else if(event.equals(OutputHandler.CONFIRM.class)) {
            Term term = (Term) args[0];
            System.out.println("[d] confirm term=" + term);
        }
        else if(event.equals(OutputHandler.DISAPPOINT.class)) {
            Term term = (Term) args[0];
            System.out.println("[d] disappoint term=" + term);
        }
    }
}
