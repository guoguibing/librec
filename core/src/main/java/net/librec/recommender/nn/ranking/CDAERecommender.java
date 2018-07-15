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
package net.librec.recommender.nn.ranking;

import net.librec.annotation.ModelData;
import net.librec.common.LibrecException;
import net.librec.math.structure.MatrixEntry;
import net.librec.recommender.MatrixRecommender;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.LossFunctions;

/**
 * Yao et al., <strong>Collaborative Denoising Auto-Encoders for Top-N Recommender Systems, WSDM 2016.
 *
 * @author Ma Chen
 */
@ModelData({"isRanking", "cdae", "CDAEModel", "predictedMatrix"})
public class CDAERecommender extends MatrixRecommender {
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
    private MultiLayerNetwork CDAEModel;

    /**
     * the data structure that stores the training data
     */
    private INDArray trainSet;

    /**
     * the data structure that stores the predicted data
     */
    private INDArray predictedMatrix;


    @Override
    protected void setup() throws LibrecException {
        super.setup();
        inputDim = numItems;
        hiddenDim = conf.getInt("rec.hidden.dimension");
        learningRate = conf.getDouble("rec.iterator.learnrate");
        lambdaReg = conf.getDouble("rec.weight.regularization");
        numIterations = conf.getInt("rec.iterator.maximum");
        hiddenActivation = conf.get("rec.hidden.activation");
        outputActivation = conf.get("rec.output.activation");

        // transform the sparse matrix to INDArray
        // the sparse training matrix has been binarized
        int[] matrixShape = {numUsers, numItems};
        trainSet = Nd4j.zeros(matrixShape);
        for (MatrixEntry me: trainMatrix) {
            trainSet.put(me.row(), me.column(), me.get());
        }
    }

    @Override
    protected void trainModel() throws LibrecException {
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(6)
                .iterations(1)
                .updater(Updater.ADAM)
                .learningRate(learningRate)
                .weightInit(WeightInit.XAVIER)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .regularization(true)
                .l2(lambdaReg)
                .list()
                .layer(0, new CDAELayer.Builder().nIn(inputDim).nOut(hiddenDim)
                        .activation(Activation.fromString(hiddenActivation))
                        .setNumUsers(numUsers)
                        .build())
                .layer(1, new OutputLayer.Builder().nIn(hiddenDim).nOut(inputDim)
                        .lossFunction(LossFunctions.LossFunction.SQUARED_LOSS)
                        .activation(Activation.fromString(outputActivation))
                        .build())
                .pretrain(false).backprop(true)
                .build();

        CDAEModel = new MultiLayerNetwork(conf);
        CDAEModel.init();

        for (int iter = 1; iter <= numIterations; iter++) {
            loss = 0.0d;

            CDAEModel.fit(trainSet, trainSet);
            loss = CDAEModel.score();

            if (isConverged(iter) && earlyStop) {
                break;
            }

            lastLoss = loss;
        }

        // calculate the predicted ratings and filter out the items that appear in training set
        predictedMatrix = CDAEModel.output(trainSet);
        for (MatrixEntry me: trainMatrix) {
            predictedMatrix.put(me.row(), me.column(), 0);
        }
    }

    @Override
    protected double predict(int userIdx, int itemIdx) throws LibrecException {
        return predictedMatrix.getDouble(userIdx, itemIdx);
    }
}
