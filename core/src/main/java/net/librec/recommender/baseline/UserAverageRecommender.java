// Copyright (C) 2014 Guibing Guo
//
// This file is part of LibRec.
//
// LibRec is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// LibRec is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with LibRec. If not, see <http://www.gnu.org/licenses/>.
//

package net.librec.recommender.baseline;

import net.librec.common.LibrecException;
import net.librec.math.structure.SparseVector;
import net.librec.recommender.AbstractRecommender;

import java.util.HashMap;
import java.util.Map;

/**
 * Baseline: predict by the average of target user's ratings
 */
public class UserAverageRecommender extends AbstractRecommender {
    /**
     * the user ratings average
     */
    private Map<Integer, Double> userMeans;

    @Override
    protected void setup() throws LibrecException {
        super.setup();
        userMeans = new HashMap<>();
    }

    @Override
    protected void trainModel() throws LibrecException {

    }

    /**
     * the user ratings average value as the predictive rating  for user userIdx on item itemIdx.
     *
     * @param userIdx user index
     * @param itemIdx item index
     * @return predictive rating for user userIdx on item itemIdx
     * @throws LibrecException if error occurs during predicting
     */
    @Override
    protected double predict(int userIdx, int itemIdx) throws LibrecException {
        if (!userMeans.containsKey(userIdx)) {
            SparseVector userRatingsVector = trainMatrix.row(userIdx);
            userMeans.put(userIdx, userRatingsVector.getCount() > 0 ? userRatingsVector.mean() : globalMean);
        }

        return userMeans.get(userIdx);
    }
}
