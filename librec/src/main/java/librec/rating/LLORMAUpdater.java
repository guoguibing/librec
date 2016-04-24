package librec.rating;

import librec.data.DenseMatrix;
import librec.data.DenseVector;
import librec.data.MatrixEntry;
import librec.data.SparseMatrix;
import librec.util.Randoms;

/**
 * <h3>Local Low-Rank Matrix Approximation</h3>
 * <p>
 * This implementation refers to the method proposed by Lee et al. at ICML 2013.
 * <p>
 * <strong>Lcoal Structure:</strong> Joonseok Lee, <strong>Local Low-Rank Matrix
 * Approximation </strong>, ICML. 2013: 82-90.
 * 
 * @author wkq
 */

public class LLORMAUpdater extends Thread {
	/**
	 * The unique identifier of the thread.
	 */
	private int threadId;
	/**
	 * The number of features.
	 */
	private int numFactors;
	/**
	 * The number of users.
	 */
	private int userCount;
	/**
	 * The number of items.
	 */
	private int itemCount;
	/**
	 * The anchor user used to learn this local model.
	 */
	private int anchorUser;
	/**
	 * The anchor item used to learn this local model.
	 */
	private int anchorItem;
	/**
	 * Learning rate parameter.
	 */
	public double lRate;
	/**
	 * The maximum number of iteration.
	 */
	public int maxIter;
	/**
	 * Regularization factor parameter.
	 */
	public double regU, regI;
	/**
	 * The vector containing each user's weight.
	 */
	private DenseVector w;
	/**
	 * The vector containing each item's weight.
	 */
	private DenseVector v;
	/**
	 * User profile in low-rank matrix form.
	 */
	private DenseMatrix P;
	/**
	 * Item profile in low-rank matrix form.
	 */
	private DenseMatrix Q;
	/**
	 * The rating matrix used for learning.
	 */
	private SparseMatrix trainMatrix;
	/**
	 * The current train error.
	 */
	private double trainErr;

	/**
	 * Construct a local model for singleton LLORMA.
	 *
	 * @param id
	 *            A unique thread ID.
	 * @param rk
	 *            The rank which will be used in this local model.
	 * @param u
	 *            The number of users.
	 * @param i
	 *            The number of items.
	 * @param au
	 *            The anchor user used to learn this local model.
	 * @param ai
	 *            The anchor item used to learn this local model.
	 * @param lr
	 *            Learning rate parameter.
	 * @param w0
	 *            Initial vector containing each user's weight.
	 * @param v0
	 *            Initial vector containing each item's weight.
	 * @param rm
	 *            The rating matrix used for learning.
	 */
	public LLORMAUpdater(int tid, int rk, int uc, int ic, int au, int ai, double lr, double regU, double regI, int iter,
			DenseVector w, DenseVector v, SparseMatrix rm) {
		threadId = tid;
		numFactors = rk;
		userCount = uc;
		itemCount = ic;
		anchorUser = au;
		anchorItem = ai;
		lRate = lr;
		this.regU = regU;
		this.regI = regI;
		maxIter = iter;
		this.w = w;
		this.v = v;
		P = new DenseMatrix(userCount, numFactors);
		Q = new DenseMatrix(itemCount, numFactors);
		trainMatrix = rm;
	}
	/**
	 * Getter method for thread ID.
	 *
	 * @return The thread ID of this local model.
	 */
	public int getThreadId() {
		return threadId;
	}

	/**
	 * Getter method for rank of this local model.
	 *
	 * @return The rank of this local model.
	 */
	public int getRank() {
		return numFactors;
	}

	/**
	 * Getter method for anchor user of this local model.
	 *
	 * @return The anchor user ID of this local model.
	 */
	public int getAnchorUser() {
		return anchorUser;
	}

	/**
	 * Getter method for anchor item of this local model.
	 *
	 * @return The anchor item ID of this local model.
	 */
	public int getAnchorItem() {
		return anchorItem;
	}

	/**
	 * Getter method for user profile of this local model.
	 *
	 * @return The user profile of this local model.
	 */
	public DenseMatrix getUserFeatures() {
		return P;
	}

	/**
	 * Getter method for item profile of this local model.
	 *
	 * @return The item profile of this local model.
	 */
	public DenseMatrix getItemFeatures() {
		return Q;
	}

	/**
	 * Getter method for current train error.
	 *
	 * @return The current train error.
	 */
	public double getTrainErr() {
		return trainErr;
	}

	/**
	 * Learn this local model based on similar users to the anchor user and
	 * similar items to the anchor item. Implemented with gradient descent.
	 */
	@Override
	public void run() {
		trainErr = Double.MAX_VALUE;

		for (int u = 0; u < userCount; u++) {
			for (int r = 0; r < numFactors; r++) {
				double rdm = Randoms.gaussian(0.0, 0.01);
				P.set(u, r, rdm);
			}
		}
		for (int i = 0; i < itemCount; i++) {
			for (int r = 0; r < numFactors; r++) {
				double rdm = Randoms.gaussian(0.0, 0.01);
				Q.set(i, r, rdm);
			}
		}
		int round = 0;
		int rateCount = trainMatrix.size();
		double prevErr = 99999;
		double currErr = 9999;

		while (Math.abs(prevErr - currErr) > 0.0001 && round < maxIter) {
			double loss = 0.0;
			for (MatrixEntry me : trainMatrix) {
				int u = me.row(); // user
				int j = me.column(); // item
				double ruj = me.get();

				double puj = 0;
				try {
					puj = predict(u, j);
				} catch (Exception e) {

				}
				double euj = ruj - puj;
				loss += euj * euj;
				double weight = w.get(u) * v.get(j);
				for (int f = 0; f < numFactors; f++) {
					double puf = P.get(u, f), qjf = Q.get(j, f);

					P.add(u, f, lRate * (euj * qjf * weight - regU * puf));
					Q.add(j, f, lRate * (euj * puf * weight - regI * qjf));

					loss += regU * puf * puf + regI * qjf * qjf;
				}
			}
			prevErr = currErr;
			currErr = loss / rateCount;
			trainErr = Math.sqrt(currErr);
			round++;
		}
	}
	protected double predict(int u, int j) throws Exception {
		return DenseMatrix.rowMult(P, u, Q, j);
	}
}
