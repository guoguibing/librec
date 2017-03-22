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
import net.librec.common.LibrecException;
import net.librec.eval.Measure;
import net.librec.eval.RecommenderEvaluator;
import net.librec.math.structure.DenseMatrix;
import net.librec.math.structure.SparseMatrix;
import net.librec.math.structure.TensorEntry;
import net.librec.recommender.TensorRecommender;
import net.librec.recommender.item.GenericRecommendedItem;
import net.librec.recommender.item.RecommendedItem;
import net.librec.recommender.item.UserItemRatingEntry;
import net.librec.util.ReflectionUtil;
import org.apache.commons.lang.StringUtils;

import java.util.*;

/**
 * EFM Recommender
 * Zhang Y, Lai G, Zhang M, et al. Explicit factor models for explainable recommendation based on phrase-level sentiment analysis[C]
 * {@code Proceedings of the 37th international ACM SIGIR conference on Research & development in information retrieval.  ACM, 2014: 83-92}.
 *
 * @author ChenXu
 */
public class EFMRecommender extends TensorRecommender {

    protected int numberOfFeatures;
    protected int featureFactor = 4;
    protected int scoreScale = 5;
    protected DenseMatrix featureMatrix;
    protected DenseMatrix userFeatureMatrix;
    protected DenseMatrix userHiddenMatrix;
    protected DenseMatrix itemFeatureMatrix;
    protected DenseMatrix itemHiddenMatrix;
    protected DenseMatrix userFeatureAttention;
    protected DenseMatrix itemFeatureQuality;
    protected double lambdaX;
    protected double lambdaY;
    protected double lambdaU;
    protected double lambdaH;
    protected double lambdaV;
    /**
     * user latent factors
     */
    protected DenseMatrix userFactors;

    /**
     * item latent factors
     */
    protected DenseMatrix itemFactors;
    /**
     * init mean
     */
    protected float initMean;

    /**
     * init standard deviation
     */
    protected float initStd;

    protected SparseMatrix trainMatrix;

    public BiMap<String, Integer> featureSentimemtPairsMappingData;

