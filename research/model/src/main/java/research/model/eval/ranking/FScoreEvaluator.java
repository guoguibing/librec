package research.model.eval.ranking;

import it.unimi.dsi.fastutil.ints.IntCollection;
import research.model.eval.AbstractRecommenderEvaluator;
import research.model.recommend.item.KeyValue;
import research.model.recommend.item.RecommendedList;

import java.util.List;
import java.util.Set;

/**
 * F1 Score Evaluator
 * https://baijiahao.baidu.com/s?id=1626042068689014641&wfr=spider&for=pc
 * @author wanghuobin
 */

public class FScoreEvaluator extends AbstractRecommenderEvaluator<IntCollection> {

     private double beta;

     FScoreEvaluator(double beta){
         this.beta = beta;
    }

     FScoreEvaluator(){
     }

    /**
     * Evaluate on the test set with the the list of recommended items.
     *
     * @param groundTruthList the given ground truth list
     * @param recommendedList the list of recommended items
     * @return evaluate result
     */
    @Override
    public double evaluate(RecommendedList groundTruthList, RecommendedList recommendedList){
        if(Double.isNaN(beta)){
            this.beta = 1.0;
        }
        double totalF1Score = 0.0;
        int nonZeroContext = 0;
        for(int index = 0; index < groundTruthList.size(); index++){
            Set<Integer> testSetByContext = groundTruthList.getKeySetByContext(index);
            if (testSetByContext.size() > 0) {
                List<KeyValue<Integer, Double>> recommendListByContext = recommendedList.getKeyValueListByContext(index);
                int numHits = 0;
                int topK = this.topN <= recommendListByContext.size() ? this.topN : recommendListByContext.size();
                for (int indexOfKey = 0; indexOfKey < topK; indexOfKey++) {
                    int key = recommendListByContext.get(indexOfKey).getKey();
                    if (testSetByContext.contains(key)) {
                        numHits++;
                    }
                }
                double  recall = numHits / (testSetByContext.size() + 0.0);
                double precision =  numHits / (this.topN + 0.0);
                totalF1Score += (beta * beta + 1) * (precision * recall) / (beta * beta *(precision + recall));
                nonZeroContext++;
            }
         }
        return nonZeroContext > 0 ? totalF1Score / nonZeroContext : 0.0d;
    }

    /**
     *
     *
     * @param itemIdSetInTest
     * @param recommendedList
     * @return
     */
    @Override
    public double evaluate(IntCollection itemIdSetInTest, List<KeyValue> recommendedList){
        if(Double.isNaN(beta)){
            this.beta = 1.0;
        }
        int numHits = 0;
        int topK = this.topN <= recommendedList.size() ? this.topN : recommendedList.size();
        for (int indexOfKey = 0; indexOfKey < topK; indexOfKey++) {
            if(itemIdSetInTest.contains(recommendedList.get(indexOfKey).getKey())){
                numHits += 1;
            }
        }
        double  recall = numHits / (itemIdSetInTest.size() + 0.0);
        double precision =  numHits / (this.topN + 0.0);
        return itemIdSetInTest.size() > 0 ? (beta * beta + 1) * (precision * recall) / (beta * beta *(precision + recall)): 0.0d;
    }






}
