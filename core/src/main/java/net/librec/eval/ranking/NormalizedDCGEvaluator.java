/**
 * Copyright (C) 2016 LibRec
 * <p>
 * This file is part of LibRec.
 * LibRec is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * LibRec is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with LibRec. If not, see <http://www.gnu.org/licenses/>.
 */
package net.librec.eval.ranking;

import net.librec.eval.AbstractRecommenderEvaluator;
import net.librec.math.algorithm.Maths;
import net.librec.recommender.item.KeyValue;
import net.librec.recommender.item.RecommendedList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * NormalizedDCGEvaluator @topN
 * <a href=https://en.wikipedia.org/wiki/Discounted_cumulative_gain>wikipedia, ideal dcg</a>
 *
 * @author Shilin Qu
 */
public class NormalizedDCGEvaluator extends AbstractRecommenderEvaluator {

    /**
     * Evaluate on the test set with the the list of recommended items.
     *
     * @param groundTruthList the given ground truth list
     * @param recommendedList the list of recommended items
     * @return evaluate result
     */
    public double evaluate(RecommendedList groundTruthList, RecommendedList recommendedList) {

        double nDCG = 0.0;
        int numContext = groundTruthList.size();
        int nonZeroContext = 0;
        for (int contextIdx = 0; contextIdx < numContext; ++contextIdx) {
            Set<Integer> testSetByContext = groundTruthList.getKeySetByContext(contextIdx);
            if (testSetByContext.size() > 0) {

                List<KeyValue<Integer, Double>> groundTruthTestSetByContext = groundTruthList.getKeyValueListByContext(contextIdx);
                List<KeyValue<Integer, Double>> recommendListByContext = recommendedList.getKeyValueListByContext(contextIdx);
                boolean hasdcgsValue = false;

                List<RankRate> groundTruthTestSet = new ArrayList<>();
                for(int i=0; i<groundTruthTestSetByContext.size(); i++) {
                    groundTruthTestSet.add(new RankRate(groundTruthTestSetByContext.get(i).getKey(), groundTruthTestSetByContext.get(i).getValue()));
                }
                // calculate DCG
                double dcg = 0.0;
                int topK = this.topN <= recommendListByContext.size() ? this.topN : recommendListByContext.size();
                for (int indexOfKey = 0; indexOfKey < topK; ++indexOfKey) {
                    int itemID = recommendListByContext.get(indexOfKey).getKey();
                    if (!testSetByContext.contains(itemID)) {
                        continue;
                    }
                    double rankvalue = getValueByKey(groundTruthTestSet, itemID);
                    hasdcgsValue = true;
                    dcg += rankvalue / Maths.log(indexOfKey + 2, 2);
                }

                if(!hasdcgsValue||dcg == 0) {
                    ++nonZeroContext;
                    continue;
                }

                // calculate iDCG
                double idcg = 0.0d;
                ArrayList idcgsValue = new ArrayList();
                for(int i=0; i<groundTruthTestSet.size(); i++){
                    idcgsValue.add(groundTruthTestSet.get(i).getValue());
                }

                Collections.sort(idcgsValue, Collections.reverseOrder());

                int validIdxNum = topK < idcgsValue.size() ? topK : idcgsValue.size();
                for(int i=0; i<validIdxNum; i++) {
                    idcg += (double)idcgsValue.get(i) / Maths.log(i + 2, 2);
                }

                if(idcg==0){
                    ++nonZeroContext;
                    continue;
                }
                nDCG += dcg / idcg;
                ++nonZeroContext;
            }
        }

        return nonZeroContext > 0 ? nDCG / nonZeroContext : 0.0d;
    }

    public double getValueByKey(List<RankRate> list,int key){
        for(RankRate keyValue: list ){
            if(key==keyValue.getIndexId()){
                return keyValue.getValue();
            }
        }
        return 0.0d;
    }

    public class RankRate{
        int indexId;
        double value;
        public int getIndexId() {
            return indexId;
        }
        public void setIndexId(int indexId) {
            this.indexId = indexId;
        }
        public double getValue() {
            return value;
        }
        public void setValue(double value) {
            this.value = value;
        }
        public RankRate(int indexId,double value){
            setIndexId(indexId);
            setValue(value);
        }
    }

}
