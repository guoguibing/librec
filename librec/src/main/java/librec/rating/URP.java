package librec.rating;

import static happy.coding.math.Gamma.digamma;
import happy.coding.io.Logs;
import librec.data.DenseMatrix;
import librec.data.DenseVector;
import librec.data.MatrixEntry;
import librec.data.SparseMatrix;
import librec.intf.GraphicRecommender;

import com.google.common.collect.HashBasedTable;

/**
 * User Rating Profile: a LDA model for rating prediction. <br>
 * 
 * Benjamin Marlin, <strong>Modeling user rating profiles for collaborative filtering</strong>, NIPS 2003.<br>
 * 
 * Nicola Barbieri, <strong>Regularized gibbs sampling for user profiling with soft constraints</strong>, ASONAM 2011.
 * 
 * @author Guo Guibing
 *
 */
public class URP extends GraphicRecommender {

	private double preRMSE;

	public URP(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);
	}

	/**
	 * number of occurrences of entry (t, i, r)
	 */
	private int[][][] Ntir;

	/**
	 * cumulative statistics of probabilities of (t, i, r)
	 */
	private double[][][] phiSum;

	/**
	 * posterior probabilities of parameters phi_{k, i, r}
	 */
	protected double[][][] phi;

	@Override
	protected void initModel() throws Exception {

		// cumulative parameters
		thetaSum = new DenseMatrix(numUsers, numFactors);
		phiSum = new double[numFactors][numItems][numLevels];

		// initialize count variables
		Nuk = new DenseMatrix(numUsers, numFactors);
		Nu = new DenseVector(numUsers);

		Ntir = new int[numFactors][numItems][numLevels];
		Nik = new DenseMatrix(numItems, numFactors);

		alpha = new DenseVector(numFactors);
		alpha.setAll(initAlpha);

		beta = new DenseVector(numLevels);
		beta.setAll(initBeta);

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
			// number of pairs (u, t) in (u, i, t)
			Nuk.add(u, t, 1);
			// total number of items of user u
			Nu.add(u, 1);

			// number of pairs (t, i, r)
			Ntir[t][i][r]++;
			// total number of words assigned to topic t
			Nik.add(i, t, 1);
		}

	}

	@Override
	protected void eStep() {

		double sumAlpha = alpha.sum();
		double sumBeta = beta.sum();

		// collapse Gibbs sampling
		for (MatrixEntry me : trainMatrix) {
			int u = me.row();
			int i = me.column();
			double rui = me.get();

			int r = (int) (rui / minRate - 1); // rating level 0 ~ numLevels
			int t = z.get(u, i);

			Nuk.add(u, t, -1);
			Nu.add(u, -1);
			Ntir[t][i][r]--;
			Nik.add(i, t, -1);

			// do multinomial sampling via cumulative method:
			double[] p = new double[numFactors];
			for (int k = 0; k < numFactors; k++) {
				p[k] = (Nuk.get(u, k) + alpha.get(k)) / (Nu.get(u) + sumAlpha) * (Ntir[k][i][r] + beta.get(r)) / (Nik.get(i, k) + sumBeta);
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
			Ntir[t][i][r]++;
			Nik.add(i, t, 1);
		}
	}

	/**
	 * Thomas P. Minka, Estimating a Dirichlet distribution, see Eq.(55)
	 */
	@Override
	protected void mStep() {
		double sumAlpha = alpha.sum();
		double sumBeta = beta.sum();
		double ak, br;

		// update alpha vector
		for (int k = 0; k < numFactors; k++) {

			ak = alpha.get(k);
			double numerator = 0, denominator = 0;
			for (int u = 0; u < numUsers; u++) {
				numerator += digamma(Nuk.get(u, k) + ak) - digamma(ak);
				denominator += digamma(Nu.get(u) + sumAlpha) - digamma(sumAlpha);
			}
			alpha.set(k, ak * (numerator / denominator));
		}

		// update beta_k
		for (int r = 0; r < numLevels; r++) {
			br = beta.get(r);
			double numerator = 0, denominator = 0;
			for (int i = 0; i < numItems; i++) {
				for (int k = 0; k < numFactors; k++) {
					numerator += digamma(Ntir[k][i][r] + br) - digamma(br);
					denominator += digamma(Nik.get(i, k) + sumBeta) - digamma(sumBeta);
				}
			}
			beta.set(r, br * (numerator / denominator));
		}

	}

	protected void readoutParams() {
		double val = 0;
		double ak = 0, br = 0;
		double sumAlpha = alpha.sum();

		for (int u = 0; u < numUsers; u++) {
			for (int k = 0; k < numFactors; k++) {
				ak = alpha.get(k);
				val = (Nuk.get(u, k) + ak) / (Nu.get(u) + sumAlpha);
				thetaSum.add(u, k, val);
			}
		}

		double sumBeta = beta.sum();
		for (int k = 0; k < numFactors; k++) {
			for (int i = 0; i < numItems; i++) {
				for (int r = 0; r < numLevels; r++) {
					br = beta.get(r);
					val = (Ntir[k][i][r] + br) / (Nik.get(i, k) + sumBeta);
					phiSum[k][i][r] += val;
				}
			}
		}
		numStats++;
	}

	@Override
	protected void postProbDistr() {
		theta = thetaSum.scale(1.0 / numStats);

		phi = new double[numFactors][numItems][numLevels];
		for (int k = 0; k < numFactors; k++) {
			for (int i = 0; i < numItems; i++) {
				for (int r = 0; r < numLevels; r++) {
					phi[k][i][r] = phiSum[k][i][r] / numStats;
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
				prob += theta.get(u, k) * phi[k][i][r];
			}

			pred += prob * rate;
		}

		return pred;
	}
}
