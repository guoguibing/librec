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
import com.google.common.collect.Table;
import net.librec.common.LibrecException;
import net.librec.eval.Measure;
import net.librec.eval.RecommenderEvaluator;
import net.librec.math.algorithm.Maths;
import net.librec.math.algorithm.Randoms;
import net.librec.math.structure.*;
import net.librec.recommender.TensorRecommender;
import net.librec.recommender.item.GenericRecommendedItem;
import net.librec.recommender.item.RecommendedItem;
import net.librec.recommender.item.UserItemRatingEntry;
import net.librec.util.ReflectionUtil;
import net.librec.util.StringUtil;
import org.apache.commons.lang.StringUtils;

import java.util.*;

/**
 * HFT Recommender
 * McAuley J, Leskovec J. Hidden factors and hidden topics: understanding rating dimensions with review text[C]
 * Proceedings of the 7th ACM conference on Recommender systems. ACM, 2013: 165-172.
 *
 * @author ChenXu
 */
public class HFTRecommender extends TensorRecommender {

    protected SparseMatrix trainMatrix;
    protected SparseStringMatrix reviewMatrix;
    protected DenseMatrix topicToWord;
    protected SparseStringMatrix topicAssignment;
    protected int K = 10;
    protected int numberOfWords;
    /**
     * user biases
     */
    protected DenseVector userBiases;

    /**
     * user biases
     */
    protected DenseVector itemBiases;
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
    /**
     * bias regularization
     */
    protected double regBias;
    /**
     * user regularization
     */
    protected float regUser;

    /**
     * item regularization
     */
    protected float regItem;

    public BiMap<String, Integer> reviewMappingData;

    protected StringUtil str = new StringUtil();
    protected Randoms rn = new Randoms();
    protected double[][] thetaus;
    protected double[][] phiks;

    @Override
    protected void setup() throws LibrecException {
        super.setup();
        reviewMappingData = allFeaturesMappingData.get(2);
        regBias = conf.getDouble("rec.bias.regularization", 0.01);
        regUser = conf.getFloat("rec.user.regularization", 0.01f);
        regItem = conf.getFloat("rec.item.regularization", 0.01f);
        trainTensor = (SparseTensor) getDataModel().getTrainDataSet();
        userBiases = new DenseVector(numUsers);
        itemBiases = new DenseVector(numItems);
        userFactors = new DenseMatrix(numUsers, numFactors);
        itemFactors = new DenseMatrix(numItems, numFactors);
        K=numFactors;
        initMean = 0.0f;
        initStd = 0.1f;
        userBiases.init(initMean, initStd);
        itemBiases.init(initMean, initStd);

        numberOfWords = 0;

        // build rating matrix
        trainMatrix = trainTensor.rateMatrix();

        // build review matrix and counting the number of words
        Table<Integer, Integer, String> res = HashBasedTable.create();
        Map<String, String> iwDict = new HashMap<String, String>();
        for (TensorEntry te : trainTensor) {
            int[] entryKeys = te.keys();
            int userIndex = entryKeys[0];
            int itemIndex = entryKeys[1];
            int reviewIndex = entryKeys[2];
            String reviewContent = reviewMappingData.inverse().get(reviewIndex);
            String[] fReviewContent = reviewContent.split(":");
            for (String word : fReviewContent) {
                if (!iwDict.containsKey(word) && StringUtils.isNotEmpty(word)) {
                    iwDict.put(word, String.valueOf(numberOfWords));
                    numberOfWords++;
                }
            }
            res.put(userIndex, itemIndex, reviewContent);
        }

        for (TensorEntry te : testTensor) {
            int[] entryKeys = te.keys();
            int reviewIndex = entryKeys[2];
            String reviewContent = reviewMappingData.inverse().get(reviewIndex);
            String[] fReviewContent = reviewContent.split(":");
            for (String word : fReviewContent) {
                if (!iwDict.containsKey(word) && StringUtils.isNotEmpty(word)) {
                    iwDict.put(word, String.valueOf(numberOfWords));
                    numberOfWords++;
                }
            }
        }

        LOG.info("number of users : " + numUsers);
        LOG.info("number of Items : " + numItems);
        LOG.info("number of words : " + numberOfWords);

        reviewMatrix = new SparseStringMatrix(numUsers, numItems, res);
        topicToWord = new DenseMatrix(K, numberOfWords);
        topicToWord.init(0.1);
        topicAssignment = new SparseStringMatrix(reviewMatrix);
        thetaus = new double[numUsers][K];
        phiks = new double[K][numberOfWords];

        for (MatrixEntry me : trainMatrix) {
            int u = me.row(); // user
            int j = me.column(); // item
            String words = reviewMatrix.get(u, j);
            String[] wordsList = words.split(":");
            String[] topicList = new String[wordsList.length];
            for (int i = 0; i < wordsList.length; i++) {
                topicList[i] = Integer.toString(rn.uniform(K));
            }
            String s = str.toString(topicList, ":");
            topicAssignment.set(u, j, s);
        }
        calculateThetas();
        calculatePhis();
    }

