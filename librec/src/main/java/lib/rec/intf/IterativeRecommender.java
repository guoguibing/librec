package lib.rec.intf;

import happy.coding.io.Logs;
import happy.coding.io.Strings;
import lib.rec.data.DenseMat;
import lib.rec.data.SparseMat;

/**
 * Interface class for iterative recommenders such as Matrix Factorization
 * 
 * @author guoguibing
 * 
 */
public abstract class IterativeRecommender extends Recommender {

	// learning rate 
	protected double lRate, initLRate, momentum;
	// user and item regularization
	protected double regU, regI;
	// number of factors
	protected int numFactors;
	// number of iterations
	protected int maxIters;

	// whether to adjust learning rate automatically
	protected boolean isBoldDriver;

	// factorized user-factor matrix
	protected DenseMat P;

	// factorized item-factor matrix
	protected DenseMat Q;

	// training errors
	protected double errs, last_errs = 0;
	// objective loss
	protected double loss, last_loss = 0;

	public IterativeRecommender(SparseMat trainMatrix, SparseMat testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);

		lRate = cf.getDouble("val.learn.rate");
		initLRate = lRate; // initial learn rate
		momentum = cf.getDouble("val.momentum");
		regU = cf.getDouble("val.reg.user");
		regI = cf.getDouble("val.reg.item");

		numFactors = cf.getInt("num.factors");
		maxIters = cf.getInt("num.max.iter");

		isBoldDriver = cf.isOn("is.bold.driver");
	}

	/**
	 * default prediction method
	 */
	@Override
	protected double predict(int u, int j) {
		return DenseMat.rowMult(P, u, Q, j);
	}

	/**
	 * Post each iteration, we do things:
	 * 
	 * <ol>
	 * <li>print debug information</li>
	 * <li>adjust learning rate</li>
	 * <li>check if converged</li>
	 * </ol>
	 * 
	 * @param iter
	 *            current iteration
	 * 
	 * @return boolean: true if it is converged; false otherwise
	 * 
	 */
	protected boolean postEachIter(int iter) {

		if (verbose) {
			String foldInfo = fold > 0 ? " fold [" + fold + "]" : "";
			Logs.debug("{}{} iter {}: errs = {}, delta_errs = {}, loss = {}, delta_loss = {}, learn_rate = {}",
					new Object[] { algoName, foldInfo, iter, (float) errs, (float) (last_errs - errs), (float) loss,
							(float) (Math.abs(last_loss) - Math.abs(loss)), (float) lRate });
		}

		if (Double.isNaN(loss)) {
			Logs.error("Loss = NaN: current settings cannot train the recommender! Try other settings instead!");
			System.exit(-1);
		}

		// more ways to adapt learning rate can refer to: http://www.willamette.edu/~gorr/classes/cs449/momrate.html
		// The update rules refers to: 
		// (1) bold driver: Gemulla et al., Large-scale matrix factorization with distributed stochastic gradient descent, ACM KDD 2011.
		// (2) constant decay: Niu et al, Hogwild!: A lock-free approach to parallelizing stochastic gradient descent, NIPS 2011.
		// (3) Leon Bottou, Stochastic Gradient Descent Tricks
		double decay = cf.getDouble("val.decay.rate");
		if (isBoldDriver && last_loss != 0.0)
			lRate = Math.abs(last_loss) > Math.abs(loss) ? lRate * 1.05 : lRate * 0.5;
		else if (decay > 0 && decay < 1)
			lRate *= decay;
		else if (decay == 0)
			lRate = initLRate / (1 + initLRate * regU * iter);

		last_loss = loss;

		// check if converged
		boolean cond1 = (errs < 1e-5);
		boolean cond2 = (last_errs >= errs && last_errs - errs < 1e-5);
		last_errs = errs;

		return cond1 || cond2;
	}

	@Override
	protected void initModel() {

		P = new DenseMat(numUsers, numFactors);
		Q = new DenseMat(numItems, numFactors);

		// initialize model
		P.init(initMean, initStd);
		Q.init(initMean, initStd);

		// set to 0 for users without any ratings
		int numTrainUsers = trainMatrix.numRows();
		for (int u = 0, um = P.numRows(); u < um; u++) {
			if (u >= numTrainUsers || trainMatrix.rowSize(u) == 0) {
				P.setRow(u, 0.0);
			}
		}
		// set to 0 for items without any ratings
		int numTrainItems = trainMatrix.numColumns();
		for (int j = 0, jm = Q.numRows(); j < jm; j++) {
			if (j >= numTrainItems || trainMatrix.colSize(j) == 0) {
				Q.setRow(j, 0.0);
			}
		}
	}

	@Override
	public String toString() {
		double learnRate = cf.getDouble("val.learn.rate"); // re-get initial learn rate in case bold driver is used. 
		return Strings.toString(new Object[] { learnRate, regU, regI, numFactors, maxIters, isBoldDriver }, ",");
	}

}
