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

import com.google.common.collect.Table;
import net.librec.common.LibrecException;
import net.librec.math.algorithm.Gamma;
import net.librec.math.algorithm.Randoms;
import net.librec.math.structure.DenseMatrix;
import net.librec.math.structure.DenseVector;
import net.librec.math.structure.MatrixEntry;
import net.librec.recommender.MatrixFactorizationRecommender;

import java.util.Map;

/**
 * Prem Gopalan, et al. Scalable Recommendation with Poisson Factorization. <br>
 *
 * @author Haidong Zhang
 */

public class BPoissMFRecommender extends MatrixFactorizationRecommender {

    // The parameters of users
    private GammaDenseMatrix userTheta;
    private GammaDenseVector userThetaRate;

    // The parameters of items
    private GammaDenseMatrix itemBeta;
    private GammaDenseVector itemBetaRate;

    // The parameters of topics;
    private Table<Integer, Integer, Map<Integer, Double>> zTopic;

    @Override
    protected void setup() throws LibrecException {
        super.setup();

        userThetaRate = new GammaDenseVector(numFactors);
        itemBetaRate = new GammaDenseVector(numFactors);

        userTheta = new GammaDenseMatrix(numUsers, numFactors);
        itemBeta = new GammaDenseMatrix(numItems, numFactors);

        userThetaRate.shapePrior = conf.getDouble("rec.recommender.user.rateShapePrior", 0.1);
        userThetaRate.ratePrior = conf.getDouble("rec.recommender.user.rateRatePrior", 0.1);
        userThetaRate.init2(numFactors);
        userThetaRate.computeExpectations();

        itemBetaRate.shapePrior = conf.getDouble("rec.recommender.item.rateShapePrior", 0.1);
        itemBetaRate.ratePrior = conf.getDouble("rec.recommender.item.rateRatePrior", 0.1);
        itemBetaRate.init2(numFactors);
        itemBetaRate.computeExpectations();

        userTheta.shapePrior = conf.getDouble("rec.recommender.user.shapePrior", 0.1);
        userTheta.ratePrior = conf.getDouble("rec.recommender.user.ratePrior", 0.1);
        userTheta.init();

        itemBeta.shapePrior = conf.getDouble("rec.recommender.item.shapePrior", 0.1);
        itemBeta.ratePrior = conf.getDouble("rec.recommender.item.ratePrior", 0.1);
        itemBeta.init();

    }

    protected DenseVector getPhi(DenseMatrix Theta, int indexTheta, DenseMatrix Beta, int indexBeta, int number) throws LibrecException {
        //get_phi(_htheta, n, _hbeta, m, phi);
        DenseVector phi = new DenseVector(number);
        phi.setAll(0.0);
        assert (Theta.numColumns == Beta.numColumns);
        for (int i = 0; i < Theta.numColumns; i++) {
            phi.add(Math.log(Beta.get(indexBeta, i)) + Theta.get(indexTheta, i));
        }
        phi = phi.scale(1.0 / phi.sum());
        for (int i = 0; i < Theta.numColumns; i++) {
            phi.set(i, Math.log(phi.get(i)));
        }
        return phi;
    }

    @Override
    protected void trainModel() throws LibrecException {

        DenseVector phi;
        double[] loglikelihood = new double[numIterations];
        for (int iter = 1; iter <= numIterations; iter++) {
            for (MatrixEntry matrixEntry : trainMatrix) {
                int user_id = matrixEntry.row();
                int item_id = matrixEntry.column();
                double rating = matrixEntry.get();

                phi = getPhi(userTheta.logValue, user_id, itemBeta.value, item_id, numFactors);
                if (rating > 1) {
                    phi.scale(rating);
                }
            }
        }

    }


    @Override
    protected double predict(int userIdx, int itemIdx) throws LibrecException {
        return DenseMatrix.rowMult(userTheta.value, userIdx, itemBeta.value, itemIdx);
    }

    public class GammaDenseVector {
        protected int size;
        protected double shapePrior, ratePrior;
        protected DenseVector shape, rate;
        protected DenseVector value, logValue;

        public GammaDenseVector(int _size) {
            size = _size;
            shape = new DenseVector(size);
            rate = new DenseVector(size);
            value = new DenseVector(size);
            logValue = new DenseVector(size);
        }

        public void init() {
            for (int i = 0; i < size; i++) {
                shape.set(i, shapePrior + 0.01 * Randoms.uniform(0.0, 1.0));
                rate.set(i, ratePrior + 0.1 * Randoms.uniform(0.0, 1.0));
            }
        }

