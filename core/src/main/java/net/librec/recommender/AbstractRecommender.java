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
import net.librec.job.progress.ProgressBar;
import net.librec.recommender.item.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
     * early-stop criteria
     */
    protected boolean earlyStop;

    /**
     * verbose
     */
    protected static boolean verbose = true;

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
     * report the training progress
     */
    protected ProgressBar progressBar;

    /**
     * user Mapping Data
     */
    public BiMap<String, Integer> userMappingData;

    /**
     * item Mapping Data
     */
    public BiMap<String, Integer> itemMappingData;

    /**
     * setup
     *
     * @throws LibrecException if error occurs during setup
     */
    protected void setup() throws LibrecException {
        conf = context.getConf();
        isRanking = conf.getBoolean("rec.recommender.isranking");
        if (isRanking) {
            topN = conf.getInt("rec.recommender.ranking.topn", 10);
            if (this.topN <= 0) {
                throw new IndexOutOfBoundsException("rec.recommender.ranking.topn should be more than 0!");
            }
        }
        earlyStop = conf.getBoolean("rec.recommender.earlystop", false);
        verbose = conf.getBoolean("rec.recommender.verbose", true);

        userMappingData = getDataModel().getUserMappingData();
        itemMappingData = getDataModel().getItemMappingData();

        if (verbose) {
            progressBar = new ProgressBar(100, 100);
        }
    }

    /**
     * train Model
     *
     * @throws LibrecException if error occurs during training model
     */
    protected abstract void trainModel() throws LibrecException;

    /**
     * recommend
     *
     * @param context recommender context
     * @throws LibrecException if error occurs during recommending
     */
    public void train(RecommenderContext context) throws LibrecException {
        this.context = context;
        setup();
        LOG.info("Job Setup completed.");
        trainModel();
        LOG.info("Job Train completed.");
        cleanup();
    }

    /**
     * cleanup
     *
     * @throws LibrecException if error occurs during cleanup
     */
    protected void cleanup() throws LibrecException {

    }

    /**
     * (non-Javadoc)
     *
     * @see net.librec.recommender.Recommender#loadModel(String)
     */
    @Override
    public void loadModel(String filePath) {

    }

    /**
     * (non-Javadoc)
     *
     * @see net.librec.recommender.Recommender#saveModel(String)
     */
    @Override
    public void saveModel(String filePath) {

    }

    /**
     * get Context
     *
     * @return recommender context
     */
    protected RecommenderContext getContext() {
        return context;
    }

    /**
     * set Context
     *
     * @param context recommender context
     */
    public void setContext(RecommenderContext context) {
        this.context = context;
    }

    /**
     * get Data Model
     *
     * @return data model
     */
    public DataModel getDataModel() {
        return context.getDataModel();
    }

    /**
     * get Recommended List
     *
     * @return Recommended List
     */
    public List<RecommendedItem> getRecommendedList(RecommendedList recommendedList) {

        if (recommendedList != null && recommendedList.size() > 0) {
            List<RecommendedItem> userItemList = new ArrayList<>();
            Iterator<ContextKeyValueEntry> recommendedEntryIter = recommendedList.iterator();
            if (userMappingData != null && userMappingData.size() > 0 && itemMappingData != null && itemMappingData.size() > 0) {
                BiMap<Integer, String> userMappingInverse = userMappingData.inverse();
                BiMap<Integer, String> itemMappingInverse = itemMappingData.inverse();
                while (recommendedEntryIter.hasNext()) {
                    ContextKeyValueEntry contextKecyValueEntry = recommendedEntryIter.next();
                    if (contextKecyValueEntry != null) {
                        String userId = userMappingInverse.get(contextKecyValueEntry.getContextIdx());
                        String itemId = itemMappingInverse.get(contextKecyValueEntry.getKeyIdx());
                        if (StringUtils.isNotBlank(userId) && StringUtils.isNotBlank(itemId)) {
                            userItemList.add(new GenericRecommendedItem(userId, itemId, contextKecyValueEntry.getValue()));
                        }
                    }
                }
                return userItemList;
            }
        }
        return null;
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

    public void updateProgress(int currentPoint) {
        if (verbose) {
            conf.setInt("train.current.progress", currentPoint);
            progressBar.showBarByPoint(conf.getInt("train.current.progress"));
        }
    }
}
