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
package research.model.eval;


import research.model.conf.Configuration;
import research.model.math.structure.SymmMatrix;
import research.model.recommend.item.KeyValue;
import research.model.recommend.item.RecommendedList;

import java.util.List;
import java.util.Map;

/**
 * Abstract Recommender Evaluator
 *
 * @author WangYuFeng
 */
public abstract class AbstractRecommenderEvaluator<T>  implements RecommenderEvaluator<T> {

    /**
     * the number of  recommended items
     */
    protected int topN;
    /**
     * configuration of the evaluator
     */
    protected Configuration conf;
    /**
     * default similarityMatrix
     */
    protected SymmMatrix similarityMatrix;


    public double evaluate(EvalContext evalContext){
        conf = evalContext.getConf();

        if (evalContext.getSimilarityMatrix() != null){
            similarityMatrix = evalContext.getSimilarityMatrix();
        }
        return evaluate(evalContext.getGroundTruthList(), evalContext.getRecommendedList());
    }

    /**
     * Evaluate on the test set with the the list of recommended items.
     *
     * @param groundTruthList
     *            the given test set ground truth List
     * @param recommendedList
     *            the list of recommended items
     * @return evaluate result
     */
    public abstract double evaluate(RecommendedList groundTruthList, RecommendedList recommendedList);


    /**
     * E
     *
     * @param itemIdSetInTest
     * @param recommendedList
     * @return evaluate result
     */
    public abstract double evaluate(T itemIdSetInTest , List<KeyValue> recommendedList);



    /**
     * Evaluate independently on the test set with the the list of recommended items.
     *
     * @param conf
     *            the configuration fo the evaluator
     * @param groundTruthList
     *            the given test set ground truth List
     * @param recommendedList
     *            the list of recommended items
     * @return evaluate result
     */
    public double evaluateIndependently(Configuration conf, RecommendedList groundTruthList, RecommendedList recommendedList){
        this.conf = conf;
        this.topN = conf != null ? conf.getInt("rec.recommender.ranking.topn", -1): -1;
        RecommendedList[] lists = groundTruthList.joinTransform(recommendedList, topN);
        return evaluate(lists[0], lists[1]);
    }

    /**
     * Set the number of recommended items.
     *
     * @param topN the number of  recommended items
     */
    @Override
    public void setTopN(int topN) {
        this.topN = topN;
    }

    /**
     * Return the configuration fo the evaluator.
     *
     * @return the configuration fo the evaluator
     */
    public Configuration getConf() {
        return conf;
    }

}
