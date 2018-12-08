package net.librec.recommender.cf.ranking;

import it.unimi.dsi.fastutil.doubles.Double2DoubleOpenHashMap;
import net.librec.annotation.ModelData;
import net.librec.common.LibrecException;
import net.librec.math.structure.DenseMatrix;
import net.librec.math.structure.MatrixEntry;
import net.librec.math.structure.SequentialSparseVector;
import net.librec.math.structure.Vector;
import net.librec.recommender.MatrixFactorizationRecommender;

import java.util.Date;

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

    @Override
    protected void setup() throws LibrecException {
        super.setup();
        weightCoefficient = conf.getFloat("rec.wrmf.weight.coefficient", 4.0f);
        ratio = conf.getFloat("rec.eals.ratio", 0.4f);
        overallWeight = conf.getFloat("rec.eals.overall", 128.0f);
        WRMFJudge = conf.getInt("rec.eals.wrmf.judge", 1);

        confidences = new double[numItems];
        initConfidencesAndWeights();
    }

    private void initConfidencesAndWeights() {
        //get ci
        if (WRMFJudge == 0 || WRMFJudge == 2) {
            double sumPopularity = 0.0;

            for (int itemIdx = 0; itemIdx < numItems; itemIdx++) {
                double alphaPopularity = Math.pow(trainMatrix.column(itemIdx).getNumEntries() * 1.0 / numRates, ratio);
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

        weightMatrix();
    }

    public double weight(double value) {
        double weight;
        if (WRMFJudge == 1 || WRMFJudge == 2) {
            weight = 1.0 + weightCoefficient * value;
            //        weight =  Math.log(1.0 + Math.pow(10, weightCoefficient) * value);

        } else {
            weight = 1.0;
        }
        return weight;
    }

    private void weightMatrix() {
        Double2DoubleOpenHashMap ratingWeightMap = new Double2DoubleOpenHashMap();
        for (double rating : ratingScale) {
            ratingWeightMap.putIfAbsent(rating, weight(rating));
        }

        for (MatrixEntry matrixEntry : trainMatrix) {
            matrixEntry.set(ratingWeightMap.get(matrixEntry.get()));
        }
    }

    @Override
    protected void trainModel() throws LibrecException {
        double[] usersPredictions = new double[numUsers];
        double[] itemsPredictions = new double[numItems];
        double weight;

        // Init item factors cache Sq
        DenseMatrix itemFactorsCache = new DenseMatrix(numFactors, numFactors);
        //Init user factors cache Sp
        DenseMatrix userFactorsCache;

        userFactors = new DenseMatrix(numUsers, numFactors);
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
                SequentialSparseVector itemVector = trainMatrix.row(userIdx);
                int itemIdx;
                for (Vector.VectorEntry vectorEntry : itemVector) {
                    itemIdx = vectorEntry.index();
                    itemsPredictions[itemIdx] = userFactors.row(userIdx).dot(itemFactors.row(itemIdx));
                }

                for (int factorCacheIdx = 0; factorCacheIdx < numFactors; factorCacheIdx++) {
                    double numer = 0, denom = regUser + itemFactorsCache.get(factorCacheIdx, factorCacheIdx);

                    for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                        if (factorCacheIdx != factorIdx) {
                            numer -= userFactors.get(userIdx, factorIdx) * itemFactorsCache.get(factorCacheIdx, factorIdx);
                        }
                    }

                    for (Vector.VectorEntry vectorEntry : itemVector) {
                        itemIdx = vectorEntry.index();
                        weight = vectorEntry.get();
                        itemsPredictions[itemIdx] -= userFactors.get(userIdx, factorCacheIdx) * itemFactors.get(itemIdx, factorCacheIdx);
                        numer += (weight - (weight - confidences[itemIdx]) * itemsPredictions[itemIdx])
                                * itemFactors.get(itemIdx, factorCacheIdx);
                        denom += (weight - confidences[itemIdx]) * itemFactors.get(itemIdx, factorCacheIdx)
                                * itemFactors.get(itemIdx, factorCacheIdx);
                    }

                    //update puf
                    userFactors.set(userIdx, factorCacheIdx, numer / denom);
                    for (Vector.VectorEntry vectorEntry : itemVector) {
                        itemIdx = vectorEntry.index();
                        itemsPredictions[itemIdx] += userFactors.get(userIdx, factorCacheIdx) * itemFactors.get(itemIdx, factorCacheIdx);
                    }
                }
            }
            // Update the Sp cache
            userFactorsCache = userFactors.transpose().times(userFactors);
            // Step 2: update item factors;
            for (int itemIdx = 0; itemIdx < numItems; itemIdx++) {
                SequentialSparseVector userVector = trainMatrix.viewColumn(itemIdx);
                int userIdx;
                for (Vector.VectorEntry vectorEntry : userVector) {
                    userIdx = vectorEntry.index();
                    usersPredictions[userIdx] = userFactors.row(userIdx).dot(itemFactors.row(itemIdx));
                }

                for (int factorCacheIdx = 0; factorCacheIdx < numFactors; factorCacheIdx++) {
                    double numer = 0, denom = confidences[itemIdx] * userFactorsCache.get(factorCacheIdx, factorCacheIdx) + regItem;

                    for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                        if (factorCacheIdx != factorIdx) {
                            numer -= itemFactors.get(itemIdx, factorIdx) * userFactorsCache.get(factorIdx, factorCacheIdx);
                        }
                    }
                    numer *= confidences[itemIdx];

                    for (Vector.VectorEntry vectorEntry : userVector) {
                        userIdx = vectorEntry.index();
                        weight = vectorEntry.get();
                        usersPredictions[userIdx] -= userFactors.get(userIdx, factorCacheIdx) * itemFactors.get(itemIdx, factorCacheIdx);
                        numer += (weight - (weight - confidences[itemIdx]) * usersPredictions[userIdx])
                                * userFactors.get(userIdx, factorCacheIdx);
                        denom += (weight - confidences[itemIdx]) * userFactors.get(userIdx, factorCacheIdx)
                                * userFactors.get(userIdx, factorCacheIdx);
                    }

                    //update qif
                    itemFactors.set(itemIdx, factorCacheIdx, numer / denom);
                    for (Vector.VectorEntry vectorEntry : userVector) {
                        userIdx = vectorEntry.index();
                        usersPredictions[userIdx] += userFactors.get(userIdx, factorCacheIdx) * itemFactors.get(itemIdx, factorCacheIdx);
                    }
                }
            }
            if (verbose) {
                LOG.info(getClass() + " runs at iteration = " + iter + " " + new Date());
            }
        }
    }
}
