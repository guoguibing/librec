package net.librec.recommender.rec;

import net.librec.common.LibrecException;
import net.librec.math.structure.*;
import net.librec.recommender.AbstractRecommender;

/**
 *
 */

public abstract class FactorizationMachineRecommender extends AbstractRecommender {

    protected static SparseMatrix rateFMData;

    protected double w0;
    protected int p; // feature vector size: number of users + number of items + number of contextual conditions
    protected int k; // number of factors
    protected int n; // N = number of ratings
    protected DenseVector W; //  p
    protected DenseMatrix V; //  p x k
    protected DenseMatrix Q; //  n x k
    protected float regW0, regW, regF;  // regularization term for weight and factors

//    static {
//        rateFMData = rateDao.getRateFMData();
//    }

    /**
     * the number of latent factors;
     */
    protected int numFactors;
    /**
     * the number of iterations
     */
    protected int numIterations;

    protected void setup() throws LibrecException {
        p = rateFMData.numColumns();
        n = trainMatrix.rows().size();
        k = numFactors;

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

    protected double predict(SparseVector x) throws LibrecException {
        double res = 0;
        // global bias
        res += w0;

        // 1-way interaction
        for (VectorEntry ve: x){
            double val = ve.get();
            int ind = ve.index();
            res += val * W.get(ind);
        }

        // 2-way interaction
        for (int f = 1; f < k; f ++){
            double sum1 = 0; double sum2 = 0;
            for (VectorEntry ve: x){
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


    protected int[] getUserItemIndex(SparseVector x) {
        int[] inds = x.getIndex();

        int userInd = inds[0];
        int itemInd = inds[1] - numUsers;

        return new int[]{userInd, itemInd};
    }

    protected int[] getUserItemIndex(int row) {
        SparseVector x = rateFMData.row(row);
        int[] inds = x.getIndex();

        int userInd = inds[0];
        int itemInd = inds[1] - numUsers;

        return new int[]{userInd, itemInd};
    }
}
