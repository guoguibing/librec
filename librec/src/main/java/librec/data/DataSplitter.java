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

package librec.data;

import happy.coding.io.FileIO;
import happy.coding.io.Logs;
import happy.coding.math.Randoms;
import happy.coding.math.Sortor;
import happy.coding.system.Debug;
import happy.coding.system.Systems;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to split/sample rating matrix
 * 
 * @author guoguibing
 * 
 */
public class DataSplitter {

	// [row-id, col-id, rate]
	private SparseMatrix rateMatrix;

	// [row-id, col-id, fold-id]
	private SparseMatrix assignMatrix;

	// number of folds
	private int numFold;

	/**
	 * Construct a data splitter to split a given matrix into kfolds
	 * 
	 * @param rateMatrix
	 *            data matrix
	 * @param kfold
	 *            number of folds to split
	 */
	public DataSplitter(SparseMatrix rateMatrix, int kfold) {
		this.rateMatrix = rateMatrix;

		splitFolds(kfold);
	}

	/**
	 * Construct a data splitter with data source of a given rate matrix
	 * 
	 * @param rateMatrix
	 *            data source
	 */
	public DataSplitter(SparseMatrix rateMatrix) {
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

		assignMatrix = new SparseMatrix(rateMatrix, false);

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
	 *            the ratio of training data over all the ratings.
	 */
	public SparseMatrix[] getRatio(double ratio) {

		assert (ratio > 0 && ratio <= 1);

		// keep both CRS and CCS for training matrix
		SparseMatrix trainMatrix = new SparseMatrix(rateMatrix);
		// keep only CRS for testing matrix
		SparseMatrix testMatrix = new SparseMatrix(rateMatrix, false);

		for (int u = 0, um = rateMatrix.numRows(); u < um; u++) {

			SparseVector uv = rateMatrix.row(u);
			for (int j : uv.getIndex()) {

				double rdm = Math.random();
				if (rdm < ratio)
					testMatrix.set(u, j, 0.0);
				else
					trainMatrix.set(u, j, 0.0);
			}
		}

		debugInfo(trainMatrix, testMatrix, -1);

		return new SparseMatrix[] { trainMatrix, testMatrix };
	}

	/**
	 * Split ratings into two parts: the training set consisting of user-item ratings where {@code numGiven} ratings are
	 * preserved for each user, and the rest are used as the testing data
	 * 
	 * @param numGiven
	 *            the number of ratings given to each user
	 */
	public SparseMatrix[] getGiven(int numGiven) throws Exception {

		assert numGiven > 0;

		// keep both CRS and CCS for training matrix
		SparseMatrix trainMatrix = new SparseMatrix(rateMatrix);
		// keep only CRS for testing matrix
		SparseMatrix testMatrix = new SparseMatrix(rateMatrix, false);

		for (int u = 0, um = rateMatrix.numRows(); u < um; u++) {

			SparseVector uv = rateMatrix.row(u);
			int numRated = uv.getCount();

			if (numRated > numGiven) {
				// a set of rated items
				int[] ratedItems = uv.getIndex();

				// a set of sampled indices of rated items
				int[] givenIndex = Randoms.nextIntArray(numGiven, numRated);

				for (int i = 0, j = 0; j < ratedItems.length; j++) {
					if (i < givenIndex.length && givenIndex[i] == j) {
						// for training
						testMatrix.set(u, ratedItems[j], 0.0);
						i++;
					} else {
						// for testing
						trainMatrix.set(u, ratedItems[j], 0.0);
					}
				}
			} else {
				// all ratings are used for training
				for (VectorEntry ve : uv)
					testMatrix.set(u, ve.index(), 0.0);
			}

		}

		debugInfo(trainMatrix, testMatrix, -1);

		return new SparseMatrix[] { trainMatrix, testMatrix };
	}

	/**
	 * Split ratings into two parts: the training set consisting of user-item ratings where {@code numGiven} ratings are
	 * preserved for each user, and the rest are used as the testing data
	 * 
	 * @param numGiven
	 *            the number of ratings given to each user
	 */
	public SparseMatrix[] getGiven(double ratio) throws Exception {

		assert ratio > 0 && ratio < 1;

		// keep both CRS and CCS for training matrix
		SparseMatrix trainMatrix = new SparseMatrix(rateMatrix);
		// keep only CRS for testing matrix
		SparseMatrix testMatrix = new SparseMatrix(rateMatrix, false);

		for (int u = 0, um = rateMatrix.numRows(); u < um; u++) {

			SparseVector uv = rateMatrix.row(u);
			int numRated = uv.getCount();

			// a set of rated items
			int[] ratedItems = uv.getIndex();

			// a set of sampled indices of rated items
			int[] givenIndex = Randoms.nextIntArray((int) (numRated * ratio), numRated);

			for (int i = 0, j = 0; j < ratedItems.length; j++) {
				if (i < givenIndex.length && givenIndex[i] == j) {
					// for training
					testMatrix.set(u, ratedItems[j], 0.0);
					i++;
				} else {
					// for testing
					trainMatrix.set(u, ratedItems[j], 0.0);
				}
			}

		}

		debugInfo(trainMatrix, testMatrix, -1);

		return new SparseMatrix[] { trainMatrix, testMatrix };
	}

	/**
	 * generate a random sample of rate matrix with specified number of users and items
	 * 
	 * @param numUsers
	 *            number of users, -1 to use all users;
	 * @param numItems
	 *            number of items, -1 to user all items;
	 */
	public void getSample(int numUsers, int numItems) throws Exception {
		int rows = rateMatrix.numRows();
		int cols = rateMatrix.numColumns();
		int users = numUsers <= 0 || numUsers > rows ? rows : numUsers;
		int items = numItems <= 0 || numItems > cols ? cols : numItems;

		int[] userIds = Randoms.nextIntArray(users, rows);
		int[] itemIds = Randoms.nextIntArray(items, cols);

		String path = FileIO.desktop + "sample.txt";
		FileIO.deleteFile(path);

		List<String> lines = new ArrayList<>(2000);
		int cnt = 0;
		for (int userId : userIds) {
			for (int itemId : itemIds) {
				double rate = rateMatrix.get(userId, itemId);
				if (rate > 0) {
					lines.add((userId + 1) + " " + (itemId + 1) + " " + (float) rate);
					cnt++;
					if (lines.size() >= 1500) {
						FileIO.writeList(path, lines, null, true);
						lines.clear();
					}
				}
			}
		}
		if (lines.size() > 0)
			FileIO.writeList(path, lines, null, true);

		Logs.debug("Sample [size: {}] has been created!", cnt);
	}

	public SparseMatrix[] getDataView(String view) {
		SparseMatrix trainMatrix = new SparseMatrix(rateMatrix);
		SparseMatrix testMatrix = new SparseMatrix(rateMatrix, false);

		switch (view.toLowerCase()) {
			case "cold-start":
				for (int u = 0, um = rateMatrix.numRows; u < um; u++) {
					SparseVector uv = rateMatrix.row(u);
					if (uv.getCount() < 5) {
						for (int i : uv.getIndex())
							trainMatrix.set(u, i, 0.0);

					} else {
						for (int i : uv.getIndex())
							testMatrix.set(u, i, 0.0);
					}
				}
				break;
			default:
				return null;
		}

		return new SparseMatrix[] { trainMatrix, testMatrix };
	}

	/**
	 * Return the k-th fold as test set (testMatrix), making all the others as train set in rateMatrix.
	 * 
	 * @param k
	 *            The index for desired fold.
	 * @return Rating matrices {k-th train data, k-th test data}
	 */
	public SparseMatrix[] getKthFold(int k) {
		if (k > numFold || k < 1)
			return null;

		SparseMatrix trainMatrix = new SparseMatrix(rateMatrix);
		SparseMatrix testMatrix = new SparseMatrix(rateMatrix, false);

		for (int u = 0, um = rateMatrix.numRows(); u < um; u++) {

			SparseVector items = rateMatrix.row(u);

			for (int j : items.getIndex()) {
				if (assignMatrix.get(u, j) == k)
					trainMatrix.set(u, j, 0.0); // keep test data and remove train data
				else
					testMatrix.set(u, j, 0.0); // keep train data and remove test data
			}
		}

		debugInfo(trainMatrix, testMatrix, k);

		return new SparseMatrix[] { trainMatrix, testMatrix };
	}

	/**
	 * print out debug information
	 */
	private void debugInfo(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		String foldInfo = fold > 0 ? "Fold [" + fold + "]: " : "";
		Logs.debug("{}training amount: {}, test amount: {}", foldInfo, trainMatrix.size(), testMatrix.size());

		if (Debug.OFF) {
			String dir = Systems.getDesktop();
			try {
				FileIO.writeString(dir + "training.txt", trainMatrix.toString());
				FileIO.writeString(dir + "test.txt", testMatrix.toString());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
