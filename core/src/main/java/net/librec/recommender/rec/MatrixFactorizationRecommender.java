package net.librec.recommender.rec;

import net.librec.common.LibrecException;
import net.librec.math.structure.DenseMatrix;
import net.librec.recommender.AbstractRecommender;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * Matrix Factorization Recommender
 * Methods with user factors and item factors: such as SVD(Singular Value Decomposition)
 * <p>
 * Created by Keqiang Wang
 */
public abstract class MatrixFactorizationRecommender extends AbstractRecommender {
    /**
     * user latent factors
     */
    protected DenseMatrix userFactors;
    /**
     * item latent factors
     */
    protected DenseMatrix itemFactors;
    /**
     * the number of latent factors;
     */
    protected int numFactors;
    /**
     * the number of iterations
     */
    protected int numIterations;
    /**
     * init mean
     */
    protected float initMean;
    /**
     * init standard deviation
     */
    protected float initStd;

    /**
     * user regularization
     */
    protected float regUser;
    /**
     * item regularization
     */
    protected float regItem;

    /**
     * global mean
     */
    protected double globalMean;


    /**
     * setup
     * init member method
     *
     * @throws LibrecException
     * @throws FileNotFoundException 
     */
    protected void setup() throws LibrecException {
        super.setup();
        numIterations = conf.getInt("rec.iterator.maximum");

        regUser = conf.getFloat("rec.user.regularization", 0.01f);
        regItem = conf.getFloat("rec.item.regularization", 0.01f);

        numFactors = conf.getInt("rec.factory.number", 10);
        userFactors = new DenseMatrix(numUsers, numFactors);
        itemFactors = new DenseMatrix(numItems, numFactors);
        globalMean = trainMatrix.mean();

        initMean = 0.0f;
        initStd = 0.1f;

        // initialize factors
        userFactors.init(initMean, initStd);
        itemFactors.init(initMean, initStd);
    }

    /**
     * predict a specific rating for user userIdx on item itemIdx.
     *
     * @param userIdx user index
     * @param itemIdx item index
     * @return predictive rating for user userIdx on item itemIdx with bound
     * @throws LibrecException
     */
    protected double predict(int userIdx, int itemIdx) throws LibrecException {
        return DenseMatrix.rowMult(userFactors, userIdx, itemFactors, itemIdx);
    }

}
