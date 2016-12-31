package net.librec.recommender.cf.ranking;

import net.librec.annotation.ModelData;
import net.librec.common.LibrecException;
import net.librec.math.structure.DenseMatrix;
import net.librec.math.structure.MatrixEntry;
import net.librec.math.structure.SparseMatrix;
import net.librec.recommender.MatrixFactorizationRecommender;

import java.util.ArrayList;
import java.util.List;

/**
 * <h3>EALS: efficient Alternating Least Square for Weighted Regularized Matrix Factorization.</h3>
 * <p>
 * This implementation refers to the method proposed by He et al. at SIGIR 2016.
 * <ul>
 * <li><strong>Real ratings:</strong> Hu et al., Collaborative filtering for implicit feedback datasets, ICDM 2008.</li>
 * <li>Fast Matrix Factorization for Online Recommendation With Implicit Feedback, SIGIR 2016</li>
 * </ul>
 *
 * @author Keqiang Wang
 */
@ModelData({"isRanking", "eals", "userFactors", "itemFactors", "trainMatrix"})
public class EALSRecommender extends MatrixFactorizationRecommender {
    /**
     * confidence weight coefficient for WRMF
     */
    protected float weightCoefficient;

    /**
     * the significance level of popular items over un-popular ones
     */
    private float ratio;

    /**
     * the overall weight of missing data c0
     */
    private float overallWeight;

    /**
     * 0：eALS MF; 1：WRMF; 2: both
     */
    private int WRMFJudge;

    /**
     * confidence that item i missed by users
     */
    private double[] confidences;

    /**
     * weights of all user-item pair (u,i)
     */
    private SparseMatrix weights;

    @Override
    protected void setup() throws LibrecException {
        super.setup();
        weightCoefficient = conf.getFloat("rec.wrmf.weight.coefficient", 4.0f);
        ratio = conf.getFloat("rec.eals.ratio", 0.4f);
        overallWeight = conf.getFloat("rec.eals.overall", 128.0f);
        WRMFJudge = conf.getInt("rec.eals.wrmf.judge", 1);

        confidences = new double[numItems];
        weights = new SparseMatrix(trainMatrix);

        initConfidencesAndWeights();
    }

    private void initConfidencesAndWeights() {
        //get ci
        if (WRMFJudge == 0 || WRMFJudge == 2) {
            double sumPopularity = 0.0;

            for (int itemIdx = 0; itemIdx < numItems; itemIdx++) {
                double alphaPopularity = Math.pow(trainMatrix.columnSize(itemIdx) * 1.0 / numRates, ratio);
                confidences[itemIdx] = overallWeight * alphaPopularity;
                sumPopularity += alphaPopularity;
            }
            for (int itemIdx = 0; itemIdx < numItems; itemIdx++) {
                confidences[itemIdx] = confidences[itemIdx] / sumPopularity;
            }
        } else {
            for (int itemIdx = 0; itemIdx < numItems; itemIdx++) {
                confidences[itemIdx] = 1;
            }
        }

        // By default, the weight for positive instance is uniformly 1.
        for (MatrixEntry matrixEntry : trainMatrix) {
            int userIdx = matrixEntry.row(); // user
            int itemIdx = matrixEntry.column(); // item
            if (WRMFJudge == 1 || WRMFJudge == 2) {
                weights.set(userIdx, itemIdx, 1.0 + weightCoefficient * matrixEntry.get());
//                w.set(u, i, 1.0 + Math.log(1.0 + Math.pow(10, alpha) * me.get())); maybe better for poi recommender
            } else {
                weights.set(userIdx, itemIdx, 1.0);
            }
        }
    }

