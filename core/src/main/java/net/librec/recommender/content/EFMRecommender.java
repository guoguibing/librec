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
import net.librec.math.structure.*;
import net.librec.recommender.TensorRecommender;
import org.apache.commons.lang.StringUtils;

import java.util.*;

/**
 * EFM Recommender
 * Zhang Y, Lai G, Zhang M, et al. Explicit factor models for explainable recommendation based on phrase-level sentiment analysis[C]
 * {@code Proceedings of the 37th international ACM SIGIR conference on Research & development in information retrieval.  ACM, 2014: 83-92}.
 *
 * @author ChenXu and SunYatong
 */
public class EFMRecommender extends TensorRecommender {

    protected int numberOfFeatures;
    protected int explicitFeatureNum;
    protected int hiddenFeatureNum;
    protected double scoreScale;
    protected DenseMatrix featureMatrix;
    protected DenseMatrix userFeatureMatrix;
    protected DenseMatrix userHiddenMatrix;
    protected DenseMatrix itemFeatureMatrix;
    protected DenseMatrix itemHiddenMatrix;
    protected SequentialAccessSparseMatrix userFeatureAttention;
    protected SequentialAccessSparseMatrix itemFeatureQuality;
    protected double lambdaX;
    protected double lambdaY;
    protected double lambdaU;
    protected double lambdaH;
    protected double lambdaV;
    protected BiMap<String, Integer> featureDict;

    protected SequentialAccessSparseMatrix trainMatrix;

    public BiMap<Integer, String> featureSentimemtPairsMappingData;

    boolean doExplain;

    /*
     * (non-Javadoc)
     *
     * @see net.librec.recommender.AbstractRecommender#setup()
     */
    @Override
    protected void setup() throws LibrecException {
        super.setup();
        scoreScale = maxRate - minRate;
        explicitFeatureNum = conf.getInt("rec.factor.explicit", 5);
        hiddenFeatureNum = numFactors - explicitFeatureNum;
        lambdaX = conf.getDouble("rec.regularization.lambdax", 0.001);
        lambdaY = conf.getDouble("rec.regularization.lambday", 0.001);
        lambdaU = conf.getDouble("rec.regularization.lambdau", 0.001);
        lambdaH = conf.getDouble("rec.regularization.lambdah", 0.001);
        lambdaV = conf.getDouble("rec.regularization.lambdav", 0.001);

        featureSentimemtPairsMappingData = DataFrame.getInnerMapping("sentiment").inverse();
        trainMatrix = trainTensor.rateMatrix();

        featureDict = HashBiMap.create();
        Map<Integer, String> userFeatureDict = new HashMap<Integer, String>();
        Map<Integer, String> itemFeatureDict = new HashMap<Integer, String>();

        numberOfFeatures = 0;

        for (TensorEntry te : trainTensor) {
            int[] entryKeys = te.keys();
            int userIndex = entryKeys[0];
            int itemIndex = entryKeys[1];
            int featureSentimentPairsIndex = entryKeys[2];
            String featureSentimentPairsString = featureSentimemtPairsMappingData.get(featureSentimentPairsIndex);
            String[] fSPList = featureSentimentPairsString.split(" ");

            for (String p : fSPList) {
                String k = p.split(":")[0];
                if (!featureDict.containsKey(k) && !StringUtils.isEmpty(k)) {
                    featureDict.put(k, numberOfFeatures);
                    numberOfFeatures++;
                }
                if (userFeatureDict.containsKey(userIndex)) {
                    userFeatureDict.put(userIndex, userFeatureDict.get(userIndex) + " " + p);
                } else {
                    userFeatureDict.put(userIndex, p);
                }
                if (itemFeatureDict.containsKey(itemIndex)) {
                    itemFeatureDict.put(itemIndex, itemFeatureDict.get(itemIndex) + " " + p);
                } else {
                    itemFeatureDict.put(itemIndex, p);
                }
            }
        }

        // Create V,U1,H1,U2,H2
        featureMatrix = new DenseMatrix(numberOfFeatures, explicitFeatureNum);
        featureMatrix.init(0.01);
        userFeatureMatrix = new DenseMatrix(numUsers, explicitFeatureNum); //userFactors.getSubMatrix(0, userFactors.numRows() - 1, 0, explicitFeatureNum - 1);
        userFeatureMatrix.init(1);
        userHiddenMatrix = new DenseMatrix(numUsers, numFactors - explicitFeatureNum); // userFactors.getSubMatrix(0, userFactors.numRows() - 1, explicitFeatureNum, userFactors.numColumns() - 1);
        userHiddenMatrix.init(1);
        itemFeatureMatrix = new DenseMatrix(numItems, explicitFeatureNum);// itemFactors.getSubMatrix(0, itemFactors.numRows() - 1, 0, explicitFeatureNum - 1);
        itemFeatureMatrix.init(1);
        itemHiddenMatrix = new DenseMatrix(numItems, numFactors - explicitFeatureNum);// itemFactors.getSubMatrix(0, itemFactors.numRows() - 1, explicitFeatureNum, itemFactors.numColumns() - 1);
        itemHiddenMatrix.init(1);

        // compute UserFeatureAttention
        Table<Integer, Integer, Double> userFeatureAttentionTable = HashBasedTable.create();
        for (int u : userFeatureDict.keySet()) {
            double[] featureValues = new double[numberOfFeatures];
            String[] fList = userFeatureDict.get(u).split(" ");
            for (String a : fList) {
                if (!StringUtils.isEmpty(a)) {
                    int fin = featureDict.get(a.split(":")[0]);
                    featureValues[fin] += 1;
                }
            }
            for (int i = 0; i < numberOfFeatures; i++) {
                if (featureValues[i] != 0.0) {
                    double v = 1 + (scoreScale - 1) * (2 / (1 + Math.exp(-featureValues[i])) - 1);
                    userFeatureAttentionTable.put(u, i, v);
                }
            }
        }
        userFeatureAttention = new SequentialAccessSparseMatrix(numUsers, numberOfFeatures, userFeatureAttentionTable);

        // Compute ItemFeatureQuality
        Table<Integer, Integer, Double> itemFeatureQualityTable = HashBasedTable.create();
        for (int p : itemFeatureDict.keySet()) {
            double[] featureValues = new double[numberOfFeatures];
            String[] fList = itemFeatureDict.get(p).split(" ");
            for (String a : fList) {
                if (!StringUtils.isEmpty(a)) {
                    int fin = featureDict.get(a.split(":")[0]);
                    featureValues[fin] += Double.parseDouble(a.split(":")[1]);
                }
            }
            for (int i = 0; i < numberOfFeatures; i++) {
                if (featureValues[i] != 0.0) {
                    double v = 1 + (scoreScale - 1) / (1 + Math.exp(-featureValues[i]));
                    itemFeatureQualityTable.put(p, i, v);
                }
            }
        }
        itemFeatureQuality = new SequentialAccessSparseMatrix(numItems, numberOfFeatures, itemFeatureQualityTable);

        doExplain = conf.getBoolean("rec.explain.flag");
        LOG.info("numUsers:" + numUsers);
        LOG.info("numItems:" + numItems);
        LOG.info("numFeatures:" + numberOfFeatures);
    }

