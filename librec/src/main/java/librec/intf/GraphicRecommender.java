package librec.intf;

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
@Configuration("factors, alpha, beta, iters, burn.in, sample.lag")
public class GraphicRecommender extends Recommender {

	/**
	 * number of topics
	 */
	protected static int numFactors;

	/**
	 * Dirichlet hyper-parameters of user-topic distribution: typical value is 50/K
	 */
	protected static double alpha;

	/**
	 * Dirichlet hyper-parameters of topic-item distribution, typical value is 0.01
	 */
	protected static double beta;
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
	protected DenseMatrix Nit;

	/**
	 * entry[u,t]: number of items of user u assigned to topic t.
	 */
	protected DenseMatrix Nut;

	/**
	 * entry[t]: total number of items assigned to topic t.
	 */
	protected DenseVector Ni;

	/**
	 * entry[u]: total number of items rated by user u.
	 */
	protected DenseVector Nu;

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

	static {
		burnIn = cf.getInt("num.burn.in");
		sampleLag = cf.getInt("num.sample.lag");

		assert burnIn > 0;
		assert sampleLag > 0;

		alpha = cf.getDouble("val.init.alpha");
		beta = cf.getDouble("val.init.beta");

		numFactors = cf.getInt("num.factors");
		numIters = cf.getInt("num.max.iter");
		numIntervals = cf.getInt("num.verbose.interval");
	}

	public GraphicRecommender(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);
	}

	@Override
	protected void buildModel() throws Exception {

		for (int iter = 1; iter <= numIters; iter++) {

			// infer parameters
			inferParams();

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
	 * parameters inference/estimation
	 */
	protected void inferParams() {
	}

	/**
	 * read out parameters for each iteration
	 */
	protected void readoutParams() {
	}

	@Override
	public String toString() {
		return Strings.toString(new Object[] { numFactors, alpha, beta, numIters, burnIn, sampleLag }, ", ");
	}

}
