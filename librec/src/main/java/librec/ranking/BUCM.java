package librec.ranking;

import happy.coding.io.Logs;
import librec.data.AddConfiguration;
import librec.data.DenseMatrix;
import librec.data.DenseVector;
import librec.data.MatrixEntry;
import librec.data.SparseMatrix;
import librec.intf.GraphicRecommender;

import com.google.common.collect.HashBasedTable;

/**
 * Bayesian UCM: Nicola Barbieri et al., <strong>Modeling Item Selection and Relevance for Accurate Recommendations: a
 * Bayesian Approach</strong>, RecSys 2011.
 * 
 * @author Guo Guibing
 *
 */
@AddConfiguration("gamma")
public class BUCM extends GraphicRecommender {

	private double preRMSE;
	private float gamma;

	public BUCM(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);
	}

	/**
	 * number of occurrences of entry (t, i, r)
	 */
	private int[][][] Nkir;

	/**
	 * cumulative statistics of probabilities of (t, i, r)
	 */
	private double[][][] epsilonSum;

	/**
	 * posterior probabilities of parameters epsilon_{k, i, r}
	 */
	protected double[][][] epsilon;

	@Override
	protected void initModel() throws Exception {

		// cumulative parameters
		thetaSum = new DenseMatrix(numUsers, numFactors);
		phiSum = new DenseMatrix(numItems, numFactors);
		epsilonSum = new double[numFactors][numItems][numLevels];

		// initialize count variables
		Nuk = new DenseMatrix(numUsers, numFactors);
		Nu = new DenseVector(numUsers);

		Nik = new DenseMatrix(numItems, numFactors);
		Nk = new DenseVector(numFactors);

		Nkir = new int[numFactors][numItems][numLevels];

		gamma = algoOptions.getFloat("-gamma");

		// initialize topics
		z = HashBasedTable.create();
		for (MatrixEntry me : trainMatrix) {

			int u = me.row();
			int i = me.column();
			double rui = me.get();

			int r = (int) (rui / minRate - 1); // rating level 0 ~ numLevels
			int t = (int) (Math.random() * numFactors); // 0 ~ k-1

			// assign a topic t to pair (u, i)
			z.put(u, i, t);
			// for users
			Nuk.add(u, t, 1);
			Nu.add(u, 1);
			// for items
			Nik.add(i, t, 1);
			Nk.add(t, 1);
			// for ratings
			Nkir[t][i][r]++;
		}

	}

	@Override
	protected void inferParams() {

		// collapse Gibbs sampling
		for (MatrixEntry me : trainMatrix) {
			int u = me.row();
			int i = me.column();
			double rui = me.get();

			int r = (int) (rui / minRate - 1); // rating level 0 ~ numLevels
			int t = z.get(u, i);

			Nuk.add(u, t, -1);
			Nu.add(u, -1);
			Nik.add(i, t, -1);
			Nk.add(t, -1);
			Nkir[t][i][r]--;

			// do multinomial sampling via cumulative method:
			double[] p = new double[numFactors];
			double v1, v2, v3;
			for (int k = 0; k < numFactors; k++) {

				v1 = (Nuk.get(u, k) + alpha) / (Nu.get(u) + numFactors * alpha);

				v2 = (Nik.get(i, k) + beta) / (Nk.get(t) + numItems * beta);

				v3 = (Nkir[k][i][r] + gamma) / (Nik.get(i, t) + numLevels * gamma);

				p[k] = v1 * v2 * v3;
			}
			// cumulate multinomial parameters
			for (int k = 1; k < p.length; k++) {
				p[k] += p[k - 1];
			}
			// scaled sample because of unnormalized p[], randomly sampled a new topic t
			double rand = Math.random() * p[numFactors - 1];
			for (t = 0; t < p.length; t++) {
				if (rand < p[t])
					break;
			}

			// new topic t
			z.put(u, i, t);

			// add newly estimated z_i to count variables
			Nuk.add(u, t, 1);
			Nu.add(u, 1);
			Nik.add(i, t, 1);
			Nk.add(t, 1);
			Nkir[t][i][r]++;
		}
	}

	protected void readoutParams() {
		double val = 0;
		for (int u = 0; u < numUsers; u++) {
			for (int k = 0; k < numFactors; k++) {
				val = (Nuk.get(u, k) + alpha) / (Nu.get(u) + numFactors * alpha);
				thetaSum.add(u, k, val);
			}
		}

		for (int i = 0; i < numItems; i++) {
			for (int k = 0; k < numFactors; k++) {
				val = (Nik.get(i, k) + beta) / (Nk.get(k) + numItems * beta);
				phiSum.add(i, k, val);
			}
		}

		for (int k = 0; k < numFactors; k++) {
			for (int i = 0; i < numItems; i++) {
				for (int r = 0; r < numLevels; r++) {
					val = (Nkir[k][i][r] + gamma) / (Nik.get(i, k) + numLevels * gamma);
					epsilonSum[k][i][r] += val;
				}
			}
		}
		numStats++;
	}

	@Override
	protected void postProbDistr() {
		theta = thetaSum.scale(1.0 / numStats);
		phi = phiSum.scale(1.0 / numStats);

		epsilon = new double[numFactors][numItems][numLevels];
		for (int k = 0; k < numFactors; k++) {
			for (int i = 0; i < numItems; i++) {
				for (int r = 0; r < numLevels; r++) {
					epsilon[k][i][r] = epsilonSum[k][i][r] / numStats;
				}
			}
		}
	}

	@Override
	protected boolean isConverged(int iter) throws Exception {

		if (validationMatrix == null)
			return false;

		// get posterior probability distribution first
		postProbDistr();

		// compute current RMSE
		int numCount = 0;
		double sum = 0;
		for (MatrixEntry me : validationMatrix) {
			double rate = me.get();

			int u = me.row();
			int j = me.column();

			double pred = predict(u, j, true);
			if (Double.isNaN(pred))
				continue;

			double err = rate - pred;

			sum += err * err;
			numCount++;
		}

		double RMSE = Math.sqrt(sum / numCount);
		double delta = RMSE - preRMSE;

		if (verbose) {
			Logs.debug("{}{} iter {} achieves RMSE = {}, delta_RMSE = {}", algoName, foldInfo, iter, (float) RMSE,
					(float) (delta));
		}

		if (numStats > 1 && delta > 0)
			return true;

		preRMSE = RMSE;
		return false;
	}

	@Override
	protected double predict(int u, int i) throws Exception {
		double pred = 0;

		for (int r = 0; r < numLevels; r++) {
			double rate = (r + 1) * minRate;

			double prob = 0;
			for (int k = 0; k < numFactors; k++) {
				prob += theta.get(u, k) * epsilon[k][i][r];
			}

			pred += prob * rate;
		}

		return pred;
	}

	@Override
	protected double ranking(int u, int j) throws Exception {
		double rank = 0;

		for (int k = 0; k < numFactors; k++) {

			double sum = 0;
			for (int r = 0; r < numLevels; r++) {
				double rate = (r + 1) * minRate;
				if (rate > globalMean) {
					sum += epsilon[k][j][r];
				}
			}

			rank += theta.get(u, k) * phi.get(j, k) * sum;
		}

		return rank;
	}

	@Override
	public String toString() {
		return super.toString() + ", " + gamma;
	}
}
