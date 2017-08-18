package net.librec.recommender.cf.rating;

import net.librec.common.LibrecException;
import net.librec.math.algorithm.KernelSmoothing;
import net.librec.math.algorithm.Randoms;
import net.librec.math.structure.DenseMatrix;
import net.librec.math.structure.DenseVector;
import net.librec.math.structure.MatrixEntry;
import net.librec.math.structure.SparseMatrix;
import net.librec.recommender.MatrixFactorizationRecommender;

import java.util.List;

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
public class LLORMARecommender extends MatrixFactorizationRecommender {
    private int globalNumFactors, localNumFactors;
    private int globalNumIterations, localNumIterations;
    private int numThreads;
    protected double globalRegUser, globalRegItem, localRegUser, localRegItem;
    private double globalLearnRate, localLearnRate;
    private SparseMatrix predictMatrix;
    private int numLocalModels;
    private DenseMatrix globalUserFactors, globalItemFactors;

    /*
     * (non-Javadoc)
     *
     * @see net.librec.recommender.AbstractRecommender#setup()
     */
    @Override
    protected void setup() throws LibrecException {
        super.setup();
        globalNumFactors = conf.getInt("rec.global.factors.num", 20);
        localNumFactors = numFactors;

        globalNumIterations = conf.getInt("rec.global.iteration.maximum", 100);
        localNumIterations = numIterations;

        globalRegUser = conf.getDouble("rec.global.user.regularization", 0.01);
        globalRegItem = conf.getDouble("rec.global.item.regularization", 0.01);
        localRegUser = regUser;
        localRegItem = regItem;

        globalLearnRate = conf.getDouble("rec.global.iteration.learnrate", 0.01);
        localLearnRate = conf.getDouble("rec.iteration.learnrate", 0.01);

        numThreads = conf.getInt("rec.thread.count", 4);
        numLocalModels = conf.getInt("rec.model.num", 50);

        numThreads = numThreads > numLocalModels ? numLocalModels : numThreads;

        //global svd P Q to calculate the kernel value between users (or items)
        globalUserFactors = new DenseMatrix(numUsers, globalNumFactors);
        globalItemFactors = new DenseMatrix(numItems, globalNumFactors);

        // initialize model
        globalUserFactors.init(initMean, initStd);
        globalItemFactors.init(initMean, initStd);
        this.buildGlobalModel();

        predictMatrix = new SparseMatrix(testMatrix);
    }


    //global svd P Q
    private void buildGlobalModel() {
        for (int globalIter = 1; globalIter <= globalNumIterations; globalIter++) {
            for (MatrixEntry matrixEntry : trainMatrix) {
                int userIdx = matrixEntry.row(); // user
                int itemIdx = matrixEntry.column(); // item
                double rating = matrixEntry.get();

                double predictRating = DenseMatrix.rowMult(globalUserFactors, userIdx, globalItemFactors, itemIdx);
                double error = rating - predictRating;

                // update factors
                for (int factorIdx = 0; factorIdx < globalNumFactors; factorIdx++) {
                    double puf = globalUserFactors.get(userIdx, factorIdx);
                    double qif = globalItemFactors.get(itemIdx, factorIdx);

                    globalUserFactors.add(userIdx, factorIdx, globalLearnRate * (error * qif - globalRegUser * puf));
                    globalItemFactors.add(itemIdx, factorIdx, globalLearnRate * (error * puf - globalRegItem * qif));
                }
            }
        }// end of training
    }

