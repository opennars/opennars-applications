package com.opennars.applications.pong;

import com.opennars.applications.componentbased.Entity;

public class Ops {
    public Entity batEntity;

    public void up() {
        if (batEntity.posY <= 0.0) {
            return;
        }

        batEntity.posY -= 10;
        batEntity.posY = Math.max(batEntity.posY, 0.0);
    }

    public void down() {
        if (batEntity.posY >= 40.0) {
            return;
        }

        batEntity.posY += 10;
        batEntity.posY = Math.min(batEntity.posY, 40.0);
    }
}
