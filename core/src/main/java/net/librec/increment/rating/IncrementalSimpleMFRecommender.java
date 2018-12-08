package net.librec.increment.rating;

import com.google.common.collect.Table;
import net.librec.common.LibrecException;
import net.librec.increment.IncrementalMFRecommender;
import net.librec.increment.TableMatrix;
import net.librec.math.structure.DenseVector;
import net.librec.math.structure.MatrixEntry;
import net.librec.math.structure.SparseTensor;
import net.librec.math.structure.VectorBasedDenseVector;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

//import net.librec.math.algorithm.Shuffle;


/**
 * Simple maxtix factorization class, Learing is preformed by SGD
 *
 */

public class IncrementalSimpleMFRecommender extends IncrementalMFRecommender {

    /**
     * regularization factor for the bias terms
     * @throws Exception
     */

    public IncrementalSimpleMFRecommender() throws Exception{
        super();
    }

    @Override
    protected void setup() throws LibrecException{
        super.setup();
    }

    @Override
    public void initModel () throws LibrecException {
        super.initModel();
        this.MaxMinRating();
    }

    @Override
    protected void trainModel() throws LibrecException {

        initModel();

        learnFactors();
    }

    /**
     *
     * @throws LibrecException
     */
    private void learnFactors() throws LibrecException{

        for(int iter = 0; iter < this.numIter; iter ++)
        {
            iterate();
            //this.updateLearnRate();
            this.updateLRate(iter);
        }
    }

    /**
     *
     * @param ratingData
     * @throws LibrecException
     */
    private void learnFactors(Table.Cell<Integer, Integer, Double> ratingData, SparseTensor rcData, boolean isRow) throws LibrecException{

        for(int iter = 0; iter < this.numIter; iter ++)
        {
            iterate(ratingData, rcData, isRow);
            //this.updateLearnRate();
            this.updateLRate(iter);
        }
    }

    /**
     *
     * @param ratingData
     * @throws LibrecException
     */
    protected void iterate(Table.Cell<Integer, Integer, Double> ratingData, SparseTensor rcData, boolean isRow) throws LibrecException {

        // 1. incremental data train (ratingData)
        int userId = ratingData.getRowKey();
        int itemId = ratingData.getColumnKey();
        double value = ratingData.getValue();

        iter(itemId, itemId, value);

        // 2. Origin data train (rcData)
        if(isRow){
            for (int i = 0; i < rcData.getItemDimension(); i++) {
                iter(userId, i, rcData.value(i));
            }
        }else{
            for (int i = 0; i < rcData.getUserDimension(); i++) {
                iter(i, itemId, rcData.value(i));
            }
        }

    }

    /**
     *
     * @param userId
     * @param itemId
     * @param realRating
     * @throws LibrecException
     */
    protected void iter(int userId, int itemId, double realRating) throws LibrecException {

        double prediction = predict(userId, itemId, false);
        double err = realRating -  prediction;

        for (int f = 0; f < this.numFactors; f++){
            double userFactorValue = userFactors.get(userId, f);
            double itemFactorValue  = itemFactors.get(itemId, f);

            if(updateUsers){
                double deltaU = err * itemFactorValue - regularization * userFactorValue;
                userFactors.add(userId, f, currentLearnrate * deltaU);
            }
            if(updateItems){
                double deltaI = err * userFactorValue - regularization * itemFactorValue;
                itemFactors.add(userId, f, currentLearnrate * deltaI);
            }

        }
    }

    /**
     *
     * @throws LibrecException
     */
    protected void iterate() throws LibrecException {

        for (MatrixEntry matrixEntry : trainMatrix) {
            // user userIdx
            int userId = matrixEntry.row();
            // item itemIdx
            int itemId = matrixEntry.column();
            // real rating on item itemIdx rated by user userIdx
            double realRating = matrixEntry.get();

            double prediction = predict(userId, itemId, false);
            double err = realRating -  prediction;

            for (int f = 0; f < this.numFactors; f++){
                double userFactorValue = userFactors.get(userId, f);
                double itemFactorValue  = itemFactors.get(itemId, f);

                if(updateUsers){
                    double deltaU = err * itemFactorValue - regularization * userFactorValue;
                    userFactors.add(userId, f, currentLearnrate * deltaU);
                }
                if(updateItems){
                    double deltaI = err * userFactorValue - regularization * itemFactorValue;
                    itemFactors.add(userId, f, currentLearnrate * deltaI);
                }

            }
        }

    }

