package research.model.eval.ranking;



import it.unimi.dsi.fastutil.ints.IntCollection;
import research.model.eval.AbstractRecommenderEvaluator;
import research.model.recommend.item.KeyValue;
import research.model.recommend.item.RecommendedList;

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
 * @author Keqiang Wang
 */

public class HitRateEvaluator extends AbstractRecommenderEvaluator<IntCollection> {

    /**
     * Evaluate on the test set with the the list of recommended items.
     *
     * @param groundTruthList the given ground truth list
     * @param recommendedList the list of recommended items
     * @return evaluate result
     */
    public double evaluate(RecommendedList groundTruthList, RecommendedList recommendedList) {

        if (groundTruthList.size() == 0) {
            return 0.0;
        }

        int totalHits = 0;
        int numContext = groundTruthList.size();
        int nonZeroContext = 0;
        for (int contextIdx = 0; contextIdx < numContext; ++contextIdx) {
            List<KeyValue<Integer, Double>> testListByContext = groundTruthList.getKeyValueListByContext(contextIdx);
            if (testListByContext.size() == 1) {
                int keyTest = testListByContext.get(0).getKey();
                List<KeyValue<Integer, Double>> recommendListByContext = recommendedList.getKeyValueListByContext(contextIdx);
                int topK = this.topN <= recommendListByContext.size() ? this.topN : recommendListByContext.size();
                for (int indexOfKey = 0; indexOfKey < topK; ++indexOfKey) {
                    int keyRec = recommendListByContext.get(indexOfKey).getKey();
                    if (keyRec == keyTest) {
                        totalHits++;
                        break;
                    }
                }
                ++nonZeroContext;
            } else if (testListByContext.size() > 1) {
                throw new IndexOutOfBoundsException("It is not a leave-one-out validation method! Please use leave-one-out validation method");
            }
        }

        return nonZeroContext > 0 ? 1.0 * totalHits / nonZeroContext : 0.0d;
    }



    public double evaluate(IntCollection itemIdSetInTest, List<KeyValue> recommendedList){
        return 0.0f;
    }

}
