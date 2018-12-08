
package net.librec.increment.rating;

import com.google.common.collect.Table;
import net.librec.common.LibrecException;
import net.librec.increment.TableMatrix;
import net.librec.math.structure.DenseVector;
import net.librec.math.structure.MatrixEntry;
import net.librec.math.structure.SparseTensor;
import net.librec.math.structure.VectorBasedDenseVector;

import java.util.List;
import java.util.Map.Entry;

//import net.librec.math.algorithm.Shuffle;
//import net.librec.math.structure.SparseVector;

public class IncrementalBiasedMFRecommender extends IncrementalSimpleMFRecommender {

    /**
     *  max rating
     */
    protected double maxRating;

    /**
     *
     */
    protected double minRating;

    /**
     *
     */
    protected double ratingRangeSize;

    /**
     *  support max threads
     */
    public int maxThreads;

    /**
     *  loss target
     */
    protected OptimizationTarget lossTarget;

    /**
     * regularizarization based on rating frequency
     * as described in the paper by Menon and Elkan
     */
    protected boolean frequencyRegularization = false;

    /**
     * regularization constant for the user factors
     *
     */
    protected double regU;

    /**
     * regularization constant for the user factors
     *
     */
    protected double regI;

    /**
     * learn rate factor for the bias terms
     *
     * */
    protected  double biasLearnReg = 1.0d;

    /**
     * regularization factor for the bias terms
     *
     */
    protected  double biasReg = 0.01d;

    /**
     *
     */
    protected  String optTarget = "rmse";

    /**
     *
     */
    protected final int FOLD_IN_BIAS_INDEX = 0;

    /**
     *
     */
    protected final int FOLD_IN_FACTORS_START = 1;


    /**
     * user biases
     */
    protected TableMatrix userBiases;

    /**
     * item biases
     */
    protected TableMatrix itemBiases;


    /**
     * regularization factor for the bias terms
     * @throws Exception
     */
    public IncrementalBiasedMFRecommender() throws Exception{

        super();

    }

    @Override
    protected void setup() throws LibrecException{

        getGlobalBias(this.globalMean);

        super.setup();
    }

    @Override
    public void initModel () throws LibrecException {


        super.initModel();

        //initialize the userBiased and itemBiased
        userBiases = new TableMatrix(numUsers);
        itemBiases = new TableMatrix(numItems);

        userBiases.init(initMean, initStd);
        itemBiases.init(initMean, initStd);
    }

    /**
     *
     * @throws LibrecException
     */
    @Override
    public void trainModel() throws LibrecException {

        initModel();

//        if(maxThreads > 1){
//
//        }

        //the parameters from  IncrementalRatingRecommender
        this.ratingRangeSize = this.maxRating - this.minRating;
        double avg =  (this.globalMean - this.minRating) / (this.maxRating - this.minRating);
        this.globalBias = (double) Math.log(avg / (1 - avg));

        for(int iter = 0; iter < this.numIter; iter ++)
        {
            iterate(this.updateUsers, this.updateItems);
            //this.updateLearnRate();
            this.updateLRate(iter);
        }

    }

