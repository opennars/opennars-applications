package com.opennars.applications.pong.components;

import com.opennars.applications.componentbased.BehaviourComponent;
import com.opennars.applications.componentbased.Entity;

import java.util.Random;

public class BallBehaviour implements BehaviourComponent {
    public Entity batEntity;
    public double batWidth = 26.0;

    public boolean bouncedOfBat;

    @Override
    public void tick(Entity entity) {
        bouncedOfBat = false;

        // TODO< parameter >
        final float dt = 1.0f / 50.0f;

        entity.posX += (entity.velocityX * dt);
        entity.posY += (entity.velocityY * dt);


        if(entity.posX > 100.0) {
            // check if it got reflected by bat

            double absDiffTobBat = entity.posY - batEntity.posY;
            absDiffTobBat = Math.abs(absDiffTobBat);

            final boolean hitBat = absDiffTobBat <= batWidth / 2.0;
            if (hitBat) {
                entity.posX = 100.0 - Float.MIN_NORMAL;
                entity.velocityX *= -1;

                bouncedOfBat = true;
            }
        }
        else if(entity.posX < 0.0) {
            entity.posX = Float.MIN_NORMAL;
            entity.velocityX *= -1;
        }

        if(entity.posY > 80.0) {
            entity.posY = 80.0 - Float.MIN_NORMAL;
            entity.velocityY *= -1;
        }
        else if(entity.posY < 0.0) {
            entity.posY = Float.MIN_NORMAL;
            entity.velocityY *= -1;
        }

    }
}
