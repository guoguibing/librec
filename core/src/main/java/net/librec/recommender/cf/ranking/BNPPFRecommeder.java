package net.librec.recommender.cf.ranking;

import net.librec.common.LibrecException;
import net.librec.math.algorithm.Gamma;
import net.librec.math.algorithm.Randoms;
import net.librec.math.structure.*;
import net.librec.recommender.MatrixFactorizationRecommender;


/**
 * Gopalan, P., Ruiz, F. J., Ranganath, R., & Blei, D. M.
 * <strong>Bayesian Nonparametric Poisson Factorization for Recommendation Systems</strong>, ICAIS 2014
 *
 * @author Yatong Sun
 */

public class BNPPFRecommeder extends MatrixFactorizationRecommender {

    /** user Gamma shape */
    private double alpha;

    /** user Gamma rate */
    private double c;

    /** item Gamma shape */
    private double a;

    /** item Gamma rate */
    private double b;

    /** variational sticks */
    private DenseMatrix v;

    private DenseMatrix pi;

    private DenseMatrix logpi;

    private GammaDenseMatrixGR beta;

    private GammaDenseVector s;

    private DenseVector eBetaSum;

    private DenseVector eThetaSum;

    private DenseMatrix zUsers;
    private DenseMatrix zItems;

    private DenseVector userBudget;

    double d_scalar;

    @Override
    protected void setup() throws LibrecException {
        super.setup();
        alpha = conf.getDouble("rec.alpha", 0.3);
        c = conf.getDouble("rec.c", 0.3);
        a = conf.getDouble("rec.a", 0.3);
        b = conf.getDouble("rec.b", 0.3);

        //_beta.initialize();
        beta = new GammaDenseMatrixGR(numItems, numFactors, a, b);
        beta.init();

        //_s.initialize();
        s = new GammaDenseVector(numUsers, alpha, c);
        s.init();

        //initialize_sticks();
        v = new DenseMatrix(numUsers, numFactors);
        v.init(0.001);

        //compute_pi();
        pi = new DenseMatrix(numUsers, numFactors);
        logpi = new DenseMatrix(numUsers, numFactors);
        for (int u=0; u<numUsers; u++) {
            double lw = 0.0;
            for (int k=0; k<numFactors; k++) {
                double v_value = v.get(u, k);
                if (v_value < 1e-30) {
                    v_value = 1e-30;
                    v.set(u, k, 1e-30);
                }
                if (k > 0) {
                    lw += Math.log(1-v.get(u, k-1));
                }
                logpi.set(u, k, Math.log(v_value) + lw);

                double pi_value = Math.exp(v_value);
                pi.set(u, k, pi_value);
                userFactors.set(u, k, pi_value * s.value.get(u));
            }
        }

        eBetaSum = new VectorBasedDenseVector(numFactors);
        eThetaSum = new VectorBasedDenseVector(numFactors);

        zUsers = new DenseMatrix(numUsers, numFactors);
        zItems = new DenseMatrix(numItems, numFactors);
        userBudget = new VectorBasedDenseVector(numUsers);
        for (int u=0; u<numUsers; u++) {
            userBudget.set(u, trainMatrix.row(u).sum());
        }

        d_scalar = (a/b) * numItems;
    }

    @Override
    protected void trainModel() throws LibrecException {

        computeExpectations();

        commputeSums();

        DenseVector phi = new VectorBasedDenseVector(numFactors);

        for (int iter = 1; iter <= numIterations; iter++) {
            clearState();
            for (int u=0; u<numUsers; u++) {
                SequentialSparseVector items = trainMatrix.row(u);
                for (Vector.VectorEntry ve: items) {
                    int itemIdx = ve.index();
                    double y = ve.get();
                    getPhi(u, itemIdx, phi);

                    if (y>1) {
                        phi.times(y);
                    }

                    zUsers.set(u, zUsers.row(u).plus(phi));
                    zItems.set(itemIdx, zItems.row(itemIdx).plus(phi));
                }
            }
            updateSticks();

            update_sticks_scalar();

            updateItems();

        }

        itemFactors = new DenseMatrix(beta.value);

    }

    private void computeExpectations() {
        s.computeExpectations();
        beta.computeExpectations();
    }

    private void commputeSums() {
        computeEThetaSum();
        computeEBetaSum();
    }

    private void computeEThetaSum() {
        for (int k=0; k<numFactors; k++) {
            eThetaSum.set(k, 0.0);
        }
        for (int u=0; u<numUsers; u++) {
            for (int k=0; k<numFactors; k++) {
                eThetaSum.set(k, eThetaSum.get(k) + s.value.get(u) * pi.get(u, k));
            }
        }
    }

    private void computeEBetaSum() {
        for (int k=0; k<numFactors; k++) {
            eBetaSum.set(k, beta.value.column(k).sum());
        }
    }

