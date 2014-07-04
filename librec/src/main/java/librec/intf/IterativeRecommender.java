// Copyright (C) 2014 Guibing Guo
//
// This file is part of LibRec.
//
// LibRec is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// LibRec is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with LibRec. If not, see <http://www.gnu.org/licenses/>.
//

package librec.intf;

import happy.coding.io.Logs;
import happy.coding.io.Strings;
import librec.data.DenseMatrix;
import librec.data.DenseVector;
import librec.data.SparseMatrix;

/**
 * Recommenders using iterative learning techniques
 * 
 * @author guoguibing
 * 
 */
public abstract class IterativeRecommender extends Recommender {

	/************************************ Static parameters for all recommenders ***********************************/
	// init, maximum learning rate, momentum
	protected static double initLRate, maxLRate, momentum;
	// user and item regularization
	protected static double regU, regI;
	// number of factors
	protected static int numFactors;
	// number of iterations
	protected static int numIters;

	// whether to adjust learning rate automatically
	protected static boolean isBoldDriver;
	// whether to undo last weight changes if negative loss observed when bold
	// driving
	protected static boolean isUndoEnabled;
	// decay of learning rate
	protected static double decay;

	/************************************ Recommender-specific parameters ****************************************/
	// factorized user-factor matrix
	protected DenseMatrix P, last_P;

	// factorized item-factor matrix
	protected DenseMatrix Q, last_Q;

	// user biases
	protected DenseVector userBiases, last_UB;
	// item biases
	protected DenseVector itemBiases, last_IB;

	// adaptive learn rate
	protected double lRate;
	// training errors
	protected double errs, last_errs = 0;
	// objective loss
	protected double loss, last_loss = 0;

	// initial models using normal distribution
	protected boolean initByNorm;

	// initialization
	static {
		initLRate = cf.getDouble("val.learn.rate");
		maxLRate = cf.getDouble("max.learn.rate");
		momentum = cf.getDouble("val.momentum");

		// to support multiple tests in one time in future
		regU = cf.getRange("val.reg.user").get(0);
		regI = cf.getRange("val.reg.item").get(0);

		numFactors = cf.getInt("num.factors");
		numIters = cf.getInt("num.max.iter");

		isBoldDriver = cf.isOn("is.bold.driver");
		isUndoEnabled = cf.isOn("is.undo.change");

		decay = cf.getDouble("val.decay.rate");
	}

	public IterativeRecommender(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);

		lRate = initLRate;
		initByNorm = true;
	}

	/**
	 * default prediction method
	 */
	@Override
	protected double predict(int u, int j) {
		return DenseMatrix.rowMult(P, u, Q, j);
	}

	/**
	 * Post each iteration, we do things:
	 * 
	 * <ol>
	 * <li>print debug information</li>
	 * <li>check if converged</li>
	 * <li>if not, adjust learning rate</li>
	 * </ol>
	 * 
	 * @param iter
	 *            current iteration
	 * 
	 * @return boolean: true if it is converged; false otherwise
	 * 
	 */
	protected boolean isConverged(int iter) {

		// print out debug info
		if (verbose) {
			Logs.debug("{}{} iter {}: errs = {}, delta_errs = {}, loss = {}, delta_loss = {}, learn_rate = {}",
					new Object[] { algoName, foldInfo, iter, (float) errs, (float) (last_errs - errs), (float) loss,
							(float) (Math.abs(last_loss) - Math.abs(loss)), (float) lRate });
		}

		if (!(isBoldDriver && isUndoEnabled) && Double.isNaN(loss)) {
			Logs.error("Loss = NaN: current settings cannot train the recommender! Try other settings instead!");
			System.exit(-1);
		}

		// check if converged
		boolean cond1 = (errs < 1e-5);
		boolean cond2 = (last_errs >= errs && last_errs - errs < 1e-5);
		boolean converged = cond1 || cond2;

		// if not converged, update learning rate
		if (!converged && lRate > 0)
			updateLRate(iter);

		last_loss = loss;
		last_errs = errs;

		return converged;
	}

	/**
	 * Update current learning rate after each epoch <br/>
	 * 
	 * <ol>
	 * <li>bold driver: Gemulla et al., Large-scale matrix factorization with
	 * distributed stochastic gradient descent, KDD 2011.</li>
	 * <li>constant decay: Niu et al, Hogwild!: A lock-free approach to
	 * parallelizing stochastic gradient descent, NIPS 2011.</li>
	 * <li>Leon Bottou, Stochastic Gradient Descent Tricks</li>
	 * <li>more ways to adapt learning rate can refer to:
	 * http://www.willamette.edu/~gorr/classes/cs449/momrate.html</li>
	 * </ol>
	 * 
	 * @param iter
	 *            the current iteration
	 */
	protected void updateLRate(int iter) {
		if (isBoldDriver && last_loss != 0.0) {
			if (Math.abs(last_loss) > Math.abs(loss)) {
				lRate *= 1.05;

				// update last weight changes
				if (isUndoEnabled)
					updates();
			} else {
				lRate *= 0.5;

				if (isUndoEnabled) {
					// undo last weight changes
					undos(iter);
					// do not update last loss and errors, since we discard
					// current loss and errors
					return;
				}
			}
		} else if (decay > 0 && decay < 1)
			lRate *= decay;
		else if (decay == 0)
			lRate = initLRate / (1 + initLRate * ((regU + regI) / 2.0) * iter);

		// limit to max-learn-rate after update
		if (maxLRate > 0 && lRate > maxLRate)
			lRate = maxLRate;
	}

	/**
	 * updates last weights
	 */
	protected void updates() {
		if (P != null)
			last_P = P.clone();
		if (Q != null)
			last_Q = Q.clone();
		if (userBiases != null)
			last_UB = userBiases.clone();
		if (itemBiases != null)
			last_IB = itemBiases.clone();
	}

	/**
	 * undo last weight changes
	 */
	protected void undos(int iter) {
		Logs.debug("{}{} iter {}: undo last weight changes and sharply decrease the learning rate !", algoName,
				foldInfo, iter);

		if (last_P != null)
			P = last_P.clone();
		if (last_Q != null)
			Q = last_Q.clone();
		if (last_UB != null)
			userBiases = last_UB.clone();
		if (last_IB != null)
			itemBiases = last_IB.clone();
	}

	@Override
	protected void initModel() {

		P = new DenseMatrix(numUsers, numFactors);
		Q = new DenseMatrix(numItems, numFactors);

		// initialize model
		if (initByNorm) {
			P.init(initMean, initStd);
			Q.init(initMean, initStd);
		} else {
			P.init();
			Q.init();
		}

	}

	@Override
	public String toString() {
		return Strings.toString(new Object[] { initLRate, maxLRate, (float) regU, (float) regI, numFactors, numIters,
				isBoldDriver }, ",");
	}

}
