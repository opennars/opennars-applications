package com.opennars.applications.pong;

import com.opennars.applications.componentbased.Entity;
import org.opennars.interfaces.NarseseConsumer;

public class Ops {
    public Pong2 pong;
    public Entity batEntity;
    public Entity ballEntity;
    public NarseseConsumer consumer;

    public void up() {
        //System.out.println("[o] up");
        //upDirection();
        upDirect();
        pong.timeoutForOps = -100;
    }

    public void down() {
        //System.out.println("[o] down");
        //downDirection();
        downDirect();
        pong.timeoutForOps = -100;
    }

    public void cmp() {
        int debugHere = 5;

        pong.compare = true;
    }

    public void upDirection() {
        pong.batDirection = 1;
    }

    public void downDirection() {
        pong.batDirection = -1;
    }

    public void upDirect() {


        double oldPosition = batEntity.posY;

        batEntity.posY -= 8;


        //System.out.println(batEntity.posY);

        batEntity.posY = Math.max(batEntity.posY, 13.0);

        //System.out.println(batEntity.posY);

        if(batEntity.posY < oldPosition) {
            pong.timeoutForOpsEffective = -100;
        }
    }

    public void downDirect() {


        double oldPosition = batEntity.posY;

        batEntity.posY += 8;
        batEntity.posY = Math.min(batEntity.posY, 80.0 - 13.0);

        if(batEntity.posY > oldPosition) {
            pong.timeoutForOpsEffective = -100;
        }
    }

    public void selectAxis(String name) {
        System.out.println("[o] select axis " + name);

        if (name.equals("x"))
            pong.perceptionAxis = 0;
        else
            pong.perceptionAxis = 1;


        // send update to the Reasoner
        double diffX = batEntity.posX - ballEntity.posX;
        double diffY = batEntity.posY - ballEntity.posY;

        if (name.equals("x")) {
            String narsese = "<x" + (int)(diffX / 10) + " --> [diffX]>. :|:";
            consumer.addInput(narsese);
        }

        if (name.equals("y")) {
            String narsese = "<y" + (int)(diffY / 10) + " --> [diffY]>. :|:";
            consumer.addInput(narsese);
        }
    }
}
