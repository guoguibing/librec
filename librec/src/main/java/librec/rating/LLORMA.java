package librec.rating;

import librec.data.DenseMatrix;
import librec.data.DenseVector;
import librec.data.MatrixEntry;
import librec.data.SparseMatrix;
import librec.intf.IterativeRecommender;
import librec.intf.Recommender;
import librec.util.KernelSmoothing;
import librec.util.Logs;
import java.util.List;

/**
 * <h3>Local Low-Rank Matrix Approximation</h3>
 * <p>
 * This implementation refers to the method proposed by Lee et al. at ICML 2013.
 * <p>
 * <strong>Lcoal Structure:</strong> Joonseok Lee, <strong>Local Low-Rank Matrix
 * Approximation </strong>, ICML. 2013: 82-90.
 *
 * @author wubin
 */

public class LLORMA extends IterativeRecommender {

	private static int localNumFactors;
	private static int localNumIters;
	private static int multiThreadCount;
	protected static float localRegU, localRegI;

	private float localLRate;
	private SparseMatrix predictMatrix;
	private static int modelMax;

	private static SparseMatrix testIndexMatrix;// test index matrix for predict

	private SparseMatrix cumPrediction, cumWeight;

	public LLORMA(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);
		algoOptions = cf.getParamOptions("LLORMA");
		localNumFactors = algoOptions.getInt("-lnf", 20);
		multiThreadCount = algoOptions.getInt("-mtc", 4);
		modelMax = algoOptions.getInt("-mm", 50);
		multiThreadCount = multiThreadCount > modelMax ? modelMax : multiThreadCount;
		localNumIters = algoOptions.getInt("-lni", 100);
		localLRate = algoOptions.getFloat("-lr", 0.01f);
		localRegI = algoOptions.getFloat("-lu", 0.001f);
		localRegU = algoOptions.getFloat("-li", 0.001f);
		predictMatrix = new SparseMatrix(testMatrix);
	}

	@Override
	protected void initModel() throws Exception {

		testIndexMatrix = new SparseMatrix(testMatrix);
		for (MatrixEntry me : testIndexMatrix) {
			int u = me.row();
			int i = me.column();
			testIndexMatrix.set(u, i, 0.0);
		}

		// global svd P Q to calculate the kernel value between users (or items)
		P = new DenseMatrix(numUsers, numFactors);
		Q = new DenseMatrix(numItems, numFactors);

		// initialize model
		if (initByNorm) {
			P.init(initMean, initStd);
			Q.init(initMean, initStd);
		} else {
			P.init(); // P.init(smallValue);
			Q.init(); // Q.init(smallValue);
		}
		this.buildGlobalModel();

	}

	// global svd P Q
	private void buildGlobalModel() throws Exception {
		for (int iter = 1; iter <= numIters; iter++) {
			for (MatrixEntry me : trainMatrix) {

				int u = me.row(); // user
				int i = me.column(); // item
				double rui = me.get();

				double pui = DenseMatrix.rowMult(P, u, Q, i);
				double eui = rui - pui;
				// update factors
				for (int f = 0; f < numFactors; f++) {
					double puf = P.get(u, f), qif = Q.get(i, f);

					P.add(u, f, lRate * (eui * qif - regU * puf));
					Q.add(i, f, lRate * (eui * puf - regI * qif));
				}
			}

		} // end of training
	}

	@Override
	protected void buildModel() throws Exception {

		// Pre-calculating similarity:
		int completeModelCount = 0;

		LLORMAUpdater[] learners = new LLORMAUpdater[multiThreadCount];
		int[] anchorUser = new int[modelMax];
		int[] anchorItem = new int[modelMax];

		int modelCount = 0;
		int[] runningThreadList = new int[multiThreadCount];
		int runningThreadCount = 0;
		int waitingThreadPointer = 0;
		int nextRunningSlot = 0;

		cumPrediction = new SparseMatrix(testIndexMatrix);
		cumWeight = new SparseMatrix(testIndexMatrix);

		// Parallel training:
		while (completeModelCount < modelMax) {
			int u_t = (int) Math.floor(Math.random() * numUsers);
			List<Integer> itemList = trainMatrix.getColumns(u_t);

			if (itemList != null) {
				if (runningThreadCount < multiThreadCount && modelCount < modelMax) {
					// Selecting a new anchor point:
					int idx = (int) Math.floor(Math.random() * itemList.size());
					int i_t = itemList.get(idx);

					anchorUser[modelCount] = u_t;
					anchorItem[modelCount] = i_t;

					// Preparing weight vectors:
					DenseVector w = kernelSmoothing(numUsers, u_t, KernelSmoothing.EPANECHNIKOV_KERNEL, 0.8, false);
					DenseVector v = kernelSmoothing(numItems, i_t, KernelSmoothing.EPANECHNIKOV_KERNEL, 0.8, true);

					// Starting a new local model learning:
					learners[nextRunningSlot] = new LLORMAUpdater(modelCount, localNumFactors, numUsers, numItems, u_t,
							i_t, localLRate, localRegU, localRegI, localNumIters, w, v, trainMatrix);
					learners[nextRunningSlot].start();

					runningThreadList[runningThreadCount] = modelCount;
					runningThreadCount++;
					modelCount++;
					nextRunningSlot++;
				} else if (runningThreadCount > 0) {
					// Joining a local model which was done with learning:
					try {
						learners[waitingThreadPointer].join();
					} catch (InterruptedException ie) {
						System.out.println("Join failed: " + ie);
					}

					int mp = waitingThreadPointer;
					int mc = completeModelCount;
					completeModelCount++;

					// Predicting with the new local model and all previous
					// models:
					predictMatrix = new SparseMatrix(testIndexMatrix);
					for (MatrixEntry me : testMatrix) {
						int u = me.row();
						int i = me.column();

						double weight = KernelSmoothing.kernelize(getUserSimilarity(anchorUser[mc], u), 0.8,
								KernelSmoothing.EPANECHNIKOV_KERNEL)
								* KernelSmoothing.kernelize(getItemSimilarity(anchorItem[mc], i), 0.8,
										KernelSmoothing.EPANECHNIKOV_KERNEL);

						double newPrediction = (learners[mp].getUserFeatures().row(u)
								.inner(learners[mp].getItemFeatures().row(i))) * weight;
						cumWeight.set(u, i, cumWeight.get(u, i) + weight);
						cumPrediction.set(u, i, cumPrediction.get(u, i) + newPrediction);
						double prediction = cumPrediction.get(u, i) / cumWeight.get(u, i);
						if (Double.isNaN(prediction) || prediction == 0.0) {
							prediction = globalMean;
						}

						if (prediction < minRate) {
							prediction = minRate;
						} else if (prediction > maxRate) {
							prediction = maxRate;
						}

						predictMatrix.set(u, i, prediction);

					}

					if (completeModelCount % 5 == 0) {
						evalRatings();

						Logs.debug("{}{} iter {}:[" + measures.getMetricNamesString() + "] {}", algoName, foldInfo,
								completeModelCount, "[" + measures.getEvalResultString() + "]");
					}
					nextRunningSlot = waitingThreadPointer;
					waitingThreadPointer = (waitingThreadPointer + 1) % multiThreadCount;
					runningThreadCount--;

				}
			}
		}
	}

	/**
	 * Calculate similarity between two users, based on the global base SVD.
	 *
	 * @param idx1
	 *            The first user's ID.
	 * @param idx2
	 *            The second user's ID.
	 * @return The similarity value between two users idx1 and idx2.
	 */
	private double getUserSimilarity(int idx1, int idx2) {
		double sim;

		DenseVector u_vec = P.row(idx1);
		DenseVector v_vec = P.row(idx2);

		sim = 1 - 2.0 / Math.PI
				* Math.acos(u_vec.inner(v_vec) / (Math.sqrt(u_vec.inner(u_vec)) * Math.sqrt(v_vec.inner(v_vec))));

		if (Double.isNaN(sim)) {
			sim = 0.0;
		}

		return sim;
	}

	/**
	 * Calculate similarity between two items, based on the global base SVD.
	 *
	 * @param idx1
	 *            The first item's ID.
	 * @param idx2
	 *            The second item's ID.
	 * @return The similarity value between two items idx1 and idx2.
	 */
	private double getItemSimilarity(int idx1, int idx2) {
		double sim;

		DenseVector i_vec = Q.row(idx1);
		DenseVector j_vec = Q.row(idx2);

		sim = 1 - 2.0 / Math.PI
				* Math.acos(i_vec.inner(j_vec) / (Math.sqrt(i_vec.inner(i_vec)) * Math.sqrt(j_vec.inner(j_vec))));
		if (Double.isNaN(sim)) {
			sim = 0.0;
		}

		return sim;
	}

	/**
	 * Given the similarity, it applies the given kernel. This is done either
	 * for all users or for all items.
	 *
	 * @param size
	 *            The length of user or item vector.
	 * @param id
	 *            The identifier of anchor point.
	 * @param kernelType
	 *            The type of kernel.
	 * @param width
	 *            Kernel width.
	 * @param isItemFeature
	 *            return item kernel if yes, return user kernel otherwise.
	 * @return The kernel-smoothed values for all users or all items.
	 */
	private DenseVector kernelSmoothing(int size, int id, int kernelType, double width, boolean isItemFeature) {
		DenseVector newFeatureVector = new DenseVector(size);
		newFeatureVector.set(id, 1.0);

		for (int i = 0; i < size; i++) {
			double sim;
			if (isItemFeature) {
				sim = getItemSimilarity(i, id);
			} else {
				sim = getUserSimilarity(i, id);
			}

			newFeatureVector.set(i, KernelSmoothing.kernelize(sim, width, kernelType));
		}

		return newFeatureVector;
	}

	@Override
	public double predict(int u, int i) throws Exception {
		return predictMatrix.get(u, i);
	}
}
