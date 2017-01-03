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
package net.librec.recommender.context.rating;

import net.librec.common.LibrecException;
import net.librec.math.algorithm.Randoms;
import net.librec.math.structure.*;
import net.librec.recommender.TensorRecommender;
import net.librec.recommender.item.RecommendedItem;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Haidong Zhang
 */
public class BPTFRecommender extends TensorRecommender {

    // user latent factors
    private DenseMatrix userFactors;
    private DenseVector userMu;
    private DenseMatrix userVariance;

    // item latent factors
    private DenseMatrix itemFactors;
    private DenseVector itemMu;
    private DenseMatrix itemVariance;

    // time latent factors
    private DenseMatrix timeFactors;
    private DenseVector timeMu;
    private DenseMatrix timeVariance;

    //
    private SparseTensor predictTensor;

    // number of users
    private int numUsers;

    // number of items
    private int numItems;

    // number of time slices
    private int numTimes;

    // number of latent factors
    private int numFactors;

    // global mean
    private double globalMean;

    // maximimum number of iterations
    private int numIters;

    // hyper-parameters for user, item and time parameters
    private DenseMatrix WI0;
    private double mu0;
    private double nu0;
    private double beta0;

    // hyper-parameters for alpha
    private double WI0Alpha;
    private double nu0Alpha;

    @Override
    protected void setup() throws LibrecException {
        super.setup();

        numFactors = conf.getInt("", 10);

        mu0 = conf.getDouble("", 0.0);
        nu0 = numFactors;

        beta0 = conf.getDouble("", 1.0);

        double initWishartW0 = conf.getDouble("", 1.0);
        WI0 = DenseMatrix.eye(numFactors);
        WI0.scaleEqual(initWishartW0);

        WI0Alpha = conf.getDouble("", 2.0);
        nu0Alpha = conf.getDouble("", 1.0);

        numIters = conf.getInt("", 100);

        numUsers = trainTensor.getUserDimension();
        numItems = trainTensor.getItemDimension();
        numTimes = trainTensor.getIndexDimension(2);

        userFactors = new DenseMatrix(numUsers, numFactors);
        itemFactors = new DenseMatrix(numItems, numFactors);
        timeFactors = new DenseMatrix(numTimes, numFactors);

        userFactors.init(0, 1);
        itemFactors.init(0, 1);
        timeFactors.init(0, 1);

        globalMean = trainTensor.mean();

        predictTensor = new SparseTensor(numUsers, numItems, numTimes);
    }

    @Override
    protected void trainModel() throws LibrecException {
        // Speed up getting user or item vector in Gibbs sampling
        List<SparseMatrix> userTrainMatrix = new ArrayList<SparseMatrix>(numUsers);
        List<SparseMatrix> itemTrainMatrix = new ArrayList<SparseMatrix>(numItems);
        List<SparseMatrix> timeTrainMatrix = new ArrayList<SparseMatrix>(numTimes);

        for (int u = 0; u < numUsers; u++) {
            userTrainMatrix.add(trainTensor.slice(1, 2, u));
        }
        for (int i = 0; i < numItems; i++) {
            itemTrainMatrix.add(trainTensor.slice(0, 2, i));
        }
        for (int t = 0; t < numTimes; t++) {
            timeTrainMatrix.add(trainTensor.slice(0, 1, t));
        }


        for (int iter = 1; iter <= numIters; iter++) {

            // Sample the prior of alpha

            double alpha = sampleAlphaHyperParameters(WI0Alpha, nu0Alpha);

            // Sample the prior of U
            sampleHyperParameters(userMu, userVariance, userFactors, WI0, mu0, beta0, nu0);

            // Sample the prior of V
            sampleHyperParameters(itemMu, itemVariance, itemFactors, WI0, mu0, beta0, nu0);

            // Sample the prior of T
            sampleTimeHyperParameters(timeMu, timeVariance, timeFactors, WI0, mu0, beta0, nu0);

            //
            for (int gibbs = 0; gibbs < 2; gibbs++) {

                // Update U
                sampleModelParameters(userMu, userVariance, userTrainMatrix, alpha, true);

                // Update V
                sampleModelParameters(itemMu, itemVariance, itemTrainMatrix, alpha, false);

                // Update T
                sampleTimeParameters(timeMu, timeVariance, timeTrainMatrix, alpha);

            }

            if (iter == 1) {
                try {
                    for (TensorEntry te : testTensor) {
                        int u = te.key(0);
                        int i = te.key(1);
                        int t = te.key(2);
                        predictTensor.set(0.0, u, i, t);
                    }
                } catch (Exception e) {
                    throw new LibrecException();
                }
            }
            int startnum = 0;
            if (iter > startnum) {
                try {
                    for (TensorEntry te : testTensor) {
                        int u = te.key(0);
                        int i = te.key(1);
                        int t = te.key(2);

                        double preRating = 0.0;
                        for (int f = 0; f < numFactors; f++) {
                            preRating += userFactors.get(u, f) * itemFactors.get(i, f) * timeFactors.get(t, f);
                        }
                        double predictValue = (predictTensor.get(u, i, t) * (iter - 1 -
                                startnum) + globalMean + preRating) / (iter - startnum);
                        predictTensor.set(predictValue, u, i, t);
                    }
                } catch (Exception e) {
                    throw new LibrecException();
                }

            }
        }
    }