    /*
     * (non-Javadoc)
     *
     * @see net.librec.recommender.AbstractRecommender#setup()
     */
    @Override
    protected void setup() throws LibrecException {
        super.setup();
        lambdaX = conf.getDouble("rec.regularization.lambdax", 0.001);
        lambdaY = conf.getDouble("rec.regularization.lambday", 0.001);
        lambdaU = conf.getDouble("rec.regularization.lambdau", 0.001);
        lambdaH = conf.getDouble("rec.regularization.lambdah", 0.001);
        lambdaV = conf.getDouble("rec.regularization.lambdav", 0.001);

        featureSentimemtPairsMappingData = allFeaturesMappingData.get(2);
        trainMatrix = trainTensor.rateMatrix();

        Map<String, String> featureDict = new HashMap<String, String>();
        Map<Integer, String> userFeatureDict = new HashMap<Integer, String>();
        Map<Integer, String> itemFeatureDict = new HashMap<Integer, String>();

        numberOfFeatures = 0;

        for (TensorEntry te : trainTensor) {
            int[] entryKeys = te.keys();
            int userIndex = entryKeys[0];
            int itemIndex = entryKeys[1];
            int featureSentimentPairsIndex = entryKeys[2];
            String featureSentimentPairsString = featureSentimemtPairsMappingData.inverse().get(featureSentimentPairsIndex);
            String[] fSPList = featureSentimentPairsString.split(" ");

            for (String p : fSPList) {
                String k = p.split(":")[0];
                if (!featureDict.containsKey(k) && !StringUtils.isEmpty(k)) {
                    featureDict.put(k, String.valueOf(numberOfFeatures));
                    numberOfFeatures++;
                }
                if (userFeatureDict.containsKey(userIndex)) {
                    userFeatureDict.put(userIndex, userFeatureDict.get(userIndex) + " " + featureSentimentPairsString);
                } else {
                    userFeatureDict.put(userIndex, featureSentimentPairsString);
                }
                if (itemFeatureDict.containsKey(itemIndex)) {
                    itemFeatureDict.put(itemIndex, itemFeatureDict.get(itemIndex) + featureSentimentPairsString);
                } else {
                    itemFeatureDict.put(itemIndex, featureSentimentPairsString);
                }
            }
        }

        // Create V,U1,H1,U2,H2
        featureMatrix = new DenseMatrix(numberOfFeatures, featureFactor);
        userFactors = new DenseMatrix(numUsers, numFactors);
        itemFactors = new DenseMatrix(numItems, numFactors);

        featureMatrix.init(0.1, 0.01);
        userFeatureMatrix = userFactors.getSubMatrix(0, userFactors.numRows() - 1, 0, featureFactor - 1);
        userFeatureMatrix.init(0.1, 0.01);
        userHiddenMatrix = userFactors.getSubMatrix(0, userFactors.numRows() - 1, featureFactor, userFactors.numColumns() - 1);
        userHiddenMatrix.init(0.1, 0.01);
        itemFeatureMatrix = itemFactors.getSubMatrix(0, itemFactors.numRows() - 1, 0, featureFactor - 1);
        itemFeatureMatrix.init(0.1, 0.01);
        itemHiddenMatrix = itemFactors.getSubMatrix(0, itemFactors.numRows() - 1, featureFactor, itemFactors.numColumns() - 1);
        itemHiddenMatrix.init(0.1, 0.01);

        userFeatureAttention = new DenseMatrix(userFactors.numRows(), numberOfFeatures);
        userFeatureAttention.init(0);
        itemFeatureQuality = new DenseMatrix(itemFactors.numRows(), numberOfFeatures);
        itemFeatureQuality.init(0);

        // compute UserFeatureAttention
        double[] featureValues = new double[numberOfFeatures];
        for (int i = 0; i < numberOfFeatures; i++) {
            featureValues[i] = 0.0;
        }
        for (int u : userFeatureDict.keySet()) {

            String[] fList = userFeatureDict.get(u).split(" ");
            for (String a : fList) {
                if (!StringUtils.isEmpty(a)) {
                    int fin = Integer.parseInt(featureDict.get(a.split(":")[0]));
                    featureValues[fin] += 1;
                }
            }
            for (int i = 0; i < numberOfFeatures; i++) {
                if (featureValues[i] != 0.0) {
                    double v = 1 + (scoreScale - 1) * (2 / (1 + Math.exp(-featureValues[i])) - 1);
                    userFeatureAttention.set(u, i, v);
                }
            }
        }
        // Compute ItemFeatureQuality
        for (int i = 0; i < numberOfFeatures; i++) {
            featureValues[i] = 0.0;
        }
        for (int p : itemFeatureDict.keySet()) {
            String[] fList = itemFeatureDict.get(p).split(" ");
            for (String a : fList) {
                if (!StringUtils.isEmpty(a)) {
                    int fin = Integer.parseInt(featureDict.get(a.split(":")[0]));
                    featureValues[fin] += Double.parseDouble(a.split(":")[1]);
                }
            }
            for (int i = 0; i < numberOfFeatures; i++) {
                if (featureValues[i] != 0.0) {
                    double v = 1 + (scoreScale - 1) / (1 + Math.exp(-featureValues[i]));
                    itemFeatureQuality.set(p, i, v);
                }
            }
        }
        LOG.info("numUsers:" + numUsers);
        LOG.info("numItems:" + numItems);
        LOG.info("numFeatures:" + numberOfFeatures);
    }

