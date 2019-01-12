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

// TODO< remove proto objects if they don't find their associated patches >

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
    long timeoutForOpsEffective = 0;

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

    // used for attention / object detection
    PixelScreen oldPixelScreen; // used for attention
    PixelScreen pixelScreenOn; // pixel screen for on switching pixels
    PixelScreen pixelScreenOff; // pixel screen for off switching pixels


    PatchTracker patchTracker = new PatchTracker();

    // protoobjects are used by higher level reasoning processes to identify and learn objects
    List<ProtoObject> protoObjects = new ArrayList<>();

    long protoObjectIdCounter = 1;

    // used as a optimization - we need to avoid to add patches where patches are already located
    PixelScreen patchScreen;



    // configurable by ops
    public int perceptionAxis = 1; // id of axis used to compare distances



    // identified bounding boxes (per frame)
    // used to create proto-objects
    public List<BoundingBox> boundingBoxes = new ArrayList<>();


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
            final double posX = 40.0;
            final double posY = 30.0;

            ballEntity = new Entity(entityID++, posX, posY, 0.0, 0.0, "ball");
            ballEntity.velocityX = -40.0 / slowdownFactor;
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
        PatchRecords.Patch patch = pixelScreen.genPatchAt(y, x, 5,5,patchIdCounter++);

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

        if (!patchTracker.checkOverlapsWithAny(trackingRecord)) { // don't add it if it overlapps completely with a known one
            patchTracker.trackingRecords.add(trackingRecord);

        }

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




    void updateProtoObjects2() {
        for (ProtoObject iProtoObject : protoObjects) {
            // search for closest patch which matches
            boolean foundBestPatch = false;
            double bestPatchX = -1;
            double bestPatchY = -1;

            for(Patch2Protoobject iPatch2po:patch2Protoobjects ) {
                if(iPatch2po.protoObjectTypeId==iProtoObject.associatedPatch2ProtoobjectId) {



                    for(PatchRecords.Patch iPatch:iPatch2po.patches) {
                        double minDist = 9999999999.0;

                        for(int dist=0; dist<8;dist++) {
                            for(int dx=-dist;dx<=dist;dx++) {
                                for(int dy=-dist;dy<=dist;dy++) {
                                    PatchRecords.Patch patchAt = pixelScreen.genPatchAt((int)iProtoObject.posY+dy, (int)iProtoObject.posX+dx,iPatch.retWidth(), iPatch.retHeight(), -1);

                                    double sim = PatchRecords.sdrSimSym(patchAt.retSdr(), iPatch.retSdr());

                                    double dist3 = Math.sqrt(dx*dx + dy*dy);

                                    if(sim < 0.5 && dist3 < minDist) {
                                        minDist = dist3;

                                        foundBestPatch = true;
                                        bestPatchX = iProtoObject.posX + dx;
                                        bestPatchY = iProtoObject.posY + dy;

                                        //System.out.println("X=" + (int)bestPatchX + " Y=" + (int)bestPatchY);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (foundBestPatch) {
                // update position
                iProtoObject.posX = bestPatchX;
                iProtoObject.posY = bestPatchY;
            }
            else {
                // remove
                iProtoObject.remove = true;
            }



        }


    }

    void tickProtoObjects() {
        for(final ProtoObject iProtoObject : protoObjects) {
            iProtoObject.age++;
        }
    }

    void tick() {
        tickProtoObjects();

        {
            pixelScreenOn.clear();
            pixelScreenOff.clear();
        }

        { // draw to virtual screen
            pixelScreen.clear();

            // ball
            pixelScreen.drawDot((int)(ballEntity.posX), (int)(ballEntity.posY));
            pixelScreen.drawDot((int)(ballEntity.posX+1), (int)(ballEntity.posY));
            pixelScreen.drawDot((int)(ballEntity.posX), (int)(ballEntity.posY+1));
            pixelScreen.drawDot((int)(ballEntity.posX+1), (int)(ballEntity.posY+1));


            // bat
            pixelScreen.drawDot((int)(batEntity.posX), (int)(batEntity.posY-1));
            pixelScreen.drawDot((int)(batEntity.posX), (int)(batEntity.posY-0));
            pixelScreen.drawDot((int)(batEntity.posX), (int)(batEntity.posY+1));
            pixelScreen.drawDot((int)(batEntity.posX), (int)(batEntity.posY+1));
            pixelScreen.drawDot((int)(batEntity.posX), (int)(batEntity.posY+2));
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

        // attention< > / object segmentation
        {
            for(int x=0;x<pixelScreen.retWidth();x++) {
                for (int y = 0; y < pixelScreen.retHeight(); y++) {
                    if (pixelScreen.arr[y][x] && !oldPixelScreen.arr[y][x]) {
                        pixelScreenOn.arr[y][x] = true;
                    }
                    else if (!pixelScreen.arr[y][x] && oldPixelScreen.arr[y][x]) {
                        pixelScreenOff.arr[y][x] = true;
                    }
                }
            }
        }

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
                    if(pixelScreen.arr[y][x] == oldPixelScreen.arr[y][x]) {
                        continue;
                    }

                    // inform nars when it turned on
                    /*if(pixelScreen.arr[y][x] == true) {
                        String str = "<(*, y" + y / 10 + ", x" + x / 10 + ") --> [on" + labelCounter + "]>. :|:";

                        h.put(str, str);

                        labelCounter++;
                    }*/

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

                /*
                if (true || perceptionAxis == 0) {
                    String narsese = "<x" + (int)(diffX / 10) + " --> [diffX]>. :|:";
                    informer2.addNarsese(narsese);
                }
                if(true || perceptionAxis == 1){
                    String narsese = "<y" + (int)(diffY / 10) + " --> [diffY]>. :|:";
                    informer2.addNarsese(narsese);
                }*/

                if (true) {
                    // image because we want to bias the system to the y difference
                    String narsese = "<{y" + (int)(diffY / 5) + "} --> (&/, [diffXYprod],_,"+ "{x" + (int)(diffX / 5) +"})>. :|:";
                    informer2.addNarsese(narsese);
                }


                //String narsese = "<{x" + (int)(diffX / 10) +",y" + (int)(diffY / 10) + ",xy" + (int)(diffX / 10) +"_" + (int)(diffY / 10) +"} --> [diff]>. :|:";
                //informer2.addNarsese(narsese);

                /*{
                    String narsese = "<x" + (int)(diffX / 10) +"_y" + (int)(diffY / 10) + " --> [diffXY]>. :|:";
                    informer2.addNarsese(narsese);
                }*/


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

        // heuristic for tight bounds of (proto)objects
        // idea here is to look only at on/off switching pixels and try to merge the bounding boxes
        {
            boundingBoxes.clear();

            for(int y=0;y<pixelScreen.retHeight();y++) {
                for (int x = 0; x < pixelScreen.retWidth(); x++) {
                    if(pixelScreenOn.arr[y][x]) {
                        // search for farest off

                        double farestOffPixelDist = 0;
                        int farestOffPixelX = -1;
                        int farestOffPixelY = -1;

                        for(int dist=6;dist>=0;dist--) {
                            for(int dx=-dist;dx<=dist;dx++) {
                                for(int dy=-dist;dy<=dist;dy++) {
                                    if (pixelScreenOff.readAt(y+dy,x+dx)) {
                                        double dist2 = Math.sqrt(dx*dx + dy*dy);
                                        if (dist2 > farestOffPixelDist) {
                                            farestOffPixelDist = dist2;
                                            farestOffPixelX = x + dx;
                                            farestOffPixelY = y + dy;
                                        }
                                    }
                                }
                            }
                        }

                        if (farestOffPixelDist > 0) {
                            createAndMergeBoundingBox(x, y, farestOffPixelX, farestOffPixelY);
                        }
                    }
                }
            }
        }

        // creating patch from bounding box if it doesn't exist
        {
            for(BoundingBox iBb : boundingBoxes) {
                int width = iBb.x1-iBb.x0 + 1;
                int height = iBb.y1 - iBb.y0 + 1;

                PatchRecords.Patch patch = new PatchRecords.Patch(width, height, patchIdCounter++);
                for(int dx=0;dx<width;dx++) {
                    for(int dy=0;dy<height;dy++) {
                        patch.arr[dy][dx] = pixelScreen.readAt(iBb.y0+ dy,iBb.x0+ dx);
                    }
                }

                // add if it doesn't exist
                boolean exist = null != patchRecords.querySdrMostSimiliarPatch(patch);
                if (!exist || patchRecords.resultSimilarity < 0.4f) {
                    patchRecords.addPatch(patch);
                }
            }
        }

        // assign/patch to closest proto-object or create proto-object from bounding object and patch
        {

            for(BoundingBox iBb : boundingBoxes) {
                int width = iBb.x1 - iBb.x0 + 1;
                int height = iBb.y1 - iBb.y0 + 1;

                double bbCenterX = iBb.x0 + width*0.5;
                double bbCenterY = iBb.y0 + height*0.5;

                // create patch from bounding box
                PatchRecords.Patch patch = new PatchRecords.Patch(width, height, patchIdCounter++);
                for (int dx = 0; dx < width; dx++) {
                    for (int dy = 0; dy < height; dy++) {
                        patch.arr[dy][dx] = pixelScreen.readAt(iBb.y0 + dy, iBb.x0 + dx);
                    }
                }

                // search for best known mapping from patch to type of proto-object
                Patch2Protoobject patch2po = tryFindPatch2Protoobject4Patch(patch);


                // TODO< try to find protoobject with same id of patch2po and update position if found >
                {
                    if (patch2po != null) {
                        double maxDistForClosestProtoObject = 10.0;

                        boolean found2 = false;

                        for(ProtoObject iPo:protoObjects) {
                            double diffX = iPo.posX - bbCenterX;
                            double diffY = iPo.posY - bbCenterY;
                            double dist = Math.sqrt(diffX*diffX + diffY*diffY);

                            // TODO< search cloest protoobject which matches >
                            if (iPo.associatedPatch2ProtoobjectId == patch2po.protoObjectTypeId && dist < maxDistForClosestProtoObject) {
                                iPo.posX = bbCenterX;
                                iPo.posY = bbCenterY;

                                found2 = true;
                            }
                        }

                        if(found2) {
                            continue;
                        }

                    }
                }


                // if not found then add patch to p2obj of cloest allowed proto-object >
                // TODO< add if not found part >

                ProtoObject foundClosestProtoObject = null;
                double cloestFoundPo = 999999999.0;
                double maxDist2Po = 15.0; // maximal distance to protoobject
                for(ProtoObject iPo:protoObjects) {
                    double distFromBb2PoX = Math.abs(bbCenterX - iPo.posX);
                    double distFromBb2PoY = Math.abs(bbCenterY - iPo.posY);

                    double dist = Math.sqrt(distFromBb2PoX*distFromBb2PoX + distFromBb2PoY*distFromBb2PoY);
                    if (dist < cloestFoundPo) {
                        cloestFoundPo = dist;
                        foundClosestProtoObject = iPo;
                    }
                }

                boolean foundClosestProtoObjectB = cloestFoundPo <= maxDist2Po;
                if (!foundClosestProtoObjectB) {
                    foundClosestProtoObject = null;
                }

                // commented because a protoobject has to search for it's cloest associated patches
                // update position
                //if (foundClosestProtoObject != null) {
                //    foundClosestProtoObject.posX = bbCenterX;
                //    foundClosestProtoObject.posY = bbCenterY;
                //}

                // add unknown new patch to group
                if (foundClosestProtoObject != null) {
                    double minPatchSimiliarity = 0.3;

                    Patch2Protoobject associatedPatch2Protoobj = derefPatch2Protoobj(foundClosestProtoObject.associatedPatch2ProtoobjectId);
                    boolean hasSimilarPatch = associatedPatch2Protoobj.hasSimilarPatch(patch);
                    if (!hasSimilarPatch) {
                        // add patch to it

                        System.out.println("#ppp="+associatedPatch2Protoobj.patches.size());

                        associatedPatch2Protoobj.patches.add(patch);
                    }
                }

                // create new proto object with new patch2po if nothing was found
                if (!foundClosestProtoObjectB) {
                    // * create patch2ob
                    Patch2Protoobject createdPatch2po = new Patch2Protoobject(patch2ProtoobjectsIdCounter++);
                    patch2Protoobjects.add(createdPatch2po);
                    createdPatch2po.patches.add(patch);

                    // create proto object
                    ProtoObject po = new ProtoObject(protoObjectIdCounter++);
                    protoObjects.add(po);
                    po.posX = bbCenterX;
                    po.posY = bbCenterY;
                    po.associatedPatch2ProtoobjectId = createdPatch2po.protoObjectTypeId;
                }
            }
        }



        //if (t%2==0) {
            patchTracker.frame(pixelScreen);
        //}

        //updateProtoObjects();
        updateProtoObjects2();
        removeProtoObjects();
        //removeOverlappingProtoObjects();
        //assignNewProtoObjects();



        if (t != oldT) {
            oldT = t;

            timeoutForOpsEffective++;
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


                if(timeoutForOps >= 50000) {
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

                    int chance = 6; // in percentage

                    if (timeoutForOpsEffective < 0) {
                        chance = 0; // disable if op which changed the world was done
                    }

                    if (rngValue2 < chance) {
                        //System.out.println("[d] FORCED random op");

                        int rngValue = rng.nextInt( 2);
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

                if (((BallBehaviour)ballEntity.behaviour).bouncedOfBat) {
                //if (absDiffY <= 13.0 && absDiffX <= 15.0) {
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

    private void removeProtoObjects() {
        for(int idx=protoObjects.size()-1;idx>=0;idx--) {
            if(protoObjects.get(idx).remove) {
                protoObjects.remove(idx);
            }
        }

    }

    private void createAndMergeBoundingBox(int x0, int y0, int x1, int y1) {
        if( x0 > x1 ) {
            int t = x0;
            x0 = x1;
            x1 = t;
        }

        if( y0 > y1 ) {
            int t = y0;
            y0 = y1;
            y1 = t;
        }



        // TODO< merge with existing bounding box >


        boundingBoxes.add(new BoundingBox(x0, y0, x1, y1));
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


        //for (Entity e : entities) {
        //    e.render(this);
        //}

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

                if (iTrackingRecord.absTimeSinceLastMove == 0) {
                    stroke(255.0f, 0,0, 0.8f * 255.0f);

                    line(0, -5, 0, 5);
                    line(-5, 0, 5, 0);
                }
                else if (false && iTrackingRecord.timeSinceLastMove < -420 && iTrackingRecord.wasMoving) {
                    stroke(0.2f * 255.0f, 0,0, 0.8f * 255.0f);


                    line(0, -5, 0, 5);
                    line(-5, 0, 5, 0);
                }
                else if (false) {
                    stroke(0, 0,0, 0.1f * 255.0f);


                    line(0, -5, 0, 5);
                    line(-5, 0, 5, 0);
                }


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

        // draw bounding-boxes (per frame)
        {
            if (boundingBoxes.size() == 2) {
                int here = 5;
            }

            for(final BoundingBox iBb: boundingBoxes) {
                color(255,0,0,127);
                fill(255, 0, 0, 0.0f);

                stroke(0, 0,255, 255.0f);

                rect(iBb.x0, iBb.y0, iBb.x1-iBb.x0+1, iBb.y1-iBb.y0+1);
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


    static class BoundingBox {
        public int x0;
        public int y0;
        public int x1;
        public int y1;
        public BoundingBox(int x0, int y0, int x1, int y1) {
            this.x0 = x0;
            this.y0 = y0;
            this.x1 = x1;
            this.y1 = y1;
        }
    }


    List<Patch2Protoobject> patch2Protoobjects = new ArrayList<>();
    long patch2ProtoobjectsIdCounter = 0;

    public Patch2Protoobject derefPatch2Protoobj(long protoObjectTypeId) {
        for(Patch2Protoobject i:patch2Protoobjects) {
            if(i.protoObjectTypeId==protoObjectTypeId) {
                return i;
            }
        }
        return null;
    }

    public Patch2Protoobject tryFindPatch2Protoobject4Patch(PatchRecords.Patch patch) {
        float comparisionThreshold = 0.4f;

        Patch2Protoobject best = null;
        PatchRecords.Patch bestPatch = null;
        double bestSimilarity = 0.0f;

        for(Patch2Protoobject ip2o:patch2Protoobjects) {

            for(PatchRecords.Patch iPatch:ip2o.patches) {
                if(iPatch.retHeight() != patch.retHeight()) {
                    continue;
                }
                if(iPatch.retWidth() != patch.retWidth()) {
                    continue;
                }

                double sim = PatchRecords.sdrSimSym(patch.retSdr(), iPatch.retSdr());
                if (sim > comparisionThreshold && sim > bestSimilarity) {
                    bestSimilarity = sim;
                    best = ip2o;
                    bestPatch = iPatch;
                }
            }
        }

        return best;
    }

    // used to find/identify proto objects based on patches
    static class Patch2Protoobject {
        List<PatchRecords.Patch> patches = new ArrayList<>();

        long protoObjectTypeId;

        public Patch2Protoobject(long protoObjectTypeId) {
            this.protoObjectTypeId = protoObjectTypeId;
        }

        public boolean hasSimilarPatch(PatchRecords.Patch patch) {
            double similarityThreshold = 0.4;

            for(PatchRecords.Patch iPatch:patches) {
                if (patch.retWidth() != iPatch.retWidth()) {
                    continue;
                }
                if (patch.retHeight() != iPatch.retHeight()) {
                    continue;
                }

                if( PatchRecords.sdrSimSym(patch.retSdr(), iPatch.retSdr()) < similarityThreshold ) {
                    return true;
                }
            }

            return false;
        }
    }
}
