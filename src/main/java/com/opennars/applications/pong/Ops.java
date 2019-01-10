package com.opennars.applications.pong;

import com.opennars.applications.componentbased.Entity;

public class Ops {
    public Pong pong;
    public Entity batEntity;

    public void up() {
        System.out.println("[o] up");

        //System.out.println(batEntity.posY);


        pong.timeoutForOps = -100;

        //if (batEntity.posY <= 0.0) {
        //    return;
        //}

        batEntity.posY -= 10;

        //System.out.println(batEntity.posY);

        batEntity.posY = Math.max(batEntity.posY, 0.0);

        //System.out.println(batEntity.posY);
    }

    public void down() {
        System.out.println("[o] down");

        //System.out.println(batEntity.posY);

        pong.timeoutForOps = -100;

        //if (batEntity.posY >= 80.0) {
        //    return;
        //}

        batEntity.posY += 10;
        batEntity.posY = Math.min(batEntity.posY, 80.0);

    }

    public void selectAxis(String name) {
        System.out.println("[o] select axis " + name);

        if (name.equals("x"))
            pong.perceptionAxis = 0;
        else
            pong.perceptionAxis = 1;
    }
}
