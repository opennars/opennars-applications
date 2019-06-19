package org.opennars.applications.cv;

import processing.core.PApplet;
import processing.core.PImage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

public class VisionContext {


    // pool used for all general work
    ThreadPoolExecutor generalPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(8);

    public ImageLoader imageLoader = new ImageLoader();

    public class ImageLoader {
        long nrLoader = 1; // iterator of image number for loader
        List<Future<PImage>> imageLoaderFutures = new ArrayList<>(); // futures for the loading of the images

        int imagePreloadCount = 4; // number of images which are preloaded

        public String videopath;

        public long overallTimeImgLoadWaitInNs = 0; // timer

        public PImage fetchNextImgage(PApplet applet) {
            while (imageLoaderFutures.size() < imagePreloadCount) {
                Future<PImage> f = generalPool.submit(() -> {
                    String nr = String.format("%05d", nrLoader);
                    PImage img2 = applet.loadImage(videopath+nr+".jpg");
                    nrLoader++;
                    return img2;
                });
                imageLoaderFutures.add(f);
            }

            // wait for loading of latest enqueed image
            long startTime = System.nanoTime();
            while(!imageLoaderFutures.get(0).isDone()){}
            overallTimeImgLoadWaitInNs += (System.nanoTime() - startTime);

            PImage img = null;
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

            return img;
        }

    }
}
