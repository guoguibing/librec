package librec.intf;

import happy.coding.io.LineConfiger;
import happy.coding.io.Logs;
import happy.coding.io.Strings;
import librec.data.Configuration;
import librec.data.DenseMatrix;
import librec.data.DenseVector;
import librec.data.SparseMatrix;

import com.google.common.collect.Table;

/**
 * Probabilistic Graphic Models
 * 
 * @author guoguibing
 * 
 */
@Configuration("factors, iters, burn.in, sample.lag, alpha, beta")
public class GraphicRecommender extends Recommender {

	/**
	 * line configer for general probabilistic graphic models
	 */
	protected static LineConfiger pgm;

	/**
	 * number of topics
	 */
	protected static int numFactors;

	/**
	 * Dirichlet hyper-parameters of user-topic distribution: typical value is 50/K
	 */
	protected static double initAlpha;

	/**
	 * Dirichlet hyper-parameters of topic-item distribution, typical value is 0.01
	 */
	protected static double initBeta;
	/**
	 * burn-in period
	 */
	protected static int burnIn;

	/**
	 * sample lag (if -1 only one sample taken)
	 */
	protected static int sampleLag;

	/**
	 * maximum number of iterations
	 */
	protected static int numIters;

	/**
	 * intervals for printing verbose information
	 */
	protected static int numIntervals;

	/*********************************** Method-specific Parameters ************************/

	/**
	 * entry[u,i]: topic assignment as sparse structure
	 */
	protected Table<Integer, Integer, Integer> z;

	/**
	 * entry[i,t]: number of instances of item i assigned to topic t.
	 */
	protected DenseMatrix Nik;

	/**
	 * entry[u,t]: number of items of user u assigned to topic t.
	 */
	protected DenseMatrix Nuk;

	/**
	 * entry[t]: total number of items assigned to topic t.
	 */
	protected DenseVector Nk;

	/**
	 * entry[u]: total number of items rated by user u.
	 */
	protected DenseVector Nu;

	/**
	 * entry[i]: total number of users having rated item i.
	 */
	protected DenseVector Ni;

	/**
	 * vector of hyperparameters for alpha and beta
	 */
	protected DenseVector alpha, beta;

	/**
	 * cumulative statistics of theta, phi
	 */
	protected DenseMatrix thetaSum, phiSum;

	/**
	 * posterior probabilities of parameters
	 * 
	 */
	protected DenseMatrix theta, phi;

	/**
	 * size of statistics
	 */
	protected int numStats = 0;

	/**
	 * objective loss
	 */
	protected double loss;

	static {

		pgm = cf.getParamOptions("pgm.setup");
		burnIn = pgm.getInt("-burn-in");
		sampleLag = pgm.getInt("-sample-lag");
		initAlpha = pgm.getDouble("-alpha", 1.0 / numFactors);
		initBeta = pgm.getDouble("-beta", 1.0 / numFactors);
		numIntervals = pgm.getInt("-interval");

		assert burnIn > 0;
		assert sampleLag > 0;

		numFactors = cf.getInt("num.factors");
		numIters = cf.getInt("num.max.iter");
	}

	public GraphicRecommender(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);
	}

	@Override
	protected void buildModel() throws Exception {

		for (int iter = 1; iter <= numIters; iter++) {

			// E-step: infer parameters
			eStep();

			// M-step: update hyper-parameters
			mStep();

			// get statistics after burn-in
			if ((iter > burnIn) && (iter % sampleLag == 0)) {
				readoutParams();

				if (isConverged(iter))
					break;
			}

			if (verbose && (iter % numIntervals == 0))
				Logs.debug("{}{} runs at iter {}/{}", algoName, foldInfo, iter, numIters);
		}

		// retrieve posterior probability distributions
		postProbDistr();

	}

	/**
	 * update the hyper-parameters
	 */
	protected void mStep() {
	}

	/**
	 * employing early stopping criteria
	 * 
	 * @param iter
	 *            current iteration
	 */
	protected boolean isConverged(int iter) throws Exception {
		return false;
	}

	/**
	 * retrieve the posterior probability distributions
	 */
	protected void postProbDistr() {
	}

	/**
	 * parameters estimation: used in the training phase
	 */
	protected void eStep() {
	}

	/**
	 * parameters inference: used if new user arrives in the test phase
	 */
	protected void inference() {
	}

	/**
	 * read out parameters for each iteration
	 */
	protected void readoutParams() {
	}

	@Override
	public String toString() {
		return Strings.toString(new Object[] { numFactors, numIters, burnIn, sampleLag, initAlpha, initBeta }, ", ");
	}

}
