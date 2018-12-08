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
 * UserKNNRecommender
 * <p>
 * optimized by Keqiang Wang
 *
 * @author WangYuFeng and Keqiang Wang
 */
@ModelData({"isRanking", "knn", "userMeans", "trainMatrix", "similarityMatrix"})
public class UserKNNRecommender extends MatrixRecommender {
    private int knn;
    private DenseVector userMeans;
    private SymmMatrix similarityMatrix;
    private List<Entry<Integer, Double>>[] userSimilarityList;
    private List<Integer> userList;

    /**
     * (non-Javadoc)
     *
     * @see net.librec.recommender.AbstractRecommender#setup()
     */
    @Override
    protected void setup() throws LibrecException {
        super.setup();
        knn = conf.getInt("rec.neighbors.knn.number");
        similarityMatrix = context.getSimilarity().getSimilarityMatrix();
    }

    /**
     * (non-Javadoc)
     *
     * @see net.librec.recommender.AbstractRecommender#trainModel()
     */
    @Override
    protected void trainModel() throws LibrecException {
        userMeans = new VectorBasedDenseVector(numUsers);
        userList = new ArrayList<>(numUsers);
        for (int userIndex = 0; userIndex < numUsers; userIndex++) {
            userList.add(userIndex);
        }
        userList.parallelStream().forEach(userIndex -> {
            SequentialSparseVector userRatingVector = trainMatrix.row(userIndex);
            userMeans.set(userIndex, userRatingVector.getNumEntries() > 0 ? userRatingVector.mean() : globalMean);
        });

        createUserSimilarityList();
    }

    /**
     * (non-Javadoc)
     *
     * @see net.librec.recommender.MatrixRecommender#predict(int, int)
     */
    @Override
    public double predict(int userIdx, int itemIdx) throws LibrecException {
        // find a number of similar users
        List<Entry<Integer, Double>> simList = userSimilarityList[userIdx];
        double predictValue = 0.0D, simSum = 0.0D;

        Entry<Integer, Double> simUserEntry;
        double sim;
        SequentialSparseVector userRatingVector = trainMatrix.column(itemIdx);
        int userRatingPosition = 0, simUserPosition = 0;
        int userRatingSize = userRatingVector.getNumEntries(), simUserSize = simList.size();
        int simUserIndex, ratingUserIndex;

        while (simUserPosition < simUserSize && userRatingPosition < userRatingSize) {
            simUserEntry = simList.get(simUserPosition);
            ratingUserIndex = userRatingVector.getIndexAtPosition(userRatingPosition);
            simUserIndex = simUserEntry.getKey();
            if (simUserIndex == ratingUserIndex) {
                sim = simUserEntry.getValue();
                if (isRanking) {
                    predictValue += sim;
                } else if (sim > 0) {
                    predictValue += sim * (userRatingVector.getAtPosition(userRatingPosition)
                            - userMeans.get(simUserIndex));
                    simSum += sim;
                }
                simUserPosition++;
                userRatingPosition++;
            } else if (simUserIndex < ratingUserIndex) {
                simUserPosition++;
            } else {
                userRatingPosition++;
            }
        }

        if (isRanking) {
            return predictValue;
        } else {
            return predictValue > 0 ? userMeans.get(userIdx) + predictValue / simSum : globalMean;
        }
    }

    /**
     * Create userSimilarityList.
     */
    private void createUserSimilarityList() {
        userSimilarityList = new ArrayList[numUsers];
        SequentialAccessSparseMatrix simMatrix = similarityMatrix.toSparseMatrix();
        userList.parallelStream().forEach(userIndex -> {
            SequentialSparseVector similarityVector = simMatrix.row(userIndex);
            userSimilarityList[userIndex] = new ArrayList<>(similarityVector.size());
            for (Vector.VectorEntry simVectorEntry : similarityVector) {
                userSimilarityList[userIndex].add(new AbstractMap.SimpleImmutableEntry<>(simVectorEntry.index(), simVectorEntry.get()));
            }
            userSimilarityList[userIndex] = Lists.sortListTopK(userSimilarityList[userIndex], true, knn);
            Lists.sortListByKey(userSimilarityList[userIndex], false);
        });
    }
}
