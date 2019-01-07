package com.opennars.applications.pong.tracker;

import com.opennars.applications.pong.InformReasoner;
import com.opennars.applications.pong.StaticInformer;
import org.opennars.interfaces.pub.Reasoner;
import org.opennars.main.Nar;
import org.opennars.middle.operatorreflection.MethodInvocationOperator;
import org.opennars.operator.Operator;

import java.lang.reflect.Method;
import java.util.Random;

/**
 * Track a object in the scene with a Reasoner
 *
 * The reasoner decides about the commands(Ops) of the tracker, for example moving
 */
public class Tracker {
    public double posX = 0.0;
    public double posY = 0.0;

    public Reasoner reasoner;
    public Reasoner reasoner2;

    private StaticInformer staticInformer;

    public int timeoutForOps = 0;

    public int timeSinceLastCenter = 0;

    public double goodLevel = 1.0;

    public long t = 0;

    Random rng = new Random();

    public Tracker(final Reasoner reasoner1, final Reasoner reasoner2) {
        this.reasoner = reasoner1;
        this.reasoner2 = reasoner2;

        staticInformer = new StaticInformer(reasoner1);

        // add ops
        TrackerOps ops = new TrackerOps();
        ops.tracker = this;

        try {
            /*
            Operator opUp = new MethodInvocationOperator("^up", ops, ops.getClass().getMethod("up"), new Class[0]);
            reasoner1.addPlugin(opUp);
            ((Nar) reasoner1).memory.addOperator(opUp);

            {
                Operator opX = new MethodInvocationOperator("^upLeft", ops, ops.getClass().getMethod("upLeft"), new Class[0]);
                reasoner1.addPlugin(opX);
                ((Nar) reasoner1).memory.addOperator(opX);
            }

            {
                Operator opX = new MethodInvocationOperator("^upRight", ops, ops.getClass().getMethod("upRight"), new Class[0]);
                reasoner1.addPlugin(opX);
                ((Nar) reasoner1).memory.addOperator(opX);
            }

            Operator opDown = new MethodInvocationOperator("^down", ops, ops.getClass().getMethod("down"), new Class[0]);
            reasoner1.addPlugin(opDown);
            ((Nar) reasoner1).memory.addOperator(opDown);

            {
                Operator opX = new MethodInvocationOperator("^downLeft", ops, ops.getClass().getMethod("downLeft"), new Class[0]);
                reasoner1.addPlugin(opX);
                ((Nar) reasoner1).memory.addOperator(opX);
            }

            {
                Operator opX = new MethodInvocationOperator("^downRight", ops, ops.getClass().getMethod("downRight"), new Class[0]);
                reasoner1.addPlugin(opX);
                ((Nar) reasoner1).memory.addOperator(opX);
            }

            Operator opLeft = new MethodInvocationOperator("^left", ops, ops.getClass().getMethod("left"), new Class[0]);
            reasoner1.addPlugin(opLeft);
            ((Nar) reasoner1).memory.addOperator(opLeft);

            Operator opRight = new MethodInvocationOperator("^right", ops, ops.getClass().getMethod("right"), new Class[0]);
            reasoner1.addPlugin(opRight);
            ((Nar) reasoner1).memory.addOperator(opRight);

            */
            {
                Operator op = new MethodInvocationOperator("^movedir", ops, ops.getClass().getMethod("movedir", new Class[]{String.class}), new Class[]{String.class});
                reasoner1.addPlugin(op);
                ((Nar)reasoner1).memory.addOperator(op);
            }



            Operator opTeleport = new MethodInvocationOperator("^teleport", ops, ops.getClass().getMethod("teleport"), new Class[0]);
            reasoner1.addPlugin(opTeleport);
            ((Nar) reasoner1).memory.addOperator(opTeleport);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        /*
        try {
            Operator opUp = new MethodInvocationOperator("^up", ops, ops.getClass().getMethod("up"), new Class[0]);
            reasoner2.addPlugin(opUp);
            ((Nar) reasoner2).memory.addOperator(opUp);

            Operator opDown = new MethodInvocationOperator("^down", ops, ops.getClass().getMethod("down"), new Class[0]);
            reasoner2.addPlugin(opDown);
            ((Nar) reasoner2).memory.addOperator(opDown);

            Operator opLeft = new MethodInvocationOperator("^left", ops, ops.getClass().getMethod("left"), new Class[0]);
            reasoner2.addPlugin(opLeft);
            ((Nar) reasoner2).memory.addOperator(opLeft);

            Operator opRight = new MethodInvocationOperator("^right", ops, ops.getClass().getMethod("right"), new Class[0]);
            reasoner2.addPlugin(opRight);
            ((Nar) reasoner2).memory.addOperator(opRight);

            Operator opTeleport = new MethodInvocationOperator("^teleport", ops, ops.getClass().getMethod("teleport"), new Class[0]);
            reasoner2.addPlugin(opTeleport);
            ((Nar) reasoner2).memory.addOperator(opTeleport);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
         */
    }

    /**
     *
     * @param code is the code of the perceived direction
     */
    public void informAndStep(String code1, String code2) {
        t++;

        goodLevel *= 0.98; // decay

        //System.out.println("[d5] good level= " + Double.toString(goodLevel));

        String narsese = "<{" + code1 + code2 + "} --> [on1]>. :|:";
        staticInformer.addNarsese(narsese);
        //narsese = "<{" + code2 + "} --> [on2]>. :|:";
        //staticInformer.addNarsese(narsese);

        staticInformer.informWhenNecessary(false);

        if( t%4 == 0) {
            // good reasoner if it is the center
            if (code1.equals("c")) {
                timeSinceLastCenter = 0;

                goodLevel += 0.1; // very good
                goodLevel = Math.min(goodLevel, 1.0);

                //System.out.println("[a ] Tracker GOOD");
                reasoner.addInput("<{SELF} --> [good1]>.:|:");
            }

            if (code2.equals("c")) {
                timeSinceLastCenter = 0;

                goodLevel += 0.1; // very good
                goodLevel = Math.min(goodLevel, 1.0);

                //System.out.println("[a ] Tracker GOOD");
                reasoner.addInput("<{SELF} --> [good1]>.:|:");
            }
        }

        timeSinceLastCenter++;
        if(timeSinceLastCenter>120) {
            //timeSinceLastCenter = 0;

            // force teleport
            // we don't inform NARS about the teleportation because it uses the teleport not wisely
            //posX = rng.nextInt(8) * 10.0;
            //posY = rng.nextInt(7) * 10.0;

            // commented because teleport seems to confuse it
            //reasoner.addInput("(^teleport, {SELF})!");
        }

        timeoutForOps++;
        // inject random action if necessary
        if (timeoutForOps > 0 || rng.nextInt(100) <= 4) {
            switch(rng.nextInt(9)) {
                /*
                case 0: reasoner.addInput("(^left, {SELF})!"); break;

                case 1: reasoner.addInput("(^right, {SELF})!"); break;

                case 2: reasoner.addInput("(^up, {SELF})!"); break;
                case 3: reasoner.addInput("(^upLeft, {SELF})!"); break;
                case 4: reasoner.addInput("(^upRight, {SELF})!"); break;

                case 5: reasoner.addInput("(^down, {SELF})!"); break;
                case 6: reasoner.addInput("(^downLeft, {SELF})!"); break;
                case 7: reasoner.addInput("(^downRight, {SELF})!"); break;
                */

                case 0: reasoner.addInput("(^movedir, {SELF}, {ul})!"); break;
                case 1: reasoner.addInput("(^movedir, {SELF}, {dl})!"); break;
                case 2: reasoner.addInput("(^movedir, {SELF}, {ur})!"); break;
                case 3: reasoner.addInput("(^movedir, {SELF}, {dr})!"); break;
                case 4: reasoner.addInput("(^movedir, {SELF}, {u})!"); break;
                case 5: reasoner.addInput("(^movedir, {SELF}, {d})!"); break;
                case 6: reasoner.addInput("(^movedir, {SELF}, {l})!"); break;
                case 7: reasoner.addInput("(^movedir, {SELF}, {r})!"); break;

                /*
                case 1: reasoner.addInput("(^right, {SELF})!"); break;

                case 2: reasoner.addInput("(^up, {SELF})!"); break;
                case 3: reasoner.addInput("(^upLeft, {SELF})!"); break;
                case 4: reasoner.addInput("(^upRight, {SELF})!"); break;

                case 5: reasoner.addInput("(^down, {SELF})!"); break;
                case 6: reasoner.addInput("(^downLeft, {SELF})!"); break;
                case 7: reasoner.addInput("(^downRight, {SELF})!"); break;
                */


                //case 4: reasoner.addInput("(^teleport, {SELF})!"); break;

                //case 0: reasoner.addInput("(^up, {SELF})!"); break;
                //case 1: reasoner.addInput("(^down, {SELF})!"); break;
                //case 4: reasoner.addInput("(^teleport, {SELF})!"); break;
            }
        }

        if (goodLevel < 0.03) {
            goodLevel = 0.8;

            // force teleport
            //reasoner.addInput("(^teleport, {SELF})!");
        }

        // let it execute two commands - maybe for each axis - and give it asymetrically time - why not
        if( t%4 == 0) {
            reasoner.addInput("<{SELF} --> [good1]>!");
            reasoner.cycles(15);
            //reasoner.addInput("<{SELF} --> [good1]>!");
            reasoner.cycles(15);
        }
    }
}
