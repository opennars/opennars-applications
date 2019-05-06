/*
            DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
                    Version 2, December 2004

 Copyright (C) 2019 Robert Wünsche <rt09@protonmail.com>

 Everyone is permitted to copy and distribute verbatim or modified
 copies of this license document, and changing it is allowed as long
 as the name is changed.

            DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
   TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION

  0. You just DO WHAT THE FUCK YOU WANT TO.
 */

package org.opennars.applications.cv;

import processing.core.PImage;

public class Conv {
    private static class KernelConf {
        public final float dx;
        public final float dy;

        public KernelConf(float dx, float dy) {
            this.dx = dx;
            this.dy = dy;
        }

        public Map2d precalculatedKernel;
    }

    // configurations of all kernels
    public static KernelConf[] kernels = new KernelConf[]{new KernelConf(1.0f, 0.0f), new KernelConf(-1.0f, 0.0f), new KernelConf(0.0f, -1.0f),new KernelConf(0.0f, 1.0f)};

    public static void precalcKernels() {
        for(int kernelIdx=0;kernelIdx<kernels.length;kernelIdx++) {
            int kernelsize = 32;
            float freq = 1.0f;
            float delta = 0.8f;

            kernels[kernelIdx].precalculatedKernel = new Map2d(kernelsize, kernelsize);

            for(int dy=-kernelsize/2;dy<kernelsize/2;dy++) {
                for(int dx=-kernelsize/2;dx<kernelsize/2;dx++) {
                    int ax = dx + kernelsize/2;
                    int ay = dy + kernelsize/2;

                    double kernelVal = calcKernel((float)dx/(kernelsize/2.0f), (float)dy/(kernelsize/2.0f), kernels[kernelIdx].dx, kernels[kernelIdx].dy, freq, delta);

                    kernels[kernelIdx].precalculatedKernel.arr[ay][ax] = (float)kernelVal;
                }
            }
        }
    }

    static {
        precalcKernels();
    }

    // convolution with standard parameters
    public static float[] convAt(PImage img, int imageX, int imageY, int imageSize, int stride) {
        //int kernelsize = 32;
        //float freq = 1.0f;
        //float delta = 0.8f;


        float[] arr = new float[imageSize*imageSize * kernels.length];

        for(int kernelIdx=0;kernelIdx<kernels.length;kernelIdx++) {
            //float dirx = kernels[kernelIdx].dx;
            //float diry = kernels[kernelIdx].dy;

            for(int dy = 0; dy < imageSize; dy++) {
                for(int dx=0; dx < imageSize; dx++) {
                    int dxRel = (dx - imageSize/2) * stride;
                    int dyRel = (dy - imageSize/2) * stride;

                    //float convRes = (float)conv(img, imageX + dxRel, imageY + dyRel, kernelsize, dirx, diry, freq, delta);
                    float convRes = (float)conv2(img, imageX + dxRel, imageY + dyRel, kernels[kernelIdx].precalculatedKernel);

                    arr[(dy*imageSize + dx)         + kernelIdx * imageSize*imageSize] = convRes;
                }
            }

        }

        return arr;
    }


    public static double conv2(PImage img, int kernelCenterX, int kernelCenterY, Map2d kernel) {

        double res = 0; // result of convolution

        int kernelSize = kernel.retHeight();

        for(int dy=-kernelSize/2;dy<kernelSize/2;dy++) {
            for(int dx=-kernelSize/2;dx<kernelSize/2;dx++) {
                int ix = kernelCenterX + dx;
                int iy = kernelCenterY + dy;

                if (ix < 0 || ix >= img.width || iy < 0 || iy >= img.height) {
                    continue;
                }

                int colorcode =  img.pixels[iy*img.width+ix];
                //TODO check if the rgb is extracted correctly
                float r = (colorcode & 0xff) / 255.0f;
                float g = ((colorcode >> 8) & 0xFF) / 255.0f;
                float b = ((colorcode >> 8*2) & 0xFF) / 255.0f;

                float grayscale = (r+g+b)/3.0f;


                double kernelVal = kernel.readAtUnbound(dy+kernelSize/2,dx+kernelSize/2); // 1.0f; //calcKernel((float)dx/(kernelSize/2.0f), (float)dy/(kernelSize/2.0f), dirx, diry, freq, delta);

                res += (grayscale*kernelVal);

            }
        }

        res /= (kernelSize*kernelSize);
        return res;
    }

    /*
    public static double conv(PImage img, int kernelCenterX, int kernelCenterY, int kernelSize, float dirx, float diry, float freq, float delta) {

        double res = 0; // result of convolution

        for(int dy=-kernelSize/2;dy<kernelSize/2;dy++) {
            for(int dx=-kernelSize/2;dx<kernelSize/2;dx++) {
                int ix = kernelCenterX + dx;
                int iy = kernelCenterY + dy;

                if (ix < 0 || ix >= img.width || iy < 0 || iy >= img.height) {
                    continue;
                }

                int colorcode =  img.pixels[iy*img.width+ix];
                //TODO check if the rgb is extracted correctly
                float r = (colorcode & 0xff) / 255.0f;
                float g = ((colorcode >> 8) & 0xFF) / 255.0f;
                float b = ((colorcode >> 8*2) & 0xFF) / 255.0f;

                float grayscale = (r+g+b)/3.0f;


                double kernelVal = 1.0f; //calcKernel((float)dx/(kernelSize/2.0f), (float)dy/(kernelSize/2.0f), dirx, diry, freq, delta);

                res += (grayscale*kernelVal);

            }
        }

        res /= (kernelSize*kernelSize);
        return res;
    }
    */


    // no idea where the original algorithm of this can be found or how it was called
    /**
     * computes a wavelet for the detection of edges
     * @param x computed x position
     * @param y computed y position
     * @param dirx direction of the wavelet - must be normalized
     * @param diry direction of the wavelet - must be normalized
     * @param freq frequency of the wavelet
     * @param delta
     * @return
     */
    public static double calcKernel(float x, float y, float dirx, float diry, float freq, float delta) {
        float dotDir = x*dirx + y*diry; // dot product to compute the wave form in that direction

        double mul = Math.sin(dotDir*freq * (2.0 * Math.PI));
        double marr = calculateMarrWavelet(x,y,delta);

        return mul*marr; // result is defined as the multiplication
    }

    // http://de.wikipedia.org/wiki/David_Marr
    /**
     *
     * also known as the "Mexican hat" function
     *
     */
    public static float calculateMarrWavelet(float x, float y, float delta) {
        float distToMean;
        float factorA;
        float factorB;
        float gaussianTerm;

        distToMean = x * x + y * y;

        factorA = -1.0f / ((float)Math.PI * delta * delta * delta * delta);
        factorB = 1.0f - distToMean / (2.0f * delta * delta);
        gaussianTerm = gaussianExponentTerm(distToMean, delta);

        return factorA * factorB * gaussianTerm;
    }

    // extra function because it is used for other stuff
    public static float gaussianExponentTerm(float distToMean, float delta) {
        return (float)Math.exp(-0.5f * ((distToMean * distToMean) / (delta * delta)));
    }
}
