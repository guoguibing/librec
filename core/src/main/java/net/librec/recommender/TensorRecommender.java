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
import net.librec.job.progress.ProgressBar;
import net.librec.math.structure.DataSet;
import net.librec.math.structure.SequentialAccessSparseMatrix;
import net.librec.math.structure.SparseTensor;
import net.librec.math.structure.TensorEntry;
import net.librec.recommender.item.KeyValue;
import net.librec.recommender.item.RecommendedList;

import java.util.ArrayList;
import java.util.List;

/**
 * Tensor Recommender
 *
 * @author WangYuFeng and Keqiang Wang
 */
public abstract class TensorRecommender extends AbstractRecommender {
    /**
     * train Tensor
     */
    protected SparseTensor trainTensor;

    /**
     * testTensor
     */
    protected SparseTensor testTensor;

    /**
     * validTensor
     */
    protected SparseTensor validTensor;

    /**
     * dimensions
     */
    protected int numDimensions;

    /**
     * dimensions indices
     */
    protected int[] dimensions;

    /**
     * number of factors
     */
    protected int numFactors;

    /**
     * learn rate, maximum learning rate
     */
    protected float learnRate, maxLearnRate;

    /**
     * the number of iterations
     */
    protected int numIterations;

    /**
     * user and item index of tensor
     */
    protected int userDimension, itemDimension;

    /**
     * regularization of user, item and all context
     */
    protected float reg;

    /**
     * the number of users
     */
    protected int numUsers;

    /**
     * the number of items
     */
    protected int numItems;

    /**
     * Maximum rate of rating scale
     */
    protected double maxRate = Double.MIN_NORMAL;

    /**
     * Minimum rate of rating scale
     */
    protected double minRate = Double.MAX_VALUE;

    /**
     * global mean of ratings
     */
    protected double globalMean;

    /**
     * report the training progress
     */
    protected ProgressBar progressBar;

    /**
     * trainMatrix
     */
    protected SequentialAccessSparseMatrix trainMatrix;

    /**
     * testMatrix
     */
    protected SequentialAccessSparseMatrix testMatrix;

    /**
     * setup
     *
     * @throws LibrecException if error occurs during setting up
     */
    protected void setup() throws LibrecException {
        super.setup();
        learnRate = conf.getFloat("rec.iterator.learnrate", 0.01f);
        maxLearnRate = conf.getFloat("rec.iterator.learnrate.maximum", 1000.0f);

        numFactors = conf.getInt("rec.factor.number", 10);
        reg = conf.getFloat("rec.tensor.regularization", 0.01f);
        numIterations = conf.getInt("rec.iterator.maximum", 100);

        trainTensor = (SparseTensor) getDataModel().getTrainDataSet();
        testTensor = (SparseTensor) getDataModel().getTestDataSet();
        validTensor = (SparseTensor) getDataModel().getValidDataSet();

        trainMatrix = trainTensor.rateMatrix();
        testMatrix = testTensor.rateMatrix();

        int size = 0;
        double sum = 0.0d;
        for (TensorEntry trainTensorEntry : trainTensor) {
            double rate = trainTensorEntry.get();
            maxRate = maxRate > rate ? maxRate : rate;
            minRate = minRate < rate ? minRate : rate;
            size++;
            sum += rate;
        }
        globalMean = sum / size;

        numDimensions = trainTensor.numDimensions();
        dimensions = trainTensor.dimensions();

        userDimension = trainTensor.getUserDimension();
        itemDimension = trainTensor.getItemDimension();

        numUsers = trainTensor.dimensions()[userDimension];
        numItems = trainTensor.dimensions()[itemDimension];

        if (verbose) {
            progressBar = new ProgressBar(100, 100);
        }



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

        /**
         * if you want to use and AUCEvaluator and nDCGEvaluator, please set rec.eval.auc.dropped.num arrays and rec.eval.key.test.max.num value like as AbstractRecommender.
         */
    }