    @Override
    protected void trainModel() throws LibrecException {
        // Pre-calculating similarity:
        int completeModelCount = 0;

        LLORMAUpdater[] learners = new LLORMAUpdater[numThreads];
        int[] anchorArrayUser = new int[numLocalModels];
        int[] anchorArrayItem = new int[numLocalModels];

        int modelCount = 0;
        int[] runningThreadList = new int[numThreads];
        int runningThreadCount = 0;
        int waitingThreadPointer = 0;
        int nextRunningSlot = 0;

        SparseMatrix cumPredictionMatrix = new SparseMatrix(testMatrix);
        SparseMatrix cumWeightMatrix = new SparseMatrix(testMatrix);
        for (MatrixEntry matrixEntry : testMatrix) {
            int userIdx = matrixEntry.row();
            int itemIdx = matrixEntry.column();
            cumPredictionMatrix.set(userIdx, itemIdx, 0.0);
            cumWeightMatrix.set(userIdx, itemIdx, 0.0);
        }

        // Parallel training:
        while (completeModelCount < numLocalModels) {
            int anchorUser = Randoms.uniform(numUsers);
            List<Integer> itemList = trainMatrix.getColumns(anchorUser);

            if (itemList != null && itemList.size() > 0) {
                if (runningThreadCount < numThreads && modelCount < numLocalModels) {
                    // Selecting a new anchor point:
                    int itemListIdx = Randoms.uniform(itemList.size());
                    int anchorItem = itemList.get(itemListIdx);

                    anchorArrayUser[modelCount] = anchorUser;
                    anchorArrayItem[modelCount] = anchorItem;

                    // Preparing weight vectors:
                    DenseVector userWeights = kernelSmoothing(numUsers, anchorUser, KernelSmoothing.EPANECHNIKOV_KERNEL, 0.8, false);
                    DenseVector itemWeights = kernelSmoothing(numItems, anchorItem, KernelSmoothing.EPANECHNIKOV_KERNEL, 0.8, true);

                    // Starting a new local model learning:
                    learners[nextRunningSlot] = new LLORMAUpdater(modelCount, localNumFactors, numUsers, numItems, anchorUser,
                            anchorItem, localLearnRate, localRegUser, localRegItem, localNumIterations, userWeights, itemWeights, trainMatrix);
                    learners[nextRunningSlot].start();

                    runningThreadList[runningThreadCount] = modelCount;
                    runningThreadCount++;
                    modelCount++;
                    nextRunningSlot++;
                } else if (runningThreadCount > 0) {
                    // Joining a local model which was done with learning:
                    try {
                        learners[waitingThreadPointer].join();
                    } catch (InterruptedException ie) {
                        LOG.error("Join failed: " + ie);
                    }

                    int currentModelThreadIdx = waitingThreadPointer;
                    int currentModelAnchorIdx = completeModelCount;
                    completeModelCount++;

                    // Predicting with the new local model and all previous models:
                    predictMatrix = new SparseMatrix(testMatrix);
                    for (MatrixEntry matrixEntry : testMatrix) {
                        int userIdx = matrixEntry.row();
                        int itemIdx = matrixEntry.column();

                        double weight = KernelSmoothing.kernelize(getUserSimilarity(anchorArrayUser[currentModelAnchorIdx], userIdx),
                                0.8, KernelSmoothing.EPANECHNIKOV_KERNEL) * KernelSmoothing.kernelize(getItemSimilarity(anchorArrayItem[currentModelAnchorIdx],
                                itemIdx), 0.8, KernelSmoothing.EPANECHNIKOV_KERNEL);

                        double newPrediction = (learners[currentModelThreadIdx].getLocalUserFactors().row(userIdx, false)
                                .inner(learners[currentModelThreadIdx].getLocalItemFactors().row(itemIdx, false))) * weight;

                        cumWeightMatrix.set(userIdx, itemIdx, cumWeightMatrix.get(userIdx, itemIdx) + weight);
                        cumPredictionMatrix.set(userIdx, itemIdx, cumPredictionMatrix.get(userIdx, itemIdx) + newPrediction);

                        double prediction = cumPredictionMatrix.get(userIdx, itemIdx) / cumWeightMatrix.get(userIdx, itemIdx);

                        prediction = Double.isNaN(prediction) || prediction == 0.0 ? globalMean : prediction;
                        prediction = prediction < minRate ? minRate : prediction;
                        prediction = prediction > maxRate ? maxRate : prediction;

                        predictMatrix.set(userIdx, itemIdx, prediction);
                    }

                    nextRunningSlot = waitingThreadPointer;
                    waitingThreadPointer = (waitingThreadPointer + 1) % numThreads;
                    runningThreadCount--;
                }
            }
        }
    }

    /**
     * Calculate similarity between two users, based on the global base SVD.
     *
     * @param userIdx1 The first user's ID.
     * @param userIdx2 The second user's ID.
     * @return The similarity value between two users idx1 and idx2.
     */
    private double getUserSimilarity(int userIdx1, int userIdx2) {
        double sim;

        DenseVector userVector1 = globalUserFactors.row(userIdx1);
        DenseVector userVector2 = globalUserFactors.row(userIdx2);

        sim = 1 - 2.0 / Math.PI * Math.acos(userVector1.inner(userVector2) / (Math.sqrt(userVector1.inner(userVector1))
                * Math.sqrt(userVector2.inner(userVector2))));

        if (Double.isNaN(sim)) {
            sim = 0.0;
        }
        return sim;
    }

    /**
     * Calculate similarity between two items, based on the global base SVD.
     *
     * @param itemIdx1 The first item's ID.
     * @param itemIdx2 The second item's ID.
     * @return The similarity value between two items idx1 and idx2.
     */
    private double getItemSimilarity(int itemIdx1, int itemIdx2) {
        double sim;

        DenseVector itemVector1 = globalItemFactors.row(itemIdx1);
        DenseVector itemVector2 = globalItemFactors.row(itemIdx2);

        sim = 1 - 2.0 / Math.PI * Math.acos(itemVector1.inner(itemVector2) / (Math.sqrt(itemVector1.inner(itemVector1))
                * Math.sqrt(itemVector2.inner(itemVector2))));
        if (Double.isNaN(sim)) {
            sim = 0.0;
        }

        return sim;
    }

    /**
     * Given the similarity, it applies the given kernel.
     * This is done either for all users or for all items.
     *
     * @param size          The length of user or item vector.
     * @param anchorIdx     The identifier of anchor point.
     * @param kernelType    The type of kernel.
     * @param width         Kernel width.
     * @param isItemFeature return item kernel if yes, return user kernel otherwise.
     * @return The kernel-smoothed values for all users or all items.
     */
    private DenseVector kernelSmoothing(int size, int anchorIdx, int kernelType, double width, boolean isItemFeature) {
        DenseVector newFeatureVector = new DenseVector(size);
        newFeatureVector.set(anchorIdx, 1.0);

        for (int index = 0; index < size; index++) {
            double sim;
            if (isItemFeature) {
                sim = getItemSimilarity(index, anchorIdx);
            } else { // userFeature
                sim = getUserSimilarity(index, anchorIdx);
            }
            newFeatureVector.set(index, KernelSmoothing.kernelize(sim, width, kernelType));
        }
        return newFeatureVector;
    }

    @Override
    protected double predict(int userIdx, int itemIdx) {
        return predictMatrix.get(userIdx, itemIdx);
    }
}
