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
import happy.coding.io.Lists;
import happy.coding.io.Logs;
import happy.coding.math.Randoms;
import happy.coding.math.Sortor;
import happy.coding.system.Debug;
import happy.coding.system.Systems;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;

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

		assignMatrix = new SparseMatrix(rateMatrix);

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
	 * Split ratings into two parts: (ratio) training, (1-ratio) test subsets.
	 * 
	 * @param ratio
	 *            the ratio of training data over all the ratings.
	 */
	public SparseMatrix[] getRatioByRating(double ratio) {

		assert (ratio > 0 && ratio < 1);

		SparseMatrix trainMatrix = new SparseMatrix(rateMatrix);
		SparseMatrix testMatrix = new SparseMatrix(rateMatrix);

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

		// remove zero entries
		SparseMatrix.reshape(trainMatrix);
		SparseMatrix.reshape(testMatrix);

		debugInfo(trainMatrix, testMatrix, -1);

		return new SparseMatrix[] { trainMatrix, testMatrix };
	}

	/**
	 * Split the ratings (by date) into two parts: (ratio) training, (1-ratio) test subsets
	 * 
	 * @param ratio
	 *            the ratio of training data
	 * @param timestamps
	 *            the timestamps of all rating data
	 */
	public SparseMatrix[] getRatioByRatingDate(double ratio, Table<Integer, Integer, Long> timestamps) {

		assert (ratio > 0 && ratio < 1);

		// sort timestamps from smaller to larger
		List<RatingContext> rcs = new ArrayList<>(timestamps.size());
		int u, i, j;
		long timestamp;
		for (MatrixEntry me : rateMatrix) {
			u = me.row();
			i = me.column();
			timestamp = timestamps.get(u, i);
			rcs.add(new RatingContext(u, i, timestamp));
		}
		Collections.sort(rcs);

		SparseMatrix trainMatrix = new SparseMatrix(rateMatrix);
		SparseMatrix testMatrix = new SparseMatrix(rateMatrix);

		int trainSize = (int) (rcs.size() * ratio);
		for (i = 0; i < rcs.size(); i++) {
			RatingContext rc = rcs.get(i);
			u = rc.getUser();
			j = rc.getItem();

			if (i < trainSize)
				testMatrix.set(u, j, 0.0);
			else
				trainMatrix.set(u, j, 0.0);
		}

		// release memory
		rcs = null;

		// remove zero entries
		SparseMatrix.reshape(trainMatrix);
		SparseMatrix.reshape(testMatrix);

		debugInfo(trainMatrix, testMatrix, -1);

		return new SparseMatrix[] { trainMatrix, testMatrix };
	}

	/**
	 * Split the ratings of each user (by date) into two parts: (ratio) training, (1-ratio) test subsets
	 * 
	 * @param ratio
	 *            the ratio of training data
	 * @param timestamps
	 *            the timestamps of all rating data
	 */
	public SparseMatrix[] getRatioByUserDate(double ratio, Table<Integer, Integer, Long> timestamps) {

		assert (ratio > 0 && ratio < 1);

		SparseMatrix trainMatrix = new SparseMatrix(rateMatrix);
		SparseMatrix testMatrix = new SparseMatrix(rateMatrix);

		for (int user = 0, um = rateMatrix.numRows; user < um; user++) {
			List<Integer> unsortedItems = rateMatrix.getColumns(user);

			int size = unsortedItems.size();
			List<RatingContext> rcs = new ArrayList<>(size);
			for (int item : unsortedItems) {
				rcs.add(new RatingContext(user, item, timestamps.get(user, item)));
			}
			Collections.sort(rcs);

			int trainSize = (int) (rcs.size() * ratio);
			for (int i = 0; i < rcs.size(); i++) {
				RatingContext rc = rcs.get(i);
				int u = rc.getUser();
				int j = rc.getItem();

				if (i < trainSize)
					testMatrix.set(u, j, 0.0);
				else
					trainMatrix.set(u, j, 0.0);
			}
		}

		// remove zero entries
		SparseMatrix.reshape(trainMatrix);
		SparseMatrix.reshape(testMatrix);

		debugInfo(trainMatrix, testMatrix, -1);

		return new SparseMatrix[] { trainMatrix, testMatrix };
	}

	/**
	 * Split the ratings of each item (by date) into two parts: (ratio) training, (1-ratio) test subsets
	 * 
	 * @param ratio
	 *            the ratio of training data
	 * @param timestamps
	 *            the timestamps of all rating data
	 */
	public SparseMatrix[] getRatioByItemDate(double ratio, Table<Integer, Integer, Long> timestamps) {

		assert (ratio > 0 && ratio < 1);

		SparseMatrix trainMatrix = new SparseMatrix(rateMatrix);
		SparseMatrix testMatrix = new SparseMatrix(rateMatrix);

		for (int item = 0, im = rateMatrix.numColumns; item < im; item++) {
			List<Integer> unsortedUsers = rateMatrix.getRows(item);

			int size = unsortedUsers.size();
			List<RatingContext> rcs = new ArrayList<>(size);
			for (int user : unsortedUsers) {
				rcs.add(new RatingContext(user, item, timestamps.get(user, item)));
			}
			Collections.sort(rcs);

			int trainSize = (int) (rcs.size() * ratio);
			for (int i = 0; i < rcs.size(); i++) {
				RatingContext rc = rcs.get(i);
				int u = rc.getUser();
				int j = rc.getItem();

				if (i < trainSize)
					testMatrix.set(u, j, 0.0);
				else
					trainMatrix.set(u, j, 0.0);
			}
		}

		// remove zero entries
		SparseMatrix.reshape(trainMatrix);
		SparseMatrix.reshape(testMatrix);

		debugInfo(trainMatrix, testMatrix, -1);

		return new SparseMatrix[] { trainMatrix, testMatrix };
	}

	/**
	 * Split ratings into: (train-ratio) training, (validation-ratio) validation, and test three subsets.
	 * 
	 * @param trainRatio
	 *            training ratio
	 * @param validRatio
	 *            validation ratio
	 */
	public SparseMatrix[] getRatio(double trainRatio, double validRatio) {

		assert (trainRatio > 0 && validRatio > 0 && (trainRatio + validRatio) < 1);

		SparseMatrix trainMatrix = new SparseMatrix(rateMatrix);
		SparseMatrix validMatrix = new SparseMatrix(rateMatrix);
		SparseMatrix testMatrix = new SparseMatrix(rateMatrix);

		double sum = trainRatio + validRatio;

		for (int u = 0, um = rateMatrix.numRows(); u < um; u++) {

			SparseVector uv = rateMatrix.row(u);
			for (int j : uv.getIndex()) {

				double rdm = Math.random();
				if (rdm < trainRatio) {
					// for training
					testMatrix.set(u, j, 0);
					validMatrix.set(u, j, 0);
				} else if (rdm < sum) {
					// for validation
					trainMatrix.set(u, j, 0);
					testMatrix.set(u, j, 0);
				} else {
					// for test
					trainMatrix.set(u, j, 0);
					validMatrix.set(u, j, 0);
				}
			}
		}

		// remove zero entries
		SparseMatrix.reshape(trainMatrix);
		SparseMatrix.reshape(validMatrix);
		SparseMatrix.reshape(testMatrix);

		return new SparseMatrix[] { trainMatrix, validMatrix, testMatrix };
	}

	/**
	 * Split ratings into two parts where one rating per user is preserved as the test set and the remaining data as the
	 * training set
	 * 
	 */
	public SparseMatrix[] getLOOByUser(boolean isByDate, Table<Integer, Integer, Long> timestamps) throws Exception {

		SparseMatrix trainMatrix = new SparseMatrix(rateMatrix);

		// for building test matrix
		Table<Integer, Integer, Double> dataTable = HashBasedTable.create();
		Multimap<Integer, Integer> colMap = HashMultimap.create();

		for (int u = 0, um = rateMatrix.numRows(); u < um; u++) {

			List<Integer> items = rateMatrix.getColumns(u);
			int i = -1;

			if (!isByDate) {
				// by random
				int randIdx = (int) (items.size() * Math.random());
				i = items.get(randIdx);
			} else {
				// by date
				List<RatingContext> rcs = new ArrayList<>();
				for (int j : items) {
					rcs.add(new RatingContext(u, j, timestamps.get(u, j)));
				}
				Collections.sort(rcs);

				i = rcs.get(rcs.size() - 1).getItem(); // most recent item
			}

			trainMatrix.set(u, i, 0); // remove from training

			dataTable.put(u, i, rateMatrix.get(u, i));
			colMap.put(i, u);
		}

		// remove zero entries
		SparseMatrix.reshape(trainMatrix);

		// build test matrix
		SparseMatrix testMatrix = new SparseMatrix(rateMatrix.numRows, rateMatrix.numColumns, dataTable, colMap);

		debugInfo(trainMatrix, testMatrix, -1);

		return new SparseMatrix[] { trainMatrix, testMatrix };
	}

	/**
	 * Split ratings into two parts where one rating per item is preserved as the test set and the remaining data as the
	 * training set
	 * 
	 */
	public SparseMatrix[] getLOOByItem(boolean isByDate, Table<Integer, Integer, Long> timestamps) throws Exception {

		SparseMatrix trainMatrix = new SparseMatrix(rateMatrix);

		// for building test matrix
		Table<Integer, Integer, Double> dataTable = HashBasedTable.create();
		Multimap<Integer, Integer> colMap = HashMultimap.create();

		for (int i = 0, im = rateMatrix.numColumns(); i < im; i++) {

			List<Integer> users = rateMatrix.getRows(i);
			int u = -1;

			if (!isByDate) {
				// by random
				int randIdx = (int) (users.size() * Math.random());
				u = users.get(randIdx);
			} else {
				// by date
				List<RatingContext> rcs = new ArrayList<>();
				for (int v : users) {
					rcs.add(new RatingContext(v, i, timestamps.get(v, i)));
				}
				Collections.sort(rcs);

				u = rcs.get(rcs.size() - 1).getUser(); // most recent rating user
			}

			trainMatrix.set(u, i, 0); // remove from training

			dataTable.put(u, i, rateMatrix.get(u, i));
			colMap.put(i, u);
		}

		// remove zero entries
		SparseMatrix.reshape(trainMatrix);

		// build test matrix
		SparseMatrix testMatrix = new SparseMatrix(rateMatrix.numRows, rateMatrix.numColumns, dataTable, colMap);

		debugInfo(trainMatrix, testMatrix, -1);

		return new SparseMatrix[] { trainMatrix, testMatrix };
	}

	/**
	 * Split ratings into two parts: the training set consisting of user-item ratings where {@code numGiven} ratings are
	 * preserved for each user, and the rest are used as the testing data
	 * 
	 */
	public SparseMatrix[] getGivenNByUser(int numGiven) throws Exception {

		assert numGiven > 0;

		SparseMatrix trainMatrix = new SparseMatrix(rateMatrix);
		SparseMatrix testMatrix = new SparseMatrix(rateMatrix);

		for (int u = 0, um = rateMatrix.numRows(); u < um; u++) {

			List<Integer> items = rateMatrix.getColumns(u);
			int numRated = items.size();

			if (numRated > numGiven) {

				// a set of sampled indices of rated items
				int[] givenIndex = Randoms.nextIntArray(numGiven, numRated);

				for (int i = 0, j = 0; j < numRated; j++) {
					if (i < givenIndex.length && givenIndex[i] == j) {
						// for training
						testMatrix.set(u, items.get(j), 0.0);
						i++;
					} else {
						// for testing
						trainMatrix.set(u, items.get(j), 0.0);
					}
				}
			} else {
				// all ratings are used for training
				for (int j : items)
					testMatrix.set(u, j, 0.0);
			}

		}

		// remove zero entries
		SparseMatrix.reshape(trainMatrix);
		SparseMatrix.reshape(testMatrix);

		debugInfo(trainMatrix, testMatrix, -1);

		return new SparseMatrix[] { trainMatrix, testMatrix };
	}

	/**
	 * Split ratings into two parts: the training set consisting of user-item ratings where {@code numGiven} earliest
	 * ratings are preserved for each user, and the rest are used as the testing data
	 * 
	 */
	public SparseMatrix[] getGivenNByUserDate(int numGiven, Table<Integer, Integer, Long> timestamps) throws Exception {

		assert numGiven > 0;

		SparseMatrix trainMatrix = new SparseMatrix(rateMatrix);
		SparseMatrix testMatrix = new SparseMatrix(rateMatrix);

		for (int u = 0, um = rateMatrix.numRows(); u < um; u++) {

			List<Integer> items = rateMatrix.getColumns(u);
			int capacity = Lists.initSize(items.size());

			List<RatingContext> rcs = new ArrayList<>(capacity);
			for (int j : items) {
				rcs.add(new RatingContext(u, j, timestamps.get(u, j)));
			}
			Collections.sort(rcs);

			for (int i = 0; i < rcs.size(); i++) {
				RatingContext rc = rcs.get(i);
				int j = rc.getItem();

				if (i < numGiven)
					testMatrix.set(u, j, 0.0);
				else
					trainMatrix.set(u, j, 0.0);
			}
		}

		// remove zero entries
		SparseMatrix.reshape(trainMatrix);
		SparseMatrix.reshape(testMatrix);

		debugInfo(trainMatrix, testMatrix, -1);

		return new SparseMatrix[] { trainMatrix, testMatrix };
	}

	/**
	 * Split ratings into two parts: the training set consisting of user-item ratings where {@code numGiven} earliest
	 * ratings are preserved for each item, and the rest are used as the testing data
	 * 
	 */
	public SparseMatrix[] getGivenNByItemDate(int numGiven, Table<Integer, Integer, Long> timestamps) throws Exception {

		assert numGiven > 0;

		SparseMatrix trainMatrix = new SparseMatrix(rateMatrix);
		SparseMatrix testMatrix = new SparseMatrix(rateMatrix);

		for (int j = 0, jm = rateMatrix.numRows(); j < jm; j++) {

			List<Integer> users = rateMatrix.getRows(j);
			int capacity = Lists.initSize(users.size());

			List<RatingContext> rcs = new ArrayList<>(capacity);
			for (int u : users) {
				rcs.add(new RatingContext(u, j, timestamps.get(u, j)));
			}
			Collections.sort(rcs);

			for (int i = 0; i < rcs.size(); i++) {
				RatingContext rc = rcs.get(i);
				int u = rc.getUser();

				if (i < numGiven)
					testMatrix.set(u, j, 0.0);
				else
					trainMatrix.set(u, j, 0.0);
			}
		}

		// remove zero entries
		SparseMatrix.reshape(trainMatrix);
		SparseMatrix.reshape(testMatrix);

		debugInfo(trainMatrix, testMatrix, -1);

		return new SparseMatrix[] { trainMatrix, testMatrix };
	}

	/**
	 * Split ratings into two parts: the training set consisting of user-item ratings where {@code numGiven} ratings are
	 * preserved for each item, and the rest are used as the testing data
	 * 
	 */
	public SparseMatrix[] getGivenNByItem(int numGiven) throws Exception {

		assert numGiven > 0;

		SparseMatrix trainMatrix = new SparseMatrix(rateMatrix);
		SparseMatrix testMatrix = new SparseMatrix(rateMatrix);

		for (int j = 0, jm = rateMatrix.numColumns(); j < jm; j++) {

			List<Integer> users = rateMatrix.getRows(j);
			int numRated = users.size();

			if (numRated > numGiven) {

				// a set of sampled indices of rated items
				int[] givenIndex = Randoms.nextIntArray(numGiven, numRated);

				for (int i = 0, k = 0; k < numRated; k++) {
					if (i < givenIndex.length && givenIndex[i] == k) {
						// for training
						testMatrix.set(users.get(k), j, 0.0);
						i++;
					} else {
						// for testing
						trainMatrix.set(users.get(k), j, 0.0);
					}
				}
			} else {
				// all ratings are used for training
				for (int u : users)
					testMatrix.set(u, j, 0.0);
			}

		}

		// remove zero entries
		SparseMatrix.reshape(trainMatrix);
		SparseMatrix.reshape(testMatrix);

		debugInfo(trainMatrix, testMatrix, -1);

		return new SparseMatrix[] { trainMatrix, testMatrix };
	}

	/**
	 * Split ratings into two parts: the training set consisting of user-item ratings where {@code ratio} percentage of
	 * ratings are preserved for each user, and the rest are used as the testing data
	 * 
	 */
	public SparseMatrix[] getRatioByUser(double ratio) throws Exception {

		assert ratio > 0 && ratio < 1;

		SparseMatrix trainMatrix = new SparseMatrix(rateMatrix);
		SparseMatrix testMatrix = new SparseMatrix(rateMatrix);

		for (int u = 0, um = rateMatrix.numRows(); u < um; u++) {

			List<Integer> items = rateMatrix.getColumns(u);

			for (int j : items) {
				double rand = Math.random();
				if (rand < ratio)
					testMatrix.set(u, j, 0.0); // for training
				else
					trainMatrix.set(u, j, 0.0); // for testing
			}

		}

		// remove zero entries
		SparseMatrix.reshape(trainMatrix);
		SparseMatrix.reshape(testMatrix);

		debugInfo(trainMatrix, testMatrix, -1);

		return new SparseMatrix[] { trainMatrix, testMatrix };
	}

	/**
	 * Split ratings into two parts: the training set consisting of user-item ratings where {@code ratio} percentage of
	 * ratings are preserved for each item, and the rest are used as the testing data
	 * 
	 */
	public SparseMatrix[] getRatioByItem(double ratio) throws Exception {

		assert ratio > 0 && ratio < 1;

		SparseMatrix trainMatrix = new SparseMatrix(rateMatrix);
		SparseMatrix testMatrix = new SparseMatrix(rateMatrix);

		for (int i = 0, im = rateMatrix.numColumns(); i < im; i++) {

			List<Integer> users = rateMatrix.getRows(i);

			for (int u : users) {

				double rand = Math.random();
				if (rand < ratio)
					// for training
					testMatrix.set(u, i, 0.0);
				else
					// for testing
					trainMatrix.set(u, i, 0.0);
			}

		}

		// remove zero entries
		SparseMatrix.reshape(trainMatrix);
		SparseMatrix.reshape(testMatrix);

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
		SparseMatrix testMatrix = new SparseMatrix(rateMatrix);

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

		// remove zero entries
		SparseMatrix.reshape(trainMatrix);
		SparseMatrix.reshape(testMatrix);

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
		SparseMatrix testMatrix = new SparseMatrix(rateMatrix);

		for (int u = 0, um = rateMatrix.numRows(); u < um; u++) {

			SparseVector items = rateMatrix.row(u);

			for (int j : items.getIndex()) {
				if (assignMatrix.get(u, j) == k)
					trainMatrix.set(u, j, 0.0); // keep test data and remove train data
				else
					testMatrix.set(u, j, 0.0); // keep train data and remove test data
			}
		}

		// remove zero entries
		SparseMatrix.reshape(trainMatrix);
		SparseMatrix.reshape(testMatrix);

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