    private void clearState() {
        for (int i=0; i<numItems; i++) {
            for (int k=0; k<numFactors; k++) {
                zItems.set(i, k, 0.0);
            }
        }
        for (int u=0; u<numUsers; u++) {
            for (int k=0; k<numFactors; k++) {
                zUsers.set(u, k, 0.0);
            }
        }
    }

    private void getPhi(int userIdx, int itemIdx, DenseVector phi) {
        for (int k=0; k<numFactors; k++) {
            phi.set(k, elogTheta(userIdx, k) + beta.logValue.get(itemIdx, k));
        }
        DenseVector s = new VectorBasedDenseVector(2);
        s.set(0, logSum(phi));
        s.set(1, compute_mult_normalizer_infsum(userIdx));

        double logSum = logSum(s);
        lognormalize(phi, logSum);
    }

    private double compute_mult_normalizer_infsum(int u) {
        double x = elogtheta_at_truncation(u) + elogbeta_at_truncation();
        double elogv_t = Gamma.digamma(alpha) - Gamma.digamma(1+alpha);
        return x - Math.log(1-Math.exp(elogv_t));
    }

    private double elogtheta_at_truncation(int u) {
        double elogvt = Gamma.digamma(1) - Gamma.digamma( 1 + alpha);
        return s.logValue.get(u) + elogvt + logpi.get(u, numFactors - 1) - Math.log(v.get(u, numFactors - 1)) + Math.log(1-v.get(u, numFactors -1));
    }

    private double elogbeta_at_truncation() {
        return Gamma.digamma(a) - Math.log(b);
    }

    private double logSum(DenseVector vector) {
        double r = 0.0;
        int count = 0;
        for (Vector.VectorEntry ve: vector) {
            double value = ve.get();
            if (count == 0) {
                r = value;
                count += 1;
                continue;
            }
            if (value < r) {
                r += Math.log(1+Math.exp(value - r));
            } else {
                r = value + Math.log(1+Math.exp(r - value));
            }
        }
        return r;
    }

    private void lognormalize(DenseVector vector, double logSum) {
        for (Vector.VectorEntry ve: vector) {
            ve.set(ve.get() - logSum);
        }
    }

    private double elogTheta(int u, int k) {
        return s.logValue.get(u) + logpi.get(u, k);
    }

    private void updateSticks() {
        for (int u=0; u<numUsers; u++) {
            double lw = 0.0;
            for (int k = 0; k < numFactors; k++) {
                double lpid_at_T = 0.0;
                double v_value = v.get(u, k);
                double[] p_and_sum = sum_of_prod_in_range(u, k+1);
                double Auk = s.value.get(u) * (-1 + prob_at_k(u, k) / v_value)
                        + p_and_sum[1] / (1-v_value) + compute_scalar_rate_infsum(u) / (1-v_value);
                if (Math.abs(Auk) < 1e-30) {
                    double x = zUsers.get(u, k) / B(u, k, Auk);
                    if (x < 1e-30) {
                        v_value = 1e-30;
                    } else {
                        v_value = x;
                    }
                    v.set(u, k, v_value);
                } else {
                    v_value = solve_quadratic(Auk, B(u, k, Auk), -zUsers.get(u, k));
                    v.set(u, k, v_value);
                }
                if (k > 0) {
                    pi.set(u, k, pi.get(u, k-1) / v.get(u, k-1) * (1-v.get(u, k-1)) * v_value);
                } else {
                    pi.set(u, k, v_value);
                }
                logpi.set(u, k, Math.log(pi.get(u, k)));
                userFactors.set(u, k, pi.get(u, k) * s.value.get(u));
            }
        }
    }

    private double[] sum_of_prod_in_range(int u, int K) {
        double sum = 0.0;
        double p = convert_oldpi_to_new(u, K-1);
        for (int k=K; k<numFactors; k++) {
            p = convert_oldpi_to_new(p, u, k);
            sum += p * eBetaSum.get(k);
        }
        return new double[] {p, sum};
    }

    private double convert_oldpi_to_new(int u, int k) {
        if (k == 0) {
            return pi.get(u, 0);
        } else {
            return pi.get(u, k-1) * ((1-v.get(u, k-1)) / v.get(u, k-1)) * v.get(u, k);
        }
    }

    private double convert_oldpi_to_new(double pi_at_kminus1, int u, int k) {
        return pi_at_kminus1 * ((1-v.get(u, k-1)) / v.get(u, k-1)) * v.get(u, k);
    }

    private double compute_scalar_rate_infsum(int u) {
        double y = computeY(u);
        return y * d_scalar;
    }

    private double computeY(int u) {
        double v_value = v.get(u, numFactors - 1);
        return pi.get(u, numFactors - 1) / v_value * (1-v_value);
    }

    private double prob_at_k(int u, int k) {
        return convert_oldpi_to_new(u, k) * eBetaSum.get(k);
    }

