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


// TODO< flood fill CA'ed attention map to get different colors for tighter region proposals >

// TODO< removal of spatial advanced spatial tracklet after timeout >
// TODO< test if AdvancedSpatialTracklet works fine and is used correctly for training data gathering & training >

// todo  attention
// todo  keep track of recent images to notice change

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

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opennars.io.events.Events;
import org.opennars.io.events.OutputHandler.DISAPPOINT;
import org.opennars.main.Nar;
import processing.core.PApplet;
import processing.core.PImage;
import processing.event.MouseEvent;

import static org.opennars.applications.cv.Util.calcDistBetweenImageAndPrototype;

public class UnrealCrossing extends PApplet {
    Nar nar;
    int entityID = 1;

    List<Prediction> predictions = new ArrayList<Prediction>();
    List<Prediction> disappointments = new ArrayList<Prediction>();
    final int streetWidth = 40;
    final int fps = 20;

    public PrototypeBasedImageSampler imageSampler = null;
    public AttentionField attentionField = null;

    CaFill caFill = new CaFill(); // used to fill with an CA


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

        // setup view
        viewport.difx = -480.0f;
        viewport.dify = -400.0f;



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
    int frameIdx = 2;

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
    List<AdvancedSpatialTracklet> advancedSpatialTracklets = new ArrayList<>();
    long trackletIdCounter = 1;

    PImage lastframe2 = null;

    Map2d[] convolutions; // channels of different convolutions applied to the complete (current) image

    List<MotionParticle> motionParticles = new ArrayList<>(); // particles to track motion


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

    private double calcDistOfParticleAt(MotionParticle mp, double posX, double posY, PImage img) {
        double sum = 0;

        for(int channelIdx=0;channelIdx<3;channelIdx++) {
            Map2d segmentChannel = mp.segment[channelIdx];

            double subimagePosX = posX - segmentChannel.retWidth()/2.0;
            double subimagePosY = posY - segmentChannel.retHeight()/2.0;

            for(int iy=0;iy<segmentChannel.retHeight();iy++) {
                for(int ix=0;ix<segmentChannel.retWidth();ix++) {
                    double samplePosX = subimagePosX+ix;
                    double samplePosY = subimagePosY+iy;

                    // TODO< sample it subpixel accurate >
                    int samplePosXInt = (int)samplePosX;
                    int samplePosYInt = (int)samplePosY;

                    int colorcode =  img.pixels[samplePosYInt*img.width+samplePosXInt];
                    //TODO check if the rgb is extracted correctly
                    float r = (colorcode & 0xff) / 255.0f;
                    float g = ((colorcode >> 8) & 0xFF) / 255.0f;
                    float b = ((colorcode >> 8*2) & 0xFF) / 255.0f;


                    float sampleValue = 0;
                    if (channelIdx == 0) {
                        sampleValue = r;
                    }
                    else if (channelIdx == 1) {
                        sampleValue = g;
                    }
                    else if (channelIdx == 2) {
                        sampleValue = b;
                    }

                    float mpChannelValue = segmentChannel.readAtUnsafe(iy, ix);

                    sum += Math.abs(sampleValue - mpChannelValue);
                }
            }

        }

        return sum / (mp.segment.length*mp.segment[0].retWidth()*mp.segment[0].retHeight());
    }

    /**
     *
     * @param mp
     * @param maxDistance
     * @return false if tracking lost
     */
    private boolean traceMotionParticle(MotionParticle mp, double maxDistance, PImage img) {
        double minMetricDist = Double.POSITIVE_INFINITY;
        double minMetricX = 0;
        double minMetricY = 0;

        for(int distance = 0; distance < maxDistance; distance++) {



            // calc distance in rectangular shape
            for(int dist:new int[]{-distance,distance}) {
                for(int dist2=-distance;dist2<distance;dist2++) {
                    {
                        double metricDist = calcDistOfParticleAt(mp,  mp.posX + dist, mp.posY+dist2, img);
                        if (metricDist < minMetricDist) {
                            minMetricDist = metricDist;
                            minMetricX = mp.posX + dist;
                            minMetricY = mp.posY+dist2;
                        }
                    }

                    {
                        double metricDist = calcDistOfParticleAt(mp,  mp.posX + dist2, mp.posY+dist, img);
                        if (metricDist < minMetricDist) {
                            minMetricDist = metricDist;
                            minMetricX = mp.posX + dist2;
                            minMetricY = mp.posY+dist;
                        }
                    }
                }
            }


        }

        double maxThreshold = 50.0; // parameter
        if (minMetricDist < maxThreshold) {
            // retrace
            mp.posX = minMetricX;
            mp.posY = minMetricY;
        }

        return minMetricDist < maxThreshold;
    }

    private void trackAndRemoveMotionparticles(double maxDistance, int imgWidth, int imgHeight, PImage img) {
        for(int particleIdx=motionParticles.size()-1;particleIdx>=0;particleIdx--) {
            boolean isAliveTracking = traceMotionParticle(motionParticles.get(particleIdx), maxDistance, img);

            boolean isOutOfBoundsX = motionParticles.get(particleIdx).posX <= motionParticles.get(particleIdx).segment[0].retWidth()/2 + maxDistance || motionParticles.get(particleIdx).posX >= imgWidth - motionParticles.get(particleIdx).segment[0].retWidth()/2 - maxDistance;
            boolean isOutOfBoundsY = motionParticles.get(particleIdx).posY <= motionParticles.get(particleIdx).segment[0].retHeight()/2 + maxDistance || motionParticles.get(particleIdx).posY >= imgHeight - motionParticles.get(particleIdx).segment[0].retHeight()/2 - maxDistance;
            boolean isOutOfBounds = isOutOfBoundsX || isOutOfBoundsY;

            boolean removeParticle = !isAliveTracking || isOutOfBounds;
            if (removeParticle) {
                motionParticles.remove(particleIdx);
            }
        }
    }

