package net.librec.recommender.cf.rating;

import net.librec.math.algorithm.Randoms;
import net.librec.math.structure.DenseMatrix;
import net.librec.math.structure.DenseVector;
import net.librec.math.structure.MatrixEntry;
import net.librec.math.structure.SparseMatrix;

/**
 * <h3> Local Low-Rank Matrix Approximation</h3>
 * <p>
 * This implementation refers to the method proposed by Lee et al. at ICML 2013.
 * <p>
 * <strong>Lcoal Structure:</strong> Joonseok Lee, <strong>Local Low-Rank Matrix Approximation
 * </strong>, ICML. 2013: 82-90.
 *
 * @author GuoGuibing and Keqiang Wang
 */
public class LLORMAUpdater extends Thread {
    /**
     * The unique identifier of the thread.
     */
    private int threadId;

    /**
     * The number of features.
     */
    private int numFactors;

    /**
     * The number of users.
     */
    private int numUsers;

    /**
     * The number of items.
     */
    private int numItems;

    /**
     * The anchor user used to learn this local model.
     */
    private int anchorUser;

    /**
     * The anchor item used to learn this local model.
     */
    private int anchorItem;

    /**
     * Learning rate parameter.
     */
    public double learnRate;

    /**
     * The maximum number of iteration.
     */
    public int localIteration;

    /**
     * Regularization factor parameter.
     */
    public double localRegUser, localRegItem;

    /**
     * The vector containing each user's weight.
     */
    private DenseVector userWeights;

    /**
     * The vector containing each item's weight.
     */
    private DenseVector itemWeights;

    /**
     * User profile in low-rank matrix form.
     */
    private DenseMatrix localUserFactors;

    /**
     * Item profile in low-rank matrix form.
     */
    private DenseMatrix localItemFactors;

    /**
     * The rating matrix used for learning.
     */
    private SparseMatrix trainMatrix;

    /**
     * Construct a local model for singleton LLORMA.
     *
     * @param threadIDParam    A unique thread ID.
     * @param numFactorsParam  The rank which will be used in this local model.
     * @param numUsersParam    The number of users.
     * @param numItemsParam    The number of items.
     * @param anchorUserParam  The anchor user used to learn this local model.
     * @param anchorItemParam  The anchor item used to learn this local model.
     * @param learnRateParam   Learning rate parameter.
     * @param userWeightsParam Initial vector containing each user's weight.
     * @param itemWeightsParam Initial vector containing each item's weight.
     * @param trainMatrixParam The rating matrix used for learning.
     * @param localIterationParam  localIterationParam
     * @param localRegItemParam  localRegItemParam
     * @param localRegUserParam  localRegUserParam
     */
    public LLORMAUpdater(int threadIDParam, int numFactorsParam, int numUsersParam, int numItemsParam, int anchorUserParam,
                         int anchorItemParam, double learnRateParam, double localRegUserParam, double localRegItemParam,
                         int localIterationParam, DenseVector userWeightsParam, DenseVector itemWeightsParam, SparseMatrix trainMatrixParam) {
        threadId = threadIDParam;
        numFactors = numFactorsParam;
        numUsers = numUsersParam;
        numItems = numItemsParam;
        anchorUser = anchorUserParam;
        anchorItem = anchorItemParam;
        learnRate = learnRateParam;
        localRegUser = localRegUserParam;
        localRegItem = localRegItemParam;
        localIteration = localIterationParam;
        userWeights = userWeightsParam;
        itemWeights = itemWeightsParam;
        localUserFactors = new DenseMatrix(numUsers, numFactors);
        localItemFactors = new DenseMatrix(numItems, numFactors);
        trainMatrix = trainMatrixParam;
    }

    /**
     * Getter method for thread ID.
     *
     * @return The thread ID of this local model.
     */
    public int getThreadId() {
        return threadId;
    }

    /**
     * Getter method for rank of this local model.
     *
     * @return The rank of this local model.
     */
    public int getRank() {
        return numFactors;
    }

    /**
     * Getter method for anchor user of this local model.
     *
     * @return The anchor user ID of this local model.
     */
    public int getUserAnchor() {
        return anchorUser;
    }

    /**
     * Getter method for anchor item of this local model.
     *
     * @return The anchor item ID of this local model.
     */
    public int getItemAnchor() {
        return anchorItem;
    }

    /**
     * Getter method for user profile of this local model.
     *
     * @return The user profile of this local model.
     */
    public DenseMatrix getLocalUserFactors() {
        return localUserFactors;
    }

    /**
     * Getter method for item profile of this local model.
     *
     * @return The item profile of this local model.
     */
    public DenseMatrix getLocalItemFactors() {
        return localItemFactors;
    }

    /**
     * Learn this local model based on similar users to the anchor user
     * and similar items to the anchor item.
     * Implemented with gradient descent.
     */
    @Override
    public void run() {
        for (int userIdx = 0; userIdx < numUsers; userIdx++) {
            for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                double rdm = Randoms.gaussian(0.0, 0.01);
                localUserFactors.set(userIdx, factorIdx, rdm);
            }
        }
        for (int itemIdx = 0; itemIdx < numItems; itemIdx++) {
            for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                double rdm = Randoms.gaussian(0.0, 0.01);
                localItemFactors.set(itemIdx, factorIdx, rdm);
            }
        }

        // Learn by Weighted RegSVD
        for (int iter = 0; iter < localIteration; iter++) {
            for (MatrixEntry matrixEntry : trainMatrix) {
                int userIdx = matrixEntry.row(); // user
                int itemIdx = matrixEntry.column(); // item
                double rating = matrixEntry.get();

                double predictRating = predict(userIdx, itemIdx);
                double error = rating - predictRating;
                double weight = userWeights.get(userIdx) * itemWeights.get(itemIdx);

                // update factors
                for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                    double userFactorValue = localUserFactors.get(userIdx, factorIdx);
                    double itemFactorValue = localItemFactors.get(itemIdx, factorIdx);

                    localUserFactors.add(userIdx, factorIdx, learnRate * (error * itemFactorValue * weight - localRegUser * userFactorValue));
                    localItemFactors.add(itemIdx, factorIdx, learnRate * (error * userFactorValue * weight - localRegItem * itemFactorValue));
                }
            }
        }
    }

    protected double predict(int userIdx, int itemIdx) {
        return DenseMatrix.rowMult(localUserFactors, userIdx, localItemFactors, itemIdx);
    }
}
