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
package net.librec.recommender.cf.rating;

import net.librec.common.LibrecException;
import net.librec.math.algorithm.Randoms;
import net.librec.math.structure.*;
import net.librec.recommender.MatrixFactorizationRecommender;

import java.util.ArrayList;
import java.util.List;

/**
 * Salakhutdinov and Mnih, <strong>Bayesian Probabilistic Matrix Factorization using Markov Chain Monte Carlo</strong>,
 * ICML 2008.
 * <p>
 * Matlab version is provided by the authors via <a href="http://www.utstat.toronto.edu/~rsalakhu/BPMF.html">this
 * link</a>. This implementation is modified from the BayesianPMF by the PREA package.
 * Bayesian Probabilistic Matrix Factorization
 */
public class BPMFRecommender extends MatrixFactorizationRecommender {

    private double userMu0, userBeta0, userWishartScale0;
    private double itemMu0, itemBeta0, itemWishartScale0;

    private DenseVector userMu, itemMu;
    private DenseMatrix userWishartScale, itemWishartScale;
    private double userBeta, itemBeta;
    private double userWishartNu, itemWishartNu;
    private double ratingSigma;

    private int gibbsIterations;

    private RowSequentialAccessSparseMatrix predictMatrix;

    public class HyperParameters {
        public DenseVector mu;
        public DenseMatrix variance;

        HyperParameters(DenseVector _mu, DenseMatrix _variance) {
            mu = _mu;
            variance = _variance;
        }
    }


    @Override
    protected void setup() throws LibrecException {
        super.setup();
        userMu0 = conf.getDouble("rec.recommender.user.mu", 0.0);
        userBeta0 = conf.getDouble("rec.recommender.user.beta", 1.0);
        userWishartScale0 = conf.getDouble("rec.recommender.user.wishart.scale", 1.0);

        itemMu0 = conf.getDouble("rec.recommender.item.mu", 0.0);
        itemBeta0 = conf.getDouble("rec.recommender.item.beta", 1.0);
        itemWishartScale0 = conf.getDouble("rec.recommender.item.wishart.scale", 1.0);

        ratingSigma = conf.getDouble("rec.recommender.rating.sigma", 2.0);
        gibbsIterations = conf.getInt("rec.recommender.gibbs.iterations",1);

    }

    /**
     * Initialize the model
     *
     * @throws LibrecException if error occurs
     */
    protected void initModel() throws LibrecException {

        userMu = new VectorBasedDenseVector(numFactors);
        userMu.assign((index, value) -> userMu0);
        itemMu = new VectorBasedDenseVector(numFactors);
        itemMu.assign((index, value) -> itemMu0);

        userBeta = userBeta0;
        itemBeta = itemBeta0;

        userWishartScale = new DenseMatrix(numFactors, numFactors);
        itemWishartScale = new DenseMatrix(numFactors, numFactors);
        for (int i = 0; i < numFactors; i++) {
            userWishartScale.set(i, i, userWishartScale0);
            itemWishartScale.set(i, i, itemWishartScale0);
        }
        userWishartScale.inverse();
        itemWishartScale.inverse();

        userWishartNu = numFactors;
        itemWishartNu = numFactors;

        predictMatrix = new RowSequentialAccessSparseMatrix(testMatrix);
    }

    /**
     *
     */
    @Override
    protected void trainModel() throws LibrecException {
        initModel();

        // Speed up getting user or item vector in Gibbs sampling
        List<SequentialSparseVector> userTrainVectors = new ArrayList<SequentialSparseVector>(numUsers);
        List<SequentialSparseVector> itemTrainVectors = new ArrayList<SequentialSparseVector>(numItems);
        for (int u = 0; u < numUsers; u++) {
            userTrainVectors.add(trainMatrix.row(u));
        }
        for (int i = 0; i < numItems; i++) {
            itemTrainVectors.add(trainMatrix.column(i));
        }

        DenseVector mu_u = new VectorBasedDenseVector(numFactors);
        DenseVector mu_m = new VectorBasedDenseVector(numFactors);
        for (int f = 0; f < numFactors; f++) {
            mu_u.set(f, userFactors.column(f).mean());
            mu_m.set(f, itemFactors.column(f).mean());
        }
        DenseMatrix variance_u = userFactors.covariance().inverse();
        DenseMatrix variance_m = itemFactors.covariance().inverse();

        HyperParameters userHyperParameters = new HyperParameters(mu_u, variance_u);
        HyperParameters itemHyperParameters = new HyperParameters(mu_m, variance_m);
        for (int iter = 0; iter < numIterations; iter++) {
            userHyperParameters = samplingHyperParameters(userHyperParameters, userFactors, userMu, userBeta, userWishartScale, userWishartNu);
            itemHyperParameters = samplingHyperParameters(itemHyperParameters, itemFactors, itemMu, itemBeta, itemWishartScale, itemWishartNu);

            for (int gibbsIteration = 0; gibbsIteration < gibbsIterations; gibbsIteration++) {

                for (int u = 0; u < numUsers; u++) {
                    SequentialSparseVector ratings = userTrainVectors.get(u);
                    int count = ratings.getNumEntries();
                    if (count == 0) {
                        continue;
                    }

                    DenseVector updatedParameters = updateParameters(itemFactors, ratings, userHyperParameters);
                    userFactors.row(u).assign((index, value) -> updatedParameters.get(index));
                }

                for (int i = 0; i < numItems; i++) {
                    SequentialSparseVector ratings = itemTrainVectors.get(i);
                    int count = ratings.getNumEntries();
                    if (count == 0) {
                        continue;
                    }

                    DenseVector updatedParameters = updateParameters(userFactors, ratings, itemHyperParameters);
                    itemFactors.row(i).assign((index, value) -> updatedParameters.get(index));
                }

            }

            if (iter == 1) {
                for (MatrixEntry me : testMatrix) {
                    int u = me.row();
                    int i = me.column();
                    predictMatrix.set(u, i, 0.0);
                }
            }
            int startnum = 0;
            if (iter > startnum) {
                for (MatrixEntry me : testMatrix) {
                    int userIdx = me.row();
                    int itemIdx = me.column();
                    double predictValue = (predictMatrix.get(userIdx, itemIdx) * (iter - 1 -
                            startnum) + globalMean +userFactors.row(userIdx).dot(itemFactors.row(itemIdx))) / (iter - startnum);
                    predictMatrix.set(userIdx, itemIdx, predictValue);
                }
            }
        }
    }