    @Override
    protected void trainModel() throws LibrecException {
        for (int iter = 1; iter <= conf.getInt("rec.iterator.maximum"); iter++) {
            loss = 0.0;
            updateProgress(0);
            // Update featureMatrix by fixing the others
            // LOG.info("iter:" + iter + ", Update featureMatrix");
            for(int featureIdx=0; featureIdx<numberOfFeatures; featureIdx++) {
                SequentialSparseVector attentionVec = userFeatureAttention.column(featureIdx);
                SequentialSparseVector qualityVec = itemFeatureQuality.column(featureIdx);
                if (attentionVec.getNumEntries() > 0 && qualityVec.getNumEntries() > 0) {
                    RandomAccessSparseVector attentionPredVec = new RandomAccessSparseVector(numUsers);
                    RandomAccessSparseVector qualityPredVec = new RandomAccessSparseVector(numItems);

                    for (int userIdx: attentionVec.getIndices()) {
                        attentionPredVec.set(userIdx, predUserAttention(userIdx, featureIdx));
                    }
                    for (int itemIdx: qualityVec.getIndices()) {
                        qualityPredVec.set(itemIdx, predItemQuality(itemIdx, featureIdx));
                    }

                    for (int factorIdx=0; factorIdx<explicitFeatureNum; factorIdx++) {
                        DenseVector factorUsersVector = userFeatureMatrix.column(factorIdx);
                        DenseVector factorItemsVector = itemFeatureMatrix.column(factorIdx);

                        double numerator = lambdaX * factorUsersVector.dot(attentionVec) + lambdaY * factorItemsVector.dot(qualityVec);
                        double denominator = lambdaX * factorUsersVector.dot(attentionPredVec) + lambdaY * factorItemsVector.dot(qualityPredVec)
                                + lambdaV * featureMatrix.get(featureIdx, factorIdx) + 1e-9;

                        featureMatrix.set(featureIdx, factorIdx, featureMatrix.get(featureIdx, factorIdx) * Math.sqrt(numerator/denominator));

                    }
                }
            }
            updateProgress(20);


            // Update UserFeatureMatrix by fixing the others
            for (int userIdx=0; userIdx<numUsers; userIdx++) {
                SequentialSparseVector itemRatingsVector = trainMatrix.row(userIdx);
                SequentialSparseVector attentionVec = userFeatureAttention.row(userIdx);

                if (itemRatingsVector.getNumEntries() > 0 && attentionVec.getNumEntries() > 0) {
                    RandomAccessSparseVector itemPredictsVector = new RandomAccessSparseVector(numItems);
                    RandomAccessSparseVector attentionPredVec = new RandomAccessSparseVector(numberOfFeatures);

                    for (int itemIdx : itemRatingsVector.getIndices()) {
                        itemPredictsVector.set(itemIdx, predictWithoutBound(userIdx, itemIdx));
                    }

                    for (int featureIdx: attentionVec.getIndices()) {
                        attentionPredVec.set(featureIdx, predUserAttention(userIdx, featureIdx));
                    }

                    for (int factorIdx = 0; factorIdx < explicitFeatureNum; factorIdx++) {
                        DenseVector factorItemsVector = itemFeatureMatrix.column(factorIdx);
                        DenseVector featureVector = featureMatrix.column(factorIdx);

                        double numerator = factorItemsVector.dot(itemRatingsVector) + lambdaX * featureVector.dot(attentionVec);
                        double denominator = factorItemsVector.dot(itemPredictsVector) + lambdaX * featureVector.dot(attentionPredVec)
                                + lambdaU * userFeatureMatrix.get(userIdx, factorIdx) + 1e-9;

                        userFeatureMatrix.set(userIdx, factorIdx, userFeatureMatrix.get(userIdx, factorIdx) * Math.sqrt(numerator/denominator));
                    }
                }
            }
            updateProgress(40);

            // Update ItemFeatureMatrix by fixing the others
            // LOG.info("iter:" + iter + ", Update ItemFeatureMatrix");
            for (int itemIdx = 0; itemIdx < numItems; itemIdx++) {
                SequentialSparseVector userRatingsVector = trainMatrix.column(itemIdx);
                SequentialSparseVector qualityVector = itemFeatureQuality.row(itemIdx);

                if (userRatingsVector.getNumEntries() > 0 && qualityVector.getNumEntries() > 0) {
                    RandomAccessSparseVector userPredictsVector = new RandomAccessSparseVector(numUsers);
                    RandomAccessSparseVector qualityPredVec = new RandomAccessSparseVector(numberOfFeatures);

                    for (int userIdx : userRatingsVector.getIndices()) {
                        userPredictsVector.set(userIdx, predictWithoutBound(userIdx, itemIdx));
                    }

                    for (int featureIdx : qualityVector.getIndices()) {
                        qualityPredVec.set(featureIdx, predItemQuality(itemIdx, featureIdx));
                    }

                    for (int factorIdx = 0; factorIdx < explicitFeatureNum; factorIdx++) {
                        DenseVector factorUsersVector = userFeatureMatrix.column(factorIdx);
                        DenseVector featureVector = featureMatrix.column(factorIdx);

                        double numerator = factorUsersVector.dot(userRatingsVector) + lambdaY * featureVector.dot(qualityVector);
                        double denominator = factorUsersVector.dot(userPredictsVector) + lambdaY * featureVector.dot(qualityPredVec)
                                + lambdaU * itemFeatureMatrix.get(itemIdx, factorIdx) + 1e-9;

                        itemFeatureMatrix.set(itemIdx, factorIdx, itemFeatureMatrix.get(itemIdx, factorIdx) * Math.sqrt(numerator/denominator));
                    }
                }
            }
            updateProgress(60);

            // Update UserHiddenMatrix by fixing the others
            // LOG.info("iter:" + iter + ", Update UserHiddenMatrix");
            for (int userIdx=0; userIdx<numUsers; userIdx++) {
                SequentialSparseVector itemRatingsVector = trainMatrix.row(userIdx);
                if (itemRatingsVector.getNumEntries() > 0) {
                    RandomAccessSparseVector itemPredictsVector = new RandomAccessSparseVector(numItems);

                    for (int itemIdx : itemRatingsVector.getIndices()) {
                        itemPredictsVector.set(itemIdx, predictWithoutBound(userIdx, itemIdx));
                    }

                    for (int factorIdx = 0; factorIdx < hiddenFeatureNum; factorIdx++) {
                        DenseVector hiddenItemsVector = itemHiddenMatrix.column(factorIdx);
                        double numerator = hiddenItemsVector.dot(itemRatingsVector);
                        double denominator = hiddenItemsVector.dot(itemPredictsVector) + lambdaH * userHiddenMatrix.get(userIdx, factorIdx) + 1e-9;
                        userHiddenMatrix.set(userIdx, factorIdx, userHiddenMatrix.get(userIdx, factorIdx) * Math.sqrt(numerator/denominator));
                    }
                }
            }
            updateProgress(90);

            // Update ItemHiddenMatrix by fixing the others
            // LOG.info("iter:" + iter + ", Update ItemHiddenMatrix");
            for (int itemIdx=0; itemIdx<numItems; itemIdx++) {
                SequentialSparseVector userRatingsVector = trainMatrix.column(itemIdx);
                if (userRatingsVector.getNumEntries() > 0) {
                    RandomAccessSparseVector userPredictsVector = new RandomAccessSparseVector(numUsers);

                    for (int userIdx : userRatingsVector.getIndices()) {
                        userPredictsVector.set(userIdx, predictWithoutBound(userIdx, itemIdx));
                    }

                    for (int factorIdx = 0; factorIdx < hiddenFeatureNum; factorIdx++) {
                        DenseVector hiddenUsersVector = userHiddenMatrix.column(factorIdx);
                        double numerator = hiddenUsersVector.dot(userRatingsVector);
                        double denominator = hiddenUsersVector.dot(userPredictsVector) + lambdaH * itemHiddenMatrix.get(itemIdx, factorIdx) + 1e-9;
                        itemHiddenMatrix.set(itemIdx, factorIdx, itemHiddenMatrix.get(itemIdx, factorIdx) * Math.sqrt(numerator/denominator));
                    }
                }
            }
            updateProgress(100);

            // Compute loss value
            for (MatrixEntry me: trainMatrix) {
                int userIdx = me.row();
                int itemIdx = me.column();
                double rating = me.get();
                double predRating = predictWithoutBound(userIdx, itemIdx);
                loss += (rating - predRating) * (rating - predRating);
            }

            for (MatrixEntry me: userFeatureAttention) {
                int userIdx = me.row();
                int featureIdx = me.column();
                double real = me.get();
                double pred = predUserAttention(userIdx, featureIdx);
                loss += (real - pred) * (real - pred);
            }

            for (MatrixEntry me: itemFeatureQuality) {
                int itemIdx = me.row();
                int featureIdx = me.column();
                double real = me.get();
                double pred = predItemQuality(itemIdx, featureIdx);
                loss += (real - pred) * (real - pred);
            }

            loss += lambdaU * (Math.pow(userFeatureMatrix.norm(), 2) + Math.pow(itemFeatureMatrix.norm(), 2));
            loss += lambdaH * (Math.pow(userHiddenMatrix.norm(), 2) + Math.pow(itemHiddenMatrix.norm(), 2));
            loss += lambdaV * Math.pow(featureMatrix.norm(), 2);

            LOG.info("iter:" + iter + ", loss:" + loss);
        }
        if (doExplain) {
            String[] userIds = conf.get("rec.explain.userids").split(" ");
            for (String userId: userIds) {
                explain(userId);
            }
        }
    }

