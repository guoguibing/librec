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

import net.librec.math.algorithm.Maths;

import java.util.List;

/**
 * Pearson Correlation Coefficient (PCC)
 *
 * @author zhanghaidong
 */
public class PCCSimilarity extends AbstractRecommenderSimilarity {

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
        // compute similarity

        if (thisList == null || thatList == null || thisList.size() < 2 || thatList.size() < 2 || thisList.size() != thatList.size()) {
            return Double.NaN;
        }

        double thisMu = Maths.mean(thisList);
        double thatMu = Maths.mean(thatList);

        double num = 0.0, thisPow2 = 0.0, thatPow2 = 0.0;
        for (int i = 0; i < thisList.size(); i++) {
            double thisMinusMu = thisList.get(i).doubleValue() - thisMu;
            double thatMinusMu = thatList.get(i).doubleValue() - thatMu;

            num += thisMinusMu * thatMinusMu;
            thisPow2 += thisMinusMu * thisMinusMu;
            thatPow2 += thatMinusMu * thatMinusMu;
        }

        return num / (Math.sqrt(thisPow2) * Math.sqrt(thatPow2));
    }
}
