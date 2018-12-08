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

import com.clearspring.analytics.util.Lists;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import net.librec.common.LibrecException;
import net.librec.math.structure.*;
import net.librec.recommender.AbstractRecommender;
import net.librec.recommender.MatrixRecommender;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Choonho Kim and Juntae Kim, <strong>A Recommendation Algorithm Using Multi-Level Association Rules</strong>, WI 2003.
 * <p>
 * Simple Association Rule Recommender: we do not consider the item categories (or multi levels) used in the original
 * paper. Besides, we consider all association rules without ruling out weak ones (by setting high support and
 * confidence threshold).
 *
 * @author guoguibing and wangkeqiang
 */
public class AssociationRuleRecommender extends MatrixRecommender {

    /**
     * confidence matrix of association rules
     */
    private SequentialAccessSparseMatrix associations;

    private DenseMatrix userItemRates;

    // /**
    // * user-vector cache, item-vector cache
    // */
    // protected LoadingCache<Integer, SparseVector> userCache;

    // /**
    // * Guava cache configuration
    // */
    // protected static String cacheSpec;

    /**
     * setup
     *
     * @throws LibrecException if error occurs
     */
    @Override
    protected void setup() throws LibrecException {
        super.setup();
        // cacheSpec = conf.get("guava.cache.spec",
        // "maximumSize=200,expireAfterAccess=2m");

        userItemRates = new DenseMatrix(trainMatrix.rowSize(), trainMatrix.columnSize());
        for (MatrixEntry entry : trainMatrix) {
            userItemRates.set(entry.row(), entry.column(), entry.get());
        }
        // userCache = trainMatrix.rowCache(cacheSpec);
    }

    @Override
    protected void trainModel() throws LibrecException {
        // plus to adapt to 3.0
        List<Map<Integer, Integer>> userItemsPosList = Lists.newArrayList();
        for (int userIdx = 0; userIdx < numUsers; userIdx++) {
            Map<Integer, Integer> itemIndexPosMap = Maps.newHashMap();
            int[] itemIndices = trainMatrix.row(userIdx).getIndices();
            for (int i = 0; i < itemIndices.length; i++) {
                itemIndexPosMap.put(itemIndices[i], i);
            }
            userItemsPosList.add(itemIndexPosMap);
        }

        // simple rule: X => Y, given that each user vector is regarded as a
        // transaction
        Table<Integer, Integer, Double> associationTable = HashBasedTable.create(numItems, numItems);
        for (int itemIdx = 0; itemIdx < numItems; itemIdx++) {
            // all transactions for item itemIdx
            SequentialSparseVector userRatingsVector = trainMatrix.column(itemIdx);
            int userCount = userRatingsVector.size();

            for (int assoItemIdx = 0; assoItemIdx < numItems; assoItemIdx++) {
                // compute confidence where containing item assoItemIdx among
                // userRatingsVector
                int count = 0;
                for (Vector.VectorEntry vectorEntry : userRatingsVector) {
                    int userIdx = vectorEntry.index();
                    Map<Integer, Integer> currItemIndexPosMap = userItemsPosList.get(userIdx);
                    if (currItemIndexPosMap.get(assoItemIdx) != null) {
                        count++;
                    }
                }

                if (count > 0) {
                    double confidence = (count + 0.0) / userCount;
                    associationTable.put(itemIdx, assoItemIdx, confidence);
                }
            }
        }
        associations = new SequentialAccessSparseMatrix(numItems, numItems, associationTable);
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

        double predictRatings = 0;
        for (Vector.VectorEntry mapEntry : associations.column(itemIdx)) {
            int assoItemIdx = mapEntry.index();
            double support = mapEntry.get();
            double value = userItemRates.get(userIdx, assoItemIdx);
            predictRatings += value * support;
        }

        return predictRatings;
    }
}
