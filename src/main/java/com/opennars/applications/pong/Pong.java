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
import org.opennars.main.Nar;
import org.opennars.middle.operatorreflection.MethodInvocationOperator;
import org.opennars.operator.Operator;
import processing.core.PApplet;
import processing.event.MouseEvent;

import java.util.*;

public class Pong extends PApplet {
    Reasoner reasoner;
    int entityID = 1;

    List<Prediction> predictions = new ArrayList<>();
    List<Prediction> disappointments = new ArrayList<>();


    List<Entity> entities = new ArrayList<>();
    int t = 0;
    int t2 = 0;
    int oldT = 0;
    public static boolean showAnomalies = false;

    int perception_update = 1;

    InformReasoner informReasoner = new InformReasoner();

    GridMapper mapper = new GridMapper();


    Entity ballEntity;
    Entity batEntity;

    Random rng = new Random();

    long timeoutForOps = 0;

    StaticInformer informer;

    StaticInformer informer2;

    double pseudoscore = 0.0;
    int emittedBalls = 0;

    int slowdownFactor = 1;


    final int fps = 60;

    // tracker which is used to track the position of the ball
    // TODO< implement very simple perception of ball >
    Tracker tracker;


    // used to keep track of snippets from the screen
    // will be used to identify objects
    PatchRecords patchRecords = new PatchRecords();

    long patchIdCounter = 1;

    PixelScreen pixelScreen;
    PixelScreen oldPixelScreen; // used for attention

    PatchTracker patchTracker = new PatchTracker();

    // protoobjects are used by higher level reasoning processes to identify and learn objects
    List<ProtoObject> protoObjects = new ArrayList<>();

    long protoObjectIdCounter = 1;

    // used as a optimization - we need to avoid to add patches where patches are already located
    PixelScreen patchScreen;



    // configurable by ops
    public int perceptionAxis = 1; // id of axis used to compare distances


