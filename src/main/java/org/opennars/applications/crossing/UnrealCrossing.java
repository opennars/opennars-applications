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




// TODO< use SDR Prototype classifier to classify the drawn SDR's >

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.opennars.applications.crossing.NarListener.Prediction;
import org.opennars.applications.cv.*;
import org.opennars.applications.gui.NarSimpleGUI;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
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
    public Map2d lastframe;

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

    public int heatmapCellsize = 16;

    SdrAllocator sdrAllocator;
    UlSdrProtoClassifier layer1Classifier = new UlSdrProtoClassifier();
    Random rng = new Random();

    // threshold for region proposal
    float regionProposalAttentionThreshold = 0.2f;

    // permutation used to "fold" images for layer2
    int[] foldImagesPerm;

    UlProtoClassifier objectPrototypeClassifier = new UlProtoClassifier();

    ConvCl convCl;

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

        if (attentionField == null) { // we need to allocate the attention field


            attentionField = new AttentionField(img.height / heatmapCellsize + 1, img.width / heatmapCellsize + 1);

            attentionField.decayFactor = 0.0f;
        }


        if (attentionField != null) { // if we compute the attention

            attentionField.decay();

            // commented because it doesn't work
            //attentionField.blur();
        }


        if (lastframe != null) {
            for(int iy=0;iy<lastframe.retHeight();iy++) {
                for(int ix=0;ix<lastframe.retWidth();ix++) {


                    int colorcode = img.get(ix* heatmapCellsize, iy* heatmapCellsize);
                    //TODO check if the rgb is extracted correctly
                    float r = (colorcode & 0xff) / 255.0f;
                    float g = ((colorcode >> 8) & 0xFF) / 255.0f;
                    float b = ((colorcode >> 8*2) & 0xFF) / 255.0f;

                    float grayscale = (r+g+b)/3.0f;



                    float diff = lastframe.readAtSafe(iy, ix) - grayscale; // difference to last frame
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

        {
            if (lastframe == null) {
                lastframe = new Map2d(img.height / heatmapCellsize + 1, img.width / heatmapCellsize + 1);
            }



            for(int iy=0;iy<lastframe.retHeight();iy++) {
                for(int ix=0;ix<lastframe.retWidth();ix++) {
                    int colorcode = img.get(ix* heatmapCellsize, iy* heatmapCellsize);
                    //TODO check if the rgb is extracted correctly
                    float r = (colorcode & 0xff) / 255.0f;
                    float g = ((colorcode >> 8) & 0xFF) / 255.0f;
                    float b = ((colorcode >> 8*2) & 0xFF) / 255.0f;

                    float grayscale = (r+g+b)/3.0f;


                    // TODO< integrate over all pixels >

                    lastframe.writeAtSafe(iy, ix, grayscale);
                }
            }

        }

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
                    rp.minX = absX - 15;
                    rp.minY = absY - 15;

                    rp.maxX = absX + 15;
                    rp.maxY = absY + 15;

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


        debugCursors.clear();

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

                    System.out.println("[d ] width=" + Integer.toString(iRegionProposal.maxX - iRegionProposal.minX) + " height=" + Integer.toString(iRegionProposal.maxY - iRegionProposal.minY));

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

                //Map2d grayscaleImage = new Map2d(img.height, img.width);
                short[] imgGrayscale = new short[img.height*img.width];
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


                for(RegionProposal iRegionProposal : regionProposals) {
                    int width = iRegionProposal.maxX-iRegionProposal.minX;
                    int height = iRegionProposal.maxY-iRegionProposal.minY;

                    if(false)   System.out.println("[d ] width="+Integer.toString(iRegionProposal.maxX-iRegionProposal.minX) + " height="+Integer.toString(iRegionProposal.maxY-iRegionProposal.minY));

                    if (width<80 && height<80) {
                        continue; // we are just interested in cars
                    }

                    int centerX = iRegionProposal.minX + 128/2;
                    int centerY = iRegionProposal.minY + 128/2;

                    int prototypeSize = 32; // size of the prototype
                    int stride = 4;

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

                        float[] convResultOfThisKernel = convCl.runConv(imgGrayscale, img.width, iKernel.precaculatedFlattenedKernel, iKernel.precalculatedKernel.retWidth(),  posXArr, posYArr,  numberOfAppliedKernel);

                        int debugHere = 5;

                        for(int idx=0;idx<convResultOfThisKernel.length;idx++) {
                            convResultList.add(convResultOfThisKernel[idx]);
                        }
                    }

                    float[] convResult = new float[convResultList.size()];
                    for(int idx=0;idx<convResultList.size();idx++) {
                        convResult[idx] = convResultList.get(idx);
                    }



                    long classification = objectPrototypeClassifier.classify(convResult);

                    System.out.println("[d ] obj classification = " + Long.toString(classification));



                    DebugCursor dc = new DebugCursor();
                    dc.posX = (iRegionProposal.minX+iRegionProposal.maxX)/2;
                    dc.posY = (iRegionProposal.minY+iRegionProposal.maxY)/2;
                    dc.text = "OBJ class=" + Long.toString(classification);

                    debugCursors.add(dc);

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

        for (DebugCursor iDebugCursor : debugCursors) {
            boolean hasExtend = iDebugCursor.extendX != 0 || iDebugCursor.extendY != 0;

            if (hasExtend) {
                fill(0, 0, 255, 127);
                rect((int)iDebugCursor.posX, (int)iDebugCursor.posY, (int)(iDebugCursor.extendX), (int)(iDebugCursor.extendY));
            }
            else
            {
                line((int)(iDebugCursor.posX - 5), (int)iDebugCursor.posY, (int)(iDebugCursor.posX + 5), (int)iDebugCursor.posY);
                line((int)iDebugCursor.posX, (int)(iDebugCursor.posY - 5), (int)iDebugCursor.posX, (int)(iDebugCursor.posY + 5));
            }

            fill(127);
            text(iDebugCursor.text, (float)iDebugCursor.posX, (float)iDebugCursor.posY);
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
                    fill(255, 0, 0, (int)(attentionField.readAtUnbound(iy, ix) * 255.0f));
                    rect(ix * heatmapCellsize, iy * heatmapCellsize, heatmapCellsize, heatmapCellsize);
                }
            }


        }


        System.out.println("Concepts: " + nar.memory.concepts.size());
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

        new IncidentSimulator().show();
        PApplet.runSketch(args2, mp);
    }
}
