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
import com.opennars.applications.pong.tracker.Tracker;
import com.opennars.sgui.NarSimpleGUI;
import org.opennars.interfaces.pub.Reasoner;
import org.opennars.io.events.Events;
import org.opennars.io.events.OutputHandler;
import org.opennars.language.*;
import org.opennars.main.Nar;
import org.opennars.middle.operatorreflection.MethodInvocationOperator;
import org.opennars.operator.Operator;
import processing.core.PApplet;
import processing.event.MouseEvent;

import java.util.*;

public class Pong2 extends PApplet {
    void tick() {
        { // draw to virtual screen
            pixelScreen.clear();

            // ball
            pixelScreen.drawDot((int)(ballEntity.posX), (int)(ballEntity.posY));
            pixelScreen.drawDot((int)(ballEntity.posX+1), (int)(ballEntity.posY));
            pixelScreen.drawDot((int)(ballEntity.posX), (int)(ballEntity.posY+1));
            pixelScreen.drawDot((int)(ballEntity.posX+1), (int)(ballEntity.posY+1));


            // bat
            pixelScreen.drawDot((int)(batEntity.posX), (int)(batEntity.posY-0));

            for(int i=0;i<=13;i++) {
                pixelScreen.drawDot((int)(batEntity.posX), (int)(batEntity.posY+i));
                pixelScreen.drawDot((int)(batEntity.posX), (int)(batEntity.posY-i));
            }

        }



        if(t%4==0) {
            final String narsese = retNarseseOfBallAndBat(ballEntity.posX, ballEntity.posY, batEntity.posX, batEntity.posY);
            informer.addNarsese(narsese);


            boolean isMiddle = Math.abs(batEntity.posY-ballEntity.posY) < 7.0;
            if (isMiddle) {
                informer.informAboutReinforcment(1.0);
            }

            informer.informWhenNecessary(false);
        }

        if(t%150 == 0) {
            // weak punishment over time
            informer.informAboutReinforcment(-.5, 0.05);
        }




        if (t != oldT) {
            oldT = t;

            timeoutForOpsEffective++;
            timeoutForOps++;

            if (t % 1500 == 2) {
                System.out.println("[d] remind NARS of tuples");

                // remind NARS of action tuples
                remindReasonerOfActionPairs();
            }

            // move bat
            {
                batEntity.posY += ((double)batDirection * 0.5); //  0.65
                batEntity.posY = Math.max(batEntity.posY, 13.0);
                batEntity.posY = Math.min(batEntity.posY, 80.0 - 13.0);
            }

            {
                if (tracker.timeSinceLastCenter > 60 ) {
                    tracker.timeSinceLastCenter = 0;

                    // force to latch on a random object - in this case the ball
                    tracker.posX = ballEntity.posX;
                    tracker.posY = ballEntity.posY;
                }
            }

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
                    ballEntity.posX = 2.0;
                    ballEntity.posY = 30.0 + rng.nextDouble() * (80.0-30.0 - 2.0);

                    // choose random y velocity
                    for(;;) {
                        ballEntity.velocityY = ( rng.nextDouble() * 2.0 - 1.0 ) * 10.0;

                        // disallow low y velocity because it might reward a still agent to much without doing any actions
                        if( Math.abs(ballEntity.velocityY) > 3.0) {
                            break;
                        }
                    }



                    // we set the tracker position because reaquiring the object (in this case the ball) takes to much time and is to unlikely
                    // NOTE< we need to initialite this by an attention mechanism for a realistic vision system >
                    tracker.posX = ballEntity.posX;
                    tracker.posY = ballEntity.posY;


                    emittedBalls++;
                }
            }


            if(t%13==0) {
                reasoner.addInput("<{SELF1} --> [good]>!");
                //System.out.println("<{SELF1} --> [good]>!");


                // give hint for attention
                //reasoner.addInput("<(&/,<?N --> [V]>,?t1,?op,?t2) =/> <{SELF1} --> [good]>>?");
            }



            if(t%8==0) {


                int explorativeTimeout = 600; // time after which a random op is injected when it didn't do anything sufficiently long


                if(timeoutForOps >= 50000) {
                    System.out.println("[d] random op");


                    timeoutForOps = -explorativeTimeout;

                    // feed random decision so NARS doesn't forget ops
                    int rngValue = rng.nextInt( 3);
                    System.out.println(rngValue);
                    switch (rngValue) {
                        case 0:
                            reasoner.addInput("(^up, {SELF})!");

                            temporalQa.inputEvent(Inheritance.makeTerm(new Product(new SetExt(new Term("SELF"))), new Term("up")));

                            break;

                        case 1:
                            reasoner.addInput("(^down, {SELF})!");

                            temporalQa.inputEvent(Inheritance.makeTerm(new Product(new SetExt(new Term("SELF"))), new Term("down")));

                            break;

                        default:
                    }
                }


                { // inject random op from time to time by chance to avoid getting stuck in cycles from which the agent can't escape
                    int rngValue2 = rng.nextInt( 100);

                    int chance = 16; // in percentage

                    if (timeoutForOpsEffective < 0) {
                        chance = 16; // disable if op which changed the world was done
                    }

                    if (rngValue2 < chance) {
                        //System.out.println("[d] FORCED random op");

                        int rngValue0 = rng.nextInt(100);
                        if(rngValue0 < 1000) {
                            int rngValue1 = rng.nextInt( 2);

                            switch (rngValue1) {
                                case 0:
                                    reasoner.addInput("(^up, {SELF})!");

                                    temporalQa.inputEvent(Inheritance.makeTerm(new Product(new SetExt(new Term("SELF"))), new Term("up")));

                                    break;

                                case 1:
                                    reasoner.addInput("(^down, {SELF})!");

                                    temporalQa.inputEvent(Inheritance.makeTerm(new Product(new SetExt(new Term("SELF"))), new Term("down")));

                                    break;
                            }

                        }

                    }
                }




            }

            // reinforce more frequently
            {
                final double absDiffY = Math.abs(batEntity.posY - ballEntity.posY);
                final double absDiffX = Math.abs(batEntity.posX - ballEntity.posX);

                if (((BallBehaviour)ballEntity.behaviour).bouncedOfBat) {
                //if (absDiffY <= 13.0 && absDiffX <= 15.0) {
                    informer.informAboutReinforcment(1.0);

                    System.out.println("GOOD NARS");

                    if (((BallBehaviour)ballEntity.behaviour).bouncedOfBat) {
                        pseudoscore += 1.0;
                    }
                }
            }

            if(t%600==0) {
                System.out.println("[i] #balls=" + emittedBalls + " pseudoscore=" + Double.toString(pseudoscore) + " t=" + Integer.toString(t));
            }


            // keep tracker in area and respawn if necessary
            {
                if (tracker.posX < -40.0 || tracker.posX > 140.0 || tracker.posY < -30.0 || tracker.posY > 80.0 + 20.0) {
                    // respawn

                    tracker.posX = rng.nextInt(8) * 10.0;
                    tracker.posY = rng.nextInt(7) * 10.0;
                }
            }

        }


        t2++;
        t = t2/slowdownFactor;

        for(int i=0;i<25;i++) {
            reasoner.cycles(1);
            temporalQa.endTimestep();
        }


        if (false) {
            System.out.println("Concepts: " + ((Nar)reasoner).memory.concepts.size());
        }

        if(false) {
            System.out.println("#patches= " + patchTracker.trackingRecords.size());
        }
    }

    private static String retNarseseOfBallAndBat(double ballX, double ballY, double batX, double batY) {
        //return "<(*,y"+(int)(ballY / 8.0)+"x"+(int)(ballX / 2000.0) + ",y"+(int)(batY / 10.0)+")-->[ballBatPos]>";
        return "<{y"+(int)(ballY / 6.0)+"y"+(int)(batY / 6.0)+"}-->[ballBatPos]>";

        /*
        if (Math.abs(ballY-batY) < 7) {
            return "<{N}-->[ballBatPos]>";
        }

        if (ballY>batY) {
            return "<{PP}-->[ballBatPos]>";
        }
        else {
            return "<{NN}-->[ballBatPos]>";
        }*/
    }



    private void remindReasonerOfActionPairs() {

        int ballX = 0;
        //for(int ballX=0; ballX < 100; ballX+=10)
        {
            for(int ballY=0; ballY<80;ballY+=8) {
                for(int batY=0; batY<80; batY+=8) {
                    String narseseOfBallBatPos = retNarseseOfBallAndBat(ballX, ballY, 0, batY);

                    boolean up = ballY < batY;
                    String narseseOpName = up ? "up" : "down";
                    String narseseOfOp = "<(*,{SELF}) --> " + narseseOpName + ">";
                    //String narseseOfOp = "<{callOp} --> [" + narseseOpName + "]>";

                    // (C, O) =/> E
                    String narsese = "<(&/," + narseseOfBallBatPos + ",+1," + narseseOfOp + ")" + "=/>" + "<{SELF1} --> [good]>>" + ".";

                    //System.out.println(narsese);

                    reasoner.addInput(narsese);
                }
            }
        }

        int debug = 5;
    }




    @Override
    public void draw() {
        for(int n=0;n<1;n++) {
            tick();
        }


        viewport.Transform();
        background(20);
        fill(0);


        for (int y=0;y<pixelScreen.retHeight();y++)
        for (int x=0;x<pixelScreen.retWidth();x++)
        {
            if (pixelScreen.arr[y][x]) {
                pushMatrix();
                translate((float) x, (float) y);

                stroke(255);
                fill(255);
                color(255);
                //fill(255, 125, 125, 255.0f);
                rect(0, 0, 1, 1);

                popMatrix();
                fill(0);
            }
        }



    }



    @Override
    public void setup() {
        { // pixel screen
            int pixelScreenWidth = 160;
            int pixelScreenHeight = 100;
            pixelScreen = new PixelScreen(pixelScreenWidth, pixelScreenHeight);

            oldPixelScreen = new PixelScreen(pixelScreenWidth, pixelScreenHeight);
            pixelScreenOn = new PixelScreen(pixelScreenWidth, pixelScreenHeight);
            pixelScreenOff = new PixelScreen(pixelScreenWidth, pixelScreenHeight);

            patchScreen = new PixelScreen(pixelScreenWidth, pixelScreenHeight);
        }


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

        // we want instrumentation
        reasoner.addPlugin(new InstrumentationDerivations());

        Reasoner reasonerOfTracker = null;
        try {
            reasonerOfTracker = new Nar();
            ((Nar)reasonerOfTracker).narParameters.VOLUME = 0;
            ((Nar)reasonerOfTracker).narParameters.DURATION*=10;

            ((Nar)reasonerOfTracker).narParameters.DECISION_THRESHOLD = 0.45f; // make it more indeciscive and noisy because we need the noise
        } catch (Exception ex) {
            System.out.println(ex);
            System.exit(1);
        }

        tracker = new Tracker(reasonerOfTracker, null);
        tracker.posX = 30.0; // so it has a chance to catch the ball

        informer = new StaticInformer(reasoner);


        setupScene();

        size(1000, 1000);
        frameRate(fps);
        new NarSimpleGUI((Nar)reasoner);

        new NarSimpleGUI((Nar)reasonerOfTracker);

        temporalQa = new TemporalQa(reasoner);
        temporalQa.goalTerms.add(Inheritance.make(new SetExt(new Term("SELF1")), new SetInt(new Term("good"))));

        //informReasoner.temporalQa = temporalQa;

        informer.temporalQa = temporalQa;
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

            entities.add(batEntity);
        }

        {
            final double posX = 40.0;
            final double posY = 30.0;

            ballEntity = new Entity(entityID++, posX, posY, 0.0, 0.0, "ball");
            ballEntity.velocityX = -40.0 / slowdownFactor;
            ballEntity.velocityY = 14.0 / slowdownFactor; //23.7;

            ballEntity.renderable = new BallRenderComponent();
            ballEntity.behaviour = new BallBehaviour();
            ((BallBehaviour) ballEntity.behaviour).batEntity = batEntity;

            entities.add(ballEntity);
        }


        {
            Ops ops = new Ops();
            ops.batEntity = batEntity;
            ops.ballEntity = ballEntity;
            ops.pong = this;
            ops.consumer = reasoner;

            try {
                Operator opUp = new MethodInvocationOperator("^up", ops, ops.getClass().getMethod("up"), new Class[0]);
                reasoner.addPlugin(opUp);
                ((Nar) reasoner).memory.addOperator(opUp);

                Operator opDown = new MethodInvocationOperator("^down", ops, ops.getClass().getMethod("down"), new Class[0]);
                reasoner.addPlugin(opDown);
                ((Nar) reasoner).memory.addOperator(opDown);

                Operator opSel = new MethodInvocationOperator("^selectAxis", ops, ops.getClass().getMethod("selectAxis", String.class), new Class[]{String.class});
                reasoner.addPlugin(opSel);
                ((Nar) reasoner).memory.addOperator(opSel);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }


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
        String[] args2 = {"Pong2"};
        Pong2 mp = new Pong2();
        PApplet.runSketch(args2, mp);
    }


    TemporalQa temporalQa;

    Reasoner reasoner;
    int entityID = 1;

    List<Prediction> predictions = new ArrayList<>();
    List<Prediction> disappointments = new ArrayList<>();


    List<Entity> entities = new ArrayList<>();
    int t = 0;
    int t2 = 0;
    int oldT = 0;
    public static boolean showAnomalies = false;

    GridMapper mapper = new GridMapper();


    Entity ballEntity;
    Entity batEntity;

    Random rng = new Random();

    long timeoutForOps = 0;
    long timeoutForOpsEffective = 0;

    StaticInformer informer;

    double pseudoscore = 0.0;
    int emittedBalls = 0;

    int slowdownFactor = 1;


    final int fps = 60;

    // tracker which is used to track the position of the ball
    // TODO< implement very simple perception of ball >
    Tracker tracker;


    PixelScreen pixelScreen;

    // used for attention / object detection
    PixelScreen oldPixelScreen; // used for attention
    PixelScreen pixelScreenOn; // pixel screen for on switching pixels
    PixelScreen pixelScreenOff; // pixel screen for off switching pixels


    PatchTracker patchTracker = new PatchTracker();


    // used as a optimization - we need to avoid to add patches where patches are already located
    PixelScreen patchScreen;



    // configurable by ops
    public int perceptionAxis = 1; // id of axis used to compare distances




    public int batDirection = 0;

    // fovea
    public int foveaBigX = 1;
    public int foveaBigY = 0;

    public int foveaFineX = 0;
    public int foveaFineY = 0;


}
