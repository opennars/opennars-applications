package com.opennars.applications.pong;

import com.opennars.applications.crossing.Entity;
import org.opennars.entity.TruthValue;

public class Prediction {
    public Entity ent;
    public long time;
    public TruthValue truth;
    public String type;
    public Prediction(Entity ent, TruthValue truth, long time, String type) {
        this.ent = ent;
        this.time = time;
        this.truth = truth;
        this.type = type;
    }
}