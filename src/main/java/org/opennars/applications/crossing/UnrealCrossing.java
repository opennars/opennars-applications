package org.opennars.applications.crossing;

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


// TODO< classify with the most recent NN and fall back to older ones if it doesn't know >

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.deeplearning4j.nn.api.Layer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.opennars.applications.crossing.NarListener.Prediction;
import org.opennars.applications.cv.*;
import org.opennars.applications.gui.NarSimpleGUI;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opennars.io.events.Events;
import org.opennars.io.events.OutputHandler.DISAPPOINT;
import org.opennars.main.Nar;
import processing.core.PApplet;
import processing.core.PImage;
import processing.event.MouseEvent;

public class UnrealCrossing extends PApplet {
    Nar nar;
    int entityID = 1;

    List<Prediction> predictions = new ArrayList<Prediction>();
    List<Prediction> disappointments = new ArrayList<Prediction>();
    final int streetWidth = 40;
    final int fps = 20;

    public PrototypeBasedImageSampler imageSampler = null;
    public AttentionField attentionField = null;


    @Override
    public void setup() {
        Camera cam = new Camera(500+streetWidth/2, 500+streetWidth/2);
        cam.radius = 600;
        cameras.add(cam);
        try {
            nar = new Nar();
            nar.narParameters.VOLUME = 0;
            nar.narParameters.DURATION*=10;
            NarListener listener = new NarListener(cameras.get(0), nar, predictions, disappointments, entities);
            nar.on(Events.TaskAdd.class, listener);
            nar.on(DISAPPOINT.class, listener);
        } catch (Exception ex) {
            System.out.println(ex);
            System.exit(1);
        }
        //int trafficLightRadius = 25;
        streets.add(new Street(false, 0, 500, 1000, 500 + streetWidth));
        streets.add(new Street(true, 500, 0, 500 + streetWidth, 1000));
        int trafficLightID = 1;
        //trafficLights.add(new TrafficLight(trafficLightID++, trafficLightRadius, 500 + streetWidth + trafficLightRadius, 500 + streetWidth/2, 0));
        //trafficLights.add(new TrafficLight(trafficLightID++, trafficLightRadius, 500 - trafficLightRadius, 500 + streetWidth/2, 0));
        //trafficLights.add(new TrafficLight(trafficLightID++, trafficLightRadius/2, 500 + streetWidth, 500 + streetWidth + trafficLightRadius, 1));
        //trafficLights.add(new TrafficLight(trafficLightID++, trafficLightRadius/2, 500, 500 - trafficLightRadius, 1));
        int cars = 4; //cars and pedestrians
        for (float i = 0; i < cars/2; i += 1.05) {
            entities.add(new Car(entityID++, 500 + streetWidth - Util.discretization+1, 900 - i * 100, 0.3, -PI / 2));
            entities.add(new Car(entityID++, 500 + Util.discretization, 900 - i * 100, 0.3, PI / 2));
        }
        int pedestrians = 4;//4;
        for (float i = 0; i < pedestrians/2; i += 1.05) {
            entities.add(new Pedestrian(entityID++, 900 - i * 100, 500 + streetWidth - Util.discretization, 0.3, 0));
            entities.add(new Pedestrian(entityID++, 900 - i * 100, 500 + Util.discretization, 0.3, -PI));
        }
        /*for (TrafficLight l : trafficLights) { //it can't move anyway, so why would the coordinates matter to NARS?
            String pos = Util.positionToTerm(l.posX, l.posY);
            String narsese = "<(*,{" + l.id + "}," + pos + ") --> at>.";
            nar.addInput(narsese);
        }*/

        size(1920, 1080);
        frameRate(fps);
        new NarSimpleGUI(nar);


        sdrAllocator = new SdrAllocator();
        sdrAllocator.sdrSize = 16000;
        sdrAllocator.sdrUsedBits = 5;

        layer1Classifier.minDistance = 1020.0f; // TODO< tune >

        foldImagesPerm = Sdr.createRandomPermutation(sdrAllocator.sdrSize, new Random()); // create permutation for folding of images

        convCl = new ConvCl();
    }

    List<Street> streets = new ArrayList<Street>();
    List<TrafficLight> trafficLights = new ArrayList<TrafficLight>();
    List<Entity> entities = new ArrayList<Entity>();
    List<Camera> cameras = new ArrayList<Camera>();

    List<DebugCursor> debugCursors = new ArrayList<>();

    int t = 0;
    public static boolean showAnomalies = false;

    String questions = "<trafficLight --> [?whatColor]>? :|:";
    int perception_update = 1;
    int i = 2;

    public String unwrap(String s) {
        return s.replace("[", "").replace("]", "");
    }

    public static String videopath="/mnt/sda1/Users/patha/Downloads/Test/Test/Test001/";
    public static String trackletpath = null; //"/home/tc/Dateien/CROSSING/Test001/";
    public static double movementThreshold = 10;

    public int heatmapCellsize = 16; // configuration

    SdrAllocator sdrAllocator;
    UlSdrProtoClassifier layer1Classifier = new UlSdrProtoClassifier();
    Random rng = new Random();

    // threshold for region proposal
    float regionProposalAttentionThreshold = 0.07f; // 0.1f was to less  was 0.2f which was to less

    // permutation used to "fold" images for layer2
    int[] foldImagesPerm;

    UlProtoClassifier objectPrototypeClassifier = new UlProtoClassifier();

    ConvCl convCl;




    ThreadPoolExecutor pool = (ThreadPoolExecutor)Executors.newFixedThreadPool(1);


    double spatialTrackletCatchDistance = 70.0; // configuration
    public int trackletMinimumTrainingSamples = 8; // configuration



    // counter for the positive classes for NN based classification
    public long nnPositiveClassCounter = 1;




    // array with all futures for NN training tasks
    public List<Future<NnTrainerRunner>> nnTrainingFutures = new ArrayList<>();



    List<SpatialTracklet> spatialTracklets = new ArrayList<>();
    long trackletIdCounter = 1;

    PImage lastframe2 = null;


    private float integrateImgGrayscalePixel(int posY, int posX, int size, int stride, PImage img) {
        int numberOfSamples = 0;

        float integral = 0;

        for(int dy=0;dy<size;dy+=stride) {
            for(int dx=0;dx<size;dx+=stride) {
                int ix = posX+dx;
                int iy = posY+dy;

                int colorcode =  img.pixels[iy*img.width+ix];
                //TODO check if the rgb is extracted correctly
                float r = (colorcode & 0xff) / 255.0f;
                float g = ((colorcode >> 8) & 0xFF) / 255.0f;
                float b = ((colorcode >> 8*2) & 0xFF) / 255.0f;

                float grayscale = (r+g+b)/3.0f;

                integral += grayscale;
                numberOfSamples++;
            }
        }

        return integral / numberOfSamples;
    }

    @Override
    public void draw() {


        viewport.Transform();
        background(64,128,64);
        fill(0);
        for (Street s : streets) {
            s.draw(this);
        }
        String nr = String.format("%05d", i);
        PImage img = loadImage(videopath+nr+".jpg"); //1 2 3 7
        image(img, 0, 0);


        short[] imgGrayscale = new short[img.height*img.width]; // flattened grayscale image
        for(int iy=0;iy<img.height;iy++) {
            for(int ix=0;ix<img.width;ix++) {

                int colorcode =  img.pixels[iy*img.width+ix];
                //TODO check if the rgb is extracted correctly
                float r = (colorcode & 0xff) / 255.0f;
                float g = ((colorcode >> 8) & 0xFF) / 255.0f;
                float b = ((colorcode >> 8*2) & 0xFF) / 255.0f;

                float grayscale = (r+g+b)/3.0f;

                //grayscaleImage.writeAtUnsafe(iy, ix, grayscale);

                imgGrayscale[iy*img.width + ix] = (short)(grayscale*255.0f);
            }
        }

        ConvCl.CachedImage cachedImage = new ConvCl.CachedImage();
        cachedImage.update(imgGrayscale, convCl.context); // send image to GPU



        if (attentionField == null) { // we need to allocate the attention field


            attentionField = new AttentionField(img.height / heatmapCellsize + 1, img.width / heatmapCellsize + 1);

            attentionField.decayFactor = 0.0f;
        }


        if (attentionField != null) { // if we compute the attention

            attentionField.decay();

            // commented because it doesn't work
            //attentionField.blur();
        }


        if (lastframe2 != null) {
            for(int iy=0;iy<attentionField.retHeight()-1;iy++) {
                for(int ix=0;ix<attentionField.retWidth()-1;ix++) {

                    float thisFrameGrayscale = integrateImgGrayscalePixel(iy*heatmapCellsize, ix*heatmapCellsize, heatmapCellsize, 2, img);
                    float lastFrameGrayscale = integrateImgGrayscalePixel(iy*heatmapCellsize, ix*heatmapCellsize, heatmapCellsize, 2, lastframe2);

                    float diff = lastFrameGrayscale - thisFrameGrayscale; // difference to last frame
                    float absDiff = Math.abs(diff);

                    attentionField.map.arr[iy*attentionField.retWidth() + ix] += absDiff;
                    attentionField.map.arr[iy*attentionField.retWidth() + ix] = Math.min(attentionField.map.arr[iy*attentionField.retWidth() + ix], 1.0f);
                }
            }


            // for testing of the kernel which we are using for filtering
            /*
            for(int iy=0;iy<32;iy++) {
                for (int ix = 0; ix < 32; ix++) {
                    float freq = 1.0f;
                    float delta = 0.8f;
                    float filterVal = Conv.calcKernel((float)(ix-16) / 16.0f, (float)(iy-16) / 16.0f, 1.0f, 0.0f, freq, delta);
                    attentionField.map.arr[iy][ix] = filterVal;
                }
            }
             */

        }


        int regionProposalWidth = 20;

        // region proposals for classification of potentially new objects from the last layer
        List<RegionProposal> regionProposals = new ArrayList<>();
        { // compute region proposals
            for(int iy=0;iy<attentionField.retHeight();iy++) {
                for(int ix=0;ix<attentionField.retWidth();ix++) {
                    if (attentionField.readAtUnbound(iy, ix) < regionProposalAttentionThreshold) {
                        continue;
                    }

                    int absX = ix*heatmapCellsize;
                    int absY = iy*heatmapCellsize;

                    RegionProposal rp = new RegionProposal(); // build region proposal
                    rp.minX = absX - regionProposalWidth;
                    rp.minY = absY - regionProposalWidth;

                    rp.maxX = absX + regionProposalWidth;
                    rp.maxY = absY + regionProposalWidth;

                    regionProposals.add(rp);
                }
            }
        }

        List<RegionProposal> bigRegions = new ArrayList<>();
        for(RegionProposal irp : regionProposals) {
            boolean merged = false;

            for(RegionProposal iBigRegion : bigRegions) {
                if (RegionProposal.checkOverlap(iBigRegion, irp)) {
                    iBigRegion.merge(irp);
                    merged = true;
                }
            }

            if (!merged) {
                bigRegions.add(irp.clone());
            }
        }

        regionProposals = bigRegions;

        // narrow region proposals again because we want to have tight bounds
        for(RegionProposal irp : regionProposals) {
            int width = irp.maxX-irp.minX;
            int height = irp.maxY-irp.minY;

            if (width > regionProposalWidth) {
                irp.maxX -= regionProposalWidth;
                irp.minX += regionProposalWidth;
            }

            if (height > regionProposalWidth) {
                irp.maxY -= regionProposalWidth;
                irp.minY += regionProposalWidth;
            }
        }


        debugCursors.clear();


        // increment idletime of tracklets
        for(SpatialTracklet iSt : spatialTracklets) {
            iSt.idletime++;
            iSt.notTrainedSince++;
        }

        // remove old tracklets
        for(int idx=spatialTracklets.size()-1;idx>=0;idx--) {
            if (spatialTracklets.get(idx).idletime > 40) {
                SpatialTracklet tracklet = spatialTracklets.get(idx);


                if( tracklet.trainingDataOfThisClass.size() > trackletMinimumTrainingSamples) {
                    // we store the samples from the tracklet as training data

                    List<float[]> trainingSamples = tracklet.trainingDataOfThisClass;
                    long class_ = tracklet.id; // simply store the tracklet id as the class

                    // store data
                    SampleData sd = new SampleData();
                    sd.class_ = class_;
                    sd.samples = trainingSamples;
                    nnSampleData.add(sd);
                }


                spatialTracklets.remove(idx);
            }
        }



        // debug region proposals
        boolean debugRegionProposals = true;
        if (debugRegionProposals) {
            for(RegionProposal iRp:regionProposals) {
                DebugCursor dc = new DebugCursor();
                dc.posX = iRp.minX;
                dc.posY = iRp.minY;
                dc.extendX = iRp.maxX - iRp.minX;
                dc.extendY = iRp.maxY - iRp.minY;
                debugCursors.add(dc);
            }
        }

        if (imageSampler != null) {




            // we need to map the heatmap with an "utility" function to prefer to sample moving regions over nonmoving ones
            Map2d remappedHeatmap = new Map2d(attentionField.map.retHeight(), attentionField.map.retWidth());
            for(int iy=0;iy<attentionField.map.retHeight();iy++) {
                for(int ix=0;ix<attentionField.map.retWidth();ix++) {
                    float v = attentionField.map.readAtSafe(iy, ix);
                    float remapped = v*v; // nonlinear function to prefer hot values
                    remappedHeatmap.writeAtSafe(iy,ix,remapped);
                }
            }

            imageSampler.heatmap = remappedHeatmap;
            imageSampler.heatmapCellsize = heatmapCellsize;

            // pull classifications from it
            List<PrototypeBasedImageSampler.Classification> classificationsOfLayer0 = imageSampler.sample(img);
            if (false) {
                for(PrototypeBasedImageSampler.Classification iClassification : classificationsOfLayer0) {
                    DebugCursor dc = new DebugCursor();
                    dc.posX = iClassification.posX;
                    dc.posY = iClassification.posY;
                    dc.text = "L0 class=" + Long.toString(iClassification.class_);

                    debugCursors.add(dc);
                }
            }

            /*
            boolean enableLayer1 = true;

            if (enableLayer1) {

                Map2d grayscaleImage = new Map2d(img.height, img.width);
                for(int iy=0;iy<img.height;iy++) {
                    for(int ix=0;ix<img.width;ix++) {

                        int colorcode =  img.pixels[iy*img.width+ix];
                        //TODO check if the rgb is extracted correctly
                        float r = (colorcode & 0xff) / 255.0f;
                        float g = ((colorcode >> 8) & 0xFF) / 255.0f;
                        float b = ((colorcode >> 8*2) & 0xFF) / 255.0f;

                        float grayscale = (r+g+b)/3.0f;

                        grayscaleImage.writeAtUnsafe(iy, ix, grayscale);
                    }
                }



                // draw classifications for processing with next layer
                for(RegionProposal iRegionProposal : regionProposals) {
                    // selected relative classifications from previous layer
                    List<PrototypeBasedImageSampler.Classification> selectedRelativeClassificationsForLayer1 = new ArrayList<>();

                    // top left corder of this classification for layer 1
                    int baseX = iRegionProposal.minX;
                    int baseY = iRegionProposal.minY;

                    // we need to classify spots of layer0 for this
                    List<PrototypeBasedImageSampler.Classification> classifcationsLayer0OfThisRegion = new ArrayList<>();
                    {
                        for(int dy=0;dy < iRegionProposal.maxY-iRegionProposal.minY; dy += 5) {
                            for(int dx=0;dx < iRegionProposal.maxX-iRegionProposal.minX; dx += 5) {
                                int posX = iRegionProposal.minX + dx;
                                int posY = iRegionProposal.minY + dy;

                                int prototypeSize = 16; // size of the prototype
                                int stride = 4;

                                float[] convResult = Conv.convAt(grayscaleImage, posX, posY, prototypeSize, stride);

                                long classification = imageSampler.classifier.classify(convResult);

                                //System.out.println("[d ] classification = " + Long.toString(classification));

                                {
                                    PrototypeBasedImageSampler.Classification cl = new PrototypeBasedImageSampler.Classification();
                                    cl.posX = posX;
                                    cl.posY = posY;
                                    cl.class_ = classification;
                                    classifcationsLayer0OfThisRegion.add(cl);
                                }
                            }
                        }


                    }



                    for(PrototypeBasedImageSampler.Classification iClassification :classifcationsLayer0OfThisRegion ) {
                        if (iClassification.posX > iRegionProposal.maxX || iClassification.posX < iRegionProposal.minX) {
                            continue;
                        }
                        if (iClassification.posY > iRegionProposal.maxY || iClassification.posY < iRegionProposal.minY) {
                            continue;
                        }

                        PrototypeBasedImageSampler.Classification relativeClassification = new PrototypeBasedImageSampler.Classification();
                        relativeClassification.class_ = iClassification.class_;
                        relativeClassification.posX = iClassification.posX - baseX;
                        relativeClassification.posX = iClassification.posX - baseY;
                        selectedRelativeClassificationsForLayer1.add(relativeClassification);
                    }

                    int sdrImageWidth = 32;
                    int sdrImageHeight = 32;

                    int sdrImageCellsize = 6;

                    Sdr[][] sdrImg = new Sdr[sdrImageHeight][sdrImageWidth];
                    for(int j=0;j<sdrImg.length;j++) {
                        for(int i=0;i<sdrImageWidth;i++) {
                            sdrImg[j][i] = Sdr.makeNull(sdrAllocator.sdrSize);
                        }
                    }

                    // draw to sdrImg
                    for(PrototypeBasedImageSampler.Classification iClassification :selectedRelativeClassificationsForLayer1) {
                        Sdr sdrColor = sdrAllocator.retSdrById(iClassification.class_);
                        int radius = 5;
                        SdrDraw.drawConeCircle(sdrImg, sdrColor, iClassification.posX/sdrImageCellsize, iClassification.posY/sdrImageCellsize, radius, rng);
                    }

                    // classify with prototype classifier
                    int sizeOfSdr = sdrImg[0][0].arr.length;
                    Sdr foldedSdr = Sdr.makeNull(sizeOfSdr);
                    {
                        for(int j=0;j<sdrImg.length;j++) {
                            for(int i=0;i<sdrImg[j].length;i++) {
                                Sdr pixelSdr = sdrImg[j][i];

                                Sdr permutedFoldedSdr = foldedSdr.permutate(foldImagesPerm);
                                foldedSdr = Sdr.union(permutedFoldedSdr, pixelSdr);
                            }
                        }
                    }
                    long layer1Classification = layer1Classifier.classify(foldedSdr);

                    System.out.println("[d ] layer1 class=" + Long.toString(layer1Classification));

                    DebugCursor dc = new DebugCursor();
                    dc.posX = (iRegionProposal.minX+iRegionProposal.maxX)/2;
                    dc.posY = (iRegionProposal.minY+iRegionProposal.maxY)/2;
                    dc.text = "L1 class=" + Long.toString(layer1Classification);

                    debugCursors.add(dc);
                }






            }
            */

            { // feed events of big enough region proposals to NARS
                entities.clear();

                for(RegionProposal iRegionProposal : regionProposals) {
                    int width = iRegionProposal.maxX - iRegionProposal.minX;
                    int height = iRegionProposal.maxY - iRegionProposal.minY;

                    //System.out.println("[d ] width=" + Integer.toString(iRegionProposal.maxX - iRegionProposal.minX) + " height=" + Integer.toString(iRegionProposal.maxY - iRegionProposal.minY));

                    if (width < 80 && height < 80) {
                        continue; // we are just interested in cars
                    }

                    int id = 0; // we don't know the ID because classification isn't working

                    //entities.add(new Car(id, iRegionProposal.minX + width/2, iRegionProposal.minY + height/2, 0, 0));
                }



                i++;

                if (t % perception_update == 0) {
                    boolean hadInput = false;
                    for(Camera c : cameras) {
                        final boolean force = false; // not required HACK
                        hadInput = hadInput || c.see(nar, entities, trafficLights, force);
                    }
                    if(hadInput) {
                        nar.addInput(questions);
                    }
                }
            }





            { // classification with one layer of Prototypes
                objectPrototypeClassifier.minDistance = 7000.0f; //20.0f; //10.0f;



                // manage and recatch spatial tracklets
                for(RegionProposal iRegionProposal : regionProposals) {
                    int width = iRegionProposal.maxX - iRegionProposal.minX;
                    int height = iRegionProposal.maxY - iRegionProposal.minY;

                    int centerX = iRegionProposal.minX + width / 2;
                    int centerY = iRegionProposal.minY + height / 2;


                    boolean wasAnyTrackletRecaptured = false;
                    for(SpatialTracklet iTracklet : spatialTracklets) {
                        double diffX = iTracklet.posX - centerX;
                        double diffY = iTracklet.posY - centerY;
                        double dist = Math.sqrt(diffX*diffX+diffY*diffY);
                        boolean inDist = dist < spatialTrackletCatchDistance;
                        if (inDist) {
                            wasAnyTrackletRecaptured = true;
                            // capture
                            iTracklet.posX = centerX;
                            iTracklet.posY = centerY;
                            iTracklet.idletime = 0;
                            break;
                        }

                    }

                    if (!wasAnyTrackletRecaptured) {
                        // spawn new tracklet
                        SpatialTracklet st = new SpatialTracklet(centerX, centerY, trackletIdCounter);
                        trackletIdCounter++;
                        spatialTracklets.add(st);
                    }

                }



                for(RegionProposal iRegionProposal : regionProposals) {
                    int width = iRegionProposal.maxX-iRegionProposal.minX;
                    int height = iRegionProposal.maxY-iRegionProposal.minY;

                    if(false)   System.out.println("[d ] width="+Integer.toString(iRegionProposal.maxX-iRegionProposal.minX) + " height="+Integer.toString(iRegionProposal.maxY-iRegionProposal.minY));

                    if (width<80 && height<80) {
                        continue; // we are just interested in cars
                    }

                    int centerX = iRegionProposal.minX + 128/2;
                    int centerY = iRegionProposal.minY + 128/2;

                    float[] convResult = convolutionImg(img, cachedImage, centerX, centerY);

                    /*{

                        List<NnPrototypeTrainer.TrainingTuple> trainingTuples = new ArrayList<>();
                        NnPrototypeTrainer.TrainingTuple trainingTuple = new NnPrototypeTrainer.TrainingTuple();
                        trainingTuple.input = convResult;
                        trainingTuple.class_ = 0;
                        trainingTuples.add(trainingTuple);

                        NnPrototypeTrainer.trainModel(trainingTuples);
                    }*/


                    //long classification = objectPrototypeClassifier.classify(convResult);

                    //System.out.println("[d ] obj classification = " + Long.toString(classification));



                    //DebugCursor dc = new DebugCursor();
                    //dc.posX = (iRegionProposal.minX+iRegionProposal.maxX)/2;
                    //dc.posY = (iRegionProposal.minY+iRegionProposal.maxY)/2;
                    //dc.text = "OBJ class=" + Long.toString(classification);

                    //debugCursors.add(dc);


                    // search best tracklet - the one which is in the region and has the most training samples to add it
                    SpatialTracklet bestTracklet = null;
                    for(SpatialTracklet iSt : spatialTracklets) {
                        double diffX = iSt.posX - centerX;
                        double diffY = iSt.posY - centerY;

                        boolean isInBound = Math.abs(diffX) < width/2 && Math.abs(diffY) < height/2;
                        if(!isInBound) {
                            continue;
                        }

                        if(bestTracklet == null) {
                            bestTracklet = iSt;
                            continue;
                        }

                        if(iSt.trainingDataOfThisClass.size() > bestTracklet.trainingDataOfThisClass.size()) {
                            bestTracklet = iSt;
                        }
                    }

                    if (bestTracklet != null) {
                        bestTracklet.trainingDataOfThisClass.add(convResult);
                    }

                }

            }








        }

        entities.clear(); //refresh
        String tracklets = "";
        if (trackletpath != null) { // use tracklets with file based provider?
            try {
                ///home/tc/Dateien/CROSSING/Test001/TKL00342.txt
                tracklets = new String(Files.readAllBytes(Paths.get(trackletpath+"TKL"+nr+".txt")));
            } catch (IOException ex) {
                Logger.getLogger(RealCrossing.class.getName()).log(Level.SEVERE, null, ex);
            }


            String[] lines = tracklets.replace("[ ","[").replace(" ]","]").replace("  "," ").replace("  "," ").split("\n");
            for(String s : lines) {
                //ClassID TrackID : [X5c Y5c W5 H5]
                String[] props = s.split(" ");
                Integer id = Integer.valueOf(props[1]);
                id = 0; //treat them as same for now, but distinguished by type!
                if(unwrap(props[3]).equals("")) {
                    System.out.println(s);
                }
                Integer X = Integer.valueOf(unwrap(props[3]));
                Integer Y = Integer.valueOf(unwrap(props[4]));

                Integer X2 = Integer.valueOf(unwrap(props[19])); //7 6
                Integer Y2 = Integer.valueOf(unwrap(props[20]));


                //use an id according to movement direction
                if(X < X2) {
                    id += 10;
                }
                if(Y < Y2) {
                    id += 1;
                }

                if(Math.sqrt((X-X2)*(X-X2) + (Y - Y2)*(Y - Y2)) < ((double)Util.discretization)/((double)movementThreshold)) {
                    continue;
                }

                if(props[0].equals("0")) { //person or vehicle for now, TODO make car motorcycle distinction
                    entities.add(new Car(id, X, Y, 0, 0));
                } else {
                    entities.add(new Pedestrian(id, X, Y, 0, 0));
                }

            }
        }


        i++;

        if (t % perception_update == 0) {
            boolean hadInput = false;
            for(Camera c : cameras) {
                final boolean force = false; // not required HACK
                hadInput = hadInput || c.see(nar, entities, trafficLights, force);
            }
            if(hadInput) {
                nar.addInput(questions);
            }
        }

        Entity.DrawDirection = false;
        Entity.DrawID = true;
        for (int i = 0; i < 3000; i += Util.discretization) {
            stroke(128);
            line(0, i, 3000, i);
            line(i, 0, i, 3000);
        }

        for (Entity e : entities) {
            e.draw(this, streets, trafficLights, entities, null, 0);
        }
        for (TrafficLight tl : trafficLights) {
            tl.draw(this, t);
        }




        // tick
        for (Entity ie : entities) {
            ie.tick();
        }


        t++;
        nar.cycles(10);
        removeOutdatedPredictions(predictions);
        removeOutdatedPredictions(disappointments);
        for (Prediction pred : predictions) {
            Entity e = pred.ent;
            e.draw(this, streets, trafficLights, entities, pred.truth, pred.time - nar.time());
        }
        if(showAnomalies) {
            for (Prediction pred : disappointments) {
                Entity e = pred.ent;
                if(e instanceof Car) {
                    fill(255,0,0);
                }
                if(e instanceof Pedestrian) {
                    fill(0,0,255);
                }
                this.text("ANOMALY", (float)e.posX, (float)e.posY);
                e.draw(this, streets, trafficLights, entities, pred.truth, pred.time - nar.time());
            }
        }

        if (attentionField != null) { // we want to draw the attention field

            for(int iy=0;iy<attentionField.retHeight();iy++) {
                for(int ix=0;ix<attentionField.retWidth();ix++) {
                    float attentionValue = attentionField.readAtUnbound(iy, ix);
                    if (attentionValue > 0.02f) {
                        float displayedTransparency = attentionValue * 10.0f; // multiply to see even small changes
                        displayedTransparency = Math.min(displayedTransparency, 1.0f); // limit for display

                        displayedTransparency *= 0.3f; // soften for better display

                        stroke(0,0,0,0); // transparent frame
                        fill(255, 0, 0, (int)(displayedTransparency * 255.0f));
                        rect(ix * heatmapCellsize, iy * heatmapCellsize, heatmapCellsize, heatmapCellsize);
                    }
                }
            }

            stroke(127); // set back to standard


        }

        // classify
        for(SpatialTracklet iSt : spatialTracklets) {
            if (iSt.idletime > 5) {
                continue; // we don't care about things which didn't move
            }

            //System.out.println("---");

            long bestClassificationClass = -1;
            double bestClassificationProbability = 0;

            float[] convResult = convolutionImg(img, cachedImage, (int)iSt.posX, (int)iSt.posY);

            for(TrainedNn iTrainedNn : trainedNns) {

                INDArray arr = Nd4j.create(convResult);
                INDArray result = iTrainedNn.network.activate(arr, Layer.TrainingMode.TEST);

                int highestPositiveClassIdx = -1;
                double highestPositiveClasssProbability = 0;

                for(int idx=0;idx<iTrainedNn.positiveClasses.size();idx++) { // iterate over classes
                    double positiveClassificationProbability = result.getDouble(idx);

                    if (positiveClassificationProbability > highestPositiveClasssProbability) {
                        highestPositiveClassIdx = idx;
                        highestPositiveClasssProbability = positiveClassificationProbability;
                    }
                }

                double negativeClassProbability = result.getDouble(iTrainedNn.positiveClasses.size());

                if (negativeClassProbability > highestPositiveClasssProbability) {
                    // negative class was stronger
                    highestPositiveClassIdx = -1;
                    highestPositiveClasssProbability = 0;
                }

                if (highestPositiveClassIdx != -1) {
                    if(false)  System.out.println("[d 5] cls=" +iTrainedNn.positiveClasses.get(highestPositiveClassIdx)+ "   classification{"+highestPositiveClassIdx+"}=" + Double.toString(highestPositiveClasssProbability));
                }

                if (highestPositiveClassIdx != -1 && highestPositiveClasssProbability > bestClassificationProbability) {
                    bestClassificationClass = iTrainedNn.positiveClasses.get(highestPositiveClassIdx); // retrieve class by index. Indirection is necessary for NN reasons
                    bestClassificationProbability = highestPositiveClasssProbability;
                }



                int here = 5;
            }

            { // add debug cursor
                if (bestClassificationClass != -1) {
                    DebugCursor dc = new DebugCursor();
                    // TODO< fetch size from applied NN >
                    dc.posX = iSt.posX - 60;
                    dc.posY = iSt.posY - 60;
                    dc.extendX = 120;
                    dc.extendY = 120;
                    dc.hasTextBackground = true; // because we want to see the text clearly
                    dc.text  = "CLASS "+Long.toString(bestClassificationClass) + " prop=" +  String.format("%.2f", (bestClassificationProbability));
                    debugCursors.add(dc);
                }
            }
        }


        for(SpatialTracklet ist : spatialTracklets) {
            int posX = (int)ist.posX;
            int posY = (int)ist.posY;

            int crossRadius = 30;

            boolean isRecent = ist.idletime < 5;

            stroke(255,0,0, isRecent ? 255 : 80);
            strokeWeight(2.0f);


            line(posX-crossRadius,posY-crossRadius,posX+crossRadius,posY+crossRadius);
            line(posX+crossRadius,posY-crossRadius,posX-crossRadius,posY+crossRadius);

            fill(255,0,0);
            text("st id="+Long.toString(ist.id) + " cnt=" +Integer.toString(ist.trainingDataOfThisClass.size()), (float)ist.posX, (float)ist.posY);
        }



        for(SpatialTracklet iSt : spatialTracklets) {
            if (iSt.trainingDataOfThisClass.size() < 8 || iSt.notTrainedSince < 500) {
                continue;
            }

            iSt.notTrainedSince = 0;


            List<NnPrototypeTrainer.TrainingTuple> trainingTuples = new ArrayList<>();

            int iPositiveClassId = 0;

            List<Long> nnPositiveClasses = new ArrayList<>(); // real classes of the positive classes
            nnPositiveClasses.add(iSt.id); // add the id as the positive class

            // add positives to training
            for(float[] iPositiveSample : iSt.trainingDataOfThisClass) {
                NnPrototypeTrainer.TrainingTuple createdTrainingTuple = new NnPrototypeTrainer.TrainingTuple();
                createdTrainingTuple.input = iPositiveSample;
                createdTrainingTuple.class_ = iPositiveClassId; // positive sample class
                trainingTuples.add(createdTrainingTuple);
            }

            int maxCountOfOtherClasses = 15; // configuration - how many other classes should be used for training

            if (nnSampleData.size() > 0) {


                List<Integer> candidateIndices = new ArrayList<>();
                for(int idx=0;idx<nnSampleData.size();idx++) { // loop to add candidate indices
                    if (nnSampleData.get(idx).class_ != iSt.id) { // class must be different - this may be always true
                        candidateIndices.add(idx);
                    }
                }

                System.out.println("DBG "+Integer.toString(candidateIndices.size()));

                List<Integer> chosenIndices = new ArrayList<>();
                for(int i=0;i<maxCountOfOtherClasses;i++) { // loop to chose indices
                    if (candidateIndices.size() == 0) {
                        break;
                    }

                    int idxidx = rng.nextInt(candidateIndices.size());
                    int idx = candidateIndices.get(idxidx);

                    candidateIndices.remove(idxidx);

                    chosenIndices.add(idx);
                }

                for(int iChosenIdx : chosenIndices) {
                    SampleData chosenSampleData = nnSampleData.get(iChosenIdx);

                    nnPositiveClasses.add(chosenSampleData.class_); // add the class as the positive class

                    for(float[] iPositiveSample : chosenSampleData.samples) {
                        NnPrototypeTrainer.TrainingTuple createdTrainingTuple = new NnPrototypeTrainer.TrainingTuple();
                        createdTrainingTuple.input = iPositiveSample;
                        createdTrainingTuple.class_ = iPositiveClassId; // positive sample class
                        trainingTuples.add(createdTrainingTuple);
                    }

                    iPositiveClassId++;
                }
            }


            int negativeClassId = iPositiveClassId+1;// allocate a class for the negative class

            // add negatives to training by sampling random positions in the image
            // TODO< ensure that the image is different by computing the distance of the samples and checking it >

            // TODO< store a global collection of negative images >

            int nnClassifierNumberOfNegativeSamples = (int)(1.5*trainingTuples.size());
            nnClassifierNumberOfNegativeSamples = Math.min(nnClassifierNumberOfNegativeSamples, 100);

            for(int iSampleCounter=0;iSampleCounter<nnClassifierNumberOfNegativeSamples;iSampleCounter++) {
                int samplePosX = rng.nextInt(img.width - 64)+64;
                int samplePosY = rng.nextInt(img.height - 64)+64;

                float[] negativeSampleVector = convolutionImg(img, cachedImage, samplePosX, samplePosY);

                NnPrototypeTrainer.TrainingTuple createdTrainingTuple = new NnPrototypeTrainer.TrainingTuple();
                createdTrainingTuple.input = negativeSampleVector;
                createdTrainingTuple.class_ = negativeClassId; // negative sample class
                trainingTuples.add(createdTrainingTuple);
            }

            int trainedNumberOfClasses = negativeClassId+1;


            enqueuedTrainingRunner.clear(); // flush because all others got outdated now

            // send to pool for async training
            NnTrainerRunner nnTrainingRunner = new NnTrainerRunner(trainingTuples);
            nnTrainingRunner.positiveClasses = nnPositiveClasses; // set the classes for which it is training for
            enqueuedTrainingRunner.add(nnTrainingRunner);

            /*
            System.out.println("[d 2] queue training of NN with #classes=" + Integer.toString(trainedNumberOfClasses));


            // send to pool for async training
            NnTrainerRunner nnTrainingRunner = new NnTrainerRunner(trainingTuples);
            nnTrainingRunner.positiveClasses = nnPositiveClasses; // set the classes for which it is training for
            Future<NnTrainerRunner> trainingtaskFuture = pool.submit(nnTrainingRunner);
            nnTrainingFutures.add(trainingtaskFuture);

            System.out.println("[i 1] queue taskCount ="+Long.toString(pool.getTaskCount()));

            int here = 5;
            */
        }

        // look for completed training of NN's and store them
        for(int idx=nnTrainingFutures.size()-1;idx>=0;idx--) {
            if (nnTrainingFutures.get(idx).isDone()) { // training is done
                // store into specific class for trained Nn together with the class for which it was trained for
                TrainedNn trainedNn = new TrainedNn();
                try {
                    NnTrainerRunner trainerRunner = nnTrainingFutures.get(idx).get();

                    // try to kick out old NN's which classify all classes
                    for(int oldNnIdx=trainedNns.size()-1;oldNnIdx>=0;oldNnIdx--) {
                        boolean oldNnClassifiesAllClasses = trainerRunner.positiveClasses.containsAll(trainedNns.get(oldNnIdx).positiveClasses);
                        if (oldNnClassifiesAllClasses) {


                            trainedNns.remove(oldNnIdx);
                        }

                    }

                    trainedNn.network = trainerRunner.trainer.network;
                    trainedNn.positiveClasses = trainerRunner.positiveClasses;
                    trainedNns.add(trainedNn);

                    System.out.println("[d 1] # trained NN's="+Long.toString(trainedNns.size()));
                } catch (InterruptedException e) {
                    //
                    int here = 5;
                } catch (ExecutionException e) {
                    //
                    int here = 5;
                }

                nnTrainingFutures.remove(idx);
            }
        }


        { // logic to fill the pool for training if it is empty

            if (pool.getActiveCount() == 0 && enqueuedTrainingRunner.size() > 0) {
                // pick first and train it
                NnTrainerRunner firstEneuqued = enqueuedTrainingRunner.get(0);
                enqueuedTrainingRunner.remove(0);

                // send to pool for async training
                Future<NnTrainerRunner> trainingtaskFuture = pool.submit(firstEneuqued);
                nnTrainingFutures.add(trainingtaskFuture);
            }

            //System.out.println("[i 1] queue taskCount ="+Long.toString(pool.getTaskCount()));

            int here = 5;
        }



        for (DebugCursor iDebugCursor : debugCursors) {
            boolean hasExtend = iDebugCursor.extendX != 0 || iDebugCursor.extendY != 0;

            if (hasExtend) {
                // set back to patrick standard
                stroke(128);
                strokeWeight(1.0f);

                fill(0, 0, 255, 50);
                rect((int)iDebugCursor.posX, (int)iDebugCursor.posY, (int)(iDebugCursor.extendX), (int)(iDebugCursor.extendY));
            }
            else
            {
                line((int)(iDebugCursor.posX - 5), (int)iDebugCursor.posY, (int)(iDebugCursor.posX + 5), (int)iDebugCursor.posY);
                line((int)iDebugCursor.posX, (int)(iDebugCursor.posY - 5), (int)iDebugCursor.posX, (int)(iDebugCursor.posY + 5));
            }

            if (iDebugCursor.hasTextBackground) {
                // draw black background for text
                fill(0,0,0);
                stroke(0,0,0,0); // transparent
                rect((int)iDebugCursor.posX, (int)iDebugCursor.posY, (int)(iDebugCursor.extendX), (int)(20));
            }

            fill(255);
            text(iDebugCursor.text, (float)iDebugCursor.posX, (float)iDebugCursor.posY+20-5);
        }




        lastframe2 = img; // store last frame for attention and so on

        // set back to patrick standard
        stroke(128);
        strokeWeight(1.0f);


        //System.out.println("[d 1] Concepts: " + nar.memory.concepts.size());
    }



    // list of all classes we are using for training of the NN
    List<SampleData> nnSampleData = new ArrayList<>();

    // training data of a classification which we remember
    private static class SampleData {
        public long class_;
        public List<float[]> samples;
    }


    // all trained NN's used for identification
    List<TrainedNn> trainedNns = new ArrayList<>();

    private static class TrainedNn {
        // metadata
        public List<Long> positiveClasses = new ArrayList<>(); // ids of the positive class for which the NN is trained for

        public MultiLayerNetwork network;
    }

    List<NnTrainerRunner> enqueuedTrainingRunner = new ArrayList<>();

    private static class NnTrainerRunner implements Callable<NnTrainerRunner> {
        private final List<NnPrototypeTrainer.TrainingTuple> trainingTuples;

        // metadata
        public List<Long> positiveClasses = new ArrayList<>(); // ids of the positive class for which the NN is trained for

        public NnPrototypeTrainer trainer;


        public NnTrainerRunner(List<NnPrototypeTrainer.TrainingTuple> trainingTuples) {
            this.trainingTuples = trainingTuples;
        }

        @Override
        public NnTrainerRunner call() throws Exception {
            trainer = new NnPrototypeTrainer();
            trainer.nEpochs = 30;

            trainer.trainModel(trainingTuples);

            return this;
        }
    }

    // convolute image and return the flattened array
    private float[] convolutionImg(PImage img, ConvCl.CachedImage cachedImage, int centerX, int centerY) {
        int prototypeSize = 64; // size of the prototype
        int stride = 2;

        //float[] convResult = Conv.convAt(grayscaleImage, posX, posY, prototypeSize, stride);

        List<Float> convResultList = new ArrayList<>();

        for(Conv.KernelConf iKernel : Conv.kernels){


            int numberOfAppliedKernel = prototypeSize*prototypeSize; // how often was the kernel applied?
            int[] posXArr = new int[prototypeSize*prototypeSize];

            int[] posYArr = new int[prototypeSize*prototypeSize];

            // fill positions of the (same) kernel
            for(int iy=0;iy<prototypeSize;iy++) {
                for(int ix=0;ix<prototypeSize;ix++) {
                    posXArr[iy*prototypeSize + ix] = centerX + ix * stride;
                    posYArr[iy*prototypeSize + ix] = centerY + iy * stride;
                }
            }

            float[] convResultOfThisKernel = convCl.runConv(cachedImage, img.width, iKernel.precaculatedFlattenedKernel, iKernel.precalculatedKernel.retWidth(),  posXArr, posYArr,  numberOfAppliedKernel);

            int debugHere = 5;

            for(int idx=0;idx<convResultOfThisKernel.length;idx++) {
                convResultList.add(convResultOfThisKernel[idx]);
            }
        }

        float[] convResult = new float[convResultList.size()];
        for(int idx=0;idx<convResultList.size();idx++) {
            convResult[idx] = convResultList.get(idx);
        }
        return convResult;
    }