    /***
     *
     * @param ratedItems
     *  Entry<Integer, Double> itemId, rating
     * @return
     * @throws LibrecException
     */
    protected  DenseVector foldIn(List<Entry<Integer, Double>> ratedItems) throws LibrecException{

        double userBias = 0;
        DenseVector userFactor = new VectorBasedDenseVector(this.numFactors);
        userFactor.init(initMean, initStdDev);

        // need Shuffle
        //ratedItems = Shuffle.shuffleEntryList(ratedItems);

        for (int iter = 0; iter > this.numIter; iter++){

            for (int index =0; index < ratedItems.size(); index++){
                int itemId = ratedItems.get(index).getKey();
                int itemRealRating = ratedItems.get(index).getKey();

                // 1. compute rating and error
                double prediction = predict(userFactor, itemId);
                double err = itemRealRating -  prediction;

                // 2. adjust userFactor
                for (int f = 0; f < this.numFactors; f++){
                    double userFactorValue = userFactor.get(f);
                    double itemFactorValue  = itemFactors.get(itemId, f);

                    double deltaU = err * itemFactorValue - regularization * userFactorValue;
                    userFactor.set(f, this.currentLearnrate * deltaU);
                }
            }
            updateLRate(iter);
        }

        DenseVector userVector = userFactor.clone();
        return userVector;
    }

    /**
     *
     * @param ratedItems
     * @param candidateItems
     * @return
     * @throws LibrecException
     */
    public List<Entry<Integer, Double>> scoreItems(List<Entry<Integer, Double>> ratedItems, List<Integer> candidateItems) throws LibrecException{

        DenseVector userVector = this.foldIn(ratedItems);
        // compute score of the items
        List<Entry<Integer, Double>> result = new ArrayList<>(candidateItems.size());
        for(int i = 0; i < candidateItems.size(); i++){
            int itemId = candidateItems.get(i);

            double itemPredictValue = predict(userVector, itemId);
            result.set(i, new SimpleEntry<>(itemId, itemPredictValue));
        }

        return result;
    }

     /**
     * predict a specific rating for user userIdx on item itemIdx.
     *
     * @param userId user index
     * @param itemId item index
     * @return predictive rating for user userIdx on item itemIdx
     * @throws LibrecException if error occurs
     */
    protected double predict(int userId, int itemId) throws LibrecException {
        return predict(userId, itemId, true);
    }

    /**
     *
     * @param userId
     * @param itemId
     * @param bound   whether there is a bound
     * @return
     */
    protected double predict(int userId, int itemId, boolean bound){

        double score = this.globalBias;

        score += TableMatrix.rowMult(userFactors, userId, itemFactors, itemId);

        if(bound){
            if(score > maxRating){
                score = maxRating;
            }
            if(score < minRating){
                score = minRating;
            }
        }
        return score;

    }

    /**
     *
     * @param userVector
     * @param itemId
     *
     * @throws LibrecException
     */
    protected double predict(DenseVector userVector, int itemId) throws LibrecException {

        return predict(userVector, itemId, true);
    }

    /**
     *
     * @param userVector
     * @param itemId
     * @param bound
     * @return
     * @throws LibrecException
     */
    protected double predict(DenseVector userVector, int itemId, boolean bound) throws LibrecException {

        List<Double> itemFactorList = itemFactors.row(itemId);

        // List to array
        double[] itemFactorArr = itemFactorList.stream().mapToDouble(Double::doubleValue).toArray();
        DenseVector itemFactor = new VectorBasedDenseVector(itemFactorArr);

        double score = this.globalBias + userVector.dot(itemFactor);

        if(bound){
            if(score > maxRating){
                score = maxRating;
            }
            if(score < minRating){
                score = minRating;
            }
        }
        return score;
    }

    /***
     *  max rating and  min rating of the train data.
     *
     */
    protected void MaxMinRating(){
        this.maxRating = this.maxRate;
        this.minRating = this.minRate;
        this.setMaxRating(this.maxRating);
        this.setMinRating(this.minRating);
    }

