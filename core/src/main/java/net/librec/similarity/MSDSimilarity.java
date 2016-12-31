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
package net.librec.similarity;

import java.util.List;

/**
 * Calculate Mean Squared Difference (MSD) similarity proposed by Shardanand and Maes [1995]:
 * <i>Social information filtering: Algorithms for automating "word of mouth"</i>
 * <p>
 * Mean Squared Difference (MSD) Similarity
 *
 * @author zhanghaidong
 */
public class MSDSimilarity extends AbstractRecommenderSimilarity {

    /**
     * Calculate the similarity between thisList and thatList.
     *
     * @param thisList
     *            this list
     * @param thatList
     *            that list
     * @return similarity
     */
    protected double getSimilarity(List<? extends Number> thisList, List<? extends Number> thatList) {
        if (thisList == null || thatList == null || thisList.size() < 1 || thatList.size() < 1 || thisList.size() != thatList.size()) {
            return Double.NaN;
        }

        double sum = 0.0;

        for (int i = 0; i < thisList.size(); i++) {
            double thisValue = thisList.get(i).doubleValue();
            double thatValue = thatList.get(i).doubleValue();

            sum += Math.pow(thisValue - thatValue, 2);
        }

        double sim = thisList.size() / sum;
        if (Double.isInfinite(sim))
            sim = 1.0;

        return sim;
    }
}