    private void addMotionParticleAt(double posX, double posY, int size, PImage img) {
        MotionParticle mp = new MotionParticle(posX, posY);
        mp.segment = new Map2d[3];

        for(int channelIdx=0;channelIdx<3;channelIdx++) {
            Map2d segmentChannel = new Map2d(size, size);

            double subimagePosX = posX - segmentChannel.retWidth()/2.0;
            double subimagePosY = posY - segmentChannel.retHeight()/2.0;

            for(int iy=0;iy<segmentChannel.retHeight();iy++) {
                for(int ix=0;ix<segmentChannel.retWidth();ix++) {
                    double samplePosX = subimagePosX+ix;
                    double samplePosY = subimagePosY+iy;

                    // TODO< sample it subpixel accurate >
                    int samplePosXInt = (int)samplePosX;
                    int samplePosYInt = (int)samplePosY;

                    int colorcode =  img.pixels[samplePosYInt*img.width+samplePosXInt];
                    //TODO check if the rgb is extracted correctly
                    float r = (colorcode & 0xff) / 255.0f;
                    float g = ((colorcode >> 8) & 0xFF) / 255.0f;
                    float b = ((colorcode >> 8*2) & 0xFF) / 255.0f;


                    float sampleValue = 0;
                    if (channelIdx == 0) {
                        sampleValue = r;
                    }
                    else if (channelIdx == 1) {
                        sampleValue = g;
                    }
                    else if (channelIdx == 2) {
                        sampleValue = b;
                    }

                    segmentChannel.writeAtSafe(iy, ix, sampleValue);
                }
            }

            mp.segment[channelIdx] = segmentChannel;
        }

        motionParticles.add(mp);
    }

    // pool used for all general work
    ThreadPoolExecutor generalPool = (ThreadPoolExecutor)Executors.newFixedThreadPool(8);

    long nrLoader = 1; // iterator of image number for loader
    List<Future<PImage>> imageLoaderFutures = new ArrayList<>(); // futures for the loading of the images

    int imagePreloadCount = 4; // number of images which are preloaded

    boolean debug_enableClassification = false; // used for debugging - best to be kept enabled in "production"

    ClassDatabase classDatabase = new ClassDatabase(); // database for all classifications

    @Override
    public void draw() {
        if (false) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
        }

        long systemTimeStart = System.nanoTime();


        if (frameIdx > Integer.MAX_VALUE) {
            // soft reset

            System.out.println("SOFT RESET");
            System.out.println("");


            frameIdx = 1;
            motionParticles.clear();
            attentionField = null;
            advancedSpatialTracklets.clear();
            spatialTracklets.clear();
            lastframe2 = null;
        }

        viewport.Transform();
        background(64,128,64);
        fill(0);
        for (Street s : streets) {
            s.draw(this);
        }

        long overallTimeImgLoadWaitInNs = 0; // wait time for image load in nanoseconds

        PImage img = null;
        {
            while (imageLoaderFutures.size() < imagePreloadCount) {
                Future<PImage> f = generalPool.submit(() -> {
                    String nr = String.format("%05d", nrLoader);
                    PImage img2 =loadImage(videopath+nr+".jpg");
                    nrLoader++;
                    return img2;
                });
                imageLoaderFutures.add(f);
            }

            // wait for loading of latest enqueed image
            long startTime = System.nanoTime();
            while(!imageLoaderFutures.get(0).isDone()){}
            overallTimeImgLoadWaitInNs += (System.nanoTime() - startTime);

            try {
                img = imageLoaderFutures.get(0).get();
            } catch (InterruptedException e) {
                //e.printStackTrace();
                int here = 5;
            } catch (ExecutionException e) {
                //e.printStackTrace();
                int here = 5;
            }
            imageLoaderFutures.remove(0);
        }



        /* commented because it is the old syncronous loading
        PImage img;
        {
            long startTime = System.nanoTime();
            String nr = String.format("%05d", frameIdx);
            img =loadImage(videopath+nr+".jpg");
            overallTimeImgLoadWaitInNs += (System.nanoTime() - startTime);
        }
        */



        short[] imgGrayscale = new short[img.height*img.width]; // flattened grayscale image

