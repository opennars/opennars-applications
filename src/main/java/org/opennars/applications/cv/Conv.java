/*
 * The MIT License
 *
 * Copyright 2019 Robert Wünsche <rt09@protonmail.com>
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

package org.opennars.applications.cv;

public class Conv {
    /**
     * Configuration of a kernel
     */
    public static final class KernelConf {
        public final float dx;
        public final float dy;

        public KernelConf(float dx, float dy) {
            this.dx = dx;
            this.dy = dy;
        }

        public Map2d precalculatedKernel;
        public float[] precaculatedFlattenedKernel;
    }




    // configurations of all kernels
    public static KernelConf[] kernels = new KernelConf[]{
        new KernelConf(1.0f, 0.0f)/*, new KernelConf(-1.0f, 0.0f), new KernelConf(0.0f, -1.0f)*/,new KernelConf(0.0f, 1.0f),
            new KernelConf(1.0f/(float)Math.sqrt(2.0f), 1.0f/(float)Math.sqrt(2.0f)),
            new KernelConf(-1.0f/(float)Math.sqrt(2.0f), 1.0f/(float)Math.sqrt(2.0f)),
            //new KernelConf(1.0f/(float)Math.sqrt(2.0f), -1.0f/(float)Math.sqrt(2.0f)),
            //new KernelConf(-1.0f/(float)Math.sqrt(2.0f), -1.0f/(float)Math.sqrt(2.0f)),
    };

    public static void precalcKernels() {
        for(int kernelIdx=0;kernelIdx<kernels.length;kernelIdx++) {
            int kernelsize = 24;
            float freq = 1.0f;
            float delta = 0.8f;

            Map2d kernel = new Map2d(kernelsize, kernelsize);

            for(int dy=-kernelsize/2;dy<kernelsize/2;dy++) {
                for(int dx=-kernelsize/2;dx<kernelsize/2;dx++) {
                    int ax = dx + kernelsize/2;
                    int ay = dy + kernelsize/2;

                    double kernelVal = calcKernel((float)dx/(kernelsize/2.0f), (float)dy/(kernelsize/2.0f), kernels[kernelIdx].dx, kernels[kernelIdx].dy, freq, delta);

                    kernel.writeAtSafe(ay, ax, (float)kernelVal);
                }
            }

            kernels[kernelIdx].precalculatedKernel = kernel;

            // compute flattened kernel
            kernels[kernelIdx].precaculatedFlattenedKernel = new float[kernel.retWidth()*kernel.retHeight()];
            for(int iy=0;iy<kernel.retHeight();iy++) {
                for(int ix=0;ix<kernel.retWidth();ix++) {
                    kernels[kernelIdx].precaculatedFlattenedKernel[iy * kernel.retWidth() + ix] = kernel.readAtSafe(iy, ix);
                }
            }
        }
    }

    static {
        precalcKernels();
    }

    // convolution with standard parameters
    public static float[] convAt(Map2d img, int imageX, int imageY, int imageSize, int stride) {
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
                    float convRes = (float)conv3(img, imageX + dxRel, imageY + dyRel, kernels[kernelIdx].precalculatedKernel);

                    arr[(dy*imageSize + dx)         + kernelIdx * imageSize*imageSize] = convRes;
                }
            }

        }


        return arr;
    }




    public static double conv3(Map2d img, int kernelCenterX, int kernelCenterY, Map2d kernel) {
        int kernelSize = kernel.retWidth();

        float res = 0; // result of convolution

        // special case which we don't handle here because the border of the image is not interesting at all
        if (kernelCenterX <= kernelSize/2 || kernelCenterX >= img.retWidth() - kernelSize/2) {
            return 0;
        }
        if (kernelCenterY <= kernelSize/2 || kernelCenterY >= img.retHeight() - kernelSize/2) {
            return 0;
        }

        for(int dy=-kernelSize/2;dy<kernelSize/2;dy++) {
            for(int dx=-kernelSize/2;dx<kernelSize/2;dx++) {
                int ix = kernelCenterX + dx;
                int iy = kernelCenterY + dy;

                float grayscale = img.readAtUnsafe(iy, ix);

                //double kernelVal = kernel.readAtSafe(dy+kernelSize/2,dx+kernelSize/2); // 1.0f; //calcKernel((float)dx/(kernelSize/2.0f), (float)dy/(kernelSize/2.0f), dirx, diry, freq, delta);
                double kernelVal = kernel.readAtUnsafe(dy+kernelSize/2,dx+kernelSize/2);

                res += (grayscale*kernelVal);
            }
        }

        res /= (kernelSize*kernelSize);
        return res;
    }


    public static double conv4(int[] imgArr, int imgWidth, int imgHeight, int kernelCenterX, int kernelCenterY, float[] kernel, int kernelSize) {

        double res = 0; // result of convolution

        for(int dy=-kernelSize/2;dy<kernelSize/2;dy++) {
            for(int dx=-kernelSize/2;dx<kernelSize/2;dx++) {
                int ix = kernelCenterX + dx;
                int iy = kernelCenterY + dy;

                if (ix < 0 || ix >= imgWidth || iy < 0 || iy >= imgHeight) {
                    continue;
                }

                int colorcode =  imgArr[iy*imgWidth+ix];
                //TODO check if the rgb is extracted correctly
                float r = (colorcode & 0xff) / 255.0f;
                float g = ((colorcode >> 8) & 0xFF) / 255.0f;
                float b = ((colorcode >> 8*2) & 0xFF) / 255.0f;

                float grayscale = (r+g+b)/3.0f;


                //double kernelVal = kernel.readAtSafe(dy+kernelSize/2,dx+kernelSize/2); // 1.0f; //calcKernel((float)dx/(kernelSize/2.0f), (float)dy/(kernelSize/2.0f), dirx, diry, freq, delta);
                double kernelVal = kernel[(dy+kernelSize/2)*kernelSize    +  dx+kernelSize/2];//.readAtSafe(dy+kernelSize/2,dx+kernelSize/2);

                res += (grayscale*kernelVal);

            }
        }

        res /= (kernelSize*kernelSize);
        return res;
    }

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
