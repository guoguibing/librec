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

import net.librec.math.structure.SparseVector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * J. I. Marden, Analyzing and modeling rank data. Boca Raton, Florida: CRC Press, 1996.
 * Mingming Chen etc. A Ranking-oriented Hybrid Approach to QoS-aware Web Service Recommendation. 2015
 * <p>
 * Kendall Rank Correlation Coefficient
 *
 * @author zhanghaidong
 */
public class KRCCSimilarity extends AbstractRecommenderSimilarity {

    /**
     * Find the common rated items by this user and that user, or the common
     * users have rated this item or that item. And then return the similarity.
     *
     * @param thisVector:
     *            the rated items by this user, or users that have rated this
     *            item .
     * @param thatVector:
     *            the rated items by that user, or users that have rated that
     *            item.
     * @return similarity
     */
    public double getCorrelation(SparseVector thisVector, SparseVector thatVector) {

        if (thisVector == null || thatVector == null || thisVector.size() != thatVector.size()) {
            return Double.NaN;
        }
        // compute similarity
        List<Double> thisList = new ArrayList<Double>();
        List<Double> thatList = new ArrayList<Double>();

        for (Integer idx : thatVector.getIndex()) {
            thisList.add(thisVector.get(idx));
            thatList.add(thatVector.get(idx));
        }

        return getSimilarity(thisList, thatList);
    }

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
        if (thisList == null || thatList == null || thisList.size() < 2 || thatList.size() < 2) {
            return Double.NaN;
        }

        Set<Integer> commonIndices = new HashSet<Integer>();
        for (int i = 0; i < thisList.size(); i++) {
            if (thisList.get(i).doubleValue() > 0.0 && thatList.get(i).doubleValue() > 0.0) {
                commonIndices.add(i);
            }
        }

        int numCommonIndices = commonIndices.size();
        if (numCommonIndices < 2) {
            return Double.NaN;
        }

        List<Integer> commonIndexList = new ArrayList<Integer>(commonIndices);
        double sum = 0.0;
        for (int i = 0; i < commonIndexList.size(); i++) {
            for (int j = i + 1; j < commonIndexList.size(); j++) {
                double thisDiff = thisList.get(commonIndexList.get(i)).doubleValue() - thisList.get(commonIndexList.get(j)).doubleValue();
                double thatDiff = thatList.get(commonIndexList.get(i)).doubleValue() - thatList.get(commonIndexList.get(j)).doubleValue();
                if (thisDiff * thatDiff < 0.0) {
                    sum += 1.0;
                }
            }
        }

        return 1.0 - 4.0 * sum / (numCommonIndices * (numCommonIndices - 1));
    }
}
