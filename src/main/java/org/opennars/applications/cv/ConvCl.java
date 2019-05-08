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

import com.nativelibs4java.opencl.*;
import org.bridj.Pointer;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

// convolutions on a grayscale image
public class ConvCl {
    CLQueue queue;
    CLContext context;
    CLProgram program;
    CLKernel kernel;

    public ConvCl() throws CLBuildException {
        // Create a context with the best double numbers support possible :
        // (try using DeviceFeature.GPU, DeviceFeature.CPU...)
        CLContext context = JavaCL.createBestContext();

        // Create a command queue, if possible able to execute multiple jobs in parallel
        // (out-of-order queues will still respect the CLEvent chaining)
        CLQueue queue = context.createDefaultOutOfOrderQueueIfPossible();

        this.queue = queue;
        this.context = queue.getContext();

        String source = "// kernel for convolution of a grayscale image\n" +
                "__kernel void conv0(\n" +
                "\t__global const short *imgGrayscale,\n" +
                "\tint imgWidth,\n" +
                "\t__global const float *kernel2,\n" +
                "\tint kernelSize,\n" +
                "\t__global float *out,\n" +
                "\t__global int *posX,\n" +
                "\t__global int *posY\n" +
                ") {\n" +
                "\tint myIdx=get_global_id(0);\n" +
                "\n" +
                "\tfloat sum = 0;\n" +
                "\tfor(int dy=-kernelSize/2;dy<kernelSize/2;dy++) {\n" +
                "\t\tfor(int dx=-kernelSize/2;dx<kernelSize/2;dx++) {\n" +
                "\t\t\tint x = posX[myIdx]+dx;\n" +
                "\t\t\tint y = posY[myIdx]+dy;\n" +
                "\n" +
                "\t\t\tshort imgShort = imgGrayscale[y*imgWidth + x];\n" +
                "\t\t\tfloat img = (float)imgShort / 255.0f;\n" +
                "\n" +
                "\t\t\tint kernelX = dx + kernelSize/2;\n" +
                "\t\t\tint kernelY = dy + kernelSize/2;\n" +
                "\t\t\tfloat krnl = kernel2[kernelY*kernelSize + kernelX];\n" +
                "\n" +
                "\t\t\tsum += (img*krnl); // actual convolution\n" +
                "\t\t}\n" +
                "\t}\n" +
                "\n" +
                "\tout[myIdx] = sum;\n" +
                "}\n";

        //String source = IOUtils.readText(cl.class.getResource("DiscreteFourierTransformProgram.cl"));
        program = context.createProgram(source);
        kernel = program.createKernel("conv0");
    }

    // method to run the convolution kernel on the GPU
    public synchronized Pointer<Float> runConvKernel(ShortBuffer imgGrayscale, int imgWidth, FloatBuffer kernel, int kernelSize, IntBuffer posX, IntBuffer posY, int length) {
        CLBuffer<Short> imgGrayscaleBuf = context.createShortBuffer(CLMem.Usage.Input, imgGrayscale, true);
        CLBuffer<Float> kernelBuf = context.createFloatBuffer(CLMem.Usage.Input, kernel, true);
        CLBuffer<Integer> posXBuf = context.createIntBuffer(CLMem.Usage.Input, posX, true);
        CLBuffer<Integer> posYBuf = context.createIntBuffer(CLMem.Usage.Input, posY, true);


        CLBuffer<Float> outBuf = context.createFloatBuffer(CLMem.Usage.Output, length);


        this.kernel.setArgs(
            imgGrayscaleBuf,
            imgWidth,

            kernelBuf,
            kernelSize,

            outBuf,

            posXBuf,
            posYBuf
        );


        // Ask for `length` parallel executions of the kernel in 1 dimension :
        CLEvent convEvent = this.kernel.enqueueNDRange(queue, new int[]{ length });

        // Return an NIO buffer read from the output CLBuffer :
        Pointer<Float> ptr = outBuf.read(queue, convEvent);
        return ptr;
    }

    // Wrapper method that takes and returns float array
    public float[] runConv(short[] imgGrayscale, int imgWidth, float[] kernel, int kernelSize, int[] posX, int[] posY, int length) {
        Pointer<Float> outBuffer = runConvKernel(ShortBuffer.wrap(imgGrayscale), imgWidth, FloatBuffer.wrap(kernel), kernelSize, IntBuffer.wrap(posX), IntBuffer.wrap(posY), length);
        float[] out = new float[length];
        for(int idx=0;idx<length;idx++) {
            out[idx]=outBuffer.get(idx);
        }
        return out;
    }

    /*

    public static void main(String[] args) throws IOException, CLBuildException {


        ConvCl dft = new ConvCl(queue);
        //DFT2 dft = new DFT2(queue);

        // Create some fake test data :
        double[] in = createTestDoubleData();

        // Transform the data (spatial -> frequency transform) :
        double[] transformed = dft.dft(in, true);

        for (int i = 0; i < transformed.length / 2; i++) {
            // Print the transformed complex values (real + i * imaginary)
            System.out.println(transformed[i * 2] + "\t + \ti * " + transformed[i * 2 + 1]);
        }

        // Reverse-transform the transformed data (frequency -> spatial transform) :
        double[] backTransformed = dft.dft(transformed, false);

        // Check the transform + inverse transform give the original data back :
        double precision = 1e-5;
        for (int i = 0; i < in.length; i++) {
            if (Math.abs(in[i] - backTransformed[i]) > precision)
                throw new RuntimeException("Different values in back-transformed array than in original array !");
        }
    }*/
}
