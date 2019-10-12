package research.model.eval.ranking;


import it.unimi.dsi.fastutil.ints.IntCollection;
import research.model.eval.AbstractRecommenderEvaluator;
import research.model.recommend.item.KeyValue;
import research.model.recommend.item.RecommendedList;

import java.util.List;
import java.util.Set;

/**
 * PrecisionEvaluator, calculate precision@n
 * <a href=https://en.wikipedia.org/wiki/Precision_and_recall>wikipedia, Precision</a>
 *
 * @author Keqiang Wang
 */
public class PrecisionEvaluator extends AbstractRecommenderEvaluator<IntCollection> {
    /**
     * Evaluate on the test set with the the list of recommended items.
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

    public double evaluate(IntCollection itemIdSetInTest, List<KeyValue> recommendedList){
        int numHits = 0;
        int topK = this.topN <= recommendedList.size()? this.topN: recommendedList.size();
        for(int itemIndex = 0; itemIndex<topK; itemIndex++){
            if(itemIdSetInTest.contains((int) recommendedList.get(itemIndex).getKey())){
                numHits += 1;
            }
        }
        return itemIdSetInTest.size()>0 ? numHits / (this.topN + 0.0) : 0.0d;
    }
}
