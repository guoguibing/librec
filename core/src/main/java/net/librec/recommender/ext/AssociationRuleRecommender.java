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
package net.librec.recommender.ext;

import com.google.common.cache.LoadingCache;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import net.librec.common.LibrecException;
import net.librec.math.structure.SparseVector;
import net.librec.math.structure.VectorEntry;
import net.librec.recommender.AbstractRecommender;

import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Choonho Kim and Juntae Kim, <strong>A Recommendation Algorithm Using Multi-Level Association Rules</strong>, WI 2003.
 * <p>
 * Simple Association Rule Recommender: we do not consider the item categories (or multi levels) used in the original
 * paper. Besides, we consider all association rules without ruling out weak ones (by setting high support and
 * confidence threshold).
 *
 * @author guoguibing and wangkeqiang
 */
public class AssociationRuleRecommender extends AbstractRecommender {

    /**
     * confidence matrix of association rules
     */
    private Table<Integer, Integer, Double> associationTable;

    /**
     * user-vector cache, item-vector cache
     */
    protected LoadingCache<Integer, SparseVector> userCache;

    /**
     * Guava cache configuration
     */
    protected static String cacheSpec;

    /**
     * setup
     *
     * @throws LibrecException if error occurs
     */
    @Override
    protected void setup() throws LibrecException {
        super.setup();
        cacheSpec = conf.get("guava.cache.spec", "maximumSize=200,expireAfterAccess=2m");

        associationTable = HashBasedTable.create(numItems, numItems);
        userCache = trainMatrix.rowCache(cacheSpec);
    }

    @Override
    protected void trainModel() throws LibrecException {
        // simple rule: X => Y, given that each user vector is regarded as a transaction
        for (int itemIdx = 0; itemIdx < numItems; itemIdx++) {
            // all transactions for item itemIdx
            SparseVector userRatingsVector = trainMatrix.column(itemIdx);
            int userCount = userRatingsVector.size();

            for (int assoItemIdx = 0; assoItemIdx < numItems; assoItemIdx++) {
                // compute confidence where containing item assoItemIdx among userRatingsVector
                int count = 0;
                for (VectorEntry vectorEntry : userRatingsVector) {
                    int userIdx = vectorEntry.index();
                    double assoRatings = trainMatrix.get(userIdx, assoItemIdx);
                    if (assoRatings > 0)
                        count++;
                }

                if (count > 0) {
                    double conf = (count + 0.0) / userCount;
                    associationTable.put(itemIdx, assoItemIdx, conf);
                }
            }
        }
    }

    /**
     * predict a specific rating for user userIdx on item itemIdx.
     *
     * @param userIdx user index
     * @param itemIdx item index
     * @return predictive rating for user userIdx on item itemIdx
     * @throws LibrecException if error occurs
     */
    @Override
    protected double predict(int userIdx, int itemIdx) throws LibrecException {
        SparseVector itemRatingsVector = null;
        try {
            itemRatingsVector = userCache.get(userIdx);
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        double predictRatings = 0;
        for (Map.Entry<Integer, Double> mapEntry : associationTable.column(itemIdx).entrySet()) {
            int assoItemIdx = mapEntry.getKey();
            double support = mapEntry.getValue();
            predictRatings += itemRatingsVector.get(assoItemIdx) * support;
        }

        return predictRatings;
    }
}
