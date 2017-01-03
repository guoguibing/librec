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
package net.librec.recommender;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import net.librec.common.LibrecException;
import net.librec.math.structure.*;
import net.librec.recommender.item.RecommendedItemList;
import net.librec.recommender.item.RecommendedList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Factorization Machine Recommender
 *
 * Rendle, Steffen, et al., <strong>Fast Context-aware Recommendations with Factorization Machines</strong>, SIGIR, 2011.
 *
 * @author Tang Jiaxi and Ma Chen
 */

public abstract class FactorizationMachineRecommender extends AbstractRecommender {
    /**
     * LOG
     */
    protected final Log LOG = LogFactory.getLog(this.getClass());
    /**
     * train Tensor
     */
    protected SparseTensor trainTensor;
    /**
     * testTensor
     */
    protected SparseTensor testTensor;
    /**
     * validTensor
     */
    protected SparseTensor validTensor;
    /**
     * global bias
     */
    protected double w0;
    /**
     * feature vector size: number of users + number of items + number of contextual conditions
     */
    protected int p;
    /**
     * number of factors
     */
    protected int k;
    /**
     * number of ratings
     */
    protected int n;
    /**
     * weight vector
     */
    protected DenseVector W; //  p
    /**
     * parameter matrix
     */
    protected DenseMatrix V; //  p x k
    /**
     * parameter matrix
     */
    protected DenseMatrix Q; //  n x k
    /**
     * regularization term for weight and factors
     */
    protected float regW0, regW, regF;

    /**
     * the number of latent factors
     */
    protected int numFactors;
    /**
     * the number of iterations
     */
    protected int numIterations;

    /**
     * setup
     *
     * @throws LibrecException
     */
    protected void setup() throws LibrecException {
        conf = context.getConf();
        isRanking = conf.getBoolean("rec.recommender.isranking");
        if (isRanking) {
            topN = conf.getInt("rec.recommender.ranking.topn", 5);
        }

        earlyStop = conf.getBoolean("rec.recommender.earlyStop");
        numIterations = conf.getInt("rec.iterator.maximum");

        trainTensor = (SparseTensor) getDataModel().getTrainDataSet();
        testTensor = (SparseTensor) getDataModel().getTestDataSet();
        validTensor = (SparseTensor) getDataModel().getValidDataSet();
        userMappingData = getDataModel().getUserMappingData();
        itemMappingData = getDataModel().getItemMappingData();
        numUsers = userMappingData.size();
        numItems = itemMappingData.size();
        globalMean = trainTensor.mean();
        maxRate = conf.getDouble("rec.recommender.maxrate", 12.0);
        minRate = conf.getDouble("rec.recommender.minrate", 0.0);

        // initialize the parameters of FM
        for (int dim = 0; dim < trainTensor.numDimensions; dim++) {
            p += trainTensor.dimensions[dim]; // set the size of feature vectors
        }
        n = trainTensor.size(); // set the number of ratings
        numFactors = k = conf.getInt("rec.factor.number");

        // init all weight with zero
        w0 = 0;
        W = new DenseVector(p);
        W.init(0);

        // init factors with small value
        V = new DenseMatrix(p, k);
        V.init(0, 0.1);

        regW0 = conf.getFloat("rec.fm.regw0", 0.01f);
        regW = conf.getFloat("rec.fm.regW", 0.01f);
        regF = conf.getFloat("rec.fm.regF", 10f);
    }

    /**
     * predict
     * * predict the rating given a sparse feature vector
     *
     * @return predicted rating
     * @throws LibrecException
     */
    protected double predict(SparseVector x) throws LibrecException {
        double res = 0;
        // global bias
        res += w0;

        // 1-way interaction
        for (VectorEntry ve : x) {
            double val = ve.get();
            int ind = ve.index();
            res += val * W.get(ind);
        }

        // 2-way interaction
        for (int f = 1; f < k; f++) {
            double sum1 = 0;
            double sum2 = 0;
            for (VectorEntry ve : x) {
                double xi = ve.get();
                int i = ve.index();
                double vif = V.get(i, f);

                sum1 += vif * xi;
                sum2 += vif * vif * xi * xi;
            }
            res += (sum1 * sum1 - sum2) / 2;
        }

        return res;
    }

    /**
     * bounded predict
     * * predict the rating given a sparse feature vector
     *
     * @return predicted rating
     * @throws LibrecException
     */
    protected double predict(SparseVector x, boolean bound) throws LibrecException {
        double pred = predict(x);

        if (bound) {
            if (pred > maxRate)
                pred = maxRate;
            if (pred < minRate)
                pred = minRate;
        }

        return pred;
    }

    /**
     * recommend
     * * predict the ratings in the test data
     *
     * @return predictive rating matrix
     * @throws LibrecException
     */
    protected RecommendedList recommendRating() throws LibrecException {
        testMatrix = testTensor.rateMatrix();
        recommendedList = new RecommendedItemList(numUsers - 1, numUsers);

        // each user-item pair appears in the final recommend list only once
        Table<Integer, Integer, Double> ratingMapping = HashBasedTable.create();

        for (TensorEntry tensorEntry : testTensor) {
            int[] entryKeys = tensorEntry.keys();
            SparseVector featureVector = tenserKeysToFeatureVector(entryKeys);
            double predictRating = predict(featureVector, true);
            if (Double.isNaN(predictRating)) {
                predictRating = globalMean;
            }
            int[] userItemInd = getUserItemIndex(featureVector);
            int userIdx = userItemInd[0];
            int itemIdx = userItemInd[1];
            if (!ratingMapping.contains(userIdx, itemIdx)) {
                ratingMapping.put(userIdx, itemIdx, predictRating);
                recommendedList.addUserItemIdx(userIdx, itemIdx, predictRating);
            }
        }

        return recommendedList;
    }

    /**
     * getUserItemIndex
     * * get the user index and item index from a sparse feature vector
     *
     * @return user index and item index
     */
    private int[] getUserItemIndex(SparseVector x) {
        int[] inds = x.getIndex();

        int userInd = inds[0];
        int itemInd = inds[1] - numUsers;

        return new int[]{userInd, itemInd};
    }

    /**
     * tenserKeysToFeatureVector
     * * transform the keys of a tensor entry into a sparse vector
     *
     * @return sparse feature vector
     */
    protected SparseVector tenserKeysToFeatureVector(int[] tenserKeys) {
        int capacity = p;
        int[] index = new int[tenserKeys.length];
        double[] data = new double[tenserKeys.length];
        int colPrefix = 0;
        for (int i = 0; i < tenserKeys.length; i++) {
            data[i] = 1;
            index[i] += colPrefix + tenserKeys[i];
            colPrefix += trainTensor.dimensions[i];
        }

        return new SparseVector(capacity, index, data);
    }
}
