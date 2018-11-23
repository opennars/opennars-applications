package com.opennars.applications.pong.components;

import com.opennars.applications.componentbased.Entity;
import com.opennars.applications.componentbased.InformReasonerComponent;

public class MappedPositionInformer implements InformReasonerComponent {
    @Override
    public String informAboutEntity(Entity entity) {
        String id = String.valueOf(entity.id);
        boolean useMultipleIDs = true;
        if(!useMultipleIDs) {
            id = "0";
        }

        // TODO< we need to invoke the mapper here >
        final String posAsString = "0";

        return "<(*," + entity.tag + id + ","+ posAsString + ") --> at>. :|:";
    }

    @Override
    public String retName() {
        return "MappedPositionInformer";
    }
}
