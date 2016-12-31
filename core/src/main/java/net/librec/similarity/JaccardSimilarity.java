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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Jaccard Similarity
 *
 * @author zhanghaidong
 */
public class JaccardSimilarity extends AbstractRecommenderSimilarity {

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
        // compute similarity
        Set<Integer> elements = new HashSet<Integer>();
        elements.addAll(thisVector.getIndexList());
        elements.addAll(thatVector.getIndexList());

        int numAllElements = elements.size();
        int numCommonElements = thisVector.size() + thatVector.size() - numAllElements;

        return (numCommonElements + 0.0) / numAllElements;
    }

    protected double getSimilarity(List<? extends Number> thisList, List<? extends Number> thatList) {
        return 0.0;
    }
}
