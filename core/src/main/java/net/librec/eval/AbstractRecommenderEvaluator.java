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
package net.librec.eval;

import net.librec.conf.Configuration;
import net.librec.data.DataModel;
import net.librec.math.structure.SymmMatrix;
import net.librec.recommender.RecommenderContext;
import net.librec.recommender.item.RecommendedList;
import net.librec.similarity.RecommenderSimilarity;

import java.util.Map;

/**
 * Abstract Recommender Evaluator
 *
 * @author WangYuFeng
 */
public abstract class AbstractRecommenderEvaluator implements RecommenderEvaluator {

    /**
     * data model for evaluator to use
     */
    protected DataModel dataModel;
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
    /**
     * all similarity maps
     */
    protected Map<String, RecommenderSimilarity> similarities;

    /**
     * Evaluate on the recommender context with the recommended list.
     *
     * @param context          the recommender context
     * @param recommendedList  the list of recommended items
     * @return  evaluate result
     */
    public double evaluate(RecommenderContext context, RecommendedList groundTruthList, RecommendedList recommendedList) {
        conf = context.getConf();
        String[] similarityKeys = conf.getStrings("rec.recommender.similarities");
        if (similarityKeys != null && similarityKeys.length > 0) {
            similarityMatrix = context.getSimilarity().getSimilarityMatrix();
            similarities = context.getSimilarities();
        }
        return evaluate(groundTruthList, recommendedList);
    }

    public double evaluate(EvalContext evalContext){
        conf = evalContext.getConf();

        if (evalContext.getSimilarityMatrix() != null){
            similarityMatrix = evalContext.getSimilarityMatrix();
        }

        if (evalContext.getSimilarities() != null){
            similarities = evalContext.getSimilarities();
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

    /**
     * Set the data model
     *
     * @param dataModel
     */
    public void setDataModel(DataModel dataModel) {this.dataModel = dataModel;}

    public DataModel getDataModel() {return dataModel;}

}