    @Override
    public void setup() {
        { // pixel screen
            int pixelScreenWidth = 160;
            int pixelScreenHeight = 100;
            pixelScreen = new PixelScreen(pixelScreenWidth, pixelScreenHeight);

            oldPixelScreen = new PixelScreen(pixelScreenWidth, pixelScreenHeight);

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

        informer2 = new StaticInformer(reasoner);

        setupScene();

        size(1000, 1000);
        frameRate(fps);
        new NarSimpleGUI((Nar)reasoner);

        new NarSimpleGUI((Nar)reasonerOfTracker);
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
            ballEntity.velocityX = 40.0 / slowdownFactor;
            ballEntity.velocityY = 14.0 / slowdownFactor; //23.7;

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

                Operator opSel = new MethodInvocationOperator("^selectAxis", ops, ops.getClass().getMethod("selectAxis", String.class), new Class[]{String.class});
                reasoner.addPlugin(opSel);
                ((Nar) reasoner).memory.addOperator(opSel);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }

            // tell NARS that it has ops
            reasoner.addInput("(^up, {SELF})!");
            reasoner.addInput("(^down, {SELF})!");
        }
    }

    void samplePatchAtPosition(int x, int y) {

        if (patchScreen.arr[y][x]) {
            return; // we don't add a patch where a patch currently is!
        }


        // cut the hit patch
        PatchRecords.Patch patch = pixelScreen.genPatchAt(y, x, patchIdCounter++);

        // compare and get best match
        PatchRecords.Patch similarPatch = patchRecords.querySdrMostSimiliarPatch(patch);

        double threshold = 0.2;
        if (patchRecords.resultSimilarity > threshold) {
            // we identified the patch as a known patch

            if (false)   System.out.println("identified patch as known patch with id=" + Long.toString(similarPatch.id) + " at x=" + x + " y=" + y);

            double bestPatchSimilarity = patchRecords.resultSimilarity;

            int bestPatchPosX = x;
            int bestPatchPosY = y;

            // search better and better patches near it

            // TODO



        }
        else {
            // we identified the patch as a unknown patch

            System.out.println("identified patch as unknown patch");

            // TODO< send signal to attention system >

            // store patch because it is yet unknown
            patchRecords.patches.add(patch);


        }

        // and store as tracking record because we want to track it
        PatchTracker.TrackingRecord trackingRecord = new PatchTracker.TrackingRecord();
        trackingRecord.lastPosX = x;
        trackingRecord.lastPosY = y;
        trackingRecord.patch = patch;

        patchTracker.trackingRecords.add(trackingRecord);

    }

    void samplePatchAtRandomPosition() {
        int posX = rng.nextInt(pixelScreen.retWidth());
        int posY = rng.nextInt(pixelScreen.retHeight());

        samplePatchAtPosition(posX, posY);
    }

    void assignNewProtoObjects() {
        double protoObjectMaxDistanceToHotPatch = 10.0;

        // search closest active/hot tracking record and assign position to it
        for (final PatchTracker.TrackingRecord iTrackingRecord : patchTracker.trackingRecords) {
            boolean isHotEnough = true; //iTrackingRecord.timeSinceLastMove < -490;
            if (!isHotEnough) {
                continue;
            }

            // search closest proto object and assign position to it

            ProtoObject closest = null;
            double closestDistance = 999999999999999999999.0;

            for (ProtoObject iProtoObject : protoObjects) {
                double diffX = iProtoObject.posX - iTrackingRecord.lastPosX;
                double diffY = iProtoObject.posY - iTrackingRecord.lastPosY;
                double distance = Math.sqrt(diffX * diffX + diffY * diffY);

                if (distance > protoObjectMaxDistanceToHotPatch) {
                    continue;
                }

                if (distance < closestDistance) {
                    closest = iProtoObject;
                    closestDistance = distance;
                }
            }

            if (closest == null) {
                // we need to assign a new proto object to it

                //if (protoObjectIdCounter == 1) {
                    ProtoObject createdProtoObject = new ProtoObject(protoObjectIdCounter++);
                    createdProtoObject.posX = iTrackingRecord.lastPosX;
                    createdProtoObject.posY = iTrackingRecord.lastPosY;

                    System.out.println("ProtoObject system: created new proto-object with id=" + createdProtoObject.classificationId + "!");

                    protoObjects.add(createdProtoObject);
                //}

            }
        }
    }

    void removeOverlappingProtoObjects() {
        int iA = 0;
        for(ProtoObject iProtoObjectA : protoObjects) {
            int iB = 0;

            for(ProtoObject iProtoObjectB : protoObjects) {
                //if(iB <= iA) {
                //    continue;
                //}

                if (iProtoObjectA.equals(iProtoObjectB)) {
                    continue;
                }

                double mergeDistance = 6;

                double diffX = iProtoObjectA.posX - iProtoObjectB.posX;
                double diffY = iProtoObjectA.posY - iProtoObjectB.posY;
                double distance = Math.sqrt(diffX*diffX + diffY*diffY);

                if(distance < mergeDistance) {
                    if(iProtoObjectA.age > iProtoObjectB.age) {
                        iProtoObjectB.remove = true; // delete younger one
                    }
                    else if(iProtoObjectB.age > iProtoObjectA.age) {
                        iProtoObjectA.remove = true; // delete younger one
                    }
                }
            }

            iA++;
        }


        // and remove
        for(int idx=protoObjects.size()-1;idx>=0;idx--) {
            if(protoObjects.get(idx).remove) {
                protoObjects.remove(idx);
            }
        }
    }

    void updateProtoObjects() {
        double protoObjectMaxDistanceToHotPatch = 20.0;

        for (ProtoObject iProtoObject : protoObjects) {
            // search next hottest tracking record which is close enough and hot enough

            long hottestTime = 0;
            PatchTracker.TrackingRecord hottestCloseEnoughTrackingRecord = null;

            for (final PatchTracker.TrackingRecord iTrackingRecord : patchTracker.trackingRecords) {
                double diffX = iProtoObject.posX - iTrackingRecord.lastPosX;
                double diffY = iProtoObject.posY - iTrackingRecord.lastPosY;
                double distance = Math.sqrt(diffX*diffX + diffY*diffY);

                if (distance > protoObjectMaxDistanceToHotPatch) {
                    continue;
                }

                boolean isHotEnough = true; //iTrackingRecord.timeSinceLastMove < -400;
                if (!isHotEnough) {
                    continue;
                }

                if (iTrackingRecord.timeSinceLastMove > hottestTime) {
                    continue;
                }

                hottestTime = iTrackingRecord.timeSinceLastMove;
                hottestCloseEnoughTrackingRecord = iTrackingRecord;
            }

            // update if possible
            if (hottestCloseEnoughTrackingRecord != null) {
                iProtoObject.posX = hottestCloseEnoughTrackingRecord.lastPosX;
                iProtoObject.posY = hottestCloseEnoughTrackingRecord.lastPosY;

                // TODO< update update time >

            }
        }

        /*
        // search closest active/hot tracking record and assign position to it
        for (final PatchTracker.TrackingRecord iTrackingRecord : patchTracker.trackingRecords) {
            boolean isHotEnough = iTrackingRecord.timeSinceLastMove < -400;
            if (!isHotEnough) {
                continue;
            }

            // search closest proto object and assign position to it


            if (protoObjects.size() == 0) {
                continue;
            }

            ProtoObject closest = null;
            double closestDistance = 999999999999999999999.0;

            for (ProtoObject iProtoObject : protoObjects) {
                double diffX = iProtoObject.posX - iTrackingRecord.lastPosX;
                double diffY = iProtoObject.posY - iTrackingRecord.lastPosY;
                double distance = Math.sqrt(diffX*diffX + diffY*diffY);

                if (distance > protoObjectMaxDistanceToHotPatch) {
                    continue;
                }

                if (distance < closestDistance) {
                    closest = iProtoObject;
                    closestDistance = distance;
                }
            }

            if (closest != null) {
                closest.posX = iTrackingRecord.lastPosX;
                closest.posY = iTrackingRecord.lastPosY;
            }
        }
        */

    }

    void tickProtoObjects() {
        for(final ProtoObject iProtoObject : protoObjects) {
            iProtoObject.age++;
        }
    }

    void tick() {
        tickProtoObjects();

        { // draw to virtual screen
            pixelScreen.clear();

            // ball
            pixelScreen.drawDot((int)(ballEntity.posX), (int)(ballEntity.posY));
            //pixelScreen.drawDot((int)(ballEntity.posX+1), (int)(ballEntity.posY));
            //pixelScreen.drawDot((int)(ballEntity.posX), (int)(ballEntity.posY+1));
            //pixelScreen.drawDot((int)(ballEntity.posX+1), (int)(ballEntity.posY+1));


            // bat
            //pixelScreen.drawDot((int)(batEntity.posX), (int)(batEntity.posY-1));
            pixelScreen.drawDot((int)(batEntity.posX), (int)(batEntity.posY-0));
            //pixelScreen.drawDot((int)(batEntity.posX), (int)(batEntity.posY+1));
            //pixelScreen.drawDot((int)(batEntity.posX), (int)(batEntity.posY+1));
            //pixelScreen.drawDot((int)(batEntity.posX), (int)(batEntity.posY+2));
        }


        // we need to draw the positions of already existing patches
        {
            patchScreen.clear();

            for(PatchTracker.TrackingRecord iPatch : patchTracker.trackingRecords) {
                if (iPatch.lastPosX < 0 || iPatch.lastPosX >= patchScreen.retWidth() || iPatch.lastPosY < 0 || iPatch.lastPosY >= patchScreen.retHeight()) {
                    continue;
                }

                patchScreen.arr[iPatch.lastPosY][iPatch.lastPosX] = true;
            }
        }


        /* commented because we don't need this sampling anymore - because we just look at the delta
        if (t%2==0) {
            samplePatchAtRandomPosition();
            samplePatchAtRandomPosition();
            samplePatchAtRandomPosition();
        }
        */

        // attention< we need to bias our attention to the changes in the environment >
        {
            HashMap<String, String> h = new HashMap<>();

            int labelCounter = 0; // fine to count labels by x because pong is ordered by x

            for(int x=0;x<pixelScreen.retWidth();x++) {
                for(int y=0;y<pixelScreen.retHeight();y++) {



                    // we don't need to sample every pixel
                    //if ((x+y) % 2 == 0) {
                    //    continue;
                    //}




                    // ignore if no change
                    //if(pixelScreen.arr[y][x] == oldPixelScreen.arr[y][x]) {
                    //    continue;
                    //}

                    // inform nars when it turned on
                    if(pixelScreen.arr[y][x] == true) {
                        String str = "<(*, y" + y / 10 + ", x" + x / 10 + ") --> [on" + labelCounter + "]>. :|:";

                        h.put(str, str);

                        labelCounter++;
                    }

                    // spawn
                    //samplePatchAtPosition(x, y);

                    /*
                    PatchTracker.TrackingRecord r = new PatchTracker.TrackingRecord();
                    r.lastPosX = x;
                    r.lastPosY = y;
                    r.timeSinceLastMove = -1;

                    patchTracker.trackingRecords.add(r);
                    */
                }
            }


            // commented because we don't anymore inform NARS with labeled pixels
            //for(String i : h.keySet()) {
            //    informer2.addNarsese(i);
            //}

            {
                double diffX = batEntity.posX - ballEntity.posX;
                double diffY = batEntity.posY - ballEntity.posY;

                if (perceptionAxis == 0) {
                    String narsese = "<x" + (int)(diffX / 10) + " --> [diffX]>. :|:";
                    informer2.addNarsese(narsese);
                }
                else {
                    String narsese = "<y" + (int)(diffY / 10) + " --> [diffY]>. :|:";
                    informer2.addNarsese(narsese);
                }


            }

            informer2.informWhenNecessary(false);
        }

        // and update/copy
        {
            for(int y=0;y<pixelScreen.retHeight();y++) {
                for(int x=0;x<pixelScreen.retWidth();x++) {
                    oldPixelScreen.arr[y][x] = pixelScreen.arr[y][x];
                }
            }
        }

        if (t%2==0) {
            patchTracker.frame(pixelScreen);
        }

        updateProtoObjects();
        removeOverlappingProtoObjects();
        assignNewProtoObjects();



        if (t != oldT) {
            oldT = t;

            timeoutForOps++;

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
                    ballEntity.posX = 1.0;
                    ballEntity.posY = 1.0 + rng.nextDouble() * (80.0 - 2.0);

                    // choose random y velocity
                    for(;;) {
                        ballEntity.velocityY = ( rng.nextDouble() * 2.0 - 1.0 ) * 20.0;

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


            // inform
            if(false && t%4==0) {
                // heuristic
                //  we sort the proto-objects by the selected axis - which is hardcoded to sort by the x axis now

                Collections.sort(protoObjects, new Comparator<ProtoObject>() {
                    @Override
                    public int compare(ProtoObject o1, ProtoObject o2) {
                        if (o1.posX > o2.posX) {
                            return 1;
                        }
                        return -1;
                    }
                });


                int quantizedBallY = (int)(ballEntity.posY / 10.0);
                int quantizedBallX = (int)(ballEntity.posX / 10.0);

                int quantizedBatX = (int)(batEntity.posX / 10.0);
                int quantizedBatY = (int)(batEntity.posY / 10.0);

                List<String> innerNarsese = new ArrayList<>();
                for(ProtoObject iProtoObject : protoObjects) { // iterate in sorted proto-objects
                    int quantizedY = (int)(iProtoObject.posY / 10.0);

                    innerNarsese.add(Integer.toString(quantizedY));
                }


                if (protoObjects.size() == 0) {
                    String narsese = "<null --> [atTuple0]>. :|:";
                    informer.addNarsese(narsese);
                }

                if (protoObjects.size() == 1) {
                    String narsese = "<(*, " +  (int)(protoObjects.get(0).posY / 10.0) + ") --> [atTuple1]>" + ". :|:";
                    informer.addNarsese(narsese);
                }

                if (protoObjects.size() >= 2) {
                    String narsese = "<(*, " +  (int)(protoObjects.get(0).posY / 10.0) + "," +  (int)(protoObjects.get(1).posY / 10.0) + ") --> [atTuple2]>" + ". :|:";
                    informer.addNarsese(narsese);
                }

                /* skip this because it is noise and we want just to see if it works this way without it
                if (protoObjects.size() >= 3) {
                    String narsese = "<(*, " +  (int)(protoObjects.get(1).posY / 10.0) + "," +  (int)(protoObjects.get(2).posY / 10.0) + ") --> [atTuple3]>" + ". :|:";
                    informer.addNarsese(narsese);
                }

                if (protoObjects.size() >= 4) {
                    String narsese = "<(*, " +  (int)(protoObjects.get(2).posY / 10.0) + "," +  (int)(protoObjects.get(3).posY / 10.0) + ") --> [atTuple4]>" + ". :|:";
                    informer.addNarsese(narsese);
                }

                if (protoObjects.size() >= 5) {
                    String narsese = "<(*, " +  (int)(protoObjects.get(3).posY / 10.0) + "," +  (int)(protoObjects.get(4).posY / 10.0) + ") --> [atTuple5]>" + ". :|:";
                    informer.addNarsese(narsese);
                }
                */



                //informer.informWhenNecessary(false); // give chance to push collected narsese to narsese consumer(which is the Nar)
            }

            if(t%4==0) {
                reasoner.addInput("<{SELF} --> [good]>!");
            }


            if(t%8==0) {


                int explorativeTimeout = 600; // time after which a random op is injected when it didn't do anything sufficiently long


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

                    int chance = 13; // in percentage

                    if (rngValue2 < chance) {
                        //System.out.println("[d] FORCED random op");

                        int rngValue = rng.nextInt( 5);
                        //System.out.println(rngValue);
                        switch (rngValue) {
                            case 0:
                                reasoner.addInput("(^up, {SELF})!");
                                break;

                            case 1:
                                reasoner.addInput("(^down, {SELF})!");
                                break;
                            case 2:
                                reasoner.addInput("(^selectAxis, {SELF}, x)!");
                                break;
                            case 3:
                                reasoner.addInput("(^selectAxis, {SELF}, y)!");
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

            // inform the tracker about the perception
            {
                // inform it about the perception - which we fake by just comparing the positions
                // TODO< do real

                final double diffX = ballEntity.posX - tracker.posX;
                final double diffY = ballEntity.posY - tracker.posY;

                String code1 = "";


                String code2 = "";

                if (diffY > 5.0) {
                    code1 +="d"; // entity on the right of the tracked position
                }
                else if(diffY < -5.0) {
                    code1 += "u"; // entity on the left of the tracked position
                }


                if (diffX > 5.0) {
                    code1 +="r"; // entity on the right of the tracked position
                }
                else if(diffX < -5.0) {
                    code1 += "l"; // entity on the left of the tracked position
                }


                //if (code1.isEmpty()) {
                //    code1 = "c";
                //}




                if (code1.isEmpty()) {
                    code1 = "c";
                }

                //tracker.informAndStep(code1, code2);
            }
        }

        //if(t%2==0) {
        //
        //}


        t2++;
        t = t2/slowdownFactor;

        reasoner.cycles(30);
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

        if(false) {
            System.out.println("#patches= " + patchTracker.trackingRecords.size());
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



        // draw tracker
        /*{

            pushMatrix();
            translate((float)tracker.posX, (float)tracker.posY);

            fill(255, 0, 0, 0.0f);
            stroke(0, 0,0, (float)255.0f);

            line(0, -50, 0, 50);
            line(-50, 0, 50, 0);

            popMatrix();
            fill(0);
        }*/


        // draw tracked patches of patch-tracker
        {
            for(final PatchTracker.TrackingRecord iTrackingRecord: patchTracker.trackingRecords) {
                float posX = (float)iTrackingRecord.lastPosX;
                float posY = (float)iTrackingRecord.lastPosY;

                pushMatrix();
                translate((float)posX, (float)posY);
                rotate(45.0f);

                fill(255, 0, 0, 0.0f);

                if (iTrackingRecord.timeSinceLastMove < -480 && iTrackingRecord.wasMoving) {
                    stroke(255.0f, 0,0, 0.8f * 255.0f);
                }
                else if (iTrackingRecord.timeSinceLastMove < -420 && iTrackingRecord.wasMoving) {
                    stroke(0.2f * 255.0f, 0,0, 0.8f * 255.0f);
                }
                else {
                    stroke(0, 0,0, 0.1f * 255.0f);
                }

                line(0, -5, 0, 5);
                line(-5, 0, 5, 0);

                popMatrix();
                fill(0);
            }
        }


        // draw proto-objects
        {
            for(final ProtoObject iTrackingRecord: protoObjects) {
                float posX = (float)iTrackingRecord.posX;
                float posY = (float)iTrackingRecord.posY;

                /*
                pushMatrix();
                translate((float)posX, (float)posY);

                fill(255, 0, 0, 0.0f);

                stroke(0, 0,255, 255.0f);


                line(0, -25, 0, 25);
                line(-25, 0, 25, 0);

                popMatrix();
                */
                drawCursor(posX, posY, 0.0f);

                {
                    fill(0, 0, 0, 255.0f);
                    text("age=" + iTrackingRecord.age, posX, posY);
                }

                fill(0);
            }
        }

    }

    private void drawCursor(float posX, float posY, float innerWidth) {
        pushMatrix();
        translate(posX, posY);

        fill(255, 0, 0, 0.0f);

        stroke(0, 0,255, 255.0f);


        line(0, -25, 0, -innerWidth);
        line(0, innerWidth, 0, 25);

        line(-25, 0, -innerWidth, 0);
        line(innerWidth, 0, 25, 0);

        popMatrix();
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
