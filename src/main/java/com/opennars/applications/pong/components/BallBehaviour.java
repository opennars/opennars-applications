package com.opennars.applications.pong.components;

import com.opennars.applications.componentbased.BehaviourComponent;
import com.opennars.applications.componentbased.Entity;

public class BallBehaviour implements BehaviourComponent {
    @Override
    public void tick(Entity entity) {
        // TODO< parameter >
        final float dt = 1.0f / 50.0f;

        entity.posX += (entity.velocityX * dt);
        entity.posY += (entity.velocityY * dt);


        if(entity.posX > 50.0) {
            // reset to origin

            entity.posX = 0.0;
            entity.posY = 0.0;
        }
    }
}
