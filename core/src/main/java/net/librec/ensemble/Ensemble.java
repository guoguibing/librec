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

package net.librec.ensemble;

import net.librec.common.LibrecException;
import net.librec.conf.Configuration;
import net.librec.conf.Configuration.Resource;
import net.librec.data.DataModel;
import net.librec.data.model.TextDataModel;
import net.librec.data.splitter.KCVDataSplitter;
import net.librec.data.splitter.LOOCVDataSplitter;
import net.librec.job.RecommenderJob;
import net.librec.math.structure.MatrixEntry;
import net.librec.math.structure.SequentialAccessSparseMatrix;
import net.librec.recommender.Recommender;
import net.librec.recommender.RecommenderContext;
import net.librec.recommender.item.RecommendedItem;
import net.librec.recommender.item.RecommendedList;
import net.librec.similarity.PCCSimilarity;
import net.librec.similarity.RecommenderSimilarity;
import net.librec.util.DriverClassUtil;
import net.librec.util.FileUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

//import net.librec.math.structure.SparseMatrix;

/**
 * Emsemble learning Case
 * {@link Ensemble}
 *
 * @author logicxin
 */

public class Ensemble {

    /**
     *
     */
    public String configFile;
    /**
     *
     */
    public Configuration conf;

    /**
     *
     */
    public String algClass;

    /**
     *
     */
    public String data;

    /**
     *
     */
    public int numsOfAlg;

    /**
     *
     */
    public String isranking;

    /**
     *
     */
    public List<Double> weights;

    /**
     *
     */
    public List<Double> evaluationList;

    /**
     *
     */
    public List<String> namesOfAlg;

    /**
     *
     */
    //public List<List<RecommendedItem>> recommendedItemListOfAlgs;
    public List<RecommendedList> recommendedItemListOfAlgs;

    /**
     *
     */
    //public List<List<RecommendedItem>> recommendedItemListResult;
    public List<RecommendedList> recommendedItemListResult;

    /**
     *
     */
    public List<RecommendedItem> recommendedItemFinal;

    /**
     *
     */
    public List validationList;

    /**
     *
     */
    public List recommendedItemListFilter;

    /**
     *
     */
    public int topN;

    /**
     *
     */
    public DataModel dataModel;

    /**
     *
     */
    public List<Double> realRating;

    /**
     *
     */
    protected final Log LOG = LogFactory.getLog(RecommenderJob.class);

    /**
     * @param configFile
     * @throws Exception
     */
    public Ensemble(String configFile) throws Exception {

        this.configFile = configFile;
        this.weights = new ArrayList<Double>();
        this.evaluationList = new ArrayList<Double>();

        this.namesOfAlg = new ArrayList<String>();


        this.recommendedItemListOfAlgs = new ArrayList<RecommendedList>();
        this.recommendedItemListResult = new ArrayList<RecommendedList>();


        this.validationList = new ArrayList<List<RecommendedItem>>();
        this.recommendedItemListFilter = new ArrayList<List<RecommendedItem>>();
        this.recommendedItemFinal = new ArrayList<RecommendedItem>();
        this.realRating = new ArrayList<Double>();

        this.conf = configInfo(configFile);
        //Configuration conff = new Configuration();
        //Configuration.Resource resource = new Configuration.Resource(configFile);
        //conff.addResource(resource);

        //this.conf = conff;

        this.numsOfAlg = conf.getInt("rec.ensemble.numsOfAlg");
        this.isranking = conf.get("rec.recommender.isranking");
        this.topN = conf.getInt("rec.recommender.topN");
        this.algClass = conf.get("rec.recommender.class");

    }

    /**
     * @throws Exception
     */
    protected void trainModel() throws Exception {

        this.dataModel = dataModel(this.conf);
        trainDataRealRating(this.dataModel);

        int tempNums = this.numsOfAlg;
        // individual newRecommemder models
        while (tempNums > 0) {
            String algNum = String.valueOf(this.numsOfAlg - tempNums);
            double weightTemp = this.conf.getDouble("rec.ensemble.weight." + algNum);
            this.weights.add(weightTemp);
            tempNums--;
        }

    }

    /**
     *
     * @param configFile
     * @return
     * @throws Exception
     */
//    protected RecommenderContext prenewRecommemder(String configFile) throws Exception{
//        Configuration confAlg = configInfo(configFile);
//        String algClass = confAlg.get("rec.recommender.fullClassName");
//        this.namesOfAlg.plus(algClass);
//        // Use the same dataModel
//        RecommenderContext context = this.context(confAlg, this.dataModel);
//        return context;
//    }

    /**
     * Recommender configuration
     *
     * @param sourceFile
     * @return
     * @throws LibrecException
     */
    protected Configuration configInfo(String sourceFile) throws LibrecException {
        //this.conf = configInfo(configFile);
        Configuration conff = new Configuration();
        Configuration.Resource resource = new Configuration.Resource(sourceFile);
        conff.addResource(resource);
        return conff;
    }

    /**
     * build data model
     *
     * @param conf
     * @return
     * @throws LibrecException
     */
    protected DataModel dataModel(Configuration conf) throws LibrecException {
        DataModel dataModel = new TextDataModel(conf);
        dataModel.buildDataModel();
        return dataModel;
    }

