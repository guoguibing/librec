package net.librec.increment.rating;

import com.google.common.collect.Table;
import net.librec.common.LibrecException;
import net.librec.increment.IncrementalRatingRecommender;
import net.librec.increment.TableMatrix;
import net.librec.math.structure.MatrixEntry;

import java.util.Iterator;

//import net.librec.math.structure.SparseVector;

/**
 *  Uses the average rating value, plus a regularized user and item bias for prediction.
 */

public class UserItemBaseline extends IncrementalRatingRecommender {

    /**
     * regularization parameter for the item biases
     */
    public double regU = 15;

    /**
     * regularization parameter for the item biases
     */
    public double regI = 10;

    public int numIter = 10;

    public double globalAverage;

    /**
     * user biases
     */
    protected TableMatrix userBiases;

    /**
     * item biases
     */
    protected TableMatrix itemBiases;

    @Override
    public void trainModel() throws LibrecException{

        //initialize the userBiased and itemBiased
        userBiases = new TableMatrix(numUsers);
        itemBiases = new TableMatrix(numItems);


        this.globalAverage = this.globalMean;

        for(int iter = 0; iter < this.numIter; iter ++){
            iterate();
        }

    }

    public void iterate(){

        optimizeItemBiases();
        optimizeUserBiases();
    }

    protected void optimizeUserBiases(){

        TableMatrix userRatingCount = new TableMatrix(this.maxUserId);
        userBiases.init(0);

        // optimize user biases
        // optimize user biases
        for(MatrixEntry matrixEntry : trainMatrix) {
            // user userId
            int userId = matrixEntry.row();
            // item itemId
            int itemId = matrixEntry.column();
            // real rating on item itemId rated by user userId
            double realRating = matrixEntry.get();
            double updatedBaise = realRating - globalAverage - itemBiases.get(itemId);
            userBiases.add(itemId, updatedBaise);
        }
        Iterator<Table.Cell<Integer, Integer, Double>>  iterator = userRatingCount.iterator();
        while (iterator.hasNext()){
            int itemId = iterator.next().getColumnKey();
            double biases = iterator.next().getValue();
            if(biases != 0 ){
                userBiases.set(itemId, userBiases.get(itemId)/(regI + userRatingCount.get(itemId)));
            }

        }

    }


    protected void optimizeItemBiases(){


        TableMatrix itemRatingCount = new TableMatrix(this.maxUserId);

        itemBiases.init(0);

        // optimize item biases
        for(MatrixEntry matrixEntry : trainMatrix) {
            // user userId
            int userId = matrixEntry.row();
            // item itemId
            int itemId = matrixEntry.column();
            // real rating on item itemId rated by user userId
            double realRating = matrixEntry.get();
            double updatedBaise = realRating - globalAverage - userBiases.get(userId);
            itemBiases.add(itemId, updatedBaise);

            itemRatingCount.add(itemId, 1);
        }
        Iterator<Table.Cell<Integer, Integer, Double>>  iterator = itemRatingCount.iterator();
        while (iterator.hasNext()){
            int itemId = iterator.next().getColumnKey();
            double biases = iterator.next().getValue();
            if(biases != 0 ){
                itemBiases.set(itemId, itemBiases.get(itemId)/(regI + itemRatingCount.get(itemId)));
            }
        }

    }

    @Override
    public double predict(int userId, int  itemId){
        double userBias = (userId < userBiases.columnSize() && userId >= 0) ? userBiases.get(userId) : 0;
        double itemBias = (userId < itemBiases.columnSize() && userId >= 0) ? itemBiases.get(userId) : 0;
        double result =  globalAverage + userBias + itemBias;
        if(result > maxRating){
            return maxRating;
        }
        if(result < minRating){
            return  minRating;
        }
        return result;
    }

    public void retrainUser(int userId){
        if(updateItems){
            //int itemCounts = trainMatrix.columnSize(userId);
            int itemCounts = 0;
            for(MatrixEntry matrixEntry : trainMatrix) {
                if(userId == matrixEntry.row()) {
                    int itemId = matrixEntry.column();
                    double realRating = matrixEntry.get();
                    userBiases.add(userId, realRating - globalAverage - itemBiases.get(userId));
                    itemCounts ++;
                }
                else{
                    continue;
                }
            }
            if(itemCounts != 0){
                userBiases.set(userId, userBiases.get(userId)/(regU + itemCounts));
            }

        }
    }

    public void retrainItem(int itemId){
        if(updateItems){
            int userCounts = 0;
            for(MatrixEntry matrixEntry : trainMatrix) {
                if(itemId == matrixEntry.column()) {
                    int userId = matrixEntry.column();
                    double realRating = matrixEntry.get();
                    itemBiases.add(itemId, realRating - globalAverage);
                    userCounts ++;
                }
                else{
                    continue;
                }
            }
            if(userCounts != 0)
            {
                itemBiases.set(itemId, userBiases.get(itemId)/(regI + userCounts));
            }
        }
    }

    public void addRatings(TableMatrix newRatings) throws LibrecException{
        super.addRatings(newRatings);
        retrainUsersAndItems(newRatings);
    }

    public void updateRatings(TableMatrix newRatings) throws LibrecException{
        super.updateRatings(newRatings);
        retrainUsersAndItems(newRatings);
    }

    public void removeRatings(TableMatrix newRatings) throws LibrecException{
        super.removeRatings(newRatings);
        retrainUsersAndItems(newRatings);
    }

    public void addUser(int userId) {
        super.addUser(userId);
    }

    public void addItems(int itemId) {
        super.addItem(itemId);
    }

    public void retrainUsersAndItems(TableMatrix newRatings)throws LibrecException{
        // iterative user data
        Iterator<Table.Cell<Integer, Integer, Double>> it = newRatings.iterator();
        int userId, itemId;
        //SparseVector itemValues, userValues;
        double ratingValue;
        while (it.hasNext()){
            // a separate piece of data
            Table.Cell<Integer, Integer, Double> iterRatingData= it.next();
            userId = iterRatingData.getRowKey();
            itemId = iterRatingData.getColumnKey();
            //ratingValue = iterRatingData.getValue();

            // train user or user
            //itemValues = trainMatrix.row(userId);
            //userValues = trainMatrix.column(itemId);

            this.retrainUser(userId);
            this.retrainItem(itemId);
        }
    }
}



