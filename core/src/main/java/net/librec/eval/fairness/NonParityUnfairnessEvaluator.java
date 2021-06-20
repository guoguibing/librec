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
package net.librec.eval.fairness;

import com.google.common.collect.BiMap;
import net.librec.eval.AbstractRecommenderEvaluator;
import net.librec.math.structure.SequentialAccessSparseMatrix;
import net.librec.recommender.item.KeyValue;
import net.librec.recommender.item.RecommendedList;
import org.apache.commons.lang.StringUtils;

import java.util.List;

/**
 * Non-parity Unfairness Evaluator is based on the method proposed by
 * T. Kamishima et. al, <strong> Fairness-aware learning through regularization approach</strong>, ICDMW 2011 <br>
 *
 * This metric measures the absolute difference between the overall average ratings of the protected and the unprotected group.
 * consumer-side fairness
 *
 * @author Nasim Sonboli
 */


public class NonParityUnfairnessEvaluator extends AbstractRecommenderEvaluator {

    /**
     * item feature matrix - indicating an item is associated to a certain feature or not
     */
    protected SequentialAccessSparseMatrix userFeatureMatrix;
    protected double nonParityUnfairness;

    /**
     * Evaluate on the test set with the the list of recommended items.
     *
     * @param groundTruthList
     *            the given test set
     * @param recommendedList
     *            the list of recommended items
     * @return evaluate result
     */

//    public double evaluate(SparseMatrix testMatrix, RecommendedList recommendedList) {
    public double evaluate(RecommendedList groundTruthList, RecommendedList recommendedList) {

        if (groundTruthList == null) {
            return 0.0;
        }

        //protected users
        String protectedAttr = "";
        if (conf != null && StringUtils.isNotBlank(conf.get("data.protected.feature"))) {
            protectedAttr = conf.get("data.protected.feature");
        }

        userFeatureMatrix = getDataModel().getFeatureAppender().getUserFeatures();
        BiMap<String, Integer> featureIdMapping = getDataModel().getFeatureAppender().getUserFeatureMap();

        int numUsers = userFeatureMatrix.rowSize();
        int numFeatures = userFeatureMatrix.columnSize();

        nonParityUnfairness = 0.0;
        int numUsersPro = 0, numUsersUnpro = 0;
        double predictedRatingPro = 0.0, predictedRatingUnpro = 0.0;

        for (int userID = 0; userID < numUsers; userID++) {

            // is the user from the protected group or not
            boolean protectedOrNot = false;
            for (int featureId = 0; featureId < numFeatures; featureId++) {
                if (userFeatureMatrix.get(userID, featureId) == 1) {
                    if (featureId == featureIdMapping.get(protectedAttr)) {
                        protectedOrNot = true;
                        numUsersPro++;
                    } else {
                        protectedOrNot = false;
                        numUsersUnpro++;
                    }
                }
            }

            double userAvgPredictedRating = 0.0;
//            List<ItemEntry<Integer, Double>> recommendListByUser = recommendedList.getItemIdxListByUserIdx(userID);
            List<KeyValue<Integer, Double>> recommendListByUser = recommendedList.getKeyValueListByContext(userID);

            int topK = this.topN <= recommendListByUser.size() ? this.topN : recommendListByUser.size(); // topK or all the recommended items?
            for (int indexOfItem = 0; indexOfItem < topK; indexOfItem++) {

                int itemId = recommendListByUser.get(indexOfItem).getKey();
                double ratingPredicted = recommendListByUser.get(indexOfItem).getValue();

                userAvgPredictedRating += ratingPredicted;
            }
            userAvgPredictedRating /= topK;

            if (protectedOrNot) {
                predictedRatingPro += userAvgPredictedRating;
            } else {
                predictedRatingUnpro += userAvgPredictedRating;
            }
        }

        predictedRatingPro /= numUsersPro;
        predictedRatingUnpro /= numUsersUnpro;

        nonParityUnfairness = Math.abs(predictedRatingPro - predictedRatingUnpro);
        return nonParityUnfairness;
    }
}
