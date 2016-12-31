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

public class HitRateEvaluator extends AbstractRecommenderEvaluator {

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

        if (testMatrix.size() == 0) {
            return 0.0;
        }

        int totalHits = 0;
        int numUsers = testMatrix.numRows();
        int nonZeroNumUsers = 0;
        for (int userID = 0; userID < numUsers; userID++) {
            List<Integer> testListByUser = testMatrix.getColumns(userID);
            if (testListByUser.size() == 1) {
                int itemIdx = testListByUser.get(0);
                List<ItemEntry<Integer, Double>> recommendListByUser = recommendedList.getItemIdxListByUserIdx(userID);
                int topK = this.topN <= recommendListByUser.size() ? this.topN : recommendListByUser.size();
                for (int indexOfItem = 0; indexOfItem < topK; indexOfItem++) {
                    int recommendItemIdx = recommendListByUser.get(indexOfItem).getKey();
                    if (recommendItemIdx == itemIdx) {
                        totalHits++;
                        break;
                    }
                }

                nonZeroNumUsers++;
            } else if (testListByUser.size() > 1) {
                throw new IndexOutOfBoundsException("It is not a leave-one-out validation method! Please use leave-one-out validation method");
            }
        }

        return nonZeroNumUsers > 0 ? 1.0 * totalHits / nonZeroNumUsers : 0.0d;
    }
}
