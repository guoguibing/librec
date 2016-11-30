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

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.collect.BiMap;

import net.librec.common.LibrecException;
import net.librec.conf.Configuration;
import net.librec.data.DataModel;
import net.librec.eval.Measure;
import net.librec.eval.Measure.MeasureValue;
import net.librec.eval.RecommenderEvaluator;
import net.librec.math.structure.MatrixEntry;
import net.librec.math.structure.SparseMatrix;
import net.librec.recommender.item.GenericRecommendedItem;
import net.librec.recommender.item.RecommendedItem;
import net.librec.recommender.item.RecommendedItemList;
import net.librec.recommender.item.RecommendedList;
import net.librec.recommender.item.UserItemRatingEntry;
import net.librec.util.ModelDataUtil;
import net.librec.util.ReflectionUtil;

/**
 * Abstract Recommender Methods
 *
 * @author WangYuFeng and Wang Keqiang
 */
public abstract class AbstractRecommender implements Recommender {
	/**
	 * LOG
	 */
	protected final Log LOG = LogFactory.getLog(this.getClass());
    /**
     * is ranking or rating
     */
    protected boolean isRanking;
    /**
     * topN
     */
    protected int topN;
    /**
     * conf
     */
    protected Configuration conf;
    /**
     * RecommenderContext
     */
    protected RecommenderContext context;
    /**
     * trainMatrix
     */
    protected SparseMatrix trainMatrix;
    /**
     * testMatrix
     */
    protected SparseMatrix testMatrix;
    /**
     * validMatrix
     */
    protected SparseMatrix validMatrix;
    /**
     * Recommended Item List
     */
    protected RecommendedList recommendedList;
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
     * Maximum rate of rating scale
     */
    protected double maxRate;
    /**
     * Minimum rate of rating scale
     */
    protected double minRate;
    // a list of rating scales
    protected static List<Double> ratingScale;
    /**
     * user Mapping Data
     */
	public BiMap<String, Integer> userMappingData;
    /**
     * item Mapping Data
     */
	public BiMap<String, Integer> itemMappingData;

    /**
     * global mean of ratings
     */
    protected double globalMean;

    /**
     * setup
     *
     * @throws LibrecException
     * @throws FileNotFoundException 
     */
    protected void setup() throws LibrecException {
        conf = context.getConf();
        isRanking = conf.getBoolean("rec.recommender.category");
        if (isRanking) {
            topN = conf.getInt("rec.recommender.ranking.topn", 5);
        }
        trainMatrix = getDataModel().getDataSplitter().getTrainData();
        testMatrix = getDataModel().getDataSplitter().getTestData();
        validMatrix = getDataModel().getDataSplitter().getValidData();
        userMappingData = getDataModel().getUserMappingData();
        itemMappingData = getDataModel().getItemMappingData();
        
        numUsers = trainMatrix.numRows();
        numItems = trainMatrix.numColumns();
        numRates = trainMatrix.size();
        ratingScale = new ArrayList<>(trainMatrix.getValueSet());
        Collections.sort(ratingScale);
        maxRate = Collections.max(trainMatrix.getValueSet());
        minRate = Collections.min(trainMatrix.getValueSet());
        globalMean = trainMatrix.size() / numRates;
    }

    /**
     * train Model
     *
     * @throws LibrecException
     */
    protected abstract void trainModel() throws LibrecException;

    /**
     * recommend
     * * predict the ranking scores or ratings in the test data
     *
     * @return predictive ranking score or rating matrix
     * @throws LibrecException
     */
    protected RecommendedList recommend() throws LibrecException {
        if (isRanking && topN > 0) {
            recommendedList = recommendRank();
        } else {
            recommendedList = recommendRating();
        }
        return recommendedList;
    }

    /**
     * recommend
     * * predict the ranking scores in the test data
     *
     * @return predictive rating matrix
     * @throws LibrecException
     */
    protected RecommendedList recommendRank() throws LibrecException {
        recommendedList = new RecommendedItemList(numUsers - 1, numUsers);

        for (int userIdx = 0; userIdx < numUsers; ++userIdx) {
            Set<Integer> itemSet = trainMatrix.getColumnsSet(userIdx);
            for (int itemIdx = 0; itemIdx < numItems; ++itemIdx) {
                if (itemSet.contains(itemIdx)) {
                    continue;
                }
                double predictRating = predict(userIdx, itemIdx);
                if (Double.isNaN(predictRating)) {
                    continue;
                }
                recommendedList.addUserItemIdx(userIdx, itemIdx, predictRating);
            }
            recommendedList.topNRankItemsByUser(userIdx, topN);

        }
        return recommendedList;
    }

