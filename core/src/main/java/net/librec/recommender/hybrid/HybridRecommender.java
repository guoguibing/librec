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
package net.librec.recommender.hybrid;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import net.librec.common.LibrecException;
import net.librec.math.structure.SparseVector;
import net.librec.recommender.AbstractRecommender;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Zhou et al., <strong>Solving the apparent diversity-accuracy dilemma of recommender systems</strong>, Proceedings of
 * the National Academy of Sciences, 2010.
 *
 * @author guoguibing and Keqiang Wang
 */
public class HybridRecommender extends AbstractRecommender {
    Table<Integer, Integer, Double> userItemRanks = HashBasedTable.create();
    protected float lambda;

    Map<Integer, Integer> itemDegrees = new HashMap<>();

    /**
     * initialization
     *
     * @throws LibrecException if error occurs
     */
    @Override
    protected void setup() throws LibrecException {
        super.setup();

        lambda = conf.getFloat("rec.hybrid.lambda");

        for (int itemIdx = 0; itemIdx < numItems; itemIdx++) {
            itemDegrees.put(itemIdx, trainMatrix.columnSize(itemIdx));
        }
    }

    /**
     * train model
     *
     * @throws LibrecException if error occurs
     */
    @Override
    protected void trainModel() throws LibrecException {

    }


    @Override
    protected double predict(int userIdx, int itemIdx) throws LibrecException {
        // Note that in ranking, we first check a user u, and then check the
        // ranking score of each candidate items
        if (!userItemRanks.containsRow(userIdx)) {
            // new user
            userItemRanks.clear();

            SparseVector itemRatingsVector = trainMatrix.row(userIdx);
            Set<Integer> itemsSet = itemRatingsVector.getIndexSet();

            // distribute resources to users, including user u
            Map<Integer, Double> userResources = new HashMap<>();
            for (int tempUserIdx = 0; tempUserIdx < numUsers; tempUserIdx++) {
                SparseVector tempItemRatingsVector = trainMatrix.row(tempUserIdx);
                double sum = 0;
                int tempItemsCount = tempItemRatingsVector.getCount();
                for (int tempItemIdx : tempItemRatingsVector.getIndex()) {
                    if (itemsSet.contains(tempItemIdx))
                        sum += 1.0 / Math.pow(itemDegrees.get(tempItemIdx), lambda);
                }

                if (tempItemsCount > 0)
                    userResources.put(tempUserIdx, sum / tempItemsCount);
            }

            // redistribute resources to items
            for (int tempItemIdx = 0; tempItemIdx < numItems; tempItemIdx++) {
                if (itemsSet.contains(tempItemIdx))
                    continue;

                SparseVector tempUserRatingsVector = trainMatrix.column(tempItemIdx);
                double sum = 0;
                for (int tempUserIdx : tempUserRatingsVector.getIndex())
                    sum += userResources.containsKey(tempUserIdx) ? userResources.get(tempUserIdx) : 0.0;

                double score = sum / Math.pow(itemDegrees.get(tempItemIdx), 1 - lambda);
                userItemRanks.put(userIdx, tempItemIdx, score);
            }
        }

        return userItemRanks.contains(userIdx, itemIdx) ? userItemRanks.get(userIdx, itemIdx) : 0.0;
    }
}