    /**
     *
     * @param updateUser
     * @param updateItem
     * @throws LibrecException
     */
    protected void iterate(boolean updateUser, boolean updateItem) throws LibrecException {

         for(MatrixEntry matrixEntry : trainMatrix) {
             // user userId
             int userId = matrixEntry.row();
             // item itemId
             int itemId = matrixEntry.column();
             // real rating on item itemId rated by user userId
             double realRating = matrixEntry.get();

             double score = globalBias + userBiases.get(userId) + itemBiases.get(itemId) + TableMatrix.rowMult(userFactors, userId, itemFactors, itemId) ;
             // kernel function (logistic)
             double sigScore = 1 / (1 + Math.exp(score));
             double prediction = minRating + sigScore * ratingRangeSize;

             double err = realRating -  prediction;
             double gradientCommon = this.computeGradientCommon(sigScore, err);

             double userRegWeight = frequencyRegularization ? (double) (regU / Math.sqrt(trainMatrix.rowSize())) : regU;
             double itemRegWeight = frequencyRegularization ? (double) (regI / Math.sqrt(trainMatrix.columnSize())) : regI;

             // update user biases
             if(updateUser){
                 userBiases.add(userId, this.biasLearnReg * currentLearnrate * (gradientCommon - biasReg * userRegWeight * userBiases.get(userId)));
             }
             // update item biases
             if(updateItem){
                 itemBiases.add(itemId, this.biasLearnReg * currentLearnrate * (gradientCommon - biasReg * itemRegWeight * itemBiases.get(itemId)));
             }

             // update user and item factors
             for (int f = 0; f < this.numFactors; f++){

                 double userFactorValue = userFactors.get(userId, f);
                 double itemFactorValue  = itemFactors.get(itemId, f);

                 if(updateUser){
                     double deltaU = gradientCommon * itemFactorValue - userRegWeight * userFactorValue;
                     userFactors.add(userId, f, currentLearnrate * deltaU);
                 }
                 if(updateItem){
                    double deltaI = gradientCommon * userFactorValue - itemRegWeight * itemFactorValue;
                    itemFactors.add(userId, f, currentLearnrate * deltaI);
                 }

             }
         }
    }

    /**
     *
     * @param sigScore
     * @param err
     * @return
     */
    protected double computeGradientCommon(double sigScore,  double err){
        // default loss target is RMSE
        lossTarget = lossTarget(this.optTarget);
        return setupLoss(sigScore, err);
    }

    /**
     *
     * @param sigScore
     * @param err
     * @return
     */
    protected double setupLoss(double sigScore, double err){

        double gradientCommon = 0.0;
        switch (lossTarget){
            case RMSE:
                gradientCommon =  (double)(Math.signum(err) * sigScore * (1 - sigScore) * ratingRangeSize);
            case MSE:
                gradientCommon =  (double)(err * sigScore * (1 - sigScore) * ratingRangeSize);
            case LogisticLoss:
                gradientCommon = (double)err;
            default:
                gradientCommon =  (double)(Math.signum(err) * sigScore * (1 - sigScore) * ratingRangeSize);

        }
        return gradientCommon;
    }

    /***
     *
     * @param ratedItems
     *  Entry<Integer, Double> itemId, rating
     * @return
     * @throws LibrecException
     */
    @Override
    protected  DenseVector foldIn(List<Entry<Integer, Double>> ratedItems) throws LibrecException{

        double userBias = 0;
        DenseVector userFactor = new VectorBasedDenseVector(this.numFactors);
        userFactor.init(initMean, initStdDev);

        double userRegWeight = frequencyRegularization ? (double) (this.regU / Math.sqrt(ratedItems.size())) : this.regU;
        //ratedItems = Shuffle.shuffleEntryList(ratedItems);

        for (int iter = 0; iter > this.numIter; iter++){

            for (int index =0; index < ratedItems.size(); index++){
                int itemId = ratedItems.get(index).getKey();
                int itemRealRating = ratedItems.get(index).getKey();

                // 1. compute rating and error

                //DenseVector itemFactor = itemFactors.row(itemId);
                List<Double> itemFactorList = itemFactors.row(itemId);
                // List to array
                double[] itemFactorArr = itemFactorList.stream().mapToDouble(Double::doubleValue).toArray();
                DenseVector itemFactor = new VectorBasedDenseVector(itemFactorArr);

                //double score = this.globalBias + userBias + itemBiases.get(itemId) + userFactor.inner(itemFactor) ;
                double score = this.globalBias + userBias + itemBiases.get(itemId) + userFactor.dot(itemFactor) ;

                double sigScore = 1 / (1 + Math.exp(score));
                double prediction = minRating + sigScore * ratingRangeSize;

                double err = itemRealRating -  prediction;
                double gradientCommon = this.computeGradientCommon(sigScore, err);

                // 2. adjust bias
                userBias += this.biasLearnReg * this.learnRate * (gradientCommon - biasReg * userRegWeight * userBias);

                // 3. adjust userFactor
                for (int f = 0; f < this.numFactors; f++){
                    double userFactorValue = userFactor.get(f);
                    double itemFactorValue  = itemFactors.get(itemId, f);

                    double deltaU = gradientCommon * itemFactorValue - userRegWeight * userFactorValue;
                    userFactor.set(f, this.learnRate * deltaU);
                }
            }

        }
        //DenseVector userVector = new DenseVector(this.numFactors +1);
        //userVector.set(FOLD_IN_BIAS_INDEX, userBias);
        DenseVector userVector = userFactor.clone();
        return userVector;
    }

