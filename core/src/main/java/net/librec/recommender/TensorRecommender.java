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

import com.google.common.collect.BiMap;
import net.librec.common.LibrecException;
import net.librec.conf.Configuration;
import net.librec.data.DataModel;
import net.librec.data.model.ArffDataModel;
import net.librec.eval.Measure.MeasureValue;
import net.librec.eval.RecommenderEvaluator;
import net.librec.math.structure.SparseTensor;
import net.librec.math.structure.TensorEntry;
import net.librec.recommender.item.RecommendedItem;
import net.librec.recommender.item.RecommendedItemList;
import net.librec.recommender.item.RecommendedList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Tensor Recommender
 *
 * @author WangYuFeng
 */
public abstract class TensorRecommender implements Recommender {
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
     * Recommended Item List
     */
    protected RecommendedList recommendedList;

    /**
     * user Mapping Data
     */
    public BiMap<String, Integer> userMappingData;

    /**
     * item Mapping Data
     */
    public BiMap<String, Integer> itemMappingData;

    public ArrayList<BiMap<String, Integer>> allFeaturesMappingData;
    /**
     * early-stop criteria
     */
    protected boolean earlyStop;

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
     * objective loss
     */
    protected double loss, lastLoss = 0.0d;

    /**
     * whether to adjust learning rate automatically
     */
    protected boolean isBoldDriver;

    /**
     * decay of learning rate
     */
    protected float decay;

    /**
     * verbose
     */
    protected boolean verbose = true;

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
     * setup
     *
     * @throws LibrecException if error occurs during setting up
     */
    protected void setup() throws LibrecException {
        conf = context.getConf();
        isRanking = conf.getBoolean("rec.recommender.isranking");
        if (isRanking) {
            topN = conf.getInt("rec.recommender.ranking.topn", 5);
        }

        earlyStop = conf.getBoolean("rec.recommender.earlyStop");
        verbose = conf.getBoolean("rec.recommender.verbose", true);

        learnRate = conf.getFloat("rec.iterator.learnrate", 0.01f);
        maxLearnRate = conf.getFloat("rec.iterator.learnrate.maximum", 1000.0f);

        numFactors = conf.getInt("rec.factor.number", 10);
        reg = conf.getFloat("rec.tensor.regularization", 0.01f);

        trainTensor = (SparseTensor) getDataModel().getTrainDataSet();
        testTensor = (SparseTensor) getDataModel().getTestDataSet();
        validTensor = (SparseTensor) getDataModel().getValidDataSet();


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

        userMappingData = getDataModel().getUserMappingData();
        itemMappingData = getDataModel().getItemMappingData();
        numUsers = userMappingData.size();
        numItems = itemMappingData.size();
        allFeaturesMappingData = ((ArffDataModel)getDataModel()).getAllFeaturesMappingData();

        userDimension = trainTensor.getUserDimension();
        itemDimension = trainTensor.getItemDimension();

    }

    /**
     * recommend
     *
     * @param context  recommender context
     * @throws LibrecException if error occurs during recommending
     */
    @Override
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
     * train Model
     *
     * @throws LibrecException if error occurs during training
     */
    protected abstract void trainModel() throws LibrecException;

    /**
     * recommend
     * * predict the ranking scores or ratings in the test data
     *
     * @return predictive ranking score or rating matrix
     * @throws LibrecException if error occurs during recommending
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
     * @throws LibrecException if error occurs during recommending
     */
    protected RecommendedList recommendRank() throws LibrecException {
        recommendedList = new RecommendedItemList(numUsers - 1, numUsers);
        //TODO
        return recommendedList;
    }

    /**
     * recommend
     * * predict the ratings in the test data
     *
     * @return predictive rating matrix
     * @throws LibrecException if error occurs during recommending
     */
    protected RecommendedList recommendRating() throws LibrecException {
        recommendedList = new RecommendedItemList(numUsers - 1, numUsers);
        for (TensorEntry testTensorEntry : testTensor) {
            int[] keys = testTensorEntry.keys();
            int userIdx = testTensorEntry.key(userDimension);
            int itemIdx = testTensorEntry.key(itemDimension);
            double predictRating = predict(keys, true);
            if (Double.isNaN(predictRating)) {
                predictRating = globalMean;
            }
            recommendedList.addUserItemIdx(userIdx, itemIdx, predictRating);
        }
        return recommendedList;
    }


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


    /**
     * predict a specific rating for user userIdx on item itemIdx with some other contexts indices. Tt is useful for
     * evalution which requires predictions are bounded.
     *
     * @param keys user index, item index and context indices
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
     * @param iter current iteration
     * @return boolean: true if it is converged; false otherwise
     * @throws LibrecException if error occurs
     */
    protected boolean isConverged(int iter) throws LibrecException {
        float delta_loss = (float) (lastLoss - loss);

        // print out debug info
        if (verbose) {
            String recName = getClass().getSimpleName().toString();
            String info = recName + " iter " + iter + ": loss = " + loss + ", delta_loss = " + delta_loss;
            LOG.info(info);
        }

        if (Double.isNaN(loss) || Double.isInfinite(loss)) {
//            LOG.error("Loss = NaN or Infinity: current settings does not fit the recommender! Change the settings and try again!");
            throw new LibrecException("Loss = NaN or Infinity: current settings does not fit the recommender! Change the settings and try again!");
        }

        // check if converged
        boolean converged = Math.abs(delta_loss) < 1e-5;

        return converged;
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

    /**
     * cleanup
     *
     * @throws LibrecException if error occurs during cleaning up
     */
    protected void cleanup() throws LibrecException {

    }

    @Override
    public double evaluate(RecommenderEvaluator evaluator) throws LibrecException {
        return 0;
    }

    @Override
    public Map<MeasureValue, Double> evaluateMap() throws LibrecException {
        return null;
    }

    @Override
    public DataModel getDataModel() {
        return context.getDataModel();
    }

    @Override
    public void loadModel(String filePath) {

    }

    @Override
    public void saveModel(String filePath) {

    }

    @Override
    public List<RecommendedItem> getRecommendedList() {
        return null;
    }

    /**
     * @param context the context to set
     */
    @Override
    public void setContext(RecommenderContext context) {
        this.context = context;
    }

}
