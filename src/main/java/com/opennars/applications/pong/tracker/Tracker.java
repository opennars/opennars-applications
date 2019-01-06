package com.opennars.applications.pong.tracker;

import com.opennars.applications.pong.InformReasoner;
import com.opennars.applications.pong.StaticInformer;
import org.opennars.interfaces.pub.Reasoner;
import org.opennars.main.Nar;
import org.opennars.middle.operatorreflection.MethodInvocationOperator;
import org.opennars.operator.Operator;

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

    private StaticInformer staticInformer;

    public int timeoutForOps = 0;

    Random rng = new Random();

    public Tracker(final Reasoner reasoner) {
        this.reasoner = reasoner;

        staticInformer = new StaticInformer(reasoner);

        // add ops
        TrackerOps ops = new TrackerOps();
        ops.tracker = this;

        try {
            //Operator opUp = new MethodInvocationOperator("^up", ops, ops.getClass().getMethod("up"), new Class[0]);
            //reasoner.addPlugin(opUp);
            //((Nar) reasoner).memory.addOperator(opUp);

            //Operator opDown = new MethodInvocationOperator("^down", ops, ops.getClass().getMethod("down"), new Class[0]);
            //reasoner.addPlugin(opDown);
            //((Nar) reasoner).memory.addOperator(opDown);

            Operator opLeft = new MethodInvocationOperator("^left", ops, ops.getClass().getMethod("left"), new Class[0]);
            reasoner.addPlugin(opLeft);
            ((Nar) reasoner).memory.addOperator(opLeft);

            Operator opRight = new MethodInvocationOperator("^right", ops, ops.getClass().getMethod("right"), new Class[0]);
            reasoner.addPlugin(opRight);
            ((Nar) reasoner).memory.addOperator(opRight);

            Operator opTeleport = new MethodInvocationOperator("^teleport", ops, ops.getClass().getMethod("teleport"), new Class[0]);
            reasoner.addPlugin(opTeleport);
            ((Nar) reasoner).memory.addOperator(opTeleport);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param code is the code of the perceived direction
     */
    public void informAndStep(String code) {
        String narsese = "<{" + code + "} --> [on]>. :|:";

        staticInformer.addNarsese(narsese);
        staticInformer.informWhenNecessary(false);

        reasoner.addInput("<{SELF} --> [good]>!");

        // good reasoner if it is the center
        if (code.equals("c")) {
            System.out.println("[a ] Tracker GOOD");
            reasoner.addInput("<{SELF} --> [good]>.:|:");
        }

        timeoutForOps++;
        // inject random action if necessary
        if (timeoutForOps > 0) {
            switch(rng.nextInt(2)) {
                case 0: reasoner.addInput("(^left, {SELF})!"); break;
                case 1: reasoner.addInput("(^right, {SELF})!"); break;


                //case 0: reasoner.addInput("(^up, {SELF})!"); break;
                //case 1: reasoner.addInput("(^down, {SELF})!"); break;
                //case 4: reasoner.addInput("(^teleport, {SELF})!"); break;
            }
        }

        reasoner.cycles(30);
    }
}