    /*
     * Sample the hyper-parameters according to A.3, A.4
     */
    protected void sampleHyperParameters(DenseVector mu, DenseMatrix lambda, DenseMatrix factors, DenseMatrix W0, double mu0, double beta0, double nu0) throws LibrecException {

        int numRows = factors.numRows();
        int numColumns = factors.numColumns();

        // mean of factors
        DenseVector factorsMean = new DenseVector(numColumns);
        for (int f = 0; f < numColumns; f++) {
            factorsMean.set(f, factors.columnMean(f));
        }

        // variance of factors
        DenseMatrix variance = factors.cov();
        variance.scaleEqual(numRows - 1);

        //beta_post
        double beta_post = beta0 + numRows;

        //nu_post
        double nu_post = nu0 + numRows;

        // mu0_post
        DenseVector mu0_post = factorsMean.scale(numRows);
        mu0_post.addEqual(beta0 * mu0);
        mu0_post.scaleEqual(1.0 / beta_post);

        // W0_post
        DenseMatrix W0_post = W0.clone();
        W0_post.addEqual(variance);
        double ratio = beta0 * numRows / beta_post;
        DenseVector factorsMeanDiff = factorsMean.add(-mu0);
        DenseMatrix coVariance = factorsMeanDiff.outer(factorsMeanDiff);
        coVariance.scaleEqual(ratio);
        W0_post.addEqual(coVariance);
        W0_post = W0_post.inv();

        // Sample variance
        DenseMatrix wishrnd = Randoms.wishart(W0_post, nu_post);
        if (wishrnd != null) {
            lambda = wishrnd;
        }

        // Sample mu
        DenseMatrix lam = variance.scale(beta0 + numRows).inv().cholesky();

        if (lam != null) {
            lam = lam.transpose();

            mu = new DenseVector(numColumns);
            for (int f = 0; f < numColumns; f++) {
                mu.set(f, Randoms.gaussian(0, 1));
            }
            mu = lam.mult(mu);
            mu.addEqual(mu0_post);
        }
    }

    /*
     * Sample the hyper-parameters according to A.3, A.4
     */
    protected void sampleTimeHyperParameters(DenseVector mu, DenseMatrix lambda, DenseMatrix factors, DenseMatrix W0, double mu0, double beta0, double nu0) throws LibrecException {

        int numTimes = factors.numRows();
        int numFactors = factors.numColumns();

        //beta_post
        double beta_post = beta0 + 1;

        //nu_post
        double nu_post = nu0 + numTimes;

        // mu0_post
        DenseVector mu0_post = factors.row(0);
        mu0_post.addEqual(beta0 * mu0);
        mu0_post.scaleEqual(1.0 / beta_post);

        // variance
        DenseMatrix variance = new DenseMatrix(numFactors, numFactors);
        DenseVector diff;
        for (int k = 1; k < numTimes; k++) {
            diff = factors.row(k);
            diff.addEqual(factors.row(k - 1));
            variance.addEqual(diff.outer(diff));
        }

        // W0_post
        DenseMatrix W0_post = W0.clone();
        W0_post.addEqual(variance);
        double ratio = beta0 / beta_post;
        DenseVector factorsMeanDiff = factors.row(0).add(-mu0);
        DenseMatrix coVariance = factorsMeanDiff.outer(factorsMeanDiff);
        coVariance.scaleEqual(ratio);
        W0_post.addEqual(coVariance);
        W0_post = W0_post.inv();

        // Sample variance
        DenseMatrix wishrnd = Randoms.wishart(W0_post, nu_post);
        if (wishrnd != null) {
            lambda = wishrnd;
        }

        // Sample mu
        DenseMatrix lam = variance.scale(beta0 + 1).inv().cholesky();

        if (lam != null) {
            lam = lam.transpose();

            mu = new DenseVector(numFactors);
            for (int f = 0; f < numFactors; f++) {
                mu.set(f, Randoms.gaussian(0, 1));
            }
            mu = lam.mult(mu);
            mu.addEqual(mu0_post);
        }
    }

    /*
     * Sample the hyper-parameters according to A.3, A.4
     */
    protected double sampleAlphaHyperParameters(double WI0, double nu0) throws LibrecException {

        // nu0_post
        double nu0_post = nu0;
        // W0_post
        DenseMatrix W0_post = DenseMatrix.eye(1);
        W0_post.scaleEqual(WI0);

        try {
            for (TensorEntry te : trainTensor) {
                int u = te.key(0);
                int i = te.key(1);
                int t = te.key(2);
                double rating = te.get();

                nu0_post += 1;

                double diff = rating - predict(u, i, t);
                W0_post.addEqual(diff * diff);
            }
            W0_post = W0_post.inv();
        } catch (Exception e) {
            throw new LibrecException();
        }

        DenseMatrix alpha = Randoms.wishart(W0_post, nu0_post);
        return alpha.get(0, 0);
    }