        long timeConvertImageToGrayscaleWaitInNs = 0;
        { // convert image to grayscale
            long timeBefore = System.nanoTime();

            int parallelYStepsize = 8;
            Future<?>[] processingFutures = new Future<?>[parallelYStepsize];
            {
                for(int iParallelYStepsize=0;iParallelYStepsize<parallelYStepsize;iParallelYStepsize++) {
                    final int iParallelYStepsize_final = iParallelYStepsize;
                    final PImage img_final = img;
                    Future<?> f = generalPool.submit(() -> {
                        for(int iy=iParallelYStepsize_final;iy<img_final.height;iy+=parallelYStepsize) {
                            for(int ix=0;ix<img_final.width;ix++) {

                                int colorcode =  img_final.pixels[iy*img_final.width+ix];
                                //TODO check if the rgb is extracted correctly
                                float r = (colorcode & 0xff) / 255.0f;
                                float g = ((colorcode >> 8) & 0xFF) / 255.0f;
                                float b = ((colorcode >> 8*2) & 0xFF) / 255.0f;

                                float grayscale = (r+g+b)/3.0f;

                                //grayscaleImage.writeAtUnsafe(iy, ix, grayscale);

                                imgGrayscale[iy*img_final.width + ix] = (short)(grayscale*255.0f);
                            }
                        }
                    });
                    processingFutures[iParallelYStepsize] = f;
                }
            }

            for(int iParallelYStepsize=0;iParallelYStepsize<processingFutures.length;iParallelYStepsize++) {
                while(!processingFutures[iParallelYStepsize].isDone()){};
            }
            timeConvertImageToGrayscaleWaitInNs += (System.nanoTime()-timeBefore);
        }




        if (!convCl.areBuffersAllocated()) {
            // allocate all buffers of the required size
            convCl.allocateBuffersForPosition(img.width*img.height);
        }


        long overalltimeWaitUploadImage = 0;
        ConvCl.CachedImage cachedImage = new ConvCl.CachedImage();
        {
            long timeBefore = System.nanoTime();
            cachedImage.update(imgGrayscale, convCl.context); // send image to GPU
            overalltimeWaitUploadImage += (System.nanoTime()-timeBefore);
        }


        if (convolutions == null) {
            // allocate
            convolutions = new Map2d[Conv.kernels.length];
            for(int idx=0;idx<convolutions.length;idx++) {
                convolutions[idx] = new Map2d(img.height, img.width);
            }
        }

        long overalltimeWaitConvl = 0;
        { // compute convolutions
            long systemTimeBefore = System.nanoTime();

            List<Future<?>> futures = new ArrayList<>();



            for(int kernelIdx=0;kernelIdx<Conv.kernels.length;kernelIdx++) {
                final int kernelIdx2 = kernelIdx;
                final PImage img2 = img;
                Future<?> f = generalPool.submit(() -> {
                    Conv.KernelConf iKernel = Conv.kernels[kernelIdx2];

                    // TODO< optimize and speed it up
                    //       we really don't need to upload all positions because we do the convolution for the complete image >

                    int[] posXArr = new int[img2.height * img2.width];
                    int[] posYArr = new int[img2.height * img2.width];

                    // fill positions of the (same) kernel
                    int kernelsize = iKernel.precalculatedKernel.retHeight();
                    for (int iy = kernelsize; iy < img2.height - kernelsize; iy++) {
                        for (int ix = kernelsize; ix < img2.width - kernelsize; ix++) {
                            posXArr[iy * img2.width + ix] = ix;
                            posYArr[iy * img2.width + ix] = iy;
                        }
                    }

                    long systemTimeBefore2 = System.nanoTime();
                    float[] convResultOfThisKernel = convCl.runConv(cachedImage, img2.width, iKernel.precaculatedFlattenedKernel, iKernel.precalculatedKernel.retWidth(), posXArr, posYArr, posXArr.length);
                    long systemTimeEnd2 = System.nanoTime();
                    //System.out.println("   runConv() us=" + Long.toString((systemTimeEnd2 - systemTimeBefore2) / 1000));

                    // write result of convolution into map
                    for (int idx = 0; idx < posXArr.length; idx++) {
                        float val = convResultOfThisKernel[idx];
                        convolutions[kernelIdx2].writeAtUnsafe(posYArr[idx], posXArr[idx], val);
                    }
                });
                futures.add(f);
            }

            for(Future<?> iFuture: futures) {
                while(!iFuture.isDone()){} // spin loop
            }

            overalltimeWaitConvl = System.nanoTime()-systemTimeBefore; // in nanoseconds
        }


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


        Map2dGeneric<Boolean> regionField = new Map2dGeneric<>(attentionField.retHeight(), attentionField.retWidth());
        { // fill and build regionField
            float attentionfieldToRegionFieldThreshold = 0.02f; // parameter

            // initialize region field by thresholding - we want only the most active pixels
            for(int iy=0;iy<regionField.retHeight();iy++) {
                for(int ix=0;ix<regionField.retWidth();ix++) {
                    float val = attentionField.readAtUnbound(iy, ix);
                    boolean bval = val > attentionfieldToRegionFieldThreshold;
                    regionField.writeAtSafe(iy, ix, bval);
                }
            }


            for(int loopI=0;loopI<30;loopI++) { // loop to prevent it from infinite hanging
                Map2dGeneric<Boolean> destRegionField = new Map2dGeneric<>(attentionField.retHeight(), attentionField.retWidth());
                for(int iy=0;iy<regionField.retHeight();iy++) {
                    for(int ix=0;ix<regionField.retWidth();ix++) {
                        destRegionField.writeAtSafe(iy,ix,false);
                    }
                }

                for(int iy=1;iy<regionField.retHeight()-1;iy++) {
                    for(int ix=1;ix<regionField.retWidth()-1;ix++) {
                        boolean[][] subField = new boolean[][]{
                                {regionField.readAtSafe(iy-1, ix-1), regionField.readAtSafe(iy-1, ix), regionField.readAtSafe(iy-1, ix+1), },
                                {regionField.readAtSafe(iy, ix-1), regionField.readAtSafe(iy, ix), regionField.readAtSafe(iy, ix+1), },
                                {regionField.readAtSafe(iy+1, ix-1), regionField.readAtSafe(iy+1, ix), regionField.readAtSafe(iy+1, ix+1), }
                        };

                        boolean destRegionFieldValue = caFill.queryFill(subField); // apply CA to fill the gaps

                        destRegionField.writeAtSafe(iy, ix, destRegionFieldValue);
                    }
                }

                boolean wasChanged = false;
                for(int iy=1;iy<regionField.retHeight()-1;iy++) {
                    for(int ix=1;ix<regionField.retWidth()-1;ix++) {
                        if (destRegionField.readAtSafe(iy, ix) != regionField.readAtSafe(iy, ix)) {
                            wasChanged = true;
                        }
                    }
                }

                regionField = destRegionField; // we want to continue with the changed region field

                // check to break out of loop when nothing changes anymore
                if (!wasChanged) {
                    break;
                }
            }

        }

