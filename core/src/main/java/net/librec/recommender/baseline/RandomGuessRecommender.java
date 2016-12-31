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
import net.librec.math.algorithm.Randoms;
import net.librec.recommender.AbstractRecommender;

/**
 * Baseline: predict by a random value in (minRate, maxRate)
 */
public class RandomGuessRecommender extends AbstractRecommender {

    @Override
    protected void setup() throws LibrecException {
        super.setup();
    }


    @Override
    protected void trainModel() throws LibrecException {

    }

    /**
     * a random value as the predictive rating for user userIdx on item itemIdx.
     *
     * @param userIdx user index
     * @param itemIdx item index
     * @return predictive rating for user userIdx on item itemIdx
     * @throws LibrecException if error occurs during predicting
     */
    @Override
    protected double predict(int userIdx, int itemIdx) throws LibrecException {
        return Randoms.uniform(minRate, maxRate);
    }
}
