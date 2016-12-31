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

import net.librec.data.DataModel;
import net.librec.math.structure.MatrixEntry;
import net.librec.math.structure.SparseMatrix;

import java.util.List;

/**
 * Constrained Pearson Correlation (CPC)
 *
 * @author zhanghaidong
 */
public class CPCSimilarity extends AbstractRecommenderSimilarity {

    private double median;

    /**
     * Build social similarity matrix with trainMatrix in dataModel.
     *
     * @param dataModel
     *            the input data model
     */
    public void buildSimilarityMatrix(DataModel dataModel) {
        SparseMatrix trainMatrix = dataModel.getDataSplitter().getTrainData();
        double maximum = 0.0;
        double minimum = 100.0;
        for (MatrixEntry me : trainMatrix) {
            if (me.get() > maximum) {
                maximum = me.get();
            }
            if (me.get() < minimum) {
                minimum = me.get();
            }
        }
        median = (maximum + minimum) / 2;

        super.buildSimilarityMatrix(dataModel);
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
        // compute similarity
        if (thisList == null || thatList == null || thisList.size() < 1 || thatList.size() < 1 || thisList.size() != thatList.size()) {
            return Double.NaN;
        }

        double innerProduct = 0.0, thisPower2 = 0.0, thatPower2 = 0.0;
        for (int i = 0; i < thisList.size(); i++) {
            double thisDiff = thisList.get(i).doubleValue() - median;
            double thatDiff = thatList.get(i).doubleValue() - median;

            innerProduct += thisDiff * thatDiff;
            thisPower2 += thisDiff * thisDiff;
            thatPower2 += thatDiff * thatDiff;
        }
        return innerProduct / Math.sqrt(thisPower2 * thatPower2);
    }
}
