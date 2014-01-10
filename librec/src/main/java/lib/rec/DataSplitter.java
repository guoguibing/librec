package lib.rec;

import happy.coding.io.FileIO;
import happy.coding.io.Logs;
import happy.coding.math.Sortor;
import happy.coding.system.Debug;
import happy.coding.system.Systems;
import no.uib.cipr.matrix.Matrices;
import no.uib.cipr.matrix.sparse.CompRowMatrix;
import no.uib.cipr.matrix.sparse.SparseVector;

public class DataSplitter {

	// [row-id, col-id, rate]
	private CompRowMatrix rateMatrix;

	// [row-id, col-id, fold-id]
	private CompRowMatrix assignMatrix;

	private int numFold;

	public DataSplitter(CompRowMatrix rateMatrix, int kfold) {
		this.rateMatrix = rateMatrix;

		splitFolds(kfold);
	}

	public DataSplitter(CompRowMatrix rateMatrix) {
		this.rateMatrix = rateMatrix;
	}

	/**
	 * Split ratings into k-fold.
	 * 
	 * @param kfold
	 *            number of folds
	 */
	private void splitFolds(int kfold) {
		assert kfold > 0;

		assignMatrix = new CompRowMatrix(rateMatrix);

		int numRates = rateMatrix.getData().length;
		numFold = kfold > numRates ? numRates : kfold;

		// divide rating data into kfold sample of equal size
		double[] rdm = new double[numRates];
		int[] fold = new int[numRates];
		double indvCount = (numRates + 0.0) / numFold;

		for (int i = 0; i < numRates; i++) {
			rdm[i] = Math.random();
			fold[i] = (int) (i / indvCount) + 1; // make sure that each fold has each size sample
		}

		Sortor.quickSort(rdm, fold, 0, numRates - 1, true);

		int[] row_ptr = rateMatrix.getRowPointers();
		int[] col_idx = rateMatrix.getColumnIndices();

		int f = 0;
		for (int u = 0, um = rateMatrix.numRows(); u < um; u++) {
			for (int idx = row_ptr[u], end = row_ptr[u + 1]; idx < end; idx++) {
				int j = col_idx[idx];
				// if randomly put an int 1-5 to entry (u, j), we cannot make sure equal size for each fold
				assignMatrix.set(u, j, fold[f++]);
			}
		}
	}

	/**
	 * Split ratings into two parts: (1-ratio) training, (ratio) testing data
	 * 
	 * @param ratio
	 *            the ratio of testing data over all the ratings.
	 */
	public CompRowMatrix[] getRatio(double ratio) {

		assert (ratio > 0 && ratio <= 1);

		CompRowMatrix trainMatrix = rateMatrix.copy();
		CompRowMatrix testMatrix = rateMatrix.copy();

		for (int u = 0, um = rateMatrix.numRows(); u < um; u++) {

			SparseVector uv = MatrixUtils.row(rateMatrix, u);
			for (int j : uv.getIndex()) {

				double rdm = Math.random();
				if (rdm < ratio)
					trainMatrix.set(u, j, 0.0);
				else
					testMatrix.set(u, j, 0.0);
			}
		}

		debugInfo(trainMatrix, testMatrix, -1);

		return new CompRowMatrix[] { trainMatrix, testMatrix };
	}

	/**
	 * Return the k-th fold as test set (testMatrix), making all the others as
	 * train set in rateMatrix.
	 * 
	 * @param k
	 *            The index for desired fold.
	 * @return Rating matrices {k-th train data, k-th test data}
	 */
	public CompRowMatrix[] getKthFold(int k) {
		if (k > numFold || k < 1)
			return null;

		CompRowMatrix trainMatrix = rateMatrix.copy();
		CompRowMatrix testMatrix = rateMatrix.copy();

		for (int u = 0, um = rateMatrix.numRows(); u < um; u++) {

			SparseVector items = MatrixUtils.row(rateMatrix, u);

			for (int j : items.getIndex()) {
				if (assignMatrix.get(u, j) == k)
					trainMatrix.set(u, j, 0.0); // keep test data and remove train data
				else
					testMatrix.set(u, j, 0.0); // keep train data and remove test data
			}
		}

		debugInfo(trainMatrix, testMatrix, k);

		return new CompRowMatrix[] { trainMatrix, testMatrix };
	}

	/**
	 * print out debug information
	 */
	private void debugInfo(CompRowMatrix trainMatrix, CompRowMatrix testMatrix, int fold) {
		String foldInfo = fold > 0 ? "Fold [" + fold + "]: " : "";
		Logs.debug("{}training amount: {}, testing amount: {}", foldInfo, Matrices.cardinality(trainMatrix),
				Matrices.cardinality(testMatrix));

		if (Debug.OFF) {
			String dir = Systems.getDesktop();
			try {
				FileIO.writeString(dir + "train.txt", trainMatrix.toString());
				FileIO.writeString(dir + "test.txt", testMatrix.toString());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
