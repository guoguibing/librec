package net.librec.increment;

// put this class to the top

import net.librec.common.LibrecException;

public abstract class IncrementalMFRecommender extends IncrementalRatingRecommender {

    /**
     * learn rate, maximum learning rate
     */
    protected double learnRate, maxLearnRate;

    /**
     * user latent factors
     */
    protected TableMatrix userFactors;

    /**
     * item latent factors
     */
    protected TableMatrix itemFactors;

    /**
     * the number of latent factors;
     */
    protected int numFactors;

    /**
     *
     */
    protected int numIterations;

    /**
     * init mean
     */
    protected double initMean;

    /**
     * init standard deviation
     */
    protected double initStd;

    /**
     * user regularization
     */
    protected double regUser;

    /**
     * item regularization
     */
    protected double regItem;

    /**
     *
     */
    public double regularization;
    /**
     *
     */
    public double decay;

    /**
     * the number of iterations
     */
    public int numIter;

    /**
     *
     */
    public double initStdDev;


    /**
     *
     */
    protected double maxRating;

    /**
     *
     */
    protected double minRating;

    /**
     *
     */
    protected double globalBias;

    /**
     *
     */
    protected double currentLearnrate;


    /**
     *  construct function
     */
    public IncrementalMFRecommender() throws Exception {

        super();

        // set default values
        regularization = 0.015f;
        learnRate = 0.01f;
        decay = 1.0f;
        numIter = 2;
        initStdDev = 0.1;
        numFactors = 10;

    }

    /**
     * setup
     * init member method
     *
     * @throws LibrecException if error occurs during setting up
     */
    protected void setup() throws LibrecException {
        super.setup();

        numIterations = conf.getInt("rec.iterator.maximum", 100);
        learnRate = conf.getDouble("rec.iterator.learnrate", 0.01d);
        maxLearnRate = conf.getDouble("rec.iterator.learnrate.maximum", 1000.0d);

        regUser = conf.getDouble("rec.user.regularization", 0.01d);
        regItem = conf.getDouble("rec.item.regularization", 0.01d);

        numFactors = conf.getInt("rec.factor.number", 10);
        isBoldDriver = conf.getBoolean("rec.learnrate.bolddriver", false);
        decay = conf.getDouble("rec.learnrate.decay", 1.0d);

        userFactors = new TableMatrix(numUsers, numFactors);
        itemFactors = new TableMatrix(numItems, numFactors);

        initMean = 0.0d;
        initStd = 0.1d;

        // initialize factors
        userFactors.init(initMean, initStd);
        itemFactors.init(initMean, initStd);
    }

    /**
     *
     */
    protected void initModel () throws LibrecException{
        // extends AbstractRecommender
        setup();

        userFactors = new TableMatrix(numUsers, numFactors);
        itemFactors = new TableMatrix(numItems, numFactors);

        // initialize factors
        userFactors.init(initMean, initStd);
        itemFactors.init(initMean, initStd);

        this.currentLearnrate = this.learnRate;
    }

    /**
     * predict a specific rating for user userIdx on item itemIdx.
     *
     * @param userIdx user index
     * @param itemIdx item index
     * @return predictive rating for user userIdx on item itemIdx with bound
     * @throws LibrecException if error occurs during predicting
     */
    protected double predict(int userIdx, int itemIdx) throws LibrecException {
        //return DenseMatrix.rowMult(userFactors, userIdx, itemFactors, itemIdx);
        return TableMatrix.rowMult(userFactors, userIdx, itemFactors, itemIdx);
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
        if (this.currentLearnrate < 0.0) {
            lastLoss = loss;
            return;
        }

        if (isBoldDriver && iter > 1) {
            this.currentLearnrate = Math.abs(lastLoss) > Math.abs(loss) ? this.currentLearnrate * 1.05f : this.currentLearnrate * 0.5f;
        } else if (decay > 0 && decay < 1) {
            this.currentLearnrate *= decay;
        }

        // limit to max-learn-rate after update
        if (maxLearnRate > 0 &&  this.currentLearnrate > maxLearnRate) {
            this.currentLearnrate = maxLearnRate;
        }
        lastLoss = loss;
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

        return globalMean;
    }

    protected void setGlobalBias(double globalBias){
       this.globalBias = globalBias;
    }

}