    @Override
    protected void trainModel() throws LibrecException {
        for (int iter = 1; iter <= conf.getInt("rec.iterator.maximum"); iter++) {
            loss = 0.0;
            // Update featureMatrix
            LOG.info("iter:" + iter + ", Update featureMatrix");
            for (int i = 0; i < featureMatrix.numRows(); i++) {
                for (int j = 0; j < featureFactor; j++) {
                    double numerator = ((userFeatureAttention.transpose().mult(userFeatureMatrix).scale(lambdaX))
                            .add(itemFeatureQuality.transpose().mult(itemFeatureMatrix).scale(lambdaY)))
                            .get(i, j);
                    double denominator = featureMatrix.mult((userFeatureMatrix.transpose().mult(userFeatureMatrix).scale(lambdaX))
                            .add(itemFeatureMatrix.transpose().mult(itemFeatureMatrix).scale(lambdaY))
                            .add(DenseMatrix.eye(featureFactor).scale(lambdaV)))
                            .get(i, j);
                    double updateValue = Math.sqrt(numerator / denominator);
                    updateValue = (Double.isInfinite(updateValue) || Double.isNaN(updateValue)) ? 1 : Math.sqrt(updateValue);
                    // LOG.info("numerator:" + numerator + ", denominator:" + denominator + ", updateValue:" + updateValue);
                    featureMatrix.set(i, j, featureMatrix.get(i, j) * updateValue);
                }
            }
            // Update UserFeatureMatrix
            LOG.info("iter:" + iter + ", Update UserFeatureMatrix");
            for (int i = 0; i < userFactors.numRows(); i++) {
                for (int j = 0; j < featureFactor; j++) {
                    double numerator = ((itemFeatureMatrix.transpose().mult(trainMatrix.transpose())).transpose()
                            .add(userFeatureAttention.mult(featureMatrix).scale(lambdaX))).get(i, j);
                    double denominator = (userFeatureMatrix.mult(itemFeatureMatrix.transpose())
                            .add(userHiddenMatrix.mult(itemHiddenMatrix.transpose())).mult(itemFeatureMatrix)
                            .add(userFeatureMatrix
                                    .mult(featureMatrix.transpose().mult(featureMatrix).scale(lambdaX)
                                    .add(DenseMatrix.eye(featureFactor).scale(lambdaU))))).get(i, j);

                    double updateValue = Math.sqrt(numerator / denominator);
                    updateValue = (Double.isInfinite(updateValue) || Double.isNaN(updateValue)) ? 1 : Math.sqrt(updateValue);
                    // LOG.info("numerator:" + numerator + ", denominator:" + denominator + ", updateValue:" + updateValue);
                    userFeatureMatrix.set(i, j, userFeatureMatrix.get(i, j) * updateValue);
                }
            }
            // Update ItemFeatureMatrix
            LOG.info("iter:" + iter + ", Update ItemFeatureMatrix");
            for (int i = 0; i < itemFactors.numRows(); i++) {
                for (int j = 0; j < featureFactor; j++) {
                    double numerator = (userFeatureMatrix.transpose().mult(trainMatrix).transpose().
                            add(itemFeatureQuality.mult(featureMatrix).scale(lambdaY))).get(i, j);
                    double denominator = (itemFeatureMatrix.mult(userFeatureMatrix.transpose())
                            .add(itemHiddenMatrix.mult(userHiddenMatrix.transpose())).mult(userFeatureMatrix)
                            .add(itemFeatureMatrix.mult(featureMatrix.transpose().mult(featureMatrix).scale(lambdaY)
                                    .add(DenseMatrix.eye(featureFactor).scale(lambdaU))))).get(i, j);

                    double updateValue = Math.sqrt(numerator / denominator);
                    updateValue = (Double.isInfinite(updateValue) || Double.isNaN(updateValue)) ? 1 : Math.sqrt(updateValue);
                    // LOG.info("numerator:" + numerator + ", denominator:" + denominator + ", updateValue:" + updateValue);
                    itemFeatureMatrix.set(i, j, itemFeatureMatrix.get(i, j) * updateValue);
                }
            }
            // Update UserHiddenMatrix
            LOG.info("iter:" + iter + ", Update UserHiddenMatrix");
            for (int i = 0; i < userFactors.numRows(); i++) {
                for (int j = 0; j < userFactors.numColumns() - featureFactor; j++) {
                    double numerator = (itemHiddenMatrix.transpose().mult(trainMatrix.transpose()).transpose()).get(i, j);
                    double denominator = (userFeatureMatrix.mult(itemFeatureMatrix.transpose())
                            .add(userHiddenMatrix.mult(itemHiddenMatrix.transpose())).mult(itemHiddenMatrix)
                            .add(userHiddenMatrix.scale(lambdaH))).get(i, j);

                    double updateValue = Math.sqrt(numerator / denominator);
                    updateValue = (Double.isInfinite(updateValue) || Double.isNaN(updateValue)) ? 1 : Math.sqrt(updateValue);
                    // LOG.info("numerator:" + numerator + ", denominator:" + denominator + ", updateValue:" + updateValue);
                    userHiddenMatrix.set(i, j, userHiddenMatrix.get(i, j) * updateValue);
                }
            }
            // Update ItemHiddenMatrix
            LOG.info("iter:" + iter + ", Update ItemHiddenMatrix");
            for (int i = 0; i < itemFactors.numRows(); i++) {
                for (int j = 0; j < itemFactors.numColumns() - featureFactor; j++) {
                    double numerator = (userHiddenMatrix.transpose().mult(trainMatrix).transpose()).get(i, j);
                    double denominator = (itemFeatureMatrix.mult(userFeatureMatrix.transpose())
                            .add(itemHiddenMatrix.mult(userHiddenMatrix.transpose())).mult(userHiddenMatrix)
                            .add(itemHiddenMatrix.scale(lambdaH))).get(i, j);

                    double updateValue = Math.sqrt(numerator / denominator);
                    updateValue = (Double.isInfinite(updateValue) || Double.isNaN(updateValue)) ? 1 : Math.sqrt(updateValue);
                    // LOG.info("numerator:" + numerator + ", denominator:" + denominator + ", updateValue:" + updateValue);
                    itemHiddenMatrix.set(i, j, itemHiddenMatrix.get(i, j) * updateValue);
                }
            }

            // Compute loss value
            loss += Math.pow((userFeatureMatrix.mult(itemFeatureMatrix.transpose()).minus(userHiddenMatrix.mult(itemHiddenMatrix.transpose()))).minus(trainMatrix).norm(), 2);
            loss += lambdaX * Math.pow((userFeatureMatrix.mult(featureMatrix.transpose()).minus(userFeatureAttention)).norm(), 2);
            loss += lambdaY * Math.pow((itemFeatureMatrix.mult(featureMatrix.transpose()).minus(itemFeatureQuality)).norm(), 2);
            loss += lambdaU * Math.pow((userFeatureMatrix.norm() + itemFeatureMatrix.norm()), 2);
            loss += lambdaH * (Math.pow(userHiddenMatrix.norm(), 2) + Math.pow(itemHiddenMatrix.norm(), 2));
            loss += lambdaV * Math.pow(featureMatrix.norm(), 2);
            LOG.info("iter:" + iter + ", loss:" + loss);
        }
    }

