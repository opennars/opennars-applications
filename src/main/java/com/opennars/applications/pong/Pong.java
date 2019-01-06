/*
 * The MIT License
 *
 * Copyright 2018 The OpenNARS authors.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.opennars.applications.pong;

import com.opennars.applications.componentbased.Entity;
import com.opennars.applications.crossing.Util;
import com.opennars.applications.crossing.Viewport;
import com.opennars.applications.pong.components.BallBehaviour;
import com.opennars.applications.pong.components.BallRenderComponent;
import com.opennars.applications.pong.components.BatBehaviour;
import com.opennars.applications.pong.components.MappedPositionInformer;
import com.opennars.sgui.NarSimpleGUI;
import org.opennars.interfaces.pub.Reasoner;
import org.opennars.io.events.Events;
import org.opennars.io.events.OutputHandler;
import org.opennars.main.Nar;
import org.opennars.middle.operatorreflection.MethodInvocationOperator;
import org.opennars.operator.Operator;
import processing.core.PApplet;
import processing.event.MouseEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Pong extends PApplet {
    Reasoner reasoner;
    int entityID = 1;

    List<Prediction> predictions = new ArrayList<>();
    List<Prediction> disappointments = new ArrayList<>();


    List<Entity> entities = new ArrayList<>();
    int t = 0;
    public static boolean showAnomalies = false;

    int perception_update = 1;

    InformReasoner informReasoner = new InformReasoner();

    GridMapper mapper = new GridMapper();


    Entity ballEntity;
    Entity batEntity;

    Random rng = new Random();

    long timeoutForOps = 0;

    StaticInformer informer;

    double pseudoscore = 0.0;


    final int fps = 50;
    @Override
    public void setup() {
        mapper.cellsize = 10;

        try {
            reasoner = new Nar();
            ((Nar)reasoner).narParameters.VOLUME = 0;
            ((Nar)reasoner).narParameters.DURATION*=10;
            ReasonerListener listener = new ReasonerListener(reasoner, predictions, disappointments, entities, mapper);
            reasoner.on(Events.TaskAdd.class, listener);
            reasoner.on(OutputHandler.DISAPPOINT.class, listener);
        } catch (Exception ex) {
            System.out.println(ex);
            System.exit(1);
        }

        informer = new StaticInformer(reasoner);

        setupScene();

        size(1000, 1000);
        frameRate(fps);
        new NarSimpleGUI((Nar)reasoner);
    }

    void setupScene() {
        {
            final double posX = 100.0;
            final double posY = 30.0;

            batEntity = new Entity(entityID++, posX, posY, 0.0, 0.0, "ball");
            batEntity.velocityX = 0.0;
            batEntity.velocityY = 0.0;

            batEntity.renderable = new BallRenderComponent();
            batEntity.behaviour = new BatBehaviour();

            //MappedPositionInformer positionInformerForBall = new MappedPositionInformer(mapper);
            //positionInformerForBall.nameOverride = "dot";
            //batEntity.components.add(positionInformerForBall);

            entities.add(batEntity);
        }

        {
            final double posX = 1.0;
            final double posY = 1.0;

            ballEntity = new Entity(entityID++, posX, posY, 0.0, 0.0, "ball");
            ballEntity.velocityX = 110.0;
            ballEntity.velocityY = 23.0; //23.7;

            ballEntity.renderable = new BallRenderComponent();
            ballEntity.behaviour = new BallBehaviour();
            ((BallBehaviour) ballEntity.behaviour).batEntity = batEntity;

            // NOTE< commented because we inform NARS about the ball position in a tuple now >
            //MappedPositionInformer positionInformerForBall = new MappedPositionInformer(mapper);
            //positionInformerForBall.nameOverride = "dot";
            //ballEntity.components.add(positionInformerForBall);

            entities.add(ballEntity);
        }


        {
            Ops ops = new Ops();
            ops.batEntity = batEntity;
            ops.pong = this;

            try {
                Operator opUp = new MethodInvocationOperator("^up", ops, ops.getClass().getMethod("up"), new Class[0]);
                reasoner.addPlugin(opUp);
                ((Nar) reasoner).memory.addOperator(opUp);

                Operator opDown = new MethodInvocationOperator("^down", ops, ops.getClass().getMethod("down"), new Class[0]);
                reasoner.addPlugin(opDown);
                ((Nar) reasoner).memory.addOperator(opDown);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }

            // tell NARS that it has ops
            reasoner.addInput("(^up, {SELF})!");
            reasoner.addInput("(^down, {SELF})!");
        }
    }

    void tick() {
        timeoutForOps++;

        // REFACTOR< TODO< use tick of entity >
        // tick
        for (Entity ie : entities) {
            if (ie.behaviour != null) {
                ie.behaviour.tick(ie);
            }
        }

        // respawn ball if it was not hit by the bat
        {
            final boolean inRange = ballEntity.posX < 120.0;
            if (!inRange) {
                ballEntity.posX = 1.0;
                ballEntity.posY = 1.0 + rng.nextDouble() * (80.0 - 2.0);

                // TODO< choose random y velocity >
            }
        }


        // inform
        {
            int quantizedBallY = (int)(ballEntity.posY / 10.0);
            int quantizedBatY = (int)(batEntity.posY / 10.0);

            String narsese = "<(*, " + Integer.toString(quantizedBallY) + "," + Integer.toString(quantizedBatY) + ") --> [atTuple]>";

            narsese += ". :|:";
            informer.addNarsese(narsese);

            informer.informWhenNecessary(false); // give chance to push collected narsese to narsese consumer(which is the Nar)
        }

        if(t%2==0) {
            reasoner.addInput("<{SELF} --> [good]>!");
        }


        if(t%8==0) {


            int explorativeTimeout = 60; // time after which a random op is injected when it didn't do anything sufficiently long


            if(timeoutForOps >= 0) {
                System.out.println("[d] random op");


                timeoutForOps = -explorativeTimeout;

                // feed random decision so NARS doesn't forget ops
                int rngValue = rng.nextInt( 3);
                System.out.println(rngValue);
                switch (rngValue) {
                    case 0:
                        reasoner.addInput("(^up, {SELF})!");
                        break;

                    case 1:
                        reasoner.addInput("(^down, {SELF})!");
                        break;

                    default:
                }
            }


            { // inject random op from time to time by chance to avoid getting stuck in cycles from which the agent can't escape
                int rngValue2 = rng.nextInt( 100);

                int chance = 10; // in percentage

                if (rngValue2 < chance) {
                    System.out.println("[d] FORCED random op");

                    int rngValue = rng.nextInt( 3);
                    System.out.println(rngValue);
                    switch (rngValue) {
                        case 0:
                            reasoner.addInput("(^up, {SELF})!");
                            break;

                        case 1:
                            reasoner.addInput("(^down, {SELF})!");
                            break;

                        default:
                    }
                }
            }




        }

        // reinforce more frequently
        {
            final double absDiffY = Math.abs(batEntity.posY - ballEntity.posY);
            final double absDiffX = Math.abs(batEntity.posX - ballEntity.posX);

            if (absDiffY <= 13.0 && absDiffX <= 15.0) {
                informReasoner.informAboutReinforcmentGood();

                System.out.println("GOOD NARS");

                pseudoscore += 1.0;
            }
        }

        if(t%600==0) {
            System.out.println("[i] pseudoscore=" + Double.toString(pseudoscore) + " t=" + Integer.toString(t));
        }

        //if(t%2==0) {
        //
        //}



        t++;
        reasoner.cycles(40);
        removeOutdatedPredictions(predictions);
        removeOutdatedPredictions(disappointments);


        informReasoner.informAboutEntities(entities);

        if (t % perception_update == 0) {
            final boolean force = false; // not required HACK
            final boolean hasInput = informReasoner.Input(reasoner, force);

            if(hasInput) {
                //reasoner.addInput(questions);
            }
        }

        if (false) {
            System.out.println("Concepts: " + ((Nar)reasoner).memory.concepts.size());
        }
    }

    @Override
    public void draw() {
        tick();

        viewport.Transform();
        background(255);
        fill(0);


        for (Entity e : entities) {
            e.render(this);
        }

        for (Prediction pred : predictions) {
            Entity e = pred.ent;

            float transparency = Util.truthToValue(pred.truth) * Util.timeToValue(pred.time - reasoner.time());

            // HACK< we need to cast to some class with translucency >

            BallRenderComponent ballRender = (BallRenderComponent)e.renderable;
            ballRender.translucency = transparency;

            e.render(this);
        }
        if(showAnomalies) {
            // REFACTOR< TODO< render >
            /*
            for (ReasonerListener.Prediction pred : disappointments) {
                Entity e = pred.ent;
                if(e instanceof Car) {
                    fill(255,0,0);
                }
                if(e instanceof Pedestrian) {
                    fill(0,0,255);
                }
                this.text("ANOMALY", (float)e.posX, (float)e.posY);
                e.draw(this, streets, trafficLights, entities, pred.truth, pred.time - ((Reasoner)reasoner).time());
            }
             */
        }

    }

    public void removeOutdatedPredictions(List<Prediction> predictions) {
        List<Prediction> toDelete = new ArrayList<>();
        for(Prediction pred : predictions) {
            if(pred.time <= reasoner.time()) {
                toDelete.add(pred);
            }
        }
        predictions.removeAll(toDelete);
    }

    float mouseScroll = 0;
    Viewport viewport = new Viewport(this);
    public void mouseWheel(MouseEvent event) {
        mouseScroll = -event.getCount();
        viewport.mouseScrolled(mouseScroll);
    }
    @Override
    public void keyPressed() {
        viewport.keyPressed();
    }
    @Override
    public void mousePressed() {
        viewport.mousePressed();
    }
    @Override
    public void mouseReleased() {
        viewport.mouseReleased();
    }
    @Override
    public void mouseDragged() {
        viewport.mouseDragged();
    }

    public static void main(String[] args) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(NarSimpleGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>
        String[] args2 = {"Pong"};
        Pong mp = new Pong();
        PApplet.runSketch(args2, mp);
    }
}
