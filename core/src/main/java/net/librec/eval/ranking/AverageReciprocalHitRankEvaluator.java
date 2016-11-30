package net.librec.eval.ranking;

import net.librec.eval.AbstractRecommenderEvaluator;
import net.librec.math.structure.SparseMatrix;
import net.librec.recommender.item.ItemEntry;
import net.librec.recommender.item.RecommendedList;

import java.util.ArrayList;
import java.util.List;

/**
 * HitRateEvaluator
 *
 * Xia Ning and George Karypis, <strong>SLIM: Sparse Linear Methods for Top-N Recommender Systems</strong>, ICDM 2011. <br>
 *
 * They apply a leave-one-out validation method to evaluate the algorithm performance. In each run, each of the datasets
 * is split into a training set and a testing set by randomly selecting one of the non-zero entries of each user and
 * placing it into the testing set.
 *
 */

public class AverageReciprocalHitRankEvaluator extends AbstractRecommenderEvaluator {

    public double evaluate(SparseMatrix testMatrix, RecommendedList recommendedList) {

        if (testMatrix.size() == 0) {
            return 0.0;
        }

        double reciprocalRank = 0.0;
        int numUsers = testMatrix.numRows();
        for (int userID = 0; userID < numUsers; userID++) {
            List<Integer> testListByUser = testMatrix.getColumns(userID);
            List<ItemEntry<Integer, Double>> recommendArrayListByUser = recommendedList.getItemIdxListByUserIdx(userID);

            List<Integer> recommendListByUser = arrayListToList(recommendArrayListByUser);

            int trueItemId = testListByUser.get(0);
            int indexOfItem = recommendListByUser.indexOf(trueItemId);
            if (indexOfItem != -1) {
                reciprocalRank += 1 / (indexOfItem + 1.0);
            }

        }

        return reciprocalRank / numUsers;
    }

    private List<Integer> arrayListToList(List<ItemEntry<Integer, Double>> recommendArrayListByUser) {

        List<Integer> recommendListByUser = new ArrayList<>();

        for (ItemEntry<Integer, Double> item: recommendArrayListByUser) {
            recommendListByUser.add(item.getKey());
        }

        return recommendListByUser;
    }
}
