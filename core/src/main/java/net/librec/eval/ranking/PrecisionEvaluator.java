package net.librec.eval.ranking;

import net.librec.eval.AbstractRecommenderEvaluator;
import net.librec.recommender.item.KeyValue;
import net.librec.recommender.item.RecommendedList;

import java.util.List;
import java.util.Set;

/**
 * PrecisionEvaluator, calculate precision@n
 * <a href=https://en.wikipedia.org/wiki/Precision_and_recall>wikipedia, Precision</a>
 *
 * @author Keqiang Wang
 */
public class PrecisionEvaluator extends AbstractRecommenderEvaluator {
    /**
     * Evaluate on the test set with the list of recommended items.
     *
     * @param groundTruthList the given ground truth list
     * @param recommendedList the list of recommended items
     * @return evaluate result
     */
    public double evaluate(RecommendedList groundTruthList, RecommendedList recommendedList) {
        double totalPrecision = 0.0;
        int numContext = groundTruthList.size();
        int nonZeroContext = 0;
        for (int contextIdx = 0; contextIdx < numContext; ++contextIdx) {
            Set<Integer> testSetByContext = groundTruthList.getKeySetByContext(contextIdx);
            if (testSetByContext.size() > 0) {
                List<KeyValue<Integer, Double>> recommendListByContext = recommendedList.getKeyValueListByContext(contextIdx);

                int numHits = 0;
                int topK = this.topN <= recommendListByContext.size() ? this.topN : recommendListByContext.size();
                for (int indexOfKey = 0; indexOfKey < topK; ++indexOfKey) {
                    int key = recommendListByContext.get(indexOfKey).getKey();
                    if (testSetByContext.contains(key)) {
                        numHits++;
                    }
                }
                totalPrecision += numHits / (this.topN + 0.0);
                nonZeroContext++;
            }
        }
        return nonZeroContext > 0 ? totalPrecision / nonZeroContext : 0.0d;
    }
}