    @Override
    protected void trainModel() throws LibrecException {
        List<List<Integer>> userItemsList = getUserItemsList(trainMatrix);
        List<List<Integer>> itemUsersList = getItemUsersList(trainMatrix);

        double[] usersPredictions = new double[numUsers];
        double[] itemsPredictions = new double[numItems];
        double[] usersWeights = new double[numUsers];
        double[] itemsWeights = new double[numItems];

        // Init item factors cache Sq
        DenseMatrix itemFactorsCache = new DenseMatrix(numFactors, numFactors);
        //Init user factors cache Sp
        DenseMatrix userFactorsCache;

        for (int iter = 1; iter <= numIterations; iter++) {
            // Update the Sq cache
            for (int factorIdx1 = 0; factorIdx1 < numFactors; factorIdx1++) {
                for (int factorIdx2 = 0; factorIdx2 <= factorIdx1; factorIdx2++) {
                    double value = 0;
                    for (int itemIdx = 0; itemIdx < numItems; itemIdx++) {
                        value += confidences[itemIdx] * itemFactors.get(itemIdx, factorIdx1) * itemFactors.get(itemIdx, factorIdx2);
                    }
                    itemFactorsCache.set(factorIdx1, factorIdx2, value);
                    itemFactorsCache.set(factorIdx2, factorIdx1, value);
                }
            }
            // Step 1: update user factors;
            for (int userIdx = 0; userIdx < numUsers; userIdx++) {
                for (int itemIdx : userItemsList.get(userIdx)) {
                    itemsPredictions[itemIdx] = DenseMatrix.rowMult(userFactors, userIdx, itemFactors, itemIdx);
                    itemsWeights[itemIdx] = weights.get(userIdx, itemIdx);
                }

                for (int factorCacheIdx = 0; factorCacheIdx < numFactors; factorCacheIdx++) {
                    double numer = 0, denom = regUser + itemFactorsCache.get(factorCacheIdx, factorCacheIdx);

                    for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                        if (factorCacheIdx != factorIdx) {
                            numer -= userFactors.get(userIdx, factorIdx) * itemFactorsCache.get(factorCacheIdx, factorIdx);
                        }
                    }

                    for (int itemIdx : userItemsList.get(userIdx)) {
                        itemsPredictions[itemIdx] -= userFactors.get(userIdx, factorCacheIdx) * itemFactors.get(itemIdx, factorCacheIdx);
                        numer += (itemsWeights[itemIdx] - (itemsWeights[itemIdx] - confidences[itemIdx]) * itemsPredictions[itemIdx])
                                * itemFactors.get(itemIdx, factorCacheIdx);
                        denom += (itemsWeights[itemIdx] - confidences[itemIdx]) * itemFactors.get(itemIdx, factorCacheIdx)
                                * itemFactors.get(itemIdx, factorCacheIdx);
                    }

                    //update puf
                    userFactors.set(userIdx, factorCacheIdx, numer / denom);
                    for (int itemIdx : userItemsList.get(userIdx)) {
                        itemsPredictions[itemIdx] += userFactors.get(userIdx, factorCacheIdx) * itemFactors.get(itemIdx, factorCacheIdx);
                    }
                }
            }
            // Update the Sp cache
            userFactorsCache = userFactors.transpose().mult(userFactors);
            // Step 2: update item factors;
            for (int itemIdx = 0; itemIdx < numItems; itemIdx++) {
                for (int userIdx : itemUsersList.get(itemIdx)) {
                    usersPredictions[userIdx] = DenseMatrix.rowMult(userFactors, userIdx, itemFactors, itemIdx);
                    usersWeights[userIdx] = weights.get(userIdx, itemIdx);
                }

                for (int factorCacheIdx = 0; factorCacheIdx < numFactors; factorCacheIdx++) {
                    double numer = 0, denom = confidences[itemIdx] * userFactorsCache.get(factorCacheIdx, factorCacheIdx) + regItem;

                    for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                        if (factorCacheIdx != factorIdx) {
                            numer -= itemFactors.get(itemIdx, factorIdx) * userFactorsCache.get(factorIdx, factorCacheIdx);
                        }
                    }
                    numer *= confidences[itemIdx];

                    for (int userIdx : itemUsersList.get(itemIdx)) {
                        usersPredictions[userIdx] -= userFactors.get(userIdx, factorCacheIdx) * itemFactors.get(itemIdx, factorCacheIdx);
                        numer += (usersWeights[userIdx] - (usersWeights[userIdx] - confidences[itemIdx]) * usersPredictions[userIdx])
                                * userFactors.get(userIdx, factorCacheIdx);
                        denom += (usersWeights[userIdx] - confidences[itemIdx]) * userFactors.get(userIdx, factorCacheIdx)
                                * userFactors.get(userIdx, factorCacheIdx);
                    }

                    //update qif
                    itemFactors.set(itemIdx, factorCacheIdx, numer / denom);
                    for (int userIdx : itemUsersList.get(itemIdx)) {
                        usersPredictions[userIdx] += userFactors.get(userIdx, factorCacheIdx) * itemFactors.get(itemIdx, factorCacheIdx);
                    }
                }
            }
        }
    }


    private List<List<Integer>> getUserItemsList(SparseMatrix sparseMatrix) {
        List<List<Integer>> userItemsList = new ArrayList<>();
        for (int userIdx = 0; userIdx < numUsers; ++userIdx) {
            userItemsList.add(sparseMatrix.getColumns(userIdx));
        }
        return userItemsList;
    }

    private List<List<Integer>> getItemUsersList(SparseMatrix sparseMatrix) {
        List<List<Integer>> itemUsersList = new ArrayList<>();
        for (int itemIdx = 0; itemIdx < numItems; ++itemIdx) {
            itemUsersList.add(sparseMatrix.getRows(itemIdx));
        }
        return itemUsersList;
    }
}
