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
package net.librec.recommender.cf;

import net.librec.annotation.ModelData;
import net.librec.common.LibrecException;
import net.librec.math.structure.*;
import net.librec.recommender.MatrixRecommender;
import net.librec.util.Lists;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

/**
 * ItemKNNRecommender
 * <p>
 * optimized by Keqiang Wang
 *
 * @author WangYuFeng and Keqiang Wang
 */
@ModelData({"isRanking", "knn", "itemMeans", "trainMatrix", "similarityMatrix"})
public class ItemKNNRecommender extends MatrixRecommender {
    private int knn;
    private DenseVector itemMeans;
    /**
     * the similarity matrix between items.
     */
    private SymmMatrix similarityMatrix;
    /**
     * Top similarity item list for each item
     */
    private List<Entry<Integer, Double>>[] itemSimilarityList;

    private List<Integer> itemList;

    /**
     * (non-Javadoc)
     *
     * @see net.librec.recommender.AbstractRecommender#setup()
     */
    @Override
    protected void setup() throws LibrecException {
        super.setup();
        knn = conf.getInt("rec.neighbors.knn.number", 50);
        similarityMatrix = context.getSimilarity().getSimilarityMatrix();
    }

    /**
     * (non-Javadoc)
     *
     * @see net.librec.recommender.AbstractRecommender#trainModel()
     */
    @Override
    protected void trainModel() throws LibrecException {
        itemMeans = new VectorBasedDenseVector(numItems);
        double globalMean = trainMatrix.mean();
        itemList = new ArrayList<>();
        for (int itemIndex = 0; itemIndex < numItems; itemIndex++) {
            itemList.add(itemIndex);
        }
        itemList.parallelStream().forEach(itemIndex -> {
            SequentialSparseVector itemRatingVector = trainMatrix.column(itemIndex);
            itemMeans.set(itemIndex, itemRatingVector.getNumEntries() > 0 ? itemRatingVector.mean() : globalMean);
        });

        createItemSimilarityList();
    }

    /**
     * (non-Javadoc)
     *
     * @see net.librec.recommender.MatrixRecommender#predict(int, int)
     */
    @Override
    public double predict(int userIdx, int itemIdx) throws LibrecException {
        List<Entry<Integer, Double>> simList = itemSimilarityList[itemIdx];
        SequentialSparseVector itemRatingVector = trainMatrix.row(userIdx);
        int itemRatingSize = itemRatingVector.getNumEntries(), simItemSize = simList.size();
        if (itemRatingSize == 0 || simItemSize == 0) {
            return isRanking ? 0D : globalMean;
        }

        double predictValue = 0.0D, simSum = 0.0D;

        Entry<Integer, Double> simUserEntry;
        double sim;
        int itemRatingPosition = 0, simItemPosition = 0;
        int simItemIndex, ratingItemIndex;

        while (simItemPosition < simItemSize && itemRatingPosition < itemRatingSize) {
            simUserEntry = simList.get(simItemPosition);
            ratingItemIndex = itemRatingVector.getIndexAtPosition(itemRatingPosition);
            simItemIndex = simUserEntry.getKey();
            if (simItemIndex == ratingItemIndex) {
                sim = simUserEntry.getValue();
                if (isRanking) {
                    predictValue += sim;
                } else if (sim > 0) {
                    predictValue += sim * (itemRatingVector.getAtPosition(itemRatingPosition)
                            - itemMeans.get(simItemIndex));
                    simSum += sim;
                }
                simItemPosition++;
                itemRatingPosition++;
            } else if (simItemIndex < ratingItemIndex) {
                simItemPosition++;
            } else {
                itemRatingPosition++;
            }
        }

        if (isRanking) {
            return predictValue;
        } else {
            return predictValue > 0 ? itemMeans.get(userIdx) + predictValue / simSum : globalMean;
        }
    }

    /**
     * Create itemSimilarityList.
     */
    public void createItemSimilarityList() {
        itemSimilarityList = new ArrayList[numItems];
        SequentialAccessSparseMatrix simMatrix = similarityMatrix.toSparseMatrix();
        itemList.parallelStream().forEach(itemIndex -> {
            SequentialSparseVector similarityVector = simMatrix.row(itemIndex);
            itemSimilarityList[itemIndex] = new ArrayList<>(similarityVector.size());
            for (Vector.VectorEntry simVectorEntry : similarityVector) {
                itemSimilarityList[itemIndex].add(new AbstractMap.SimpleImmutableEntry<>(simVectorEntry.index(), simVectorEntry.get()));
            }
            itemSimilarityList[itemIndex] = Lists.sortListTopK(itemSimilarityList[itemIndex], true, knn);
            Lists.sortListByKey(itemSimilarityList[itemIndex], false);

        });
    }
}