    @Override
    protected double predict(int[] indices) {
        return predict(indices[0], indices[1]);
    }

    protected double predict(int u, int j) {
        double pred = DenseMatrix.rowMult(userFeatureMatrix, u, itemFeatureMatrix, j) + DenseMatrix.rowMult(userHiddenMatrix, u, itemHiddenMatrix, j);
        if (pred < 1.0)
            return 1.0;
        if (pred > 5.0)
            return 5.0;
        return pred;
    }

    @Override
    public Map<Measure.MeasureValue, Double> evaluateMap() throws LibrecException {
        Map<Measure.MeasureValue, Double> evaluatedMap = new HashMap<>();
        List<Measure.MeasureValue> measureValueList = Measure.getMeasureEnumList(isRanking, topN);
        if (measureValueList != null) {
            for (Measure.MeasureValue measureValue : measureValueList) {
                RecommenderEvaluator evaluator = ReflectionUtil
                        .newInstance(measureValue.getMeasure().getEvaluatorClass());
                if (isRanking && measureValue.getTopN() != null && measureValue.getTopN() > 0) {
                    evaluator.setTopN(measureValue.getTopN());
                }
                double evaluatedValue = evaluator.evaluate(context, recommendedList);
                evaluatedMap.put(measureValue, evaluatedValue);
            }
        }
        return evaluatedMap;
    }

    @Override
    public List<RecommendedItem> getRecommendedList() {
        if (recommendedList != null && recommendedList.size() > 0) {
            List<RecommendedItem> userItemList = new ArrayList<>();
            Iterator<UserItemRatingEntry> recommendedEntryIter = recommendedList.entryIterator();
            if (userMappingData != null && userMappingData.size() > 0 && itemMappingData != null && itemMappingData.size() > 0) {
                BiMap<Integer, String> userMappingInverse = userMappingData.inverse();
                BiMap<Integer, String> itemMappingInverse = itemMappingData.inverse();
                while (recommendedEntryIter.hasNext()) {
                    UserItemRatingEntry userItemRatingEntry = recommendedEntryIter.next();
                    if (userItemRatingEntry != null) {
                        String userId = userMappingInverse.get(userItemRatingEntry.getUserIdx());
                        String itemId = itemMappingInverse.get(userItemRatingEntry.getItemIdx());
                        if (StringUtils.isNotBlank(userId) && StringUtils.isNotBlank(itemId)) {
                            userItemList.add(new GenericRecommendedItem(userId, itemId, userItemRatingEntry.getValue()));
                        }
                    }
                }
                return userItemList;
            }
        }
        return null;
    }
}
