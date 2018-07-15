/**
 * Copyright (C) 2016 LibRec
 * <p>
 * This file is part of LibRec.
 * LibRec is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * LibRec is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with LibRec. If not, see <http://www.gnu.org/licenses/>.
 */
package net.librec.recommender.nn.rating;

import net.librec.annotation.ModelData;
import net.librec.common.LibrecException;
import net.librec.math.structure.MatrixEntry;
import net.librec.recommender.MatrixRecommender;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

/**
 * Suvash et al., <strong>AutoRec: Autoencoders Meet Collaborative Filtering</strong>, WWW Companion 2015.
 *
 * @author Ma Chen
 */
@ModelData({"isRating", "autorec", "autoRecModel", "trainSet"})
public class AutoRecRecommender extends MatrixRecommender {
    /**
     * the dimension of input units
     */
    private int inputDim;

    /**
     * the dimension of hidden units
     */
    private int hiddenDim;

    /**
     * the learning rate of the optimization algorithm
     */
    private double learningRate;

    /**
     * the momentum of the optimization algorithm
     */
    private double momentum;

    /**
     * the regularization coefficient of the weights in the neural network
     */
    private double lambdaReg;

    /**
     * the number of iterations
     */
    private int numIterations;

    /**
     * the activation function of the hidden layer in the neural network
     */
    private String hiddenActivation;

    /**
     * the activation function of the output layer in the neural network
     */
    private String outputActivation;

    /**
     * the autorec model
     */
    private MultiLayerNetwork autoRecModel;

    /**
     * the data structure that stores the training data
     */
    private INDArray trainSet;

    /**
     * the data structure that indicates which element in the user-item is non-zero
     */
    private INDArray trainSetMask;


    @Override
    protected void setup() throws LibrecException {
        super.setup();
        inputDim = numUsers;
        hiddenDim = conf.getInt("rec.hidden.dimension");
        learningRate = conf.getDouble("rec.iterator.learnrate");
        lambdaReg = conf.getDouble("rec.weight.regularization");
        numIterations = conf.getInt("rec.iterator.maximum");
        hiddenActivation = conf.get("rec.hidden.activation");
        outputActivation = conf.get("rec.output.activation");

        // transform the sparse matrix to INDArray
        int[] matrixShape = {numItems, numUsers};
        trainSet = Nd4j.zeros(matrixShape);
        trainSetMask = Nd4j.zeros(matrixShape);
        for (MatrixEntry me: trainMatrix) {
            trainSet.put(me.column(), me.row(), me.get());
            trainSetMask.put(me.column(), me.row(), 1);
        }
    }

    @Override
    protected void trainModel() throws LibrecException {
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .iterations(1)
                .updater(Updater.NESTEROVS)
                .learningRate(learningRate)
                .weightInit(WeightInit.XAVIER_UNIFORM)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .regularization(true)
                .l2(lambdaReg)
                .list()
                .layer(0, new DenseLayer.Builder().nIn(inputDim).nOut(hiddenDim)
                        //.activation(Activation.SIGMOID)
                        .activation(Activation.fromString(hiddenActivation))
                        .biasInit(0.1)
                        .build())
                .layer(1, new OutputLayer.Builder(new AutoRecLossFunction()).nIn(hiddenDim).nOut(inputDim)
                        //.activation(Activation.IDENTITY)
                        .activation(Activation.fromString(outputActivation))
                        .biasInit(0.1)
                        .build())
                .pretrain(false).backprop(true)
                .build();

        autoRecModel = new MultiLayerNetwork(conf);
        autoRecModel.init();

        for (int iter = 1; iter <= numIterations; iter++) {
            loss = 0.0d;

            AutoRecLossFunction.trainMask = trainSetMask;
            autoRecModel.fit(trainSet, trainSet);
            loss = autoRecModel.score();

            if (isConverged(iter) && earlyStop) {
                break;
            }
            lastLoss = loss;
        }
    }

    @Override
    protected double predict(int userIdx, int itemIdx) throws LibrecException {
        INDArray predictedRatingVector = autoRecModel.output(trainSet.getRow(itemIdx));
        return predictedRatingVector.getDouble(userIdx);
    }
}
