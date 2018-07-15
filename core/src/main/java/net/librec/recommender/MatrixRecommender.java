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
import net.librec.data.structure.*;
import net.librec.math.structure.DataSet;
import net.librec.math.structure.MatrixEntry;
import net.librec.math.structure.SequentialAccessSparseMatrix;
import net.librec.recommender.item.KeyValue;
import net.librec.recommender.item.RecommendedList;

import java.util.*;

/**
 * Matrix Recommender
 *
 * @author Keqiang Wang (sei.wkq2008@gmail.com)
 */
public abstract class MatrixRecommender extends AbstractRecommender {
    /**
     * trainMatrix
     */
    protected SequentialAccessSparseMatrix trainMatrix;

    /**
     * testMatrix
     */
    protected SequentialAccessSparseMatrix testMatrix;

    /**
     * validMatrix
     */
    protected SequentialAccessSparseMatrix validMatrix;

    /**
     * the number of users
     */
    protected int numUsers;

    /**
     * the number of items
     */
    protected int numItems;

    /**
     * the number of rates
     */
    protected int numRates;

    /**
     * Maximum rate of rating times
     */
    protected double maxRate;

    /**
     * Minimum rate of rating times
     */
    protected double minRate;

    /**
     * a list of rating scales
     */
    protected static List<Double> ratingScale;


    /**
     * global mean of ratings
     */
    protected double globalMean;

    protected void setup() throws LibrecException{
        super.setup();
        trainMatrix = (SequentialAccessSparseMatrix) getDataModel().getTrainDataSet();
        testMatrix = (SequentialAccessSparseMatrix) getDataModel().getTestDataSet();
        validMatrix = (SequentialAccessSparseMatrix) getDataModel().getValidDataSet();

        numUsers = trainMatrix.rowSize();
        numItems = trainMatrix.columnSize();
        numRates = trainMatrix.size();
        Set<Double> ratingSet = new HashSet<>();
        for (MatrixEntry matrixEntry : trainMatrix) {
            ratingSet.add(matrixEntry.get());
        }
        ratingScale = new ArrayList<>(ratingSet);
        Collections.sort(ratingScale);
        maxRate = Collections.max(ratingScale);
        minRate = Collections.min(ratingScale);
        if (minRate == maxRate) {
            minRate = 0;
        }
        globalMean = trainMatrix.mean();

        int[] numDroppedItemsArray = new int[numUsers]; // for AUCEvaluator
        int maxNumTestItemsByUser = 0; //for idcg
        for (int userIdx = 0; userIdx < numUsers; ++userIdx) {
            numDroppedItemsArray[userIdx] = numItems - trainMatrix.row(userIdx).getNumEntries();
            int numTestItemsByUser = testMatrix.row(userIdx).getNumEntries();
            maxNumTestItemsByUser = maxNumTestItemsByUser < numTestItemsByUser ? numTestItemsByUser : maxNumTestItemsByUser;
        }

        int[] itemPurchasedCount = new int[numItems]; // for NoveltyEvaluator
        for (int itemIdx = 0; itemIdx < numItems; ++itemIdx) {
            itemPurchasedCount[itemIdx] = trainMatrix.column(itemIdx).getNumEntries()
                    + testMatrix.column(itemIdx).getNumEntries();
        }

        conf.setInts("rec.eval.auc.dropped.num", numDroppedItemsArray);
        conf.setInt("rec.eval.key.test.max.num", maxNumTestItemsByUser); //for nDCGEvaluator
        conf.setInt("rec.eval.item.num", testMatrix.columnSize()); // for EntropyEvaluator
        conf.setInts("rec.eval.item.purchase.num", itemPurchasedCount); // for NoveltyEvaluator
    }

    /**
     * recommend
     * * predict the ranking scores in the test data
     *
     * @return predictive rating matrix
     * @throws LibrecException if error occurs during recommending
     */
    public RecommendedList recommendRank() throws LibrecException {
        LibrecDataList<AbstractBaseDataEntry> librecDataList = new BaseDataList<>();
        for (int userIdx = 0; userIdx < numUsers; ++userIdx) {
            BaseRankingDataEntry baseRankingDataEntry = new BaseRankingDataEntry(userIdx);
            librecDataList.addDataEntry(baseRankingDataEntry);
        }
        return recommendRank(librecDataList);
    }