    protected void sampleModelParameters(DenseVector mu, DenseMatrix lambda, List<SparseMatrix> trainMatrices, double alpha, boolean isUserUpdate) throws LibrecException {

        int number;
        DenseMatrix firstFactors, secondFactors = timeFactors, updateFactors;
        if (!isUserUpdate) {
            number = numUsers;
            firstFactors = itemFactors;
            updateFactors = userFactors;
        } else {
            number = numUsers;
            firstFactors = userFactors;
            updateFactors = itemFactors;
        }

        for (int i = 0; i < number; i++) {
            // list of items rated by user u:
            SparseMatrix rv = trainMatrices.get(i);
            int count = rv.size();

            if (count == 0)
                continue;

            // feature of items rated by user u
            DenseMatrix Q = new DenseMatrix(count, numFactors);
            DenseVector rr = new DenseVector(count);
            int idx = 0;
            for (MatrixEntry me : rv) {
                int j = me.row();
                int k = me.column();
                double r = me.get();
                rr.set(idx, r - globalMean);
                for (int f = 0; f < numFactors; f++)
                    Q.set(idx, f, firstFactors.get(j, f) * secondFactors.get(k, f));
                idx++;
            }

            DenseMatrix covar = lambda.add((Q.transpose().mult(Q)).scale(alpha)).inv();
            DenseVector a = Q.transpose().mult(rr).scale(alpha);
            DenseVector b = lambda.mult(mu);
            DenseVector mean_u = covar.mult(a.add(b));
            DenseMatrix lam = covar.cholesky();

            DenseVector normalRdn = new DenseVector(numFactors);
            if (lam != null) {
                lam = lam.transpose();
                for (int f = 0; f < numFactors; f++)
                    normalRdn.set(f, Randoms.gaussian(0, 1));

                DenseVector w1_P1_u = lam.mult(normalRdn).add(mean_u);

                for (int f = 0; f < numFactors; f++) {
                    updateFactors.set(i, f, w1_P1_u.get(f));
                }
            }
        }
    }

    /**
     * Sampling time parameters
     *
     * @param lambda           lambda
     * @param alpha            alpha
     * @param mu               mu
     * @param trainMatrices    trainMatrices
     * @throws LibrecException if error occurs
     */
    protected void sampleTimeParameters(DenseVector mu, DenseMatrix lambda, List<SparseMatrix> trainMatrices, double alpha) throws LibrecException {


        for (int k = 0; k < numTimes; k++) {
            SparseMatrix rv = trainMatrices.get(k);
            int count = rv.size();

            if (count == 0)
                continue;

            DenseMatrix P = new DenseMatrix(count, numFactors);
            DenseVector rr = new DenseVector(count);
            int idx = 0;
            for (MatrixEntry me : rv) {
                int u = me.row();
                int j = me.column();
                double r = me.get();
                rr.set(idx, r - globalMean);
                for (int f = 0; f < numFactors; f++)
                    P.set(idx, f, userFactors.get(u, f) * itemFactors.get(j, f));
                idx++;
            }

            DenseMatrix covar = lambda.add((P.transpose().mult(P)).scale(alpha));
            DenseVector a = P.transpose().mult(rr).scale(alpha);
            DenseVector mean_T = new DenseVector(numFactors);

            if (k == 0) {
                covar = covar.add(lambda).inv();
                DenseVector b = lambda.mult(timeFactors.row(1).add(mu));
                mean_T = covar.mult(a.add(b));
            } else if (k == numTimes) {
                covar = covar.inv();
                DenseVector b = lambda.mult(timeFactors.row(k - 1));
                mean_T = covar.mult(a.add(b));
            } else {
                covar = covar.add(lambda).inv();
                DenseVector b = lambda.mult(timeFactors.row(k - 1).add(timeFactors.row(k)));
                mean_T = covar.mult(a.add(b));
            }
            DenseMatrix lam = covar.cholesky();

            DenseVector normalRdn = new DenseVector(numFactors);
            if (lam != null) {
                lam = lam.transpose();
                for (int f = 0; f < numFactors; f++)
                    normalRdn.set(f, Randoms.gaussian(0, 1));

                DenseVector w1_P1_T = lam.mult(normalRdn).add(mean_T);

                for (int f = 0; f < numFactors; f++)
                    timeFactors.set(k, f, w1_P1_T.get(f));
            }
        }

    }

    protected double predict(int userId, int itemId, int timeId) throws Exception {
        return predictTensor.get(userId, itemId, timeId);
    }

    @Override
    protected double predict(int[] keys) throws LibrecException {
        return 0;
    }

    @Override
    public List<RecommendedItem> getRecommendedList() {
        return null;
    }
}
