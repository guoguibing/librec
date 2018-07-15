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
import net.librec.recommender.Recommender;
import net.librec.recommender.item.GenericRecommendedItem;
import net.librec.recommender.item.RecommendedItem;
import net.librec.recommender.item.RecommendedList;
import org.apache.commons.math3.linear.*;

import java.util.ArrayList;
import java.util.List;


/**
 * Emsemble learning Case
 * {@link EnsembleLinear}
 *
 * @author logic
 */

public class EnsembleLinear extends Ensemble{

    protected   List<Double> weightList;

    /**
     *
     * @param configFile
     * @throws Exception
     */
    public  EnsembleLinear(String configFile) throws Exception{
        super(configFile);

    }

    /**
     *
     * @throws Exception
     */
    @Override
    protected void trainModel() throws Exception {

        super.trainModel();
        int tempNums = this.numsOfAlg;
        // individual training models
        while(tempNums > 0){

            // loading configuration
            String algNum = String.valueOf(this.numsOfAlg - tempNums);
            String configFilePath = this.conf.get("rec.recommender.location." + algNum);

            // training recommender
            Recommender recommender = recommender(configFilePath, this.dataModel);
            // get recommend result

            RecommendedList resultRecommend = getRecommendedList(recommender);
            // collect the recommended results
            this.recommendedItemListOfAlgs.add(resultRecommend);
            tempNums --;
        }

        //LeastSquaretrainModel();
    }

    /**
     *
     * @return
     */
    protected RecommendedList getRecommendedList(Recommender recommender) throws LibrecException{

        RecommendedList recommendedList;
        if(Boolean.valueOf(this.isranking)){
            recommendedList =  recommender.recommendRank();
        }else{
            recommendedList = recommender.recommendRating(this.dataModel.getTestDataSet());
        }

        return recommendedList;
    }

    /**
     *
     * @return
     * @throws Exception
     */
    // LeastSquare use commons-math3
    protected List<Double> LeastSquaretrainModel() throws Exception {

        double[][] myList = getMList();
        double[] yList = getYList();

        RealMatrix coefficients =
                new Array2DRowRealMatrix(myList,
                        false);

        DecompositionSolver solver = new LUDecomposition(coefficients).getSolver();
        RealVector constants = new ArrayRealVector(yList, false);
        RealVector solution = solver.solve(constants);
        this.weightList = getAlgorithmWeight(solution);

        return this.weightList;
    }

    /**
     *
     * @return
     */
    protected double[][] getMList(){

        List OneAlgSum = getOneAlgSum();

        int n = getTotalItems();
        int k = getNumsOfAlg();

        double[][] myList = new double[k][k];
        for (int i=0; i<k; i++){
            for(int j =0; j<k; j++){
                if(i==0){
                    if(j==0){
                        myList[i][j] = n;
                        continue;
                    }
                    myList[i][j] = (Double)OneAlgSum.get(i);
                }
                else{
                    if(j==0){
                        myList[i][j] = (Double)OneAlgSum.get(i);
                        continue;
                    }
                    myList[i][j] = getMutiAlgSum(i, j);
                }

            }
        }
        return myList;
    }

    /**
     *
     * @return
     */
    protected List getOneAlgSum(){
        //List recommendedItemListTemp = this.recommendedItemListFilter;
        List recommendedItemListTemp = this.recommendedItemListOfAlgs;
        // 1
        List<Double> algSum = new ArrayList();

        for(int i=0; i< recommendedItemListTemp.size(); i++){
            RecommendedList reList = (RecommendedList)recommendedItemListTemp.get(i);
            List list= (List)recommendedItemListTemp.get(i);
            //List list = reList.
            double algSumTemp = 0.00d;
            for(int j = 0; j < list.size(); j++)
            {
                //GenericRecommendedItem tem = (GenericRecommendedItem)list.get(j);
                //algSumTemp += tem.getValue();
            }
            algSum.add(algSumTemp);
        }

        return algSum;

    }

    /**
     *
     * @param i
     * @param k
     * @return
     */
    protected double getMutiAlgSum(int i, int k){

        List recommendedItemListTemp = this.recommendedItemListOfAlgs;

        List list1= (List)recommendedItemListTemp.get(i);
        List listk= (List)recommendedItemListTemp.get(k);
        double algSumIK= 0.00d;
        for(int j = 0; j < list1.size(); j++)
        {
            GenericRecommendedItem tem1= (GenericRecommendedItem)list1.get(j);
            GenericRecommendedItem tem2= (GenericRecommendedItem)list1.get(j);
            algSumIK += tem1.getValue()*tem2.getValue();

        }
        return algSumIK;
    }

    //4
    protected  double[] getYList(){
        int k = getNumsOfAlg();
        double[] yList = new double[k];
        List<Double> yReal = this.realRating;

        // Get target
        /*
        for(int t=0; t<k; t++){
            yReal.plus(3.0);
        }
        */

        List recommendedItemListTemp = this.recommendedItemListOfAlgs;
        for(int i=0; i< recommendedItemListTemp.size(); i++){
            List list= (List)recommendedItemListTemp.get(i);
            double algSumTemp = 0.00d;
            if(i == 0)
            {
                algSumTemp += yReal.get(i);
            }
            else{
                for(int j = 0; j < list.size(); j++)
                {
                    GenericRecommendedItem tem = (GenericRecommendedItem)list.get(j);
                    algSumTemp += tem.getValue()*yReal.get(j);
                }

            }
            yList[i] = algSumTemp;
        }

        return yList;
    }