    /**
     * train Model
     *
     * @throws LibrecException if error occurs during training
     */
    protected abstract void trainModel() throws LibrecException;


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
            List<KeyValue<Integer, Double>> itemValueList = new ArrayList<>();
            for (int itemIdx = 0, trainItemIndex = 0; itemIdx < numItems; ++itemIdx) {
                if (trainItemIndex < items.length && items[trainItemIndex] == itemIdx) {
                    trainItemIndex++;
                    continue;
                }

                double predictRating = 0;
                try {
                    predictRating = predict(userIdx, itemIdx);
                } catch (LibrecException e) {
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
        testTensor = (SparseTensor) predictDataSet;

        RecommendedList recommendedList = new RecommendedList(numUsers);
        for (int userIdx = 0; userIdx < numUsers; ++userIdx) {
            recommendedList.addList(new ArrayList<>());
        }
        for (TensorEntry testTensorEntry : testTensor) {
            int[] keys = testTensorEntry.keys();
            int userIdx = testTensorEntry.key(userDimension);
            int itemIdx = testTensorEntry.key(itemDimension);
            double predictRating = predict(keys, true);
            if (Double.isNaN(predictRating)) {
                predictRating = globalMean;
            }
            recommendedList.add(userIdx, itemIdx, predictRating);
        }
        return recommendedList;
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

        for (int userIdx = 0; userIdx < numUsers; ++userIdx) {
            recommendedList.addList(new ArrayList<>());
        }

        for (int contextIdx = 0; contextIdx < numDataEntries; ++contextIdx) {
            recommendedList.addList(new ArrayList<>());
            BaseContextRatingDataEntry baseContextRatingDataEntry = (BaseContextRatingDataEntry) dataList.getDataEntry(contextIdx);
            int userIdx = baseContextRatingDataEntry.getUserId();
            int[] itemIdsArray = baseContextRatingDataEntry.getItemIdsArray();
            int[][] contexts = baseContextRatingDataEntry.getContexts();

            for (int index = 0; index < itemIdsArray.length; index++) {
                int itemIdx = itemIdsArray[index];
                int[] keys = new int[contexts[index].length + 2];
                keys[0] = userIdx;
                keys[1] = itemIdx;
                System.arraycopy(contexts[index], 0, keys, 2, contexts[index].length);
                double predictRating = predict(keys, true);
                if (Double.isNaN(predictRating)) {
                    predictRating = globalMean;
                }
                recommendedList.add(contextIdx, itemIdx, predictRating);
            }
        }

        return recommendedList;
    }

//    /**
//     * get Recommended List
//     *
//     * @return Recommended List
//     */
//    public List<RecommendedItem> getRecommendedList(RecommendedList recommendedList) {
//        if (recommendedList != null && recommendedList.cardinality() > 0) {
//            RecommendedContextList<AbstractContext> contextList = this.getContextList();
//            List<RecommendedItem> userItemList = new ArrayList<>();
//            Iterator<ContextKeyValueEntry> recommendedEntryIter = recommendedList.iterator();
//            if (userMappingData != null && userMappingData.cardinality() > 0 && itemMappingData != null && itemMappingData.cardinality() > 0) {
//                BiMap<Integer, String> userMappingInverse = userMappingData.inverse();
//                BiMap<Integer, String> itemMappingInverse = itemMappingData.inverse();
//                while (recommendedEntryIter.hasNext()) {
//                    ContextKeyValueEntry contextKecyValueEntry = recommendedEntryIter.next();
//                    if (contextKecyValueEntry != null) {
//                        AbstractContext context = contextList.getContext(contextKecyValueEntry.getContextIdx());
//                        int userIdx = context.getUserIdx();
//                        String userId = userMappingInverse.get(userIdx);
//                        String itemId = itemMappingInverse.get(contextKecyValueEntry.getKey());
//                        if (StringUtils.isNotBlank(userId) && StringUtils.isNotBlank(itemId)) {
//                            userItemList.plus(new GenericRecommendedItem(userId, itemId, contextKecyValueEntry.getValue()));
//                        }
//                    }
//                }
//                return userItemList;
//            }
//        }
//        return null;
//    }


    /**
     * predict a specific rating for user userIdx on item itemIdx with some other contexts indices, note that the
     * prediction is not bounded. It is useful for building models with no need
     * to bound predictions.
     *
     * @param keys user index, item index and context indices
     * @return predictive rating for user userIdx on item itemIdx with some other contexts indices without bound
     * @throws LibrecException if error occurs during predicting
     */
    protected abstract double predict(int[] keys) throws LibrecException;


    protected double predict(int userIdx, int itemIdx) throws LibrecException {
        return 0.0;
    }


    /**
     * predict a specific rating for user userIdx on item itemIdx with some other contexts indices. Tt is useful for
     * evalution which requires predictions are bounded.
     *
     * @param keys  user index, item index and context indices
     * @param bound whether there is a bound
     * @return predictive rating for user userIdx on item itemIdx with some other contexts indices with bound
     * @throws LibrecException if error occurs during predicting
     */
    protected double predict(int[] keys, boolean bound) throws LibrecException {
        double predictRating = predict(keys);

        if (bound) {
            if (predictRating > maxRate) {
                predictRating = maxRate;
            } else if (predictRating < minRate) {
                predictRating = minRate;
            }
        }

        return predictRating;
    }

    /**
     * Post each iteration, we do things:
     * <ol>
     * <li>print debug information</li>
     * <li>check if converged</li>
     * <li>if not, adjust learning rate</li>
     * </ol>
     *
     * @param iter current iteration
     * @return boolean: true if it is converged; false otherwise
     * @throws LibrecException if error occurs
     */
    protected boolean isConverged(int iter) throws LibrecException {
        float delta_loss = (float) (lastLoss - loss);

        // print out debug info
        if (verbose) {
            String recName = getClass().getSimpleName();
            String info = recName + " iter " + iter + ": loss = " + loss + ", delta_loss = " + delta_loss;
            LOG.info(info);
        }

        if (Double.isNaN(loss) || Double.isInfinite(loss)) {
//            LOG.error("Loss = NaN or Infinity: current settings does not fit the recommender! Change the settings and try again!");
            throw new LibrecException("Loss = NaN or Infinity: current settings does not fit the recommender! Change the settings and try again!");
        }

        // check if converged

        return Math.abs(delta_loss) < 1e-5;
    }

    /**
     * Update current learning rate after each epoch <br>
     * <ol>
     * <li>bold driver: Gemulla et al., Large-scale matrix factorization with distributed stochastic gradient descent,
     * KDD 2011.</li>
     * <li>constant decay: Niu et al, Hogwild!: A lock-free approach to parallelizing stochastic gradient descent, NIPS
     * 2011.</li>
     * <li>Leon Bottou, Stochastic Gradient Descent Tricks</li>
     * <li>more ways to adapt learning rate can refer to: http://www.willamette.edu/~gorr/classes/cs449/momrate.html</li>
     * </ol>
     *
     * @param iter the current iteration
     */
    protected void updateLRate(int iter) {
        if (learnRate < 0.0) {
            return;
        }

        if (isBoldDriver && iter > 1) {
            learnRate = Math.abs(lastLoss) > Math.abs(loss) ? learnRate * 1.05f : learnRate * 0.5f;
        } else if (decay > 0 && decay < 1) {
            learnRate *= decay;
        }

        // limit to max-learn-rate after update
        if (maxLearnRate > 0 && learnRate > maxLearnRate) {
            learnRate = maxLearnRate;
        }
        lastLoss = loss;
    }
}
