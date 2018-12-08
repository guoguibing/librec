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
package net.librec.recommender.content;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Table;
import net.librec.common.LibrecException;
import net.librec.math.algorithm.Maths;
import net.librec.math.structure.*;
import net.librec.recommender.TensorRecommender;

import java.util.ArrayList;
import java.util.List;

/**
 * TopicMF-AT Recommender
 * Yang Bao, Hui Fang, Jie Zhang. TopicMF: Simultaneously Exploiting Ratings and Reviews for Recommendation[C]
 * {@code 2014, Association for the Advancement of Artificial Intelligence (www.aaai.org)}.
 *
 * @author SunYatong
 */
public class TopicMFATRecommender extends TensorRecommender {

    protected SequentialAccessSparseMatrix trainMatrix;
    protected SequentialAccessSparseMatrix W;
    protected DenseMatrix theta;
    protected DenseMatrix phi;
    protected double K1, K2;
    protected VectorBasedDenseVector userBiases;
    protected VectorBasedDenseVector itemBiases;
    protected DenseMatrix userFactors;
    protected DenseMatrix itemFactors;
    protected int numTopics;
    protected int numWords;
    protected int numDocuments;
    protected BiMap<Integer, String> reviewMappingData;
    protected double lambda, lambdaU, lambdaV, lambdaB;
    protected BiMap<String, Integer> wordIdToWordIndex;
    protected Table<Integer, Integer, Integer> userItemToDocument;
    protected float initMean;
    protected float initStd;
    protected int[][] documentTopWordIdices;
    protected int topNum = 5;

    @Override
    protected void setup() throws LibrecException {
        super.setup();
        reviewMappingData = DataFrame.getInnerMapping("review").inverse();
        // init hyper-parameters
        lambda = conf.getDouble("rec.regularization.lambda", 0.001);
        lambdaU = conf.getDouble("rec.regularization.lambdaU", 0.001);
        lambdaV = conf.getDouble("rec.regularization.lambdaV", 0.001);
        lambdaB = conf.getDouble("rec.regularization.lambdaB", 0.001);
        numTopics = conf.getInt("rec.topic.number", 10);
        learnRate = conf.getFloat("rec.iterator.learnrate", 0.01f);
        numIterations = conf.getInt("rec.iterator.maximum", 10);
        trainTensor = (SparseTensor) getDataModel().getTrainDataSet();
        trainMatrix = trainTensor.rateMatrix();
        numDocuments = trainMatrix.size();

        // count the number of words, build the word dictionary and userItemToDoc dictionary
        wordIdToWordIndex = HashBiMap.create();
        Table<Integer, Integer, Double> res = HashBasedTable.create();
        int rowCount = 0;
        userItemToDocument = HashBasedTable.create();
        for (TensorEntry te : trainTensor) {
            int[] entryKeys = te.keys();
            int userIndex = entryKeys[0];
            int itemIndex = entryKeys[1];
            int reviewIndex = entryKeys[2];
            userItemToDocument.put(userIndex, itemIndex, rowCount);
            // convert wordIds to wordIndices
            String reviewContent = reviewMappingData.get(reviewIndex);
            String[] fReviewContent = reviewContent.split(":");
            for (String word : fReviewContent) {
                if (!wordIdToWordIndex.containsKey(word)) {
                    wordIdToWordIndex.put(word, numWords);
                    numWords++;
                }
            }
            List<Integer> wordIndexList = new ArrayList<>();
            for (String word : fReviewContent) {
                wordIndexList.add(wordIdToWordIndex.get(word));
            }
            double denominator = wordIndexList.size();
            for (int wordIdx : wordIndexList) {
                Double oldValue = res.get(rowCount, wordIdx);
                if (oldValue == null) {
                    oldValue = 0.0;
                }
                double newValue = oldValue + 1 / denominator;
                res.put(rowCount, wordIdx, newValue);
            }
            rowCount++;
        }
        // build W
        W = new SequentialAccessSparseMatrix(numDocuments, numWords, res);

        // init parameters
        initMean = conf.getFloat("rec.init.mean", 0.0f);
        initStd = conf.getFloat("rec.init.std", 0.01f);
        userBiases = new VectorBasedDenseVector(numUsers);
        userBiases.init(initMean, initStd);
        itemBiases = new VectorBasedDenseVector(numItems);
        itemBiases.init(initMean, initStd);
        userFactors = new DenseMatrix(numUsers, numTopics);
        userFactors.init(initMean, initStd);
        itemFactors = new DenseMatrix(numItems, numTopics);
        itemFactors.init(initMean, initStd);
        K1 = initStd;
        K2 = initStd;

        // init theta and phi
        theta = new DenseMatrix(numDocuments, numTopics);
        calculateTheta();
        phi = new DenseMatrix(numTopics, numWords);
        phi.init(0.01);

        LOG.info("number of users : " + numUsers);
        LOG.info("number of Items : " + numItems);
        LOG.info("number of words : " + wordIdToWordIndex.size());
    }

