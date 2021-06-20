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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Value Unfairness Evaluator is based on the method proposed by
 * Sirui Yao and Bert Huang, <strong> Beyond Parity: Fairness Objective for Collaborative Filtering</strong>, NIPS 2017 <br>
 *
 * This metric measures the signed estimation error across the user types
 * consumer-side fairness
 *
 * Value unfairness occurs when one class of users is consistently given higher or lower predictions than their true preferences.
 * larger values shows that estimations for one class is consistently over-estimated and
 * the estimations for the other class is consistently under-estimated.
 *
 * @author Nasim Sonboli
 */

public class ValueUnfairnessEvaluator extends AbstractRecommenderEvaluator {

    /**
     * item feature matrix - indicating an item is associated to a certain feature or not
     */
    protected SequentialAccessSparseMatrix userFeatureMatrix;
    protected double valueUnfairness;

    /**
     * Evaluate on the test set with the the list of recommended items.
     *
     * @param groundTruthList
     *            the given test set
     * @param recommendedList
     *            the list of recommended items
     * @return evaluate result
     */

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

        int numItems = getDataModel().getItemMappingData().size();

        int numUsers = userFeatureMatrix.rowSize();
        int numFeatures = userFeatureMatrix.columnSize();
//        int numItems = groundTruthList.numColumns();

        // to avoid getting NaN for cases when no rating or predicted-rating is present, we set the counters to 1.
        List<Double> testItemRatingSumByProUsers = new ArrayList<>(Collections.nCopies(numItems,0.0));
        List<Double> testProUsersCounter = new ArrayList<>(Collections.nCopies(numItems,1.0));

        List<Double> testItemRatingSumByUnproUsers = new ArrayList<>(Collections.nCopies(numItems,0.0));
        List<Double> testUnproUsersCounter = new ArrayList<>(Collections.nCopies(numItems,1.0));

        List<Double> recItemRatingSumByProUsers = new ArrayList<>(Collections.nCopies(numItems,0.0));
        List<Double> recProUsersCounter = new ArrayList<>(Collections.nCopies(numItems,1.0));

        List<Double> recItemRatingSumByUnproUsers = new ArrayList<>(Collections.nCopies(numItems,0.0));
        List<Double> recUnproUsersCounter = new ArrayList<>(Collections.nCopies(numItems,1.0));


        for (int userID = 0; userID < numUsers; userID++) {
            // is the user from the protected group or not
            boolean isProtected = false;
            for (int featureId = 0; featureId < numFeatures; featureId++) {
                if (userFeatureMatrix.get(userID, featureId) == 1) {
                    if (featureId == featureIdMapping.get(protectedAttr)) {
                        isProtected = true;
                    } else {
                        isProtected = false;
                    }
                }
            }

//            Set<Integer> testSetByUser = testMatrix.getColumnsSet(userID);
            List<KeyValue<Integer, Double>> testListByUser = groundTruthList.getKeyValueListByContext(userID);
            if (testListByUser.size() > 0) {
                // iterate through the items in the testMatrix
                // we calculate this measure over all the items
                for (int indexOfItem=0; indexOfItem < testListByUser.size(); indexOfItem++) {
                    int itemId = testListByUser.get(indexOfItem).getKey();
                    double ratingActual = testListByUser.get(indexOfItem).getValue();

                    if (isProtected) {
                        testItemRatingSumByProUsers.set(itemId, testItemRatingSumByProUsers.get(itemId) + ratingActual);
                        testProUsersCounter.set(itemId, testProUsersCounter.get(itemId) + 1.0);
                    } else {
                        testItemRatingSumByUnproUsers.set(itemId, testItemRatingSumByUnproUsers.get(itemId) + ratingActual);
                        testUnproUsersCounter.set(itemId, testUnproUsersCounter.get(itemId) + 1.0);
                    }
                }

                List<KeyValue<Integer, Double>> recommendListByUser = recommendedList.getKeyValueListByContext(userID);
                int topK = this.topN <= recommendListByUser.size() ? this.topN : recommendListByUser.size(); // topK or all the recommended items?
                for (int indexOfItem=0; indexOfItem < topK; indexOfItem++) {

                    int itemId = recommendListByUser.get(indexOfItem).getKey();
                    double ratingPredicted = recommendListByUser.get(indexOfItem).getValue();

                    if (isProtected) {
                        recItemRatingSumByProUsers.set(itemId, recItemRatingSumByProUsers.get(itemId) + ratingPredicted);
                        recProUsersCounter.set(itemId, recProUsersCounter.get(itemId) + 1.0);
                    } else {
                        recItemRatingSumByUnproUsers.set(itemId, recItemRatingSumByUnproUsers.get(itemId) + ratingPredicted);
                        recUnproUsersCounter.set(itemId, recUnproUsersCounter.get(itemId) + 1.0);
                    }
                }
            }
        }

        valueUnfairness = 0.0;
        double avgRatingPro = 0.0, avgRecPro = 0.0, avgRatingUnpro = 0.0, avgRecUnpro = 0.0;
        // iterate through the items that has been recommended to each user
        for (int indexOfItem = 0; indexOfItem < numItems; indexOfItem++) {

            // some of the items are not rated at all by the protected group and some are not recommended to this group
            avgRecPro = recItemRatingSumByProUsers.get(indexOfItem) / recProUsersCounter.get(indexOfItem);
            avgRatingPro = testItemRatingSumByProUsers.get(indexOfItem) / testProUsersCounter.get(indexOfItem);

            avgRecUnpro = recItemRatingSumByUnproUsers.get(indexOfItem) / recUnproUsersCounter.get(indexOfItem);
            avgRatingUnpro = testItemRatingSumByUnproUsers.get(indexOfItem) / testUnproUsersCounter.get(indexOfItem);

            valueUnfairness += Math.abs((avgRecPro - avgRatingPro) - (avgRecUnpro - avgRatingUnpro));
        }

        // taking average over all the items
        valueUnfairness /= numItems;
        return valueUnfairness;
    }
}
