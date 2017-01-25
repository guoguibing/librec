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

import java.util.List;

/**
 * Binary cosine similarity
 *
 * @author Ma Chen
 */
public class BinaryCosineSimilarity extends AbstractRecommenderSimilarity {
    /**
     * Get the binary cosine similarity of two sparse vectors.
     *
     * @param thisVector: the rated items by this user, or users that have rated this
     *                    item .
     * @param thatVector: the rated items by that user, or users that have rated that
     *                    item.
     * @return similarity
     */
    public double getCorrelation(SparseVector thisVector, SparseVector thatVector) {
        return thisVector.inner(thatVector) / (Math.sqrt(thisVector.inner(thisVector)) * Math.sqrt(thatVector.inner(thatVector)));
    }

    protected double getSimilarity(List<? extends Number> thisList, List<? extends Number> thatList) {
        return 0.0;
    }
}