        Map<Integer, Region> regionsByColor = new HashMap<>();
        { // extract regions from regionField

            Map2dGeneric<Integer> colorMap = FloodFill.fill(regionField);

            // extract regions by color

            for(int iy=0;iy<colorMap.retHeight();iy++) {
                for(int ix=0;ix<colorMap.retWidth();ix++) {
                    int color = colorMap.readAtSafe(iy,ix);

                    if (color == -1) { // is color not set?
                        continue;
                    }

                    if (regionsByColor.containsKey(color)) {
                        Region region = regionsByColor.get(color);
                        region.merge(ix, iy);
                    }
                    else {
                        Region createdRegion = new Region();
                        createdRegion.merge(ix, iy);
                        regionsByColor.put(color, createdRegion);
                    }
                }
            }
        }



        double motionparticleMaxDistance = 8.0; // configuration - maximal distance a motion particle can travel
        trackAndRemoveMotionparticles(motionparticleMaxDistance, img.width, img.height, img);

        { // add new motion particles
            int motionparticleSize = 12; // configuration - size of a motion particle

            int numberOfSpawnedMotionParticles = 0; // NOTE< disabled motion particles for now because cars move to fast, but pedestriants seem to have the right speed to "pick up" >

            for(int i=0;i<numberOfSpawnedMotionParticles;i++) {
                double spawnPosX = 16 + rng.nextDouble() * (img.width-16*2);
                double spawnposY = 16 + rng.nextDouble() * (img.height-16*2);

                addMotionParticleAt(spawnPosX, spawnposY, motionparticleSize, img);
            }
        }


        List<RegionProposal> regionProposals = new ArrayList<>();
        { // translate regions to region proposals
            for(Map.Entry<Integer, Region> iRegion : regionsByColor.entrySet()){
                RegionProposal rp = new RegionProposal(); // build region proposal
                rp.minX = iRegion.getValue().minX * heatmapCellsize;
                rp.minY = iRegion.getValue().minY * heatmapCellsize;

                // add one to take the last cell into account
                rp.maxX = (iRegion.getValue().maxX + 1)* heatmapCellsize;
                rp.maxY = (iRegion.getValue().maxY + 1)* heatmapCellsize;

                int width = rp.maxX-rp.minX;
                int height = rp.maxY-rp.minY;

                if (width <= 1*heatmapCellsize || height <= 1*heatmapCellsize ) {
                    continue; // disallow regions of size one
                }

                regionProposals.add(rp);
            }
        }



        debugCursors.clear();


        // increment idletime of tracklets
        for(AdvancedSpatialTracklet iSt : advancedSpatialTracklets) {
            iSt.idletime++;
            iSt.notTrainedSince++;
        }
        for(SpatialTracklet iSt : spatialTracklets) {
            iSt.idletime++;
        }

        // remove old tracklets
        for(int idx=advancedSpatialTracklets.size()-1;idx>=0;idx--) {
            if (advancedSpatialTracklets.get(idx).idletime > 40) {
                AdvancedSpatialTracklet tracklet = advancedSpatialTracklets.get(idx);


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


                advancedSpatialTracklets.remove(idx);
            }
        }