    protected List  getAlgorithmWeight(RealVector solution) throws Exception{
        List<Double> weightList = new ArrayList<>();
        Integer n = solution.getDimension();
        for(int i=0; i<n; i++){
            weightList.add(solution.getEntry(i));
        }
        return  weightList;
    }

    /**
     *
     * @throws Exception
     */
    protected void getRecommandResultByLinear() throws Exception{
        List<Double> recommandResultByLinear = new ArrayList<>();
        List recommendedItemListTemp = this.recommendedItemListOfAlgs;
        for(int i=0; i< recommendedItemListTemp.size(); i++){
            List list= (List)recommendedItemListTemp.get(i);
            for(int j = 0; j < list.size(); j++)
            {
                Double weight = this.weightList.get(i);
                GenericRecommendedItem tem = (GenericRecommendedItem)list.get(j);
                tem.setValue(tem.getValue()*weight);
                list.set(j, tem);

            }
            recommendResultBlend(recommendedItemListTemp);
        }
    }

    /**
     *
     * @param recommendedItemListTemp
     * @throws Exception
     */
    protected  void recommendResultBlend( List recommendedItemListTemp) throws Exception{
        int n = this.getTotalItems();
        for (int j =0; j< n; j++){
            Double valueSum = 0.0d;
            String userId = "";
            String itemId = "";
            for(int i=0; i< recommendedItemListTemp.size(); i++){
                List list= (List)recommendedItemListTemp.get(i);
                GenericRecommendedItem tem = (GenericRecommendedItem)list.get(j);
                userId = tem.getUserId();
                itemId = tem.getItemId();
                valueSum += tem.getValue();
            }
            GenericRecommendedItem genericObj = new GenericRecommendedItem(userId, itemId, valueSum);
            this.recommendedItemFinal.add(genericObj);
        }
    }


    protected void validationListFilter(){
        List validationListTem = this.validationList;
        List recommendedItemListTemp = this.recommendedItemListOfAlgs;

        for(int i=0; i< recommendedItemListTemp.size(); i++){
            List<GenericRecommendedItem> recommendedItemListOne = new ArrayList<GenericRecommendedItem>();
            List list= (List)recommendedItemListTemp.get(i);
            for(int j = 0; j < list.size(); j++)
            {
                GenericRecommendedItem tem = (GenericRecommendedItem)list.get(j);
                String userId = tem.getUserId();
                String itemId = tem.getItemId();
                for(int m=0; m< validationListTem.size(); m++){
                    GenericRecommendedItem temp = (GenericRecommendedItem)validationListTem.get(m);
                    if(userId == temp.getUserId() && itemId == temp.getItemId()){
                        recommendedItemListOne.add(temp);
                    }
                }

            }
            this.recommendedItemListFilter.add(recommendedItemListOne);
        }

    }

    /**
     *
     * @return
     */
    public List<Double> ensembelWeight(){
        return  this.weightList;
    }

    /**
     *
     * @return
     * @throws LibrecException
     */
    public  List<RecommendedItem>  getEnsembleRecommendedList() throws LibrecException{

        String userId, itemId;
        double value;
        double weightedValue = 0.0;
        RecommendedItem rating;
        int userItemPairs = recommendedItemListOfAlgs.get(0).size();

        List<RecommendedItem> result = new ArrayList<RecommendedItem>();
        for(int k =0; k < userItemPairs; k++){
            result.set(k, new GenericRecommendedItem("", "", 0.0));
        }

        for(int i=0; i< recommendedItemListOfAlgs.size(); i++){
            // i-algorithm
            List list= (List)recommendedItemListOfAlgs.get(i);

            for(int j = 0; j < list.size(); j++)
            {
                rating= (RecommendedItem)list.get(j);
                value = rating.getValue();
                weightedValue = value * this.weightList.get(i);
                userId = rating.getUserId();
                itemId = rating.getItemId();
                RecommendedItem men = new GenericRecommendedItem(userId, itemId, result.get(j).getValue() + weightedValue);
                result.set(i, men);
            }

        }

        return result;
    };

    /**
     * Evaluation
     * @param recommender
     * @throws LibrecException
     */
    public void evaluate (List<RecommendedItem> recommender) throws  LibrecException{

//        RecommenderEvaluator evaluator = new RMSEEvaluator();
//        int numUsers = recommender.cardinality();
//        RecommendedList groundTruthList = new RecommendedList(numUsers);
//        for (int userIdx = 0; userIdx < numUsers; ++userIdx) {
//            groundTruthList.addList(new ArrayList<KeyValue<Integer, Double>>());
//        }
//        // testMatrix
//        for (MatrixEntry matrixEntry : testMatrix) {
//            int userIdx = matrixEntry.row();
//            int itemIdx = matrixEntry.column();
//            double rating = matrixEntry.get();
//            groundTruthList.plus(userIdx, itemIdx, rating);
//        }
//        //
//        return evaluator.evaluate(context, groundTruthList, recommendedList);
    }

}