     /**
     * predict a specific rating for user userId on item itemId.
     *
     * @param userId user index
     * @param itemId item index
     * @return predictive rating for user userId on item itemId
     * @throws LibrecException if error occurs
     */
    @Override
    protected double predict(int userId, int itemId) throws LibrecException {
       double score = this.globalBias;
       //userBiases.size ?
       if(userId < this.numUsers){
           score += userBiases.get(userId);
       }
       if(itemId < this.numIter){
           score += itemBiases.get(itemId);
       }
       if(userId < userFactors.rowSize() && itemId < itemFactors.columnSize())
           score += TableMatrix.rowMult(userFactors, userId, itemFactors, itemId);

       return  (double) (this.minRating + (1 / (1 + Math.exp(-score))) * this.ratingRangeSize);

    }

    /**
     *
     * @param userVector
     * @param itemId
     *
     * @throws LibrecException
     */
    @Override
    protected double predict(DenseVector userVector, int itemId) throws LibrecException {

        // FOLD_IN_FACTORS_START
        // FOLD_IN_BIAS_INDEX
        DenseVector userFactor = userVector;
        // this.numFactors replaced by userVector

        double score = this.globalBias + userVector.get(0);
        //
        if(itemId < this.numUsers){
            //DenseVector itemFactor = itemFactors.row(itemId);
            List<Double> itemFactorList = itemFactors.row(itemId);
            // List to array
            double[] itemFactorArr = itemFactorList.stream().mapToDouble(Double::doubleValue).toArray();
            DenseVector itemFactor = new VectorBasedDenseVector(itemFactorArr);
            score += userFactor.dot(itemFactor);
        }

        return (double)(minRating + 1 / (1 + Math.exp(-score) * ratingRangeSize));
    }

    /**
     * global bias
     * @param globalMean
     * @return
     */
    @Override
    public double getGlobalBias(double globalMean){
        double avg =  (globalMean - this.minRating) / (this.maxRating - this.minRating);
        this.globalBias = (double) Math.log(avg / (1 - avg));
        return this.globalBias;
    }

    /**
     *
     * @param iterRatingData
     * @param itemValues
     * @throws LibrecException
     */
    protected void reTrianUser(Table.Cell<Integer, Integer, Double> iterRatingData, SparseTensor itemValues) throws LibrecException {
        // 1. incremental data train (ratingData)
        int userId = iterRatingData.getRowKey();
        userBiases.set(userId, 0);
        super.reTrianUser(iterRatingData, itemValues);
    }

    /**
     *
     * @param iterRatingData
     * @param itemValues
     * @throws LibrecException
     */
    protected void reTrianItem(Table.Cell<Integer, Integer, Double> iterRatingData, SparseTensor itemValues) throws LibrecException {
        int itemId = iterRatingData.getColumnKey();
        itemFactors.set(itemId, 0);
        super.reTrianUser(iterRatingData, itemValues);
    }


    /***
     *
     * @param userId
     */
    @Override
    protected void addUser(int userId){

        super.addUser(userId);

        userBiases.add(this.maxUserId + 1);
    }

    /**
     *
     * @param itemId
     */
    @Override
    protected void addItem(int itemId){
        super.addItem(itemId);

        // resize user bias
        itemBiases.add(this.maxItemId + 1);
    }


    /**
     *
     * @param userId
     */

    @Override
    public void removeUser(int userId){
        userBiases.set(userId, 0);
        super.removeUser(userId);

    }

    /**
     *
     * @param itemId
     */
    @Override
    public void removeItem(int itemId){
        itemBiases.set(itemId, 0);
        super.removeItem(itemId);
    }



}
