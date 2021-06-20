package net.librec.eval.ranking;

import net.librec.eval.AbstractRecommenderEvaluator;
import net.librec.math.structure.SymmMatrix;
import net.librec.recommender.item.KeyValue;
import net.librec.recommender.item.RecommendedList;

import java.util.List;

/**
 * DiversityEvaluator, average dissimilarity of all pairs of items in the
 * recommended list at a specific cutoff position.
 * This extended version, rebuilds a similarity matrix which uses features instead of ratings.
 * Reference: Avoiding monotony:
 * improving the diversity of recommendation lists, ReSys, 2008
 *
 * @author Nasim Sonboli, Florencia Cabral extended the method implemented by Keqiang Wang
 */

public class DiversityByFeaturesEvaluator extends AbstractRecommenderEvaluator {

    /**
     * Evaluate on the test set with the the list of recommended items.
     *
     * @param groundTruthList
     *            the given test set
     * @param recommendedList
     *            the list of recommended items
     * @return evaluate result
     */
//    public double evaluate(SparseMatrix testMatrix, RecommendedList recommendedList) {
    public double evaluate(RecommendedList groundTruthList, RecommendedList recommendedList) {

        double totalDiversity = 0.0;
        int numUsers = groundTruthList.size();
        int nonZeroNumUsers = 0;

        if (similarities.containsKey("itemfeature")) {
            SymmMatrix itemSimilarity = similarities.get("itemfeature").getSimilarityMatrix();

            if (itemSimilarity.getData().size() == 0.0) {
                return 0.0d;
            }
            for (int userID = 0; userID < numUsers; userID++) {

//                List<ItemEntry<Integer, Double>> recommendArrayListByUser = recommendedList.getItemIdxListByUserIdx(userID);
                List<KeyValue<Integer, Double>> recommendArrayListByUser = recommendedList.getKeyValueListByContext(userID);

                if (recommendArrayListByUser.size() > 1) {
                    // calculate the sum of dissimilarities for each pair of items per user
                    double totalDisSimilarityPerUser = 0.0;
                    int topK = this.topN <= recommendArrayListByUser.size() ? this.topN : recommendArrayListByUser.size();
                    for (int i = 0; i < topK; i++) {
                        for (int j = 0; j < topK; j++) {
                            if (i == j) {
                                continue;
                            }
                            int item1 = recommendArrayListByUser.get(i).getKey();
                            int item2 = recommendArrayListByUser.get(j).getKey();
                            totalDisSimilarityPerUser += 1.0 - itemSimilarity.get(item1, item2);
                        }
                    }
                    totalDiversity += totalDisSimilarityPerUser * 2 / (topK * (topK - 1));
                    nonZeroNumUsers++;
                }
            }
        }
        return nonZeroNumUsers > 0 ? totalDiversity / nonZeroNumUsers : 0.0d;
    }
}

