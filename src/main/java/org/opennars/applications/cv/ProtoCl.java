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

import com.nativelibs4java.opencl.*;
import org.bridj.Pointer;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class ProtoCl {
    CLQueue queue;
    public CLContext context;
    CLProgram program;
    CLKernel kernel;

    public ProtoCl() throws CLBuildException {
        // Create a context with the best double numbers support possible :
        // (try using DeviceFeature.GPU, DeviceFeature.CPU...)
        CLContext context = JavaCL.createBestContext();

        // Create a command queue, if possible able to execute multiple jobs in parallel
        // (out-of-order queues will still respect the CLEvent chaining)
        CLQueue queue = context.createDefaultOutOfOrderQueueIfPossible();

        this.queue = queue;
        this.context = queue.getContext();

        String source = "// kernel for matching of color prototype to acutal image\n" +
                "__kernel void krnlMatchProto(\n" +
                "\t__global const int *imgColor,\n" +
                "\tint imgWidth,\n" +
                "\n" +
                "\t__global const float *prototypesRgb, // rgb color array of all prototypes\n" +
                "\t__global const int *prototypesRgbDistN, // sample count array of all prototypes\n" +
                "\t\n" +
                "\t__global const int *prototypesRgbIdx, // index into array prototypesRgb\n" +
                "\n" +
                "\t__global const int *prototypesSizeX, // size of single prototypes\n" +
                "\t__global const int *prototypesSizeY, // size of single prototypes\n" +
                "\n" +
                "\t__global const int *prototypesPosX, // positions of prototypes\n" +
                "\t__global const int *prototypesPosY, // positions of prototypes\n" +
                "\n" +
                "\t__global float *outRes // array of output result\n" +
                ") {\n" +
                "\tint myIdx=get_global_id(0);\n" +
                "\n" +
                "\tfloat sum = 0;\n" +
                "\n" +
                "\tint prototypeRgbIdx = prototypesRgbIdx[myIdx];\n" +
                "\tint prototypeSizeX = prototypesSizeX[myIdx];\n" +
                "\tint prototypeSizeY = prototypesSizeY[myIdx];\n" +
                "\n" +
                "\tint prototypePosX = prototypesPosX[myIdx];\n" +
                "\tint prototypePosY = prototypesPosY[myIdx];\n" +
                "\n" +
                "\tfloat nMass = 0; // overall mass of the samples\n" +
                "\n" +
                "\tfor(int iy=0;iy<prototypeSizeY;iy++) {\n" +
                "\t\tfor(int ix=0;ix<prototypeSizeX;ix++) {\n" +
                "\t\t\tint imgPosX = ix-prototypeSizeX/2 + prototypePosX;\n" +
                "\t\t\tint imgPosY = iy-prototypeSizeY/2 + prototypePosY;\n" +
                "\n" +
                "\t\t\tint nR = prototypesRgbDistN[prototypeRgbIdx + (iy*prototypeSizeX + ix)*3 + 0];\n" +
                "\t\t\tint nG = prototypesRgbDistN[prototypeRgbIdx + (iy*prototypeSizeX + ix)*3 + 1];\n" +
                "\t\t\tint nB = prototypesRgbDistN[prototypeRgbIdx + (iy*prototypeSizeX + ix)*3 + 2];\n" +
                "\n" +
                "\t\t\tfloat pR = prototypesRgb[prototypeRgbIdx + (iy*prototypeSizeX + ix)*3 + 0];\n" +
                "\t\t\tfloat pG = prototypesRgb[prototypeRgbIdx + (iy*prototypeSizeX + ix)*3 + 1];\n" +
                "\t\t\tfloat pB = prototypesRgb[prototypeRgbIdx + (iy*prototypeSizeX + ix)*3 + 2];\n" +
                "\n" +
                "\t\t\tint imgVal = imgColor[imgPosY*imgWidth + imgPosX];\n" +
                "\n" +
                "\t\t\tfloat imgR = (float)(imgVal & 255) / 255.0f;\n" +
                "\t\t\tfloat imgG = (float)((imgVal >> 8) & 255) / 255.0f;\n" +
                "\t\t\tfloat imgB = (float)((imgVal >> 16) & 255) / 255.0f;\n" +
                "\n" +
                "\t\t\tfloat diffR = fabs(pR - imgR);\n" +
                "\t\t\tfloat diffG = fabs(pG - imgG);\n" +
                "\t\t\tfloat diffB = fabs(pB - imgB);\n" +
                "\n" +
                "\t\t\tnMass += (nR + nG + nB);\n" +
                "\n" +
                "\t\t\tsum += (diffR*(float)nR + diffG*(float)nG + diffB*(float)nB);\n" +
                "\t\t\t//sum += (diffR + diffG + diffB);\n" +
                "\t\t}\n" +
                "\t}\n" +
                "\n" +
                "\tsum /= (prototypeSizeX*prototypeSizeY);\n" +
                "\n" +
                "\tif (nMass > 0) {\n" +
                "\t\tsum /= (float)nMass;\n" +
                "\t}\n" +
                "\n" +
                "\toutRes[myIdx] = sum;\n" +
                "}\n";

        //String source = IOUtils.readText(cl.class.getResource("DiscreteFourierTransformProgram.cl"));
        program = context.createProgram(source);
        kernel = program.createKernel("krnlMatchProto");
    }

    // class for caching the image between calls
    public static class CachedImage {
        public CLBuffer<Integer> imgColorBuf;

        // convert && upload image
        public void update(int[] imgColor, CLContext ctx) {
            IntBuffer imgColorBuffer = IntBuffer.wrap(imgColor);
            imgColorBuf = ctx.createIntBuffer(CLMem.Usage.Input, imgColorBuffer, true);
        }
    }

    /**
     * allocates the buffers for the positions
     * (is relativly expensive)
     * @param maxsize
     */
    public void allocateBuffersForPosition(int maxsize, int maxsizePrototypesRgbBuffer) {
        prototypesRgbBuf = context.createFloatBuffer(CLMem.Usage.Input, maxsizePrototypesRgbBuffer);
        prototypesRgbDistNBuf = context.createIntBuffer(CLMem.Usage.Input, maxsizePrototypesRgbBuffer);

        prototypesRgbIdxBuf = context.createIntBuffer(CLMem.Usage.Input, maxsize);
        prototypesSizeXBuf = context.createIntBuffer(CLMem.Usage.Input, maxsize);
        prototypesSizeYBuf = context.createIntBuffer(CLMem.Usage.Input, maxsize);
        prototypesPosXBuf = context.createIntBuffer(CLMem.Usage.Input, maxsize);
        prototypesPosYBuf = context.createIntBuffer(CLMem.Usage.Input, maxsize);

        outBuf = context.createFloatBuffer(CLMem.Usage.Output, maxsize);
    }

    public boolean areBuffersAllocated() {
        return prototypesRgbBuf != null;
    }

    CLBuffer<Float> prototypesRgbBuf;
    CLBuffer<Integer> prototypesRgbIdxBuf;
    CLBuffer<Integer> prototypesSizeXBuf, prototypesSizeYBuf;
    CLBuffer<Integer> prototypesPosXBuf, prototypesPosYBuf;
    CLBuffer<Integer> prototypesRgbDistNBuf;

    CLBuffer<Float> outBuf;

    // method to run the convolution kernel on the GPU
    // is a special method because it has to be syncronized because it is the only place where a single kernel is used
    private synchronized CLEvent runConvKernelInner(CLBuffer<Integer> imgColorBuf, int imgWidth, int length) {
        this.kernel.setArgs(
                imgColorBuf,
                imgWidth,

                prototypesRgbBuf,
                prototypesRgbDistNBuf,

                prototypesRgbIdxBuf,
                prototypesSizeXBuf,
                prototypesSizeYBuf,
                prototypesPosXBuf,
                prototypesPosYBuf,

                outBuf
        );

        //  parallel executions of the kernel in 1 dimension
        CLEvent convEvent = this.kernel.enqueueNDRange(queue, new int[]{ length });
        return convEvent;
    }

    // method to run the convolution kernel on the GPU
    public Pointer<Float> runKrnlMatchProto(CachedImage cachedImage, int imgWidth, FloatBuffer prototypesRgb, IntBuffer prototypesRgbDistN, IntBuffer prototypesRgbIdx, IntBuffer prototypesSizeX, IntBuffer prototypesSizeY, IntBuffer prototypesPosX, IntBuffer prototypesPosY, int length) {
        CLBuffer<Integer> imgColorBuf = cachedImage.imgColorBuf;

        long before = System.nanoTime();
        CLEvent e0 = prototypesRgbBuf.write(queue, 0, prototypesRgb.capacity(), prototypesRgb, false);
        CLEvent e1 = prototypesRgbDistNBuf.write(queue, 0, prototypesRgbDistN.capacity(), prototypesRgbDistN, false);
        CLEvent e2 = prototypesRgbIdxBuf.write(queue, 0, prototypesRgbIdx.capacity(), prototypesRgbIdx, false);

        CLEvent e3 = prototypesSizeXBuf.write(queue, 0, prototypesSizeX.capacity(), prototypesSizeX, false);
        CLEvent e4 = prototypesSizeYBuf.write(queue, 0, prototypesSizeY.capacity(), prototypesSizeY, false);

        CLEvent e5 = prototypesPosXBuf.write(queue, 0, prototypesPosX.capacity(), prototypesPosX, false);
        CLEvent e6 = prototypesPosYBuf.write(queue, 0, prototypesPosY.capacity(), prototypesPosY, false);

        queue.enqueueWaitForEvents(e0, e1, e2, e3, e4, e5, e6);


        CLEvent convEvent = runConvKernelInner(imgColorBuf, imgWidth, length);

        // Return an NIO buffer read from the output CLBuffer
        Pointer<Float> ptr = outBuf.read(queue, convEvent);
        long after = System.nanoTime();
        //System.out.println(Long.toString((after-before)/1000));

        return ptr;
    }

    // Wrapper method that takes and returns float array
    public float[] runKrnl(CachedImage cachedImage,  int imgWidth, float[] prototypesRgb, int[] prototypesRgbDistN, int[] prototypesRgbIdx, int[] prototypesSizeX, int[] prototypesSizeY, int[] prototypesPosX, int[] prototypesPosY, int length) {
        if (length == 0) {
            return new float[0]; // special case
        }

        Pointer<Float> outBuffer = runKrnlMatchProto(cachedImage, imgWidth, FloatBuffer.wrap(prototypesRgb), IntBuffer.wrap(prototypesRgbDistN), IntBuffer.wrap(prototypesRgbIdx), IntBuffer.wrap(prototypesSizeX), IntBuffer.wrap(prototypesSizeY), IntBuffer.wrap(prototypesPosX), IntBuffer.wrap(prototypesPosY), length);
        long before = System.nanoTime();
        float[] out = new float[length];
        for(int idx=0;idx<length;idx++) {
            out[idx]=outBuffer.get(idx);
        }
        long after = System.nanoTime();
        //System.out.println("readout time=" + Long.toString((after-before)/1000));
        return out;
    }
}
