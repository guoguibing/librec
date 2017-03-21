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
package net.librec.recommender;

import net.librec.common.LibrecException;
import net.librec.data.convertor.appender.SocialDataAppender;
import net.librec.math.algorithm.Maths;
import net.librec.math.structure.SparseMatrix;

/**
 * Social Recommender
 *
 * @author Keqiang Wang
 */
public abstract class SocialRecommender extends MatrixFactorizationRecommender {
    /**
     * socialMatrix: social rate matrix, indicating a user is connecting to a number of other users
     */
    protected SparseMatrix socialMatrix;

    /**
     * social regularization
     */
    protected float regSocial;

    @Override
    public void setup() throws LibrecException {
        super.setup();
        regSocial = conf.getFloat("rec.social.regularization", 0.01f);
        // social path for the socialMatrix
        socialMatrix = ((SocialDataAppender) getDataModel().getDataAppender()).getUserAppender();
    }

    @Override
    protected double predict(int userIdx, int itemIdx, boolean bounded) throws LibrecException {
        double predictRating = predict(userIdx, itemIdx);

        if (bounded)
            return denormalize(Maths.logistic(predictRating));

        return predictRating;
    }

    /**
     * denormalize a prediction to the region (minRate, maxRate)
     *
     * @param predictRating a prediction to the region (minRate, maxRate)
     * @return  a denormalized prediction to the region (minRate, maxRate)
     */
    protected double denormalize(double predictRating) {
        return minRate + predictRating * (maxRate - minRate);
    }

    /**
     * normalize a rating to the region (0, 1)
     *
     * @param rating a given rating
     * @return  a normalized rating
     */
    protected double normalize(double rating) {
        return (rating - minRate) / (maxRate - minRate);
    }
}

