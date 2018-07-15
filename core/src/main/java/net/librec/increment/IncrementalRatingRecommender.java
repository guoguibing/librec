package net.librec.increment;

// put this class to the top

import com.google.common.collect.Table;
import net.librec.common.LibrecException;

import java.util.Iterator;

//import net.librec.math.structure.SparseMatrix;

public abstract class IncrementalRatingRecommender extends IncrementalRecommender implements IIncrementalRatingRecommender{

    public boolean updateUsers;
    public boolean updateItems;

    /// <summary>Default constructor</summary>
    public IncrementalRatingRecommender()
    {
        super();
        updateUsers = true;
        updateItems = true;
    }

    // set default values
    public double regularization;
    public double learnRate;
    public double decay;
    public int numIter;
    public double initStdDev;
    public int numFactors;

    // the bias
    protected double maxRating;
    protected double minRating;
    protected double globalBias;

    /***
     * Enumeration to represent different optimization targets
     *
     */
    public enum OptimizationTarget{
        // root mean square error
        RMSE,
        // mean absolute error
        MSE,
        // log likelihood of the data
        LogisticLoss
    }

    /**
     * @param optTarget
     * @return
     */
    public OptimizationTarget lossTarget(String optTarget){
        switch (optTarget){
            case "rmse":
                return OptimizationTarget.RMSE;
            case "mse":
                return OptimizationTarget.MSE;
            case "logistic":
                return OptimizationTarget.LogisticLoss;
            default:
                return OptimizationTarget.RMSE;
        }
    }

    /**
     * regularization
     *
     */

    public double getRegularization() {
        return this.regularization;
    }
    public void setRegularization(double regularization){
        this.regularization = regularization;
    }

    /**
     *
     *
     */
    public double getLearnRate() {
        return this.regularization;
    }
    public void setLearnRate(double learnRate){
        this.learnRate = learnRate;
    }

    /**
     *
     *
     */
    public double getDecay() {
        return this.decay;
    }
    public void setDecay(double decay){
        this.decay = decay;
    }

    /**
     *
     *
     */
    public double getInitStdDev() {
        return this.initStdDev;
    }
    public void setInitStdDev(double initStdDev){
        this.initStdDev = initStdDev;
    }

    /**
     *
     *
     */
    public int getNumFactors() {
        return this.numFactors;
    }
    public void setNumFactors(int numFactors){
        this.numFactors = numFactors;
    }

    /**
     *
     *
     */
    public int getNumIter() {
        return this.numIter;
    }
    public void setNumIter(int numIter){
        this.numIter = numIter;
    }


    /**
     * maxRating
     *
     */

    public double getMaxRating() {
        return this.maxRating;
    }
    public void setMaxRating(double maxRating){
        this.maxRating = maxRating;
    }

    /**
     * minRating
     *
     */

    public double getMinRating() {
        return this.minRating;
    }
    public void setMinRating(double minRating){
        this.minRating = minRating;
    }

    /**
     * global bias
     * @param globalMean
     * @return
     */
    public double getGlobalBias(double globalMean){
        double avg =  (globalMean - this.minRating) / (this.maxRating - this.minRating);
        this.globalBias = (double) Math.log(avg / (1 - avg));
        return this.globalBias;
    }

    protected void setGlobalBias(double globalBias){
       this.globalBias = globalBias;
    }


    /***
     *
     * @param newRatings
     */
    public void  addRatings(TableMatrix newRatings) throws LibrecException{

        Iterator<Table.Cell<Integer, Integer, Double>> it = newRatings.iterator();
        int userId, itemId;
        double ratingValue;
        while (it.hasNext()){
            Table.Cell<Integer, Integer, Double> ratingData = it.next();
            userId = ratingData.getRowKey();
            itemId = ratingData.getColumnKey();
            ratingValue = ratingData.getValue();
            if(userId > this.maxUserId){
                this.addUser(userId);
            }
            if(itemId > this.maxItemId){
                this.addItem(itemId);
            }
            // trainMatrix.plus(userId, itemId, ratingValue);
            // if necessary, new ratings to original rating data
            //ratingAdd(userId, itemId, ratingValue);
        }
    }

    /***
     *
     * @param newRatings
     */
    public void updateRatings(TableMatrix newRatings) throws LibrecException{
        Iterator<Table.Cell<Integer, Integer, Double>> it = newRatings.iterator();
        int userId, itemId;
        double ratingValue;
        while (it.hasNext()) {
            Table.Cell<Integer, Integer, Double> ratingData = it.next();
            userId = ratingData.getRowKey();
            itemId = ratingData.getColumnKey();
            ratingValue = ratingData.getValue();
            if (userId > this.maxUserId || itemId > this.maxItemId) {
                continue;
            }
            //ratingUpdate(userId, itemId, ratingValue);
        }
    }

    /***
     *
     * @param removeRatings
     */
    public void removeRatings(TableMatrix removeRatings)  throws LibrecException{
        Iterator<Table.Cell<Integer, Integer, Double>> it = removeRatings.iterator();
        int userId, itemId;
        double ratingValue;
        while (it.hasNext()) {
            Table.Cell<Integer, Integer, Double> ratingData = it.next();
            userId = ratingData.getRowKey();
            itemId = ratingData.getColumnKey();
            if (userId > this.maxUserId || itemId > this.maxItemId) {
                continue;
            }
            //ratingRemove(userId, itemId);
        }
    }


    /***
     *
     * @param userId
     */
    @Override
    protected void addUser(int userId) {
        if (userId > this.maxUserId){
           this.maxItemId = userId;
        }
    }

    @Override
    public void removeUser(int userId){
        if(userId == this.maxUserId){
            this.maxUserId --;
        }
        //RatingRemove(int userId);
    }

    /**
     *
     * @param itemId
     */
    @Override
    protected void addItem(int itemId){
        if(itemId > this.maxItemId){
            this.maxItemId = itemId;
        }
        //ratingAdd(itemId);
    }

    @Override
    public void removeItem(int itemId){
        if(itemId == this.maxItemId){
            this.maxItemId--;
        }
    }
}
