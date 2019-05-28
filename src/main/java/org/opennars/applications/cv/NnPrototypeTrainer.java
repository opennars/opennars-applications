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

import org.deeplearning4j.datasets.iterator.impl.ListDataSetIterator;
import org.deeplearning4j.nn.api.Model;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.api.BaseTrainingListener;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class NnPrototypeTrainer {

    public int outputNum = 50;
    public int nEpochs = 1000;

    public int batchsize = 5;

    public int depthPerPixel = 4;

    public MultiLayerNetwork network;


    public static class TrainingTuple {
        public float[] input;
        public int class_;
    }

    public void trainModel(List<TrainingTuple> tuples) {

        int rngSeed = 43;
        int rngSeed2 = 44;

        int numRows = 64;
        int numColumns = 64;

        int hiddenNeurons = 40;



        Random rng2 = new Random(rngSeed2);

        // generate the training data
        DataSetIterator iterator = getTrainingData(tuples,batchsize,rng2);

        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(rngSeed)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .updater(new Adam())
                .l2(1e-4)
                .list()

                .layer(new DenseLayer.Builder()

            .nIn(numRows * numColumns * depthPerPixel)
            .activation(Activation.RELU) // Activation function.
            .weightInit(WeightInit.XAVIER) // Weight initialization.
            .nOut(hiddenNeurons)
            .build())



                .layer(new DenseLayer.Builder()
                        .nIn(hiddenNeurons) // Number of input datapoints.
                        .nOut(50) // Number of output datapoints.
                        .activation(Activation.RELU) // Activation function.
                        .weightInit(WeightInit.XAVIER) // Weight initialization.
                        .build())
                .layer(new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                        .nIn(50)
                        .nOut(outputNum)
                        .activation(Activation.SOFTMAX)
                        .weightInit(WeightInit.XAVIER)
                        .build())
                //.pretrain(false).backprop(true) // commented because deprecated
                .build();



        // create the MLN
        network = new MultiLayerNetwork(conf);
        network.init();

        // pass a training listener that reports score every 10 iterations
        int eachIterations = 10;
        network.addListeners(new MyScoreIterationListener(eachIterations));

        // fit a dataset for a single epoch
        for( int iEpoch=0; iEpoch<nEpochs; iEpoch++ ){
            iterator.reset();
            network.fit(iterator);
        }

        System.out.println("trained!");


        // fit for multiple epochs
        // val numEpochs = 2
        // network.fit(new MultipleEpochsIterator(numEpochs, emnistTrain))

    }

    private DataSetIterator getTrainingData(List<TrainingTuple> tuples, int batchSize, Random rand){
        final Random rng = new Random(44);



        //List<org.nd4j.linalg.dataset.DataSet> listDs = dataSet.asList();

        List<org.nd4j.linalg.dataset.DataSet> listDs = new ArrayList<>();

        for(TrainingTuple iTuple : tuples) {
            if (iTuple.input.length != 64*64*depthPerPixel) {
                int here = 5;
            }

            double []out = new double[outputNum];
            for (int i= 0; i< out.length; i++) {
                out[i] = 0.0;
            }
            out[iTuple.class_] = 1;



            INDArray inputNDArray1 = Nd4j.create(iTuple.input);
            INDArray outPut = Nd4j.create(out);
            DataSet dataSet = new org.nd4j.linalg.dataset.DataSet(inputNDArray1, outPut);

            listDs.add(dataSet.asList().get(0));
        }


        Collections.shuffle(listDs, rng);
        return new ListDataSetIterator(listDs,batchSize);
    }

    private static class MyScoreIterationListener extends BaseTrainingListener {
        private final int eachIterations;

        public MyScoreIterationListener(int eachIterations) {
            this.eachIterations = eachIterations;
        }

        public void iterationDone(Model model, int iteration, int epoch) {
            if (iteration % eachIterations == 0) {
                double score = model.score();
                System.out.println("Score at iteration " +Integer.toString(iteration) + " is " + Double.toString(score));
            }
        }
    }
}