    protected void sampleZ() throws Exception {
        calculateThetas();
        calculatePhis();
        for (MatrixEntry me : trainMatrix) {
            int u = me.row(); // user
            int j = me.column(); // item
            String words = reviewMatrix.get(u, j);
            if (!StringUtils.isEmpty(words)) {
                String[] wordsList = words.split(":");
                String s = sampleTopicsToWords(wordsList, u);
                topicAssignment.set(u, j, s);
            }
            // LOG.info("user:" + u + ", item:" + j + ", topics:" + s);
        }
    }

    /**
     * Update function for thetas and phiks, check if softmax comes in to NaN
     * and update the parameters.
     *
     * @param oldValues old values of the parameter
     * @param newValues new values to update the parameter
     * @return the old values if new values contain NaN
     * @throws Exception if error occurs
     */
    protected double[] updateArray(double[] oldValues, double [] newValues) throws Exception {
        boolean containNan = false;
        double[] newDoubles = Maths.softmax(newValues);
        for (double doubleValue : newDoubles) {
            if (Double.isNaN(doubleValue)) {
                containNan = true;
                break;
            }
        }
        if (!containNan) {
            return newValues;
        } else {
            return oldValues;
        }
    }

    protected void calculateThetas() {
        for (int i = 0; i < numUsers; i++) {
            try {
                thetaus[i] = updateArray(thetaus[i], Maths.softmax(userFactors.row(i).getData()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected void calculatePhis() {
        for (int i = 0; i < K; i++) {
            try {
                phiks[i] = updateArray(phiks[i], Maths.softmax(topicToWord.row(i).getData()));
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    protected String sampleTopicsToWords (String[] wordsList, int u) throws Exception {
        String[] topicList = new String[wordsList.length];
        for (int i = 0; i < wordsList.length; i++) {
            double[] topicDistribute = new double[K];
            for (int s = 0; s < K; s++) {
                topicDistribute[s] = thetaus[u][s] * phiks[s][Integer.parseInt(wordsList[i])];
            }
            topicDistribute = Maths.norm(topicDistribute);
            topicList[i] = Integer.toString(rn.discrete(topicDistribute));
        }
        return str.toString(topicList, ":");
    }

    /**
     * The training approach is SGD instead of L-BFGS, so it can be slow if the dataset
     * is big.
     */
    @Override
    protected void trainModel() {
        for (int iter = 1; iter <= conf.getDouble("rec.iterator.maximum"); iter++) {
            // SGD training
            for (int sgditer = 1; sgditer <= 5; sgditer++) {
                loss = 0.0;
                for (MatrixEntry me : trainMatrix) {

                    int u = me.row(); // user
                    int j = me.column(); // item
                    double ruj = me.get();
                    String[] ws = reviewMatrix.get(u, j).split(":");
                    String[] wk = topicAssignment.get(u, j).split(":");

                    double pred = predict(u, j);
                    double euj = ruj - pred;

                    loss += euj * euj;

                    // update factors
                    double bu = userBiases.get(u);
                    double sgd = euj - regBias * bu;
                    userBiases.add(u, learnRate * sgd);
                    // loss += regB * bu * bu;
                    double bj = itemBiases.get(j);
                    sgd = euj - regBias * bj;
                    itemBiases.add(j, learnRate * sgd);
                    // loss += regB * bj * bj;

                    if (StringUtils.isEmpty(ws[0])) {
                        continue;
                    }

                    for (int f = 0; f < numFactors; f++) {
                        double puf = userFactors.get(u, f);
                        double qjf = itemFactors.get(j, f);

                        double sgd_u = euj * qjf - regUser * puf;
                        double sgd_j = euj * (puf) - regItem * qjf;

                        userFactors.add(u, f, learnRate * sgd_u);

                        itemFactors.add(j, f, learnRate * sgd_j);

                        for (int x = 0; x < ws.length; x++) {

                            int k = Integer.parseInt(wk[x]);
                            if (f == k)
                                userFactors.add(u, f, learnRate * (1 - thetaus[u][k]));
                            else
                                userFactors.add(u, f, learnRate * (-thetaus[u][k]));

                            loss -= Maths.log(thetaus[u][k] * phiks[k][Integer.parseInt(ws[x])], 2);
                        }
                    }

                    for (int x = 0; x < ws.length; x++) {
                        int k = Integer.parseInt(wk[x]);
                        for (int ss = 0; ss < numberOfWords; ss++) {
                            if (ss == Integer.parseInt(ws[x]))
                                topicToWord.add(k, Integer.parseInt(ws[x]), learnRate * (-1 + phiks[k][Integer.parseInt(ws[x])]));
                            else
                                topicToWord.add(k, Integer.parseInt(ws[x]), learnRate * (phiks[k][Integer.parseInt(ws[x])]));
                        }
                    }
                }
                loss *= 0.5;
            } // end of SGDtraining
            LOG.info(" iter:" + iter + ", loss:" + loss);
            try {
                LOG.info(" iter:" + iter + ", sampling");
                sampleZ();
                LOG.info(" iter:" + iter + ", sample finished");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected double predict(int[] indices) {
        return predict(indices[0], indices[1]);
    }

    protected double predict(int u, int j) {
        double pred = globalMean + userBiases.get(u) + itemBiases.get(j) + DenseMatrix.rowMult(userFactors, u, itemFactors, j);
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