    public void removeOutdatedPredictions(List<Prediction> predictions) {
        List<Prediction> toDelete = new ArrayList<Prediction>();
        for(Prediction pred : predictions) {
            if(pred.time <= nar.time()) {
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

    public static void main2(String[] args) {
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
        System.out.println("args: videopath trackletpath [discretization movementThreshold]");
        System.out.println("example: java -cp \"*\" org.opennars.applications.crossing.RealCrossing /mnt/sda1/Users/patha/Downloads/Test/Test/Test001/ /home/tc/Dateien/CROSSING/Test001/ 100 10");
        Util.discretization = 100;
        if(args.length == 2) {
            RealCrossing.videopath = args[0];
            RealCrossing.trackletpath = args[1];
        }
        if(args.length == 4) {
            RealCrossing.videopath = args[0];
            RealCrossing.trackletpath = args[1];
            Util.discretization = Integer.valueOf(args[2]);
            RealCrossing.movementThreshold = Integer.valueOf(args[3]);

        }
        String[] args2 = {"Crossing"};
        RealCrossing mp = new RealCrossing();
        new IncidentSimulator().show();
        PApplet.runSketch(args2, mp);
    }

    public static void main(String[] args) {
        Util.discretization = 100;

        String[] args2 = {"Crossing"};
        UnrealCrossing mp = new UnrealCrossing();

        // configure
        mp.imageSampler = new PrototypeBasedImageSampler();

        videopath = "S:\\win10host\\files\\nda\\traffic\\Train\\Train001\\";
        videopath = "S:\\win10host\\files\\nda\\traffic\\Train\\Train046\\";
        videopath = "S:\\win10host\\files\\nda\\traffic\\Test\\Test010\\";

        new IncidentSimulator().show();
        PApplet.runSketch(args2, mp);
    }
}