    protected void explain(String userId) throws LibrecException {
        // get useridx and itemidices
        int userIdx = userMappingData.get(userId);
        double[] predRatings = new double[numItems];

        for (int itemIdx=0; itemIdx<numItems; itemIdx++) {
            predRatings[itemIdx] = predictWithoutBound(userIdx, itemIdx);
        }

        // get the max\min predRating's index
        int maxIndex = 0;
        int minIndex = 0;
        for (int itemIdx = 0; itemIdx < numItems; itemIdx++) {
            double newnumber = predRatings[itemIdx];
            if (newnumber > predRatings[maxIndex]) {
                maxIndex = itemIdx;
            }
            if (newnumber < predRatings[minIndex]) {
                minIndex = itemIdx;
            }
        }

        int recommendedItemIdx = maxIndex;
        int disRecommendedItemIdx = minIndex;
        String recommendedItemId = itemMappingData.inverse().get(recommendedItemIdx);
        String disRecommendedItemId = itemMappingData.inverse().get(disRecommendedItemIdx);

        // get feature and values
        double[] userFeatureValues = featureMatrix.times(userFeatureMatrix.row(userIdx)).getValues();
        double [] recItemFeatureValues = featureMatrix.times(itemFeatureMatrix.row(recommendedItemIdx)).getValues();
        double [] disRecItemFeatureValues = featureMatrix.times(itemFeatureMatrix.row(disRecommendedItemIdx)).getValues();
        Map<Integer, Double> userFeatureValueMap = new HashMap<>();
        for (int i=0; i<numberOfFeatures; i++) {
            userFeatureValueMap.put(i, userFeatureValues[i]);
        }
        // sort features by values
        userFeatureValueMap = sortByValue(userFeatureValueMap);

        // get top K feature and its values
        int numFeatureToExplain = conf.getInt("rec.explain.numfeature");
        Object[] userTopFeatureIndices = Arrays.copyOfRange(userFeatureValueMap.keySet().toArray(), numberOfFeatures - numFeatureToExplain, numberOfFeatures);
        String[] userTopFeatureIds = new String[numFeatureToExplain];
        double[] userTopFeatureValues = new double[numFeatureToExplain];
        double[] recItemTopFeatureValues = new double[numFeatureToExplain];
        double[] disRecItemTopFeatureIdValues = new double[numFeatureToExplain];
        for (int i=0; i<numFeatureToExplain; i++) {
            int featureIdx = (int) userTopFeatureIndices[numFeatureToExplain - 1 - i];
            userTopFeatureValues[i] = userFeatureValues[featureIdx];
            recItemTopFeatureValues[i] = recItemFeatureValues[featureIdx];
            disRecItemTopFeatureIdValues[i] = disRecItemFeatureValues[featureIdx];
            userTopFeatureIds[i] = featureDict.inverse().get(featureIdx);
        }

        StringBuilder userFeatureSb = new StringBuilder();
        StringBuilder recItemFeatureSb = new StringBuilder();
        StringBuilder disRecItemFeatureSb = new StringBuilder();
        for (int i=0; i<numFeatureToExplain; i++) {
            userFeatureSb.append(userTopFeatureIds[i]).append(":").append(normalize(userTopFeatureValues[i])).append("\n");
            recItemFeatureSb.append(userTopFeatureIds[i]).append(":").append(normalize(recItemTopFeatureValues[i])).append("\n");
            disRecItemFeatureSb.append(userTopFeatureIds[i]).append(":").append(normalize(disRecItemTopFeatureIdValues[i])).append("\n");
        }
        LOG.info("user " + userId + "'s most cared features are \n" + userFeatureSb);
        LOG.info("item " + recommendedItemId + "'s feature values are\n" + recItemFeatureSb);
        LOG.info("item " + disRecommendedItemId + "'s feature values are\n" + disRecItemFeatureSb);
        LOG.info("So we recommend item " + recommendedItemId + ", disRecommend item " + disRecommendedItemId + " to user " + userId);
        LOG.info("___________________________");
    }

