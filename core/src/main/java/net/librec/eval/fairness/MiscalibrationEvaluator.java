package net.librec.eval.fairness;

import com.google.common.collect.BiMap;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;
import net.librec.eval.AbstractRecommenderEvaluator;
import net.librec.math.algorithm.Maths;
import net.librec.math.structure.SequentialAccessSparseMatrix;
import net.librec.recommender.item.KeyValue;
import net.librec.recommender.item.RecommendedList;

import java.util.*;

/**
 * CalibrationEvaluator
 *
 * <p>
 * Steck, Harald, <strong>"Calibrated recommendations."</strong>, Proceedings of the 12th ACM conference on recommender systems. ACM, 2018. <br>
 * <p>
 * This method is based on calculating KullbackLeiblerDivergence.
 *
 * Properties
 *      (a) it is zero in case of perfect calibration.
 *      (b) it is very sensative to small discrepancies between the two distributions.
 *      (c) it favors more uniform and less extreme distributions.
 *
 * The overall calibration metric is obtained by averaging over the metric over all users.
 *
 * @author Nasim Sonboli
 */

public class MiscalibrationEvaluator extends AbstractRecommenderEvaluator {

    /**
     * item feature matrix - indicating an item is associated to certain feature or not
     */
    protected SequentialAccessSparseMatrix itemFeatureMatrix;

    /**
     * @param interactedDist
     *      a probability distribution
     * @param recommendationDist
     *      a probability distribution
     *
     * Returns the KL divergence, K(p1 || p2), the lower the better.
     * The log is w.r.t. base 2. <p>
     *
     * KL-divergence is always non-negative.
     * It is not symmetric.
     *
     *
     * Calculates the KL divergence between the two distributions.
     * That is, it calculates KL(from || to).
     * In other words, how well can d1 be represented by d2.
     *
     * *Note*: If any value in <tt>p2</tt> is <tt>0.0</tt> then the KL-divergence
     * is <tt>infinite</tt>. Limin changes it to zero instead of infinite.
     *
     *
     * @return The KL divergence between the distributions
     */

    private double KullbackLeiblerDivergence(List<Double> interactedDist, List<Double> recommendationDist) {

        double alpha = 0.01; // not really a tuning parameter, it's there to make the computation more numerically stable.
        double klDiv = 0.0;

        for (int i = 0; i < interactedDist.size() ; ++i) {
            // By convention, 0 * ln(0/a) = 0, so we can ignore keys in q that aren't in p
            if (interactedDist.get(i) == 0.0) { continue; }
//            if (recommendationDist.get(i) == 0.0) { continue; } // Limin

            //if q = recommendationDist and p = interactedDist, q-hat is the adjusted q.
            // given that KL divergence diverges if recommendationDist or q is zero, we instead use q-hat = (1-alpha).q + alpha . p
            recommendationDist.set(i, ((1 - alpha) * recommendationDist.get(i) + alpha * interactedDist.get(i)));

            klDiv += interactedDist.get(i) * Maths.log( (interactedDist.get(i) / recommendationDist.get(i)) , 2); // express it in log base 2
        }

        return klDiv;
    }

    /**
     * given a list of items calculate the genre distribution for it.
     * @param itemList
     * @return the genre distribution.
     */
//    private double ComputeGenreDistribution(List<Integer> itemList) {
    private List<Double> ComputeGenreDistribution(Set<Integer> itemList) {

        itemFeatureMatrix = getDataModel().getFeatureAppender().getItemFeatures();
        BiMap<String, Integer> featureIdMapping = getDataModel().getFeatureAppender().getItemFeatureMap();

        int numItems = itemList.size();
        int numFeatures = itemFeatureMatrix.columnSize();

        List<Double> featureCount = new ArrayList<>(Collections.nCopies(numFeatures,0.0));

        //give me a list of items, i will look and see what genre they belong to and calculate the probability distribution.
        for (int itemId :itemList) {
            for (int featureId = 0; featureId < numFeatures; featureId ++) {
                if (itemFeatureMatrix.get(itemId, featureId) == 1) {
                    featureCount.set(featureId, featureCount.get(featureId) + 1);
                }
            }
        }

        // normalizing by the number of items in the list, so it turns into probabilities
        for (int featureId = 0; featureId < numFeatures; featureId ++) {
            featureCount.set(featureId, featureCount.get(featureId) / itemList.size());
        }

        return featureCount;
    }