    /**
     * recommend
     * * predict the ratings in the test data
     *
     * @return predictive rating matrix
     * @throws LibrecException
     */
    protected RecommendedList recommendRating() throws LibrecException {
        recommendedList = new RecommendedItemList(numUsers - 1, numUsers);

        for (MatrixEntry matrixEntry : testMatrix) {
            int userIdx = matrixEntry.row();
            int itemIdx = matrixEntry.column();
            double predictRating = predict(userIdx, itemIdx, true);
            if (Double.isNaN(predictRating)) {
                predictRating = globalMean;
            }
            recommendedList.addUserItemIdx(userIdx, itemIdx, predictRating);
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
     * @throws LibrecException
     */
    protected abstract double predict(int userIdx, int itemIdx) throws LibrecException;


    /**
     * predict a specific rating for user userIdx on item itemIdx. It is useful for evalution which requires predictions are
     * bounded.
     *
     * @param userIdx user index
     * @param itemIdx item index
     * @return predictive rating for user userIdx on item itemIdx with bound
     * @throws LibrecException
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

    /**
     * recommend
     *
     * @param context
     * @throws LibrecException
     */
    public void recommend(RecommenderContext context) throws LibrecException {
        this.context = context;
        setup();
        LOG.info("Job Setup completed.");
        trainModel();
        LOG.info("Job Train completed.");
        this.recommendedList = recommend();
        LOG.info("Job End.");
        cleanup();
    }

    /**
     * evaluate
     *
     * @param evaluator
     */
    public double evaluate(RecommenderEvaluator evaluator) throws LibrecException {
        evaluator.setTopN(this.topN);
        return evaluator.evaluate(context, recommendedList);
    }

    /**
     * evaluate Map
     *
     * @return
     * @throws LibrecException
     */
    public Map<MeasureValue, Double> evaluateMap() throws LibrecException {
        Map<MeasureValue, Double> evaluatedMap = new HashMap<>();
        List<MeasureValue> measureValueList = Measure.getMeasureEnumList(isRanking, topN);
        if (measureValueList != null) {
            for (MeasureValue measureValue : measureValueList) {
                RecommenderEvaluator evaluator = ReflectionUtil
                        .newInstance(measureValue.getMeasure().getEvaluatorClass());
                if (isRanking && measureValue.getTopN() != null && measureValue.getTopN() > 0) {
                    evaluator.setTopN(measureValue.getTopN());
                }
                double evaluatedValue = evaluator.evaluate(context, recommendedList);
                evaluatedMap.put(measureValue, evaluatedValue);
            }
        }
        return evaluatedMap;
    }

    /**
     * cleanup
     *
     * @throws LibrecException
     */
    protected void cleanup() throws LibrecException {

    }

    /*
     * (non-Javadoc)
     *
     * @see net.librec.recommender.Recommender#loadModel()
     */
    @Override
    public void loadModel() {
        String filePath = conf.get("");
//        ModelDataUtil.loadRecommenderModel(this, filePath);
    }

    /*
     * (non-Javadoc)
     *
     * @see net.librec.recommender.Recommender#saveModel()
     */
    @Override
    public void saveModel() {
        String filePath = conf.get("");
//        ModelDataUtil.saveRecommenderModel(this, filePath);
    }

    /**
     * get Context
     *
     * @return
     */
    protected RecommenderContext getContext() {
        return context;
    }

    /**
     * set Context
     *
     * @param context
     */
    public void setContext(RecommenderContext context) {
        this.context = context;
    }

    /**
     * get Data Model
     */
    public DataModel getDataModel() {
        return context.getDataModel();
    }

    /**
     * get Recommended List
     */
    public List<RecommendedItem> getRecommendedList() {
    	if (recommendedList != null && recommendedList.size() > 0) {
    		List<RecommendedItem> userItemList = new ArrayList<>();
    		Iterator<UserItemRatingEntry> recommendedEntryIter = recommendedList.entryIterator();
    		if (userMappingData != null && userMappingData.size() > 0 && itemMappingData != null && itemMappingData.size() > 0) {
    			BiMap<Integer, String> userMappingInverse = userMappingData.inverse();
    			BiMap<Integer, String> itemMappingInverse = itemMappingData.inverse();
    			while (recommendedEntryIter.hasNext()) {
    				UserItemRatingEntry userItemRatingEntry = recommendedEntryIter.next();
    				if (userItemRatingEntry != null) {
    					String userId = userMappingInverse.get(userItemRatingEntry.getItemIdx());
    					String itemId = itemMappingInverse.get(userItemRatingEntry.getItemIdx());
    					if (StringUtils.isNotBlank(userId) && StringUtils.isNotBlank(itemId)) {
    						userItemList.add(new GenericRecommendedItem(userId, itemId, userItemRatingEntry.getValue()));
						}
					}
				}
    			return userItemList;
			}
		}
        return null;
    }

}