    @Override
    protected double predict(int[] indices) {
        return predict(indices[0], indices[1]);
    }

    protected double predict(int u, int j) {
        double pred = userFeatureMatrix.row(u).dot(itemFeatureMatrix.row(j)) + userHiddenMatrix.row(u).dot(itemHiddenMatrix.row(j));
        if (pred < minRate)
            return minRate;
        if (pred > maxRate)
            return maxRate;
        return pred;
    }

    protected double predictWithoutBound(int u, int j) {
        return userFeatureMatrix.row(u).dot(itemFeatureMatrix.row(j)) + userHiddenMatrix.row(u).dot(itemHiddenMatrix.row(j));
    }

    protected double predUserAttention(int userIdx, int featureIdx) {
        return userFeatureMatrix.row(userIdx).dot(featureMatrix.row(featureIdx));
    }

    protected double predItemQuality(int itemIdx, int featureIdx) {
        return itemFeatureMatrix.row(itemIdx).dot(featureMatrix.row(featureIdx));
    }

    /**
     * Sort a map by value.
     *
     * @param map the map to sort
     * @param <K> key type
     * @param <V> value type
     * @return a sorted map of the input
     */
    protected static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        List<Map.Entry<K, V>> list = new LinkedList<>( map.entrySet() );
        Collections.sort(list, new Comparator<Map.Entry<K, V>>()
        {
            public int compare( Map.Entry<K, V> o1, Map.Entry<K, V> o2 )
            {
                return (o1.getValue()).compareTo( o2.getValue() );
            }
        } );

        Map<K, V> result = new LinkedHashMap<K, V>();
        for (Map.Entry<K, V> entry : list)
        {
            result.put( entry.getKey(), entry.getValue() );
        }
        return result;
    }

    /**
     * Normalize the value into [0, 1]
     *
     * @param rating the input value
     * @return the normalized value
     */
    protected double normalize(double rating) {
        return  (rating - minRate) / (maxRate - minRate);
    }
}