    /**
     * @param configFilePath
     * @param dataModel
     * @return
     * @throws Exception
     */
    protected Recommender recommender(String configFilePath, DataModel dataModel) throws Exception {
        // configuration
        Configuration conf = this.configInfo(configFilePath);
        // context
        RecommenderContext context = this.context(conf, dataModel);
        // training
        String algClass = conf.get("rec.recommender.fullClassName");
        Recommender newRecommender = this.newRecommemder(context, algClass);

        return newRecommender;
    }

    /**
     * Set recommendation context
     *
     * @param conf
     * @param dataModel
     * @return
     * @throws LibrecException
     */
    protected RecommenderContext context(Configuration conf, DataModel dataModel) throws LibrecException {
        RecommenderContext context = new RecommenderContext(conf, dataModel);
        RecommenderSimilarity similarity = new PCCSimilarity();
        similarity.buildSimilarityMatrix(dataModel);
        context.setSimilarity(similarity);
        return context;
    }

    /**
     * @param context
     * @param algClass
     * @return
     * @throws Exception
     */
    protected Recommender newRecommemder(RecommenderContext context, String algClass) throws Exception {
        Object newAlg = Class.forName(algClass).newInstance();
        Recommender recommender = (Recommender) newAlg;
        recommender.setContext(context);
        recommender.train(context);
        return recommender;
    }

    /**
     *
     * @param recommender
     * @return
     * @throws LibrecException
     */
//    protected double evaluation(Recommender recommender) throws LibrecException {
//        RecommenderEvaluator evaluator = new RMSEEvaluator();
//        double evaluateValue = recommender.evaluate(evaluator);
//        System.out.println("RMSE:"+ recommender.evaluate(evaluator));
//        return evaluateValue;
//    }

    /**
     *
     * @param recommender
     * @return
     * @throws LibrecException
     */
//     protected List filterRecommandResult(Recommender recommender) throws LibrecException {
//        List recommendedItemList = recommender.getRecommendedList();
//        RecommendedFilter filter = new GenericRecommendedFilter();
//        recommendedItemList = filter.filter(recommendedItemList);
//        return recommendedItemList;
//    }

    /**
     * @return
     */
    protected List recommendedResult() {
        return this.recommendedItemFinal;
    }

    /**
     * @return
     */
    protected int getTotalItems() {

        return recommendedItemListOfAlgs.size();
    }

    /**
     * @return
     */
    protected int getNumsOfAlg() {

        return this.numsOfAlg;
    }

    /**
     * @param dataModel
     */
    protected void trainDataRealRating(DataModel dataModel) {
        //Get SparseMatrix
        SequentialAccessSparseMatrix trainMatrix = (SequentialAccessSparseMatrix) dataModel.getTrainDataSet();
        Iterator<MatrixEntry> trainMatrixIter = trainMatrix.iterator();
        while (trainMatrixIter.hasNext()) {
            MatrixEntry trainMatrixEntry = trainMatrixIter.next();
            double trainRealRating = trainMatrixEntry.get();
            this.realRating.add(trainRealRating);
        }
    }

    /**
     * Save result.
     *
     * @param recommendedList list of recommended items
     * @throws LibrecException        if error occurs
     * @throws IOException            if I/O error occurs
     * @throws ClassNotFoundException if class not found error occurs
     */
    public void saveResult(List<RecommendedItem> recommendedList, Configuration confAlg, int tempNums, String resultType) throws LibrecException, IOException, ClassNotFoundException {
        if (recommendedList != null && recommendedList.size() > 0) {

            String algoSimpleName = DriverClassUtil.getDriverName(recommenderClass(confAlg));
            String outputPath = this.conf.get("dfs.result.dir") + "/ensemble/" + algoSimpleName + "-" + tempNums + "/" + resultType;

            if (null != dataModel && (dataModel.getDataSplitter() instanceof KCVDataSplitter || dataModel.getDataSplitter() instanceof LOOCVDataSplitter) && null != conf.getInt("data.splitter.cv.index")) {
                outputPath = outputPath + "/fold-" + String.valueOf(this.conf.getInt("data.splitter.cv.index"));
            }
            LOG.info("Result path is " + outputPath);
            // Convert itemList to string
            StringBuilder sb = new StringBuilder();
            for (RecommendedItem recItem : recommendedList) {
                String userId = recItem.getUserId();
                String itemId = recItem.getItemId();
                String value = String.valueOf(recItem.getValue());
                sb.append(userId).append(",").append(itemId).append(",").append(value).append("\n");
            }
            String resultData = sb.toString();
            // save resultData
            try {
                FileUtil.writeString(outputPath, resultData);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Get recommender class. {@code Recommender}.
     *
     * @return recommender class object
     * @throws ClassNotFoundException if can't find the class of recommender
     * @throws IOException            If an I/O error occurs.
     */
    @SuppressWarnings("unchecked")
    public Class<? extends Recommender> recommenderClass(Configuration confAlg) throws ClassNotFoundException, IOException {
        return (Class<? extends Recommender>) DriverClassUtil.getClass(confAlg.get("rec.recommender.class"));
    }

    /**
     * @return
     */
    protected List<Double> ensembelParameter() {
        return Collections.emptyList();
    }

    /**
     *
     * @param savePath
     * @return
     */
//    protected Boolean saveRecommendResult(String savePath){
//        return Boolean.TRUE;
//    }
}