    /**
     * Evaluate on the train set with the the list of recommended items.
     *
     * @param recommendedList
     *            the list of recommended items
     *            and the training set
     * @return evaluate result
     */

//    public double evaluate(SparseMatrix testMatrix, RecommendedList recommendedList) {
    public double evaluate(RecommendedList groundTruthList, RecommendedList recommendedList) {

//        int numUsers = testMatrix.numRows();
        int numUsers = groundTruthList.size();
        SequentialAccessSparseMatrix trainMatrix = dataModel.getDataSplitter().getTrainData();


        double klDivSum = 0.0;
        int nonZeroNumUsers = 0;
        for (int contextIdx = 0; contextIdx < numUsers; contextIdx++) {
            Set<Integer> testSetByContext = groundTruthList.getKeySetByContext(contextIdx);
//            int[] trainSetByContext = trainMatrix.row(contextIdx).getIndices(); // items from train
            Set<Integer> trainSetByContext = Sets.newHashSet(Ints.asList(trainMatrix.row(contextIdx).getIndices()));


            List<Double> p, q;
            if (testSetByContext.size() > 0) {
                List<KeyValue<Integer, Double>> recommendListByUser = recommendedList.getKeyValueListByContext(contextIdx);
                Set<Integer> itemSetByUser = new HashSet<>();

                int topK = this.topN <= recommendListByUser.size() ? this.topN : recommendListByUser.size();
                for (int indexOfItem = 0; indexOfItem < topK; indexOfItem++) {
                    int itemIdRecom = recommendListByUser.get(indexOfItem).getKey();
                    itemSetByUser.add(itemIdRecom);
                }

                p = ComputeGenreDistribution(itemSetByUser);
                q = ComputeGenreDistribution(trainSetByContext);
                // question: how do I turn them into inner ids? check if it's correct!

                //compute KL-Divergence
                double klDiv = KullbackLeiblerDivergence(p, q);
                klDivSum += klDiv;
                nonZeroNumUsers++;
            }
        }

        return nonZeroNumUsers > 0 ? klDivSum / nonZeroNumUsers : 0.0d;
    }


//        public double evaluate(RecommendedList groundTruthList, RecommendedList recommendedList) {
//
////        int numUsers = testMatrix.numRows();
//            int numUsers = groundTruthList.size();
//
//
//
//            double klDivSum = 0.0;
//            int nonZeroNumUsers = 0;
//            for (int userID = 0; userID < numUsers; userID++) {
////            Set<Integer> testSetByUser = testMatrix.getColumnsSet(userID);
//                Set<Integer> testSetByUser = groundTruthList.getKeySetByContext(userID);
//
//                List<Double> p, q;
//                if (testSetByUser.size() > 0) {
////                List<ItemEntry<Integer, Double>> recommendListByUser = recommendedList.getItemIdxListByUserIdx(userID);
//                    List<KeyValue<Integer, Double>> recommendListByUser = recommendedList.getKeyValueListByContext(userID);
//
//
////                List<Integer> itemSetByUser = new ArrayList<>();
//                    Set<Integer> itemSetByUser = new HashSet<>();
//
//                    int topK = this.topN <= recommendListByUser.size() ? this.topN : recommendListByUser.size();
//                    for (int indexOfItem = 0; indexOfItem < topK; indexOfItem++) {
//                        int itemIdRecom = recommendListByUser.get(indexOfItem).getKey();
//                        itemSetByUser.add(itemIdRecom);
//                    }
//
//                    p = ComputeGenreDistribution(itemSetByUser);
//                    q = ComputeGenreDistribution(testSetByUser);
//                    // question: how do I turn them into inner ids? check if it's correct!
//
//                    //compute KL-Divergence
//                    double klDiv = KullbackLeiblerDivergence(p, q);
//                    klDivSum += klDiv;
//                    nonZeroNumUsers++;
//                }
//            }
//
//            return nonZeroNumUsers > 0 ? klDivSum / nonZeroNumUsers : 0.0d;
//        }

}
