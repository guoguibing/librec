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
package net.librec.eval.rating;

import net.librec.eval.AbstractRecommenderEvaluator;
import net.librec.recommender.item.ContextKeyValueEntry;
import net.librec.recommender.item.RecommendedList;

import java.util.Iterator;

/**
 * MPE Evaluator
 *
 * @author Keqiang Wang
 */
public class MPEEvaluator extends AbstractRecommenderEvaluator {

    @Override
    public double evaluate(RecommendedList groundTruthList, RecommendedList recommendedList) {
        if (groundTruthList.size() == 0) {
            return 0.0;
        }

        int mpeNum = 0;
        int testSize = 0;
        double mpe = conf.getDouble("rec.measure.mpe", 0.01d);

        Iterator<ContextKeyValueEntry> groundTruthIter = groundTruthList.iterator();
        Iterator<ContextKeyValueEntry> recommendedEntryIter = recommendedList.iterator();

        while (groundTruthIter.hasNext()) {

            if (recommendedEntryIter.hasNext()) {

                ContextKeyValueEntry groundEntry = groundTruthIter.next();
                ContextKeyValueEntry recommendedEntry = recommendedEntryIter.next();

                if (groundEntry.getContextIdx() == recommendedEntry.getContextIdx()
                        && groundEntry.getKey() == recommendedEntry.getKey()) {

                    double realRating = groundEntry.getValue();
                    double predictRating = recommendedEntry.getValue();

                    double error = Math.abs(realRating - predictRating);
                    if (error > mpe) {
                        mpeNum++;
                    }
                    testSize++;

                } else {
                    throw new IndexOutOfBoundsException("index of recommendedList does not equal testMatrix index");
                }

            } else {
                throw new IndexOutOfBoundsException("index cardinality of recommendedList does not equal testMatrix index cardinality");
            }
        }

        return testSize > 0 ? (mpeNum + 0.0) / testSize : 0.0d;
    }
}
