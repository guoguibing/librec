package net.librec.eval.ranking;

import net.librec.eval.AbstractRecommenderEvaluator;
import net.librec.math.structure.SparseMatrix;
import net.librec.recommender.item.ItemEntry;
import net.librec.recommender.item.RecommendedList;

import java.util.List;

/**
 * HitRateEvaluator
 * <p>
 * Xia Ning and George Karypis, <strong>SLIM: Sparse Linear Methods for Top-N Recommender Systems</strong>, ICDM 2011. <br>
 * <p>
 * They apply a leave-one-out validation method to evaluate the algorithm performance. In each run, each of the datasets
 * is split into a training set and a testing set by randomly selecting one of the non-zero entries of each user and
 * placing it into the testing set.
 *
 * @author WangYuFeng and Keqiang Wang
 */

public class AverageReciprocalHitRankEvaluator extends AbstractRecommenderEvaluator {

    /**
     * Evaluate on the test set with the the list of recommended items.
     *
     * @param testMatrix
     *            the given test set
     * @param recommendedList
     *            the list of recommended items
     * @return evaluate result
     */
    public double evaluate(SparseMatrix testMatrix, RecommendedList recommendedList) {
        double reciprocalRank = 0.0;
        int numUsers = testMatrix.numRows();
        int nonZeroNumUsers = 0;
        for (int userID = 0; userID < numUsers; userID++) {
            List<Integer> testListByUser = testMatrix.getColumns(userID);
            if (testListByUser.size() > 0) {

                List<ItemEntry<Integer, Double>> recommendListByUser = recommendedList.getItemIdxListByUserIdx(userID);
                int trueItemIdx = testListByUser.get(0);
                int topK = this.topN <= recommendListByUser.size() ? this.topN : recommendListByUser.size();
                for (int indexOfItem = 0; indexOfItem < topK; indexOfItem++) {
                    if (recommendListByUser.get(indexOfItem).getKey() == trueItemIdx) {
                        reciprocalRank += 1.0 / (indexOfItem + 1.0);
                        break;
                    }
                }
                nonZeroNumUsers++;
            }
        }

        return nonZeroNumUsers > 0 ? reciprocalRank / nonZeroNumUsers : 0.0d;
    }
}
