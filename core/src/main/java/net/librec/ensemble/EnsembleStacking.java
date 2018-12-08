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
import net.librec.data.convertor.TextDataConvertor;
import net.librec.data.splitter.KCVDataSplitter;
import net.librec.data.splitter.RatioDataSplitter;
import net.librec.math.structure.SequentialAccessSparseMatrix;
import net.librec.recommender.RecommenderContext;

//import net.librec.math.structure.SparseMatrix;

/**
 * Combines several classifiers using the stacking method. Can do classification or regression.<br/>
 *
 * @author logicxin
 */

public class EnsembleStacking extends Ensemble{

    // default 5-fold
    public int fold = 5;
    public double ratio = 0.8;
    SequentialAccessSparseMatrix  trainData, testData;
    SequentialAccessSparseMatrix[] trainSliceData;


    public String stacker;
    //SparseMatrix testMatrix;

    public EnsembleStacking(String configFile) throws Exception{
        super(configFile);
    }

    protected void trainModel() throws Exception {
        //Get the results of algrithms
        super.trainModel();
        this.fold = Integer.parseInt(this.conf.get("data.splitter.cv.number"));
        this.ratio = Integer.parseInt(this.conf.get("data.splitter.trainset.ratio"));
        EnsembleStackModel();
    }

    protected void EnsembleStackModel() throws Exception {
        int numsOfAlg = this.numsOfAlg;
        RecommenderContext context;
        dataKFlodSpliter(this.fold, this.ratio);

        while(numsOfAlg > 0){
            // i-Model
            String algNum = String.valueOf(this.numsOfAlg-numsOfAlg);
            String configFileForOneAlg = this.conf.get(algNum+".rec.recommender.location");
            String algClass = this.namesOfAlg.get(Integer.parseInt(algNum));
            // i-Fold
            for (int i = 1; i <= this.fold; i++) {
                context = preTraining(configFileForOneAlg, "kcv", i);
                //List<RecommendedItem> recommendedList = algorithmExecute(context, algClass);
                // Save recommand result to local
                //saveResult(recommendedList, context.getConf(), numsOfAlg, "cross");
            }
            if(numsOfAlg == 1){
                // True testset
                context = preTraining(configFileForOneAlg, "ratio", 0);
                //List<RecommendedItem> recommendedList = algorithmExecute(context, algClass);
                //saveResult(recommendedList, context.getConf(), numsOfAlg, "test");
                break;
            }
            numsOfAlg--;
        }
        //stackerModel();
    }

    protected void dataKFlodSpliter(int kFold, double ratio) throws Exception{
        TextDataConvertor convertor;
        //conf.set("inputDataPath", conf.get("dfs.data.dir") + "/test/datamodeltest/matrix4by4A.txt");

        //System.out.println(conf.get("dfs.data.dir") +  conf.get("data.input.path"));

        conf.set("inputDataPath", conf.get("dfs.data.dir") +  conf.get("data.input.path"));
        convertor = new TextDataConvertor(
                new String[]{"user","item","rating"},
                new String[]{"STRING","STRING","NUMERIC"},
                conf.get("inputDataPath"), " ");

        convertor.processData();
        SequentialAccessSparseMatrix allData = convertor.getPreferenceMatrix();
        RatioDataSplitter ratioSplitter = new RatioDataSplitter();
        ratioSplitter.setPreferenceMatrix(allData);
        ratioSplitter.getRatioByRating(ratio);
        trainData = ratioSplitter.getTrainData();
        testData  = ratioSplitter.getTestData();

        KCVDataSplitter kcvSplitter =  new KCVDataSplitter();
        kcvSplitter.setPreferenceMatrix(trainData);
        kcvSplitter.splitData(kFold);
        trainSliceData = (SequentialAccessSparseMatrix[])  kcvSplitter.getAssignMatrixList().toArray(new SequentialAccessSparseMatrix[kFold]);

        System.out.println("trainsSlice cardinality:");
        for ( int i = 0; i < trainSliceData.length; i++) {
            System.out.println(trainSliceData[i].size());
        }
        System.out.println("train cardinality: " + trainData.size());
        System.out.println("test cardinality: " + testData.size());
    }

    protected RecommenderContext preTraining(String configFile, String splitterType, int iFold) throws Exception {
        Configuration confAlg = configInfo(configFile);
        double weightTemp = conf.getDouble("rec.ensemble.weight", 1.00d);
        this.weights.add(weightTemp);
        String  algClass = confAlg.get("rec.recommender.fullClassName");
        //splitterData(splitterType, Integer.toString(iFold));
        RecommenderContext context = context(confAlg, this.dataModel);
        return context;
    }

    // Splitter data
//    protected void splitterData(String splitterType, String validationTestValue) throws Exception {
//        // dataModel corresponds to a unique configuration file
//        //
//        if(splitterType == "kcv"){
//            this.conf.set("data.splitter.cv.index", validationTestValue);
//            this.dataModel = dataModel(this.conf);
//        }
//        else if(splitterType == "ratio"){
//            //
//            this.conf.set("data.model.splitter", "ratio");
//            this.conf.set("data.splitter.trainset.ratio", "0.8");
//            this.dataModel = dataModel(this.conf);
//            //this.testMatrix = (SparseMatrix) this.dataModel.getTestDataSet();
//        }
//
//    }

    protected  void stackerModel(RecommenderContext context, String algClass) throws Exception {
//        Recommender recommender = (context, algClass);
//
//        List recommendedItemList = filterRecommandResult(recommender);
//        return recommendedItemList;
    }


    /**
     *
     * @param recommender
     * @return
     * @throws LibrecException
     */
//    protected double stackingEvaluation(Recommender recommender) throws LibrecException {
//        RecommenderEvaluator evaluator = new AUCEvaluator();
//        double evaluateValue = recommender.evaluate(evaluator);
//        return evaluateValue;
//    }

    /**
     *
     * @param recommender
     * @param filter
     * @return
     * @throws LibrecException
     */
//    protected List getStackingResult(Recommender recommender, RecommendedFilter filter ) throws LibrecException {
//        List recommendedItemList = recommender.getRecommendedList();
//        recommendedItemList = filter.filter(recommendedItemList);
//        return recommendedItemList;
//    }

}