    private double B(int u, int k, double Auk) {
        return alpha - 1 + zUsers.get(u, k) - Auk + compute_zUser_sum(u, k);
    }

    private double compute_zUser_sum(int u, int tok) {
        double sum = 0.0;
        for (int k=0; k<tok; k++) {
            sum += zUsers.get(u, k);
        }
        return userBudget.get(u) - sum;
    }

    private double solve_quadratic(double a, double b, double c) {
        double s1 = (-b + Math.sqrt(b*b - 4*a*c)) / (2*a);
        double s2 = (-b - Math.sqrt(b*b - 4*a*c)) / (2*a);

        if (s1 > .0 && s1 <= 1.0 && s2 > .0 && s2 <= 1.0) {

            if (s1 < s2)
                return s1 + 1e-30;
            else
                return s2 + 1e-30;
        }

        if (s1 > .0 && s1 <= 1.0)
            return s1;

        if (s2 > .0 && s2 <= 1.0)
            return s2;

        // TODO
        if (Math.abs(s1 - .0) < 1e-30)
            return 1e-30;

        if (Math.abs(s1 - 1.0) < 1e-30)
            return 1 - 1e-30;

        if (Math.abs(s2 - .0) < 1e-30)
            return 1e-30;

        if (Math.abs(s2 - 1.0) < 1e-30)
            return 1 - 1e-30;

        return s1;
    }

    private void update_sticks_scalar() {
        for (int u=0; u<numUsers; u++) {
            double infsum = compute_scalar_rate_infsum(u);
            double fnsum = compute_scalar_rate_finitesum(u);
            s.shape.set(u, userBudget.get(u));
            s.rate.set(u, fnsum + infsum);
            s.computeExpectations();
            computeEThetaSum();
        }
    }

    private double compute_scalar_rate_finitesum(int u) {
        return pi.row(u).sum() + eBetaSum.sum();
    }

    private void updateItems() {
        for (int i=0; i<numItems; i++) {
            beta.shape.set(i, zItems.row(i));
        }
        beta.rate = new VectorBasedDenseVector(eThetaSum);
        beta.computeExpectations();
        computeEBetaSum();
    }

    public class GammaDenseVector {
        protected int size;
        protected double shapePrior, ratePrior;
        protected DenseVector shape, rate;
        protected DenseVector value, logValue;

        public GammaDenseVector(int _size, double shapeP, double rateP) {
            shapePrior = shapeP;
            ratePrior = rateP;
            size = _size;
            shape = new VectorBasedDenseVector(size);
            rate = new VectorBasedDenseVector(size);
            value = new VectorBasedDenseVector(size);
            logValue = new VectorBasedDenseVector(size);
        }

        public void init() {
            for (int i = 0; i < size; i++) {
                shape.set(i, shapePrior + 0.01 * Randoms.uniform(0.0, 1.0));
                rate.set(i, ratePrior + 0.1 * Randoms.uniform(0.0, 1.0));
            }
            for (int i = 0; i < size; i++) {
                value.set(i, shape.get(i) / rate.get(i));
                logValue.set(i, Gamma.digamma(shape.get(i)) - Math.log(rate.get(i)));
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
            value = new VectorBasedDenseVector(size);
            logValue = new VectorBasedDenseVector(size);
            for (int i = 0; i < size; i++) {
                double av = shape.get(i);
                double bv = rate.get(i);
                // assert (av >= 0 && bv >= 0);
                if (av <= 0) {
                    a = 1e-30;
                } else {
                    a = av;
                }
                if (bv <= 0) {
                    b = 1e-30;
                } else {
                    b = bv;
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

        public GammaDenseMatrix(int _numRows, int _numColumns, double shapeP, double rateP) {
            shapePrior = shapeP;
            ratePrior = rateP;
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
                    double av = shape.get(i, j);
                    double bv = rate.get(i, j);
                    // assert (av >= 0 && bv >= 0);
                    if (av <= 0) {
                        a = 1e-30;
                    } else {
                        a = av;
                    }
                    if (bv <= 0) {
                        b = 1e-30;
                    } else {
                        b = bv;
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

        public GammaDenseMatrixGR(int _numRows, int _numColumns, double shapeP, double rateP) {
            shapePrior = shapeP;
            ratePrior = rateP;
            numRows = _numRows;
            numColumns = _numColumns;
            shape = new DenseMatrix(numRows, numColumns);
            rate = new VectorBasedDenseVector(numColumns);
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
                    double av = shape.get(i, j);
                    double bv = rate.get(j);
                    // assert (av >= 0 && bv >= 0);
                    if (av <= 0) {
                        a = 1e-30;
                    } else {
                        a = av;
                    }
                    if (bv <= 0) {
                        b = 1e-30;
                    } else {
                        b = bv;
                    }
                    value.set(i, j, a / b);
                    logValue.set(i, j, Gamma.digamma(a) - Math.log(b));
                }
            }
        }
    }
}