    /**
     *
     * @param iterRatingData
     * @param itemValues
     * @throws LibrecException
     */
    protected void reTrianUser(Table.Cell<Integer, Integer, Double> iterRatingData, SparseTensor itemValues) throws LibrecException {
        if(this.updateUsers){
            //1.userFactors structure reconstructure
            //userFactors.rowInitNormal(userId, this.initMean, this.initStd);

            //2.need all the data of this user or item in updated training data
            learnFactors(iterRatingData, itemValues, true);
        }
    }

    /**
     *
     * @param iterRatingData
     * @param userValues
     * @throws LibrecException
     */
    protected void reTrianItem(Table.Cell<Integer, Integer, Double> iterRatingData, SparseTensor userValues) throws LibrecException {
        if(this.updateItems){
            learnFactors(iterRatingData, userValues, false);
        }
    }

    /***
     *
     * @param newRatings
     */
    public void addRatings(TableMatrix newRatings) throws LibrecException{

        super.addRatings(newRatings);

        // iterative user data
        Iterator<Table.Cell<Integer, Integer, Double>> it = newRatings.iterator();
        int userId, itemId;
        SparseTensor itemValues, userValues;
        double ratingValue;
        while (it.hasNext()){
            // a separate piece of data
            Table.Cell<Integer, Integer, Double> iterRatingData= it.next();
            userId = iterRatingData.getRowKey();
            itemId = iterRatingData.getColumnKey();
            ratingValue = iterRatingData.getValue();

            // ? train user or user
            //itemValues = trainMatrix.row(userId);
            //userValues = trainMatrix.column(itemId);
            //this.reTrianUser(iterRatingData, itemValues);
            //this.reTrianItem(iterRatingData, userValues);
        }

    }

    /***
     *
     * @param newRatings
     */
    @Override
    public void updateRatings(TableMatrix newRatings) throws LibrecException{

        super.updateRatings(newRatings);

        // iterative user data
        Iterator<Table.Cell<Integer, Integer, Double>> it = newRatings.iterator();
        int userId, itemId;
        SparseTensor itemValues, userValues;
        double ratingValue;
        while (it.hasNext()){
            // a separate piece of data
            Table.Cell<Integer, Integer, Double> iterRatingData= it.next();
            userId = iterRatingData.getRowKey();
            itemId = iterRatingData.getColumnKey();
            ratingValue = iterRatingData.getValue();

            // ? train user or user
            //itemValues = trainMatrix.row(userId);
            //userValues = trainMatrix.column(itemId);
            //this.reTrianUser(iterRatingData, itemValues);
            //this.reTrianItem(iterRatingData, userValues);
        }

    }

    /***
     *
     * @param removeRatings
     */
    @Override
    public void removeRatings(TableMatrix removeRatings)  throws LibrecException{

        super.removeRatings(removeRatings);

        // iterative user data
        Iterator<Table.Cell<Integer, Integer, Double>> it = removeRatings.iterator();
        int userId, itemId;
        SparseTensor itemValues, userValues;
        double ratingValue;
        while (it.hasNext()){
            // a separate piece of data
            Table.Cell<Integer, Integer, Double> iterRatingData= it.next();
            userId = iterRatingData.getRowKey();
            itemId = iterRatingData.getColumnKey();
            ratingValue = iterRatingData.getValue();

            // ? train user or user
            //itemValues = trainMatrix.row(userId);
            //userValues = trainMatrix.column(itemId);

            //this.reTrianUser(iterRatingData, itemValues);
            //this.reTrianItem(iterRatingData, userValues);
        }

    }

    /***
     *
     * @param userId
     */
    @Override
    protected void addUser(int userId){

        super.addUser(userId);
        //userfactors plus row
         userFactors.addRow(userId);
    }

    /**
     *
     * @param itemId
     */
    @Override
    protected void addItem(int itemId){
        super.addItem(itemId);
        // item factors plus rows
        itemFactors.addRow(itemId);
    }


    @Override
    public void removeUser(int userId){
        super.removeUser(userId);
        // user factors set one row zero
        userFactors.setRowToOneValue(userId, 0.0d);
}


    @Override
    public void removeItem(int itemId){
        super.removeItem(itemId);
        // item factors set one row zero
        itemFactors.setRowToOneValue(itemId, 0.0d);
    }

}
