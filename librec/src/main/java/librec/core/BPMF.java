package librec.core;

import librec.data.SparseMatrix;
import librec.data.SparseVector;
import librec.intf.IterativeRecommender;

/**
 * Salakhutdinov and Mnih, <strong>Bayesian Probabilistic Matrix Factorization
 * using Markov Chain Monte Carlo</strong>, ICML 2008. <br/>
 * 
 * Matlab version is provided by the authors via <a
 * href="http://www.utstat.toronto.edu/~rsalakhu/BPMF.html">this link</a>.
 * 
 * @author guoguibing
 * 
 */
public class BPMF extends IterativeRecommender {

	public BPMF(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);

		algoName = "BayesianPMF";
	}

	@Override
	protected void buildModel() {
		// Initialize hierarchical priors
		int beta = 2; // observation noise (precision)
		SparseVector mu_u = new SparseVector(numFactors);
		SparseVector mu_m = new SparseVector(numFactors);
		SparseMatrix alpha_u = SparseMatrix.eye(numFactors);
		SparseMatrix alpha_m = SparseMatrix.eye(numFactors);

		// parameters of Inv-Whishart distribution
		SparseMatrix WI_u = SparseMatrix.eye(numFactors);
		int b0_u = 2;
		int df_u = numFactors;
		SparseVector mu0_u = new SparseVector(numFactors);

		SparseMatrix WI_m = SparseMatrix.eye(numFactors);
		int b0_m = 2;
		int df_m = numFactors;
		SparseVector mu0_m = new SparseVector(numFactors);
	}

}
