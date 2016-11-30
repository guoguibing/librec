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

package net.librec.recommender.rec.baseline;

import net.librec.common.LibrecException;
import net.librec.math.structure.MatrixEntry;
import net.librec.recommender.AbstractRecommender;
import net.librec.recommender.item.RecommendedList;

import java.util.concurrent.ExecutionException;

/**
 * Baseline: predict by average rating of all users
 */
public class GlobalAverageRecommender extends AbstractRecommender {

    @Override
    protected void trainModel() throws LibrecException {

    }


    /**
     * the global average value as the predictive rating for user userIdx on item itemIdx.
     *
     * @param userIdx user index
     * @param itemIdx item index
     * @return predictive rating for user userIdx on item itemIdx
     * @throws LibrecException
     */
    @Override
    protected double predict(int userIdx, int itemIdx) {
        return globalMean;
    }

}