        for(int idx=spatialTracklets.size()-1;idx>=0;idx--) {
            if (spatialTracklets.get(idx).idletime > 40) {
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
                        for(int frameIdx=0;frameIdx<sdrImageWidth;frameIdx++) {
                            sdrImg[j][frameIdx] = Sdr.makeNull(sdrAllocator.sdrSize);
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
                            for(int frameIdx=0;frameIdx<sdrImg[j].length;frameIdx++) {
                                Sdr pixelSdr = sdrImg[j][frameIdx];

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
                for (RegionProposal iRegionProposal : regionProposals) {
                    int width = iRegionProposal.maxX - iRegionProposal.minX;
                    int height = iRegionProposal.maxY - iRegionProposal.minY;

                    int centerX = iRegionProposal.minX + width / 2;
                    int centerY = iRegionProposal.minY + height / 2;


                    boolean wasAnyTrackletRecaptured = false;
                    for (SpatialTracklet iTracklet : spatialTracklets) {
                        double diffX = iTracklet.posX - centerX;
                        double diffY = iTracklet.posY - centerY;
                        double dist = Math.sqrt(diffX * diffX + diffY * diffY);
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

                        // spawn new advanced tracklet to capture it
                        AdvancedSpatialTracklet advst = new AdvancedSpatialTracklet(centerX, centerY, trackletIdCounter);
                        trackletIdCounter++;
                        advancedSpatialTracklets.add(advst);
                    }

                }
            }
        }

        entities.clear(); //refresh
        String tracklets = "";
        if (trackletpath != null) { // use tracklets with file based provider?
            try {
                String nr = String.format("%05d", frameIdx);
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


        frameIdx++;

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

        long overallTimeNarWaitInNs = 0; // wait time for NAR in nanoseconds
        {
            long startTimeWait = System.nanoTime();
            nar.cycles(10);
            overallTimeNarWaitInNs += (System.nanoTime() - startTimeWait);
        }

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


        if (regionField != null) { // we want to draw the attention field

            for(int iy=0;iy<regionField.retHeight();iy++) {
                for(int ix=0;ix<regionField.retWidth();ix++) {
                    boolean fieldValue = regionField.readAtSafe(iy, ix);
                    if (fieldValue) {
                        stroke(255,0,0,200); // transparent frame
                        fill(255, 0, 0, 0);
                        rect(ix * heatmapCellsize, iy * heatmapCellsize, heatmapCellsize, heatmapCellsize);
                    }
                }
            }

            stroke(127); // set back to standard
        }

        long overallTimeWaitClassifyInNs = 0;
        // classify
        {
            long timeBefore = System.nanoTime();

            if (debug_enableClassification) {
                for (SpatialTracklet iSt : spatialTracklets) {
                    if (iSt.idletime > 5) {
                        continue; // we don't care about things which didn't move
                    }

                    //System.out.println("---");

                    long bestClassificationClass = -1;
                    double bestClassificationProbability = 0;

                    float[] convResult = null;
                    if (trainedNns.size() > 0) { // we just need convolution when a NN was trained
                        convResult = convolutionImg(img, cachedImage, (int) iSt.posX, (int) iSt.posY);
                    }

                    for (int iTrainedNnIdx = trainedNns.size() - 1; iTrainedNnIdx >= 0; iTrainedNnIdx--) {
                        TrainedNn iTrainedNn = trainedNns.get(iTrainedNnIdx);

                        INDArray arr = Nd4j.create(convResult);
                        INDArray result = iTrainedNn.network.activate(arr, Layer.TrainingMode.TEST);

                        int highestPositiveClassIdx = -1;
                        double highestPositiveClasssProbability = 0;

                        for (int idx = 0; idx < iTrainedNn.positiveClasses.size(); idx++) { // iterate over classes
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
                            if (false)
                                System.out.println("[d 5] cls=" + iTrainedNn.positiveClasses.get(highestPositiveClassIdx) + "   classification{" + highestPositiveClassIdx + "}=" + Double.toString(highestPositiveClasssProbability));
                        }

                        if (highestPositiveClassIdx != -1 && highestPositiveClasssProbability > bestClassificationProbability) {
                            bestClassificationClass = iTrainedNn.positiveClasses.get(highestPositiveClassIdx); // retrieve class by index. Indirection is necessary for NN reasons
                            bestClassificationProbability = highestPositiveClasssProbability;
                        }

                        if (highestPositiveClassIdx != -1) { // if it classified a positive class
                            break; // then don't check any other older NN's because they are outdated anyways for this classification
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
                            dc.text = "CLASS " + Long.toString(bestClassificationClass) + " prop=" + String.format("%.2f", (bestClassificationProbability));
                            debugCursors.add(dc);
                        }
                    }
                }
            }

            overallTimeWaitClassifyInNs += (System.nanoTime()-timeBefore);
        }






        for(AdvancedSpatialTracklet iSt : advancedSpatialTracklets) {
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

            System.out.println("[frameIdx 1] queue taskCount ="+Long.toString(pool.getTaskCount()));

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

            //System.out.println("[frameIdx 1] queue taskCount ="+Long.toString(pool.getTaskCount()));

            int here = 5;
        }


        { // try to enlarge prototypes
            // we do this because the (proto)objects may be very small when they are first detected

            for(AdvancedSpatialTracklet iSt : advancedSpatialTracklets) {

                // look for overlapping region proposals
                RegionProposal bestOverlappingRegionProposal = null;
                for (RegionProposal iRegionProposal : regionProposals) {
                    if (iRegionProposal.minX < iSt.centerX && iRegionProposal.maxX > iSt.centerX && iRegionProposal.minY < iSt.centerY && iRegionProposal.maxY > iSt.centerY) {
                        bestOverlappingRegionProposal = iRegionProposal;// just take any
                    }
                }

                if (bestOverlappingRegionProposal != null) {
                    int thisRegionProposalWidth = bestOverlappingRegionProposal.maxX - bestOverlappingRegionProposal.minX;
                    int thisRegionProposalheight = bestOverlappingRegionProposal.maxY - bestOverlappingRegionProposal.minY;

                    if (iSt.prototypeClassifier != null && iSt.prototypeClassifier.hasPrototypes()) {

                        // do we need to enlarge it?
                        int areaOfThisRegionProposal = thisRegionProposalWidth*thisRegionProposalheight;

                        int areaOfPrototypes = iSt.prototypeClassifier.retPrototypeWidth()*iSt.prototypeClassifier.retPrototypeHeight();

                        boolean needToEnlarge = areaOfThisRegionProposal > areaOfPrototypes * 1.1; // multiply with 1.1 as factor to ensure that we don't enlarge always
                        if (needToEnlarge) {
                            // create prototype classifier and add sample

                            iSt.prototypeClassifier = new MultichannelProtoClassifier();

                            // (*) add sample
                            long class_ = iSt.prototypeClassifier.forceAddPrototype((int)iSt.centerX, (int)iSt.centerY, thisRegionProposalWidth, thisRegionProposalheight, img);


                            { // add debug cursor
                                DebugCursor dc = new DebugCursor();
                                dc.text = "PROTOTYPE ENLARGED";
                                dc.posX = iSt.centerX;
                                dc.posY = iSt.centerY;
                                debugCursors.add(dc);
                            }
                        }


                    }
                }
            }
        }

        // overall time of the search for prototypes in this frame
        long overalltimePrototypeSearchInNs = 0;

        { // try to track spatial tracklet with prototypes
            for(AdvancedSpatialTracklet iSt : advancedSpatialTracklets) {
                //reset to default values
                iSt.prototypeCenterX = -1;
                iSt.prototypeCenterY = -1;

                // look for overlapping region proposals
                RegionProposal bestOverlappingRegionProposal = null;
                for (RegionProposal iRegionProposal : regionProposals) {
                    if (iRegionProposal.minX < iSt.centerX && iRegionProposal.maxX > iSt.centerX && iRegionProposal.minY < iSt.centerY && iRegionProposal.maxY > iSt.centerY) {
                        bestOverlappingRegionProposal = iRegionProposal;// just take any
                    }
                }

                if (bestOverlappingRegionProposal == null) {
                    { // debug
                        fill(0,0,0);
                        text("NULL   x="+(int) iSt.centerX + " y="+(int) iSt.centerY, (float)iSt.centerX, (float)iSt.centerY);
                    }
                }

                if (bestOverlappingRegionProposal != null) {
                    int width = bestOverlappingRegionProposal.maxX - bestOverlappingRegionProposal.minX;
                    int height = bestOverlappingRegionProposal.maxY - bestOverlappingRegionProposal.minY;

                    double regionProposalCenterX = bestOverlappingRegionProposal.minX + width/2.0;
                    double regionProposalCenterY = bestOverlappingRegionProposal.minY + height/2.0;

                    fill(0,0,0);
                    text("C", (float)regionProposalCenterX, (float)regionProposalCenterY);

                    if (iSt.prototypeClassifier == null) {
                        // create prototype classifier and add sample

                        iSt.prototypeClassifier = new MultichannelProtoClassifier();

                        // (*) add sample
                        long class_ = iSt.prototypeClassifier.forceAddPrototype((int)iSt.centerX, (int)iSt.centerY, width, height, img);

                        int debugHere = 5;

                        { // debug
                            fill(0,0,0);
                            text("RESET  x="+(int) iSt.centerX + " y="+(int) iSt.centerY, (float)iSt.centerX, (float)iSt.centerY);
                        }
                    }
                    else {
                        // track with classifier

                        { // debug
                            fill(0,0,0);
                            text("PROTO  x="+(int) iSt.centerX + " y="+(int) iSt.centerY, (float)iSt.centerX, (float)iSt.centerY);
                        }

                        // for this we search for the prototype and pick the best position

                        int prototypeSearchDistance = 20;//;40;//20; // distance in pixels for the search of the same prototype

                        int bestPositionX = 0;
                        int bestPositionY = 0;
                        float bestClassificationDistance = Float.POSITIVE_INFINITY;

                        long timeStartInNs = System.nanoTime();

                        // TODO< fearch for a pixel distance of 2 inside >
                        // search around old center
                        /* commented because not necessary
                        for(int dy=-prototypeSearchDistance;dy<prototypeSearchDistance;dy+=3) {

                            for(int dx=-prototypeSearchDistance;dx<prototypeSearchDistance;dx+=3) {
                                iSt.prototypeClassifier.classifyAt((int)iSt.centerX + dx, (int)iSt.centerY + dy, img);
                                float classificationDistance = iSt.prototypeClassifier.classificationLastDistance;

                                if (classificationDistance < bestClassificationDistance) {
                                    bestClassificationDistance = classificationDistance;
                                    bestPositionX = (int)iSt.centerX + dx;
                                    bestPositionY = (int)iSt.centerY + dy;
                                }
                            }
                        }
                        //*/

                        // TODO< fearch for a pixel distance of 2 inside >
                        // search in region proposal
                        for(int dy=-prototypeSearchDistance;dy<prototypeSearchDistance;dy+=3) {
                            boolean offsetXCoordinate = (dy % 2) == 0; // do we offset the x coordinate? to reduce the overall distance
                            int startX = -prototypeSearchDistance + (offsetXCoordinate?3/2:0);

                            for(int dx=startX;dx<prototypeSearchDistance;dx+=3) {
                                int prototypeClassificationStepsize = 2; // ship every 2nd pixel of the compared prototype

                                iSt.prototypeClassifier.classifyAt((int)regionProposalCenterX + dx, (int)regionProposalCenterY + dy, prototypeClassificationStepsize, img);
                                float classificationDistance = iSt.prototypeClassifier.classificationLastDistanceMse;

                                if (classificationDistance < bestClassificationDistance) {
                                    bestClassificationDistance = classificationDistance;
                                    bestPositionX = (int)regionProposalCenterX + dx;
                                    bestPositionY = (int)regionProposalCenterY + dy;
                                }
                            }
                        }

                        long timeEndInNs = System.nanoTime();
                        overalltimePrototypeSearchInNs += (timeEndInNs-timeStartInNs); // add up time



                        // (*) set information for advanced spatial tracklet
                        if (bestClassificationDistance < Float.POSITIVE_INFINITY) {
                            System.out.println("retrace " + bestPositionX + "," + bestPositionY);

                            iSt.prototypeCenterX = bestPositionX;
                            iSt.prototypeCenterY = bestPositionY;

                            // set position to the position where the prototype was found, because the old center may be a first guess or the old position of the old found prototype
                            iSt.centerX = bestPositionX;
                            iSt.centerY = bestPositionY;

                            fill(0,0,0);
                            text("RETR  x="+(int) iSt.centerX + " y="+(int) iSt.centerY, (float)iSt.centerX, (float)iSt.centerY+20);


                            // update so it tracks the new image
                            iSt.prototypeClassifier = new MultichannelProtoClassifier();
                            iSt.prototypeClassifier.forceAddPrototype((int)iSt.centerX, (int)iSt.centerY, width, height, img);
                        }


                        { // add debug cursor
                            if (bestClassificationDistance < Float.POSITIVE_INFINITY) {
                                DebugCursor dc = new DebugCursor();
                                dc.text = "PROTOTYPE BEST FOUND";
                                dc.posX = bestPositionX;
                                dc.posY = bestPositionY;
                                debugCursors.add(dc);
                            }
                        }
                    }
                }
            }
        }

        { // store training sample
            for(RegionProposal iRegionProposal : regionProposals) {
                int width = iRegionProposal.maxX-iRegionProposal.minX;
                int height = iRegionProposal.maxY-iRegionProposal.minY;

                if(false)   System.out.println("[d ] width="+Integer.toString(iRegionProposal.maxX-iRegionProposal.minX) + " height="+Integer.toString(iRegionProposal.maxY-iRegionProposal.minY));

                if (width<80 && height<80) {
                    continue; // we are just interested in cars
                }

                int regionCenterX = iRegionProposal.minX + 128/2;
                int regionCenterY = iRegionProposal.minY + 128/2;





                // search best tracklet - the one which is in the region, has an identified prototype, and has the most training samples to add it
                AdvancedSpatialTracklet bestTracklet = null;
                for(AdvancedSpatialTracklet iSt : advancedSpatialTracklets) {
                    if (iSt.prototypeCenterX == -1) {
                        continue; // ignore because we need one with a identified prototype
                    }

                    double diffX = iSt.centerX - regionCenterX;
                    double diffY = iSt.centerY - regionCenterY;

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
                    float[] convResult = convolutionImg(img, cachedImage, (int)bestTracklet.prototypeCenterX, (int)bestTracklet.prototypeCenterY);

                    bestTracklet.trainingDataOfThisClass.add(convResult);
                }

            }

        }



        { // maintain classes from Advanced tracklets
            for(AdvancedSpatialTracklet iSt : advancedSpatialTracklets) {
                if (iSt.prototypeCenterX == -1) {
                    continue; // ignore because we need one with a identified prototype
                }

                // subsection of image
                Map2d[] subImg = org.opennars.applications.cv.Util.subimage((int)iSt.prototypeCenterX, (int)iSt.prototypeCenterY, 128, 128, img);
                if (subImg == null) {
                    continue;
                }

                boolean hasAssociatedClass = iSt.associatedClass != Long.MIN_VALUE;

                if (!hasAssociatedClass) {
                    float maxDifferenceToCreateNewClass = 0.03f; // maximal distance between existing class and this categorization to create a new class

                    float bestDist = Float.POSITIVE_INFINITY;
                    long bestGlobalClassId = 0;

                    for(Map.Entry<Long, ClassDatabase.Class> iEntry : classDatabase.classesByClassId.entrySet()) {
                        float dist = calcDistBetweenImageAndPrototype(subImg, iEntry.getValue().prototype);
                        if (dist < bestDist) {
                            bestDist = dist;
                            bestGlobalClassId = iEntry.getValue().class_;
                        }
                    }

                    boolean wasFoundBestClass = bestDist < maxDifferenceToCreateNewClass;
                    if (wasFoundBestClass) {
                        iSt.associatedClass = bestGlobalClassId;
                    }
                    else {
                        ClassDatabase.Class createdClass = classDatabase.createNewClass(128,128);
                        iSt.associatedClass = createdClass.class_; // link tracklet class to class in database
                    }
                }

                ClassDatabase.Class createdClass = classDatabase.retrieveById(iSt.associatedClass);

                // revise
                boolean isSubImageValid = subImg != null;
                if (isSubImageValid) {
                    createdClass.revise(subImg, this);
                }
            }
        }


        boolean showSpatialTracklets = true; // true
        boolean showDebugCursor = true; // true
        boolean showMotionparticles = true;


        if (showSpatialTracklets) {
            for(SpatialTracklet ist : spatialTracklets) {
                int posX = (int)ist.posX;
                int posY = (int)ist.posY;

                int crossRadius = 10;

                boolean isRecent = ist.idletime < 5;

                stroke(0,255,0, isRecent ? 255 : 80);
                strokeWeight(2.0f);


                line(posX-crossRadius,posY-crossRadius,posX+crossRadius,posY+crossRadius);
                line(posX+crossRadius,posY-crossRadius,posX-crossRadius,posY+crossRadius);

            }
        }

        if (showSpatialTracklets) {
            for(AdvancedSpatialTracklet iSt : advancedSpatialTracklets) {
                int posX = (int)iSt.centerX;
                int posY = (int)iSt.centerY;

                int crossRadius = 30;

                boolean isRecent = iSt.idletime < 5;

                stroke(255,0,0, isRecent ? 255 : 80);
                strokeWeight(4.0f);


                line(posX-crossRadius,posY-crossRadius,posX+crossRadius,posY+crossRadius);
                line(posX+crossRadius,posY-crossRadius,posX-crossRadius,posY+crossRadius);

                fill(255,0,0);
                text("st id="+Long.toString(iSt.id) + " cnt=" +Integer.toString(iSt.trainingDataOfThisClass.size()), (float)iSt.centerX, (float)iSt.centerY);


                if (iSt.prototypeCenterX != -1 && iSt.prototypeCenterY != -1) {
                    DebugCursor dc2 = new DebugCursor();
                    dc2.text = "PROTOTYPE BEST FOUND";
                    dc2.posX = (int)iSt.prototypeCenterX;
                    dc2.posY = (int)iSt.prototypeCenterY;
                    debugCursors.add(dc2);
                }
            }
        }

        // debug distances between subimages and prototypes
        if (showSpatialTracklets) {
            for (AdvancedSpatialTracklet iSt : advancedSpatialTracklets) {
                int posX = (int) iSt.centerX;
                int posY = (int) iSt.centerY;


                Map2d[] subimage = org.opennars.applications.cv.Util.subimage(posX, posY, 128, 128, img);
                boolean isSubimageValid = subimage != null;
                if (isSubimageValid) {
                    int idx = 0;

                    for(Map.Entry<Long, ClassDatabase.Class> iEntry : classDatabase.classesByClassId.entrySet()) {
                        MultichannelCentralDistPrototype prototype = iEntry.getValue().prototype;

                        // compute difference between actual (sub)image and the iterated prototype

                        calcDistBetweenImageAndPrototype(subimage, prototype);

                        fill(255,255,255);
                        //text("proto diff ["+idx+"]="+dist, posX, posY + idx * 20);
                        //System.out.println("proto diff ["+idx+"]="+dist);

                        idx++;
                    }

                }
            }
        }


        if (showDebugCursor) {
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
        }



        if (showMotionparticles) {
            fill(255);
            stroke(0,0,0,0); // transparent
            for(MotionParticle iMp : motionParticles) {
                rect((int)iMp.posX, (int)iMp.posY, 3, 3);
            }
        }

        boolean showDbPrototypes = true; // show the prototypes of all classes in the db

        if (showDbPrototypes) {
            int idx = 0;

            for( Map.Entry<Long, ClassDatabase.Class> iDbEntry : classDatabase.classesByClassId.entrySet()) {
                ClassDatabase.Class class_ = iDbEntry.getValue();

                int displayX = (idx % 8) * (128/2 + 30); // coordinate of the display of the class
                int displayY = 800 + (128/2 + 30)*(idx / 8);

                /*
                PImage protoImg = createImage(128,128, RGB); // image of prototype

                noStroke();

                for(int y=0;y<class_.prototype.channels[0].retHeight();y++) {
                    for(int x=0;x<class_.prototype.channels[0].retWidth();x++) {
                        double r = class_.prototype.channels[0].readAtSafe(y,x).mean;
                        double g = class_.prototype.channels[1].readAtSafe(y,x).mean;
                        double b = class_.prototype.channels[2].readAtSafe(y,x).mean;

                        protoImg.pixels[y*protoImg.width + x] = color((int)(r*255),(int)(g*255),(int)(b*255));


                    }
                }
                */

                boolean isProtoMeanImgValid = class_.protoMeanImg != null;
                if (isProtoMeanImgValid) {
                    image(class_.protoMeanImg, displayX, displayY, 128/2, 128/2);
                }

                idx++;
            }
        }

        boolean showStats = true; // show system statistics
        if (showStats) {
            fill(0);
            text("prototype search us="+(overalltimePrototypeSearchInNs/1000), 0, 0*15+15);

            text("img load wait    us="+(overallTimeImgLoadWaitInNs/1000), 0, 1*15+15);
            text("gpu upload:img  wait us="+(overalltimeWaitUploadImage/1000), 0, (2+1)*15); // wait time for finishing GPU upload of image
            text("gpu convl  wait us="+(overalltimeWaitConvl/1000),0,(3+1)*15); // wait time for finishing of GPU convolution
            text("conv to gray     us="+(timeConvertImageToGrayscaleWaitInNs/1000),0,4*15+15);

            text("classify NN      us"+(overallTimeWaitClassifyInNs/1000), 0, (5+1)*15);

            text("NAR wait         us="+(overallTimeNarWaitInNs/1000), 0, (6+1)*15);

            text("frametime        us="+((System.nanoTime()-systemTimeStart)/1000),0,(7+1)*15);
        }



        lastframe2 = img; // store last frame for attention and so on

        // set back to patrick standard
        stroke(128);
        strokeWeight(1.0f);


        //System.out.println("[d 1] Concepts: " + nar.memory.concepts.size());
    }


    // used to collect regions from the image
    private static class Region {
        public int minX = Integer.MAX_VALUE;
        public int minY = Integer.MAX_VALUE;
        public int maxX = Integer.MIN_VALUE;
        public int maxY = Integer.MIN_VALUE;

        public void merge(int x, int y) {
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }
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