    protected HyperParameters samplingHyperParameters(HyperParameters hyperParameters, DenseMatrix factors, DenseVector normalMu0, double normalBeta0, DenseMatrix WishartScale0, double WishartNu0) throws LibrecException {
        int numRows = factors.rowSize();
        int numColumns = factors.columnSize();
        DenseVector mean = new VectorBasedDenseVector(numFactors);
        for (int i = 0; i < numColumns; i++) {
            mean.set(i, factors.column(i).mean());
        }

        DenseMatrix populationVariance = factors.covariance();

        double betaPost = normalBeta0 + numRows;
        DenseVector muPost = normalMu0.times(normalBeta0).plus(mean.times(numRows)).times(1.0 / betaPost);

        DenseMatrix WishartScalePost = WishartScale0.plus(populationVariance.times(numRows));
        DenseVector muError = normalMu0.minus(mean);
        WishartScalePost = WishartScalePost.plus(muError.outer(muError).times(normalBeta0 * numRows / betaPost));
        WishartScalePost = WishartScalePost.inverse();
        WishartScalePost = WishartScalePost.plus(WishartScalePost.transpose()).times(0.5);
        DenseMatrix variance = Randoms.wishart(WishartScalePost, numRows + numColumns);
        if (variance != null) {
            hyperParameters.variance = variance;
        }

        DenseMatrix normalVariance = hyperParameters.variance.times(normalBeta0).inverse().cholesky();
        if (normalVariance != null) {
            normalVariance = normalVariance.transpose();

            DenseVector normalRdn = new VectorBasedDenseVector(numColumns);
            for (int f = 0; f < numFactors; f++)
                normalRdn.set(f, Randoms.gaussian(0, 1));

            hyperParameters.mu = normalVariance.times(normalRdn).plus(muPost);
        }
        return hyperParameters;
    }

    protected DenseVector updateParameters(DenseMatrix factors, SequentialSparseVector ratings, HyperParameters hyperParameters) throws LibrecException {
        int num = ratings.getNumEntries();
        DenseMatrix XX = new DenseMatrix(num, numFactors);
        DenseVector ratingsReg = new VectorBasedDenseVector(num);

        int index = 0;
        for (int j : ratings.getIndices()) {
            ratingsReg.set(index, ratings.get(j) - globalMean);
            XX.row(index).assign((index1, value) -> factors.row(j).get(index1));
            index++;
        }

        DenseMatrix covar = hyperParameters.variance.plus((XX.transpose().times(XX)).times(ratingSigma)).inverse();
        DenseVector mu = XX.transpose().times(ratingsReg).times(ratingSigma);
        DenseVector result = mu.plus(hyperParameters.variance.times(hyperParameters.mu));
        mu.assign((index1, value) -> result.get(index1));
        mu = covar.times(mu);

        DenseVector factorVector = new VectorBasedDenseVector(numFactors);

        DenseMatrix lam = covar.cholesky();
        if (lam != null) {
            lam = lam.transpose();
            for (int f = 0; f < numFactors; f++)
                factorVector.set(f, Randoms.gaussian(0, 1));

            DenseVector w1_P1_u = lam.times(factorVector).plus(mu);

            for (int f = 0; f < numFactors; f++) {
                factorVector.set(f, w1_P1_u.get(f));
            }
        }
        return factorVector;
    }

    @Override
    protected double predict(int userIdx, int itemIdx) {
        return predictMatrix.get(userIdx, itemIdx);
    }

}
