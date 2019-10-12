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


import research.model.recommend.item.KeyValue;
import research.model.recommend.item.RecommendedList;

import java.util.List;

/**
 * <p>
 * Implementations of this interface evaluate the quality of a
 * {@link }'s recommendations.
 * </p>
 *
 * @author WangYuFeng
 */
public interface RecommenderEvaluator<T> {

    /**
     * Evaluate on the recommender context with the recommended list.
     *
     * @param recommendedList  RecommendedList object
     * @return  evaluate result
     */
    double evaluate(RecommendedList groundTruthList, RecommendedList recommendedList);

    /**
     * Evaluate on the evaluate context
     * @param evalContext evaluate context, including conf, groundTruthList, recommendedList, and other things used
     *                    in the evaluation
     * @return evaluate result
     */
    double evaluate(EvalContext evalContext);


    /**
     * Evaluate on the evaluate c
     *
     * @param itemIdSetInTest
     * @param recommendedList
     * @return
     */
    double evaluate(T itemIdSetInTest , List<KeyValue> recommendedList);


    /**
     * Set the number of recommended items.
     *
     * @param topN the number of recommended items
     */
    void setTopN(int topN);




}
