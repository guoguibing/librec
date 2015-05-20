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
@Configuration("iters, burn.in, sample.lag")
public class GraphicRecommender extends Recommender {

	/**
	 * line configer for general probabilistic graphic models
	 */
	protected static LineConfiger pgmOptions;

	/**
	 * number of topics
	 */
	protected static int numFactors;

	/**
	 * Dirichlet hyper-parameters of user-topic distribution: typical value is 50/K
	 */
	protected static float initAlpha;

	/**
	 * Dirichlet hyper-parameters of topic-item distribution, typical value is 0.01
	 */
	protected static float initBeta;
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

	// indicator of initialization of the general recommender
	public static boolean isInitialized = false;

	/*********************************** Method-specific Parameters ************************/

	/**
	 * entry[u, i, k]: topic assignment as sparse structure
	 */
	protected Table<Integer, Integer, Integer> z;

	/**
	 * entry[i, k]: number of tokens assigned to topic k, given item i.
	 */
	protected DenseMatrix Nik;
	
	/**
	 * entry[k, i]: number of tokens assigned to topic k, given item i.
	 */
	protected DenseMatrix Nki;

	/**
	 * entry[u, k]: number of tokens assigned to topic k, given user u.
	 */
	protected DenseMatrix Nuk;

	/**
	 * entry[k]: number of tokens assigned to topic t.
	 */
	protected DenseVector Nk;

	/**
	 * entry[u]: number of tokens rated by user u.
	 */
	protected DenseVector Nu;

	/**
	 * entry[i]: number of tokens rating item i.
	 */
	protected DenseVector Ni;

	/**
	 * vector of hyperparameters for alpha and beta
	 */
	protected DenseVector alpha, beta;

	/**
	 * cumulative statistics of theta, phi
	 */
	protected DenseMatrix PukSum, PikSum, PkiSum;

	/**
	 * posterior probabilities of parameters
	 * 
	 */
	protected DenseMatrix Puk, Pki, Pik;

	/**
	 * size of statistics
	 */
	protected int numStats = 0;

	/**
	 * objective loss
	 */
	protected double loss, lastLoss;

	public GraphicRecommender(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);

		if (!isInitialized) {
			isInitialized = true;
			
			numFactors = cf.getInt("num.factors", 10);
			numIters = cf.getInt("num.max.iter", 30);

			pgmOptions = cf.getParamOptions("pgm.setup");
			if (pgmOptions != null) {
				burnIn = pgmOptions.getInt("-burn-in");
				sampleLag = pgmOptions.getInt("-sample-lag");
				numIntervals = pgmOptions.getInt("-interval");

				initAlpha = pgmOptions.getFloat("-alpha", 1.0f / numFactors);
				initBeta = pgmOptions.getFloat("-beta", 1.0f / numFactors);

				assert burnIn > 0;
				assert sampleLag > 0;
			}
		}
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
		estimateParams();

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
	 * estimate the model parameters
	 */
	protected void estimateParams() {
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
		return Strings.toString(new Object[] { numIters, burnIn, sampleLag }, ", ");
	}

}