    @Override
    protected void trainModel() throws LibrecException {
        for (int iter = 1; iter <= numIterations; iter++) {
            loss = 0.0;
            double wordLoss = 0.0;
            for (MatrixEntry me : trainMatrix) {
                int i = me.row();     // userIdx
                int j = me.column();  // itemIdx
                int documentId = userItemToDocument.get(i, j);
                double y_true = me.get();
                double y_pred = predict(i, j);

                double error = y_true - y_pred;
                loss += error * error;

                // update user item biases
                double userBiasValue = userBiases.get(i);
                userBiases.plus(i, learnRate * (error - lambdaB * userBiasValue));
                loss += lambdaB * userBiasValue * userBiasValue;

                double itemBiasValue = itemBiases.get(j);
                itemBiases.plus(j, learnRate * (error - lambdaB * itemBiasValue));
                loss += lambdaB * itemBiasValue * itemBiasValue;

                // update user item factors
                for (int factorIdx = 0; factorIdx < numTopics; factorIdx++) {
                    double userFactorValue = userFactors.get(i, factorIdx);
                    double itemFactorValue = itemFactors.get(j, factorIdx);

                    userFactors.plus(i, factorIdx, learnRate * (error * itemFactorValue - lambdaU * userFactorValue));
                    itemFactors.plus(j, factorIdx, learnRate * (error * userFactorValue - lambdaV * itemFactorValue));
                    loss += lambdaU * userFactorValue * userFactorValue + lambdaV * itemFactorValue * itemFactorValue;

                    SequentialSparseVector wordVec = W.row(documentId);
                    for (Vector.VectorEntry ve : wordVec) {
                        int wordIdx = ve.index();
                        double w_true = ve.get();
                        double w_pred = theta.row(documentId).dot(phi.column(wordIdx));
                        double w_error = w_true - w_pred;
                        wordLoss += w_error;

                        double derivative = 0.0;
                        for (int topicIdx = 0; topicIdx < numTopics; topicIdx++) {
                            if (factorIdx == topicIdx) {
                                derivative += w_error * phi.get(topicIdx, wordIdx) * theta.get(documentId, topicIdx) * (1 - theta.get(documentId, topicIdx));
                            } else {
                                derivative += w_error * phi.get(topicIdx, wordIdx) * theta.get(documentId, topicIdx) * (-theta.get(documentId, factorIdx));
                            }
                            //update K1 K2
                            K1 += learnRate * lambda * w_error * phi.get(topicIdx, wordIdx) * theta.get(documentId, topicIdx) * (1 - theta.get(documentId, topicIdx)) * Math.abs(userFactors.get(i, topicIdx));
                            K2 += learnRate * lambda * w_error * phi.get(topicIdx, wordIdx) * theta.get(documentId, topicIdx) * (1 - theta.get(documentId, topicIdx)) * Math.abs(itemFactors.get(j, topicIdx));
                        }
                        userFactors.plus(i, factorIdx, learnRate * K1 * derivative);
                        itemFactors.plus(j, factorIdx, learnRate * K2 * derivative);

                    }
                }
            }
            // calculate theta
            LOG.info(" iter:" + iter + ", finish factors update");
            calculateTheta();
            LOG.info(" iter:" + iter + ", finish theta update");
            // update phi by NMF
            DenseMatrix thetaTW = theta.transpose().times(W);
            DenseMatrix denominatorMatrix = theta.transpose().times(theta).times(phi);
            for (int i = 0; i < numTopics; i++) {
                for (int j = 0; j < numWords; j++) {
                    double numerator = phi.get(i, j) * (thetaTW.get(i, j));
                    double denominator = denominatorMatrix.get(i, j);
                    phi.set(i, j, numerator / denominator);
                }
            }
            LOG.info(" iter:" + iter + ", finish phi update");

            // calculate wordLoss and loss
            wordLoss = wordLoss / numTopics;
            loss += wordLoss;
            loss *= 0.5d;
            LOG.info(" iter:" + iter + ", loss:" + loss + ", wordLoss:" + wordLoss / 2);
        }
    }

    @Override
    protected double predict(int[] keys) throws LibrecException {
        return predict(keys[0], keys[1]);
    }

    protected double predict(int userIdx, int itemIdx) {
        return userFactors.row(userIdx).dot(itemFactors.row(itemIdx)) + userBiases.get(userIdx) + itemBiases.get(itemIdx) + globalMean;
    }

    /**
     * Calculate theta vectors via userFactors and itemFactors.
     * thetaVector = softmax( exp(K1|u| + K2|v|) )
     */
    private void calculateTheta() {
        for (MatrixEntry me : trainMatrix) {
            int u = me.row();
            int i = me.column();
            double[] k1uAddk2v = new double[numTopics];
            for (int k = 0; k < numTopics; k++) {
                k1uAddk2v[k] = Math.abs(userFactors.get(u, k)) * K1 + Math.abs(itemFactors.get(i, k)) * K2;
            }
            int documentIdx = userItemToDocument.get(u, i);
            try {
                VectorBasedDenseVector newValues = new VectorBasedDenseVector(Maths.softmax(k1uAddk2v));
                theta.set(documentIdx, newValues);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