    /**
     * recommend
     * * predict the ranking scores in the test data
     *
     * @return predictive rating matrix
     * @throws LibrecException if error occurs during recommending
     */
    public RecommendedList recommendRank(LibrecDataList<AbstractBaseDataEntry> dataList) throws LibrecException {
        LOG.info("begin recommend");

        int numDataEntries = dataList.size();
        RecommendedList recommendedList = new RecommendedList(numUsers);
        List<Integer> contextList = new ArrayList<>();
        for (int contextIdx = 0; contextIdx < numDataEntries; ++contextIdx) {
            contextList.add(contextIdx);
            recommendedList.addList(new ArrayList<>());
        }

        contextList.parallelStream().forEach((Integer contextIdx) -> {
            BaseRankingDataEntry baseRankingDataEntry = (BaseRankingDataEntry) dataList.getDataEntry(contextIdx);
            int userIdx = baseRankingDataEntry.getUserId();

            int[] items = trainMatrix.row(userIdx).getIndices();
            List<KeyValue<Integer, Double>> itemValueList = new ArrayList<>(numItems);
            for (int itemIdx = 0, trainItemIndex = 0; itemIdx < numItems; ++itemIdx) {
                if (trainItemIndex < items.length && items[trainItemIndex] == itemIdx) {
                    trainItemIndex++;
                    continue;
                }

                double predictRating = 0;
                try {
                    predictRating = predict(userIdx, itemIdx);
                } catch (LibrecException e) {
                    System.out.println(userIdx + ", " + itemIdx + ": "+ predictRating);
                    e.printStackTrace();
                } catch (Exception e) {
                    System.out.println(userIdx + ", " + itemIdx + ": "+ predictRating);
                    e.printStackTrace();
                }
                if (Double.isNaN(predictRating)) {
                    continue;
                }
                itemValueList.add(new KeyValue<>(itemIdx, predictRating));
            }
            recommendedList.setList(contextIdx, itemValueList);
            recommendedList.topNRankByIndex(contextIdx, topN);
        });

        if (recommendedList.size() == 0) {
            throw new IndexOutOfBoundsException("No item is recommended, " +
                    "there is something error in the recommendation algorithm! Please check it!");
        }
        LOG.info("end recommend");
        return recommendedList;
    }


    /**
     * recommend
     * * predict the ratings in the test data
     *
     * @return predictive rating matrix
     * @throws LibrecException if error occurs during recommending
     */
    public RecommendedList recommendRating(DataSet predictDataSet) throws LibrecException {
        SequentialAccessSparseMatrix predictMatrix = (SequentialAccessSparseMatrix) predictDataSet;
        LibrecDataList<AbstractBaseDataEntry> librecDataList = new BaseDataList<>();
        for (int userIdx = 0; userIdx < numUsers; ++userIdx) {
            int[] itemIdsArray = predictMatrix.row(userIdx).getIndices();
            AbstractBaseDataEntry baseRatingDataEntry = new BaseRatingDataEntry(userIdx, itemIdsArray);
            librecDataList.addDataEntry(baseRatingDataEntry);
        }

        return this.recommendRating(librecDataList);
    }

    /**
     * recommend
     * * predict the ratings in the test data
     *
     * @return predictive rating matrix
     * @throws LibrecException if error occurs during recommending
     */
    public RecommendedList recommendRating(LibrecDataList<AbstractBaseDataEntry> dataList) throws LibrecException {
        int numDataEntries = dataList.size();
        RecommendedList recommendedList = new RecommendedList(numDataEntries);
        for (int contextIdx = 0; contextIdx < numDataEntries; ++contextIdx) {
            recommendedList.addList(new ArrayList<>());
            BaseRatingDataEntry baseRatingDataEntry = (BaseRatingDataEntry) dataList.getDataEntry(contextIdx);
            int userIdx = baseRatingDataEntry.getUserId();
            int[] itemIdsArray = baseRatingDataEntry.getItemIdsArray();
            for (int itemIdx : itemIdsArray) {
                double predictRating = predict(userIdx, itemIdx, true);
                if (Double.isNaN(predictRating)) {
                    predictRating = globalMean;
                }
                recommendedList.add(contextIdx, itemIdx, predictRating);
            }
        }

        return recommendedList;
    }

    /**
     * predict a specific rating for user userIdx on item itemIdx, note that the
     * prediction is not bounded. It is useful for building models with no need
     * to bound predictions.
     *
     * @param userIdx user index
     * @param itemIdx item index
     * @return predictive rating for user userIdx on item itemIdx without bound
     * @throws LibrecException if error occurs during predicting
     */
    protected abstract double predict(int userIdx, int itemIdx) throws LibrecException;

    /**
     * predict a specific rating for user userIdx on item itemIdx. It is useful for evalution which requires predictions are
     * bounded.
     *
     * @param userIdx user index
     * @param itemIdx item index
     * @param bound   whether there is a bound
     * @return predictive rating for user userIdx on item itemIdx with bound
     * @throws LibrecException if error occurs during predicting
     */
    protected double predict(int userIdx, int itemIdx, boolean bound) throws LibrecException {
        double predictRating = predict(userIdx, itemIdx);

        if (bound) {
            if (predictRating > maxRate) {
                predictRating = maxRate;
            } else if (predictRating < minRate) {
                predictRating = minRate;
            }
        }

        return predictRating;
    }
}