        public void init2(double v) {
            for (int i = 0; i < size; i++) {
                shape.set(i, shapePrior + 0.01 * Randoms.uniform(0.0, 1.0));
                rate.set(i, ratePrior + v);
            }
        }

        public void computeExpectations() {
            double a = 0.0, b = 0.0;

            for (int i = 0; i < size; i++) {
                if (shape.get(i) <= 0.0) {
                    a = 1e-30;
                }
                if (rate.get(i) <= 0.0) {
                    b = 1e-30;
                }
                value.set(i, a / b);
                logValue.set(i, Gamma.digamma(a) - Math.log(b));
            }

        }
    }

    public class GammaDenseMatrix {

        protected int numRows, numColumns;
        protected double shapePrior;
        protected double ratePrior;
        protected DenseMatrix shape;
        protected DenseMatrix rate;
        protected DenseMatrix value;
        protected DenseMatrix logValue;

        public GammaDenseMatrix(int _numRows, int _numColumns) {
            numRows = _numRows;
            numColumns = _numColumns;
            shape = new DenseMatrix(numRows, numColumns);
            rate = new DenseMatrix(numRows, numColumns);
            value = new DenseMatrix(numRows, numColumns);
            logValue = new DenseMatrix(numRows, numColumns);
        }

        public void init() {
            for (int i = 0; i < numRows; i++) {
                for (int j = 0; j < numColumns; j++) {
                    shape.set(i, j, shapePrior + 0.01 * Randoms.uniform(0.0, 1.0));

                    if (i == 0) {
                        rate.set(0, j, ratePrior + 0.1 * Randoms.uniform(0.0, 1.0));
                    } else {
                        rate.set(i, j, rate.get(0, j));
                    }
                }
            }

            for (int i = 0; i < numRows; i++) {
                for (int j = 0; j < numColumns; j++) {
                    value.set(i, j, shape.get(i, j) / rate.get(i, j));
                    logValue.set(i, j, Gamma.digamma(shape.get(i, j)) - Math.log(rate.get(i, j)));
                }
            }
        }

        public void computeExpectations() {
            double a = 0.0, b = 0.0;
            for (int i = 0; i < numRows; i++) {
                for (int j = 0; j < numColumns; j++) {
                    if (shape.get(i, j) <= 0) {
                        a = 1e-30;
                    }
                    if (rate.get(i, j) <= 0) {
                        b = 1e-30;
                    }
                    value.set(i, j, a / b);
                    logValue.set(i, j, Gamma.digamma(a) - Math.log(b));
                }
            }
        }
    }

    public class GammaDenseMatrixGR {

        protected int numRows, numColumns;
        protected double shapePrior;
        protected double ratePrior;
        protected DenseMatrix shape;
        protected DenseVector rate;
        protected DenseMatrix value;
        protected DenseMatrix logValue;

        public GammaDenseMatrixGR(int _numRows, int _numColumns) {
            numRows = _numRows;
            numColumns = _numColumns;
            shape = new DenseMatrix(numRows, numColumns);
            rate = new DenseVector(numColumns);
            value = new DenseMatrix(numRows, numColumns);
            logValue = new DenseMatrix(numRows, numColumns);
        }

        public void init() {
            for (int i = 0; i < numRows; i++) {
                for (int j = 0; j < numColumns; j++) {
                    shape.set(i, j, shapePrior + 0.01 * Randoms.uniform(0.0, 1.0));
                }
            }

            for (int j = 0; j < numColumns; j++) {
                rate.set(j, ratePrior + 0.1 * Randoms.uniform(0.0, 1.0));
            }

            for (int i = 0; i < numRows; i++) {
                for (int j = 0; j < numColumns; j++) {
                    value.set(i, j, shape.get(i, j) / rate.get(j));
                    logValue.set(i, j, Gamma.digamma(shape.get(i, j)) - Math.log(rate.get(j)));
                }
            }
        }

        public void computeExpectations() {
            double a = 0.0, b = 0.0;
            for (int i = 0; i < numRows; i++) {
                for (int j = 0; j < numColumns; j++) {
                    if (shape.get(i, j) <= 0) {
                        a = 1e-30;
                    }
                    if (rate.get(j) <= 0) {
                        b = 1e-30;
                    }
                    value.set(i, j, a / b);
                    logValue.set(i, j, Gamma.digamma(a) - Math.log(b));
                }
            }
        }
    }
}
