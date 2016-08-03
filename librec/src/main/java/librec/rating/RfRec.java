// Copyright (C) 2014-2015 Guibing Guo
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

package librec.rating;

import librec.data.DenseMatrix;
import librec.data.DenseVector;
import librec.data.MatrixEntry;
import librec.data.SparseMatrix;
import librec.intf.IterativeRecommender;

/**
 * Gedikli et al., <strong>RF-Rec: Fast and Accurate Computation of Recommendations based on Rating
 * Frequencies</strong>, IEEE (CEC) 2011, Luxembourg, 2011, pp. 50-57. <br>
 * 
 * <strong>Remark:</strong> This implementation does not support half-star ratings.
 * 
 * @author bin wu
 * 
 */
public class RfRec extends IterativeRecommender {
	/**
	 * The average ratings of users
	 */
	private DenseVector userAverages;

	/**
	 * The average ratings of items
	 */
	private DenseVector itemAverages;

	/**
	 * The number of ratings per rating value per user
	 */
	private DenseMatrix userRatingFrequencies;

	/**
	 * The number of ratings per rating value per item
	 */
	private DenseMatrix itemRatingFrequencies;

	/**
	 * User weights learned by the gradient solver
	 */
	private DenseVector userWeights;

	/** Item weights learned by the gradient solver. */
	private DenseVector itemWeights;

	public RfRec(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);
	}

	@Override
	protected void initModel() throws Exception {
		// Calculate the average ratings
		userAverages = new DenseVector(numUsers);
		itemAverages = new DenseVector(numItems);
		userWeights = new DenseVector(numUsers);
		itemWeights = new DenseVector(numItems);
		for (int u = 0; u < numUsers; u++) {
			userAverages.set(u, trainMatrix.row(u).mean());
			userWeights.set(u, 0.6 + Math.random() * 0.01);
		}
		for (int j = 0; j < numItems; j++) {
			itemAverages.set(j, trainMatrix.column(j).mean());
			itemWeights.set(j, 0.4 + Math.random() * 0.01);
		}
		// Calculate the frequencies.
		// Users,items
		userRatingFrequencies = new DenseMatrix(numUsers, ratingScale.size());
		itemRatingFrequencies = new DenseMatrix(numItems, ratingScale.size());
		for (MatrixEntry me : trainMatrix) {
			int u = me.row();
			int j = me.column();
			int ruj = (int) me.get();
			userRatingFrequencies.add(u, ruj - 1, 1);
			itemRatingFrequencies.add(j, ruj - 1, 1);
		}
		userWeights = new DenseVector(numUsers);
		itemWeights = new DenseVector(numItems);
	}

	@Override
	protected void buildModel() throws Exception {
		for (int i = 1; i <= numIters; i++) {
			loss = 0;
			for (MatrixEntry me : trainMatrix) {
				int u = me.row();
				int j = me.column();
				double ruj = me.get();

				double pred = predict(u, j);
				double euj = ruj - pred;
				loss += euj * euj;

				double userWeight = userWeights.get(u) + lRate * (euj - regU * userWeights.get(u));
				userWeights.set(u, userWeight);

				// Gradient-Step on item weights.
				double itemWeight = itemWeights.get(j) + lRate * (euj - regI * itemWeights.get(j));
				itemWeights.set(j, itemWeight);
				
				loss += regU * userWeights.get(u) * userWeights.get(u) + regI * itemWeights.get(j) * itemWeights.get(j);

			}
			loss *= 0.5;

			if (isConverged(i))
				break;
		}
	}

	/**
	 * Returns 1 if the rating is similar to the rounded average value
	 * 
	 * @param avg
	 *            the average
	 * @param rating
	 *            the rating
	 * @return 1 when the values are equal
	 */
	private int isAvgRating(double avg, int rating) {
		return Math.round(avg) == rating ? 1 : 0;
	}

	public double predict(int u, int j) {

		double estimate = globalMean;
		float enumeratorUser = 0;
		float denominatorUser = 0;
		float enumeratorItem = 0;
		float denominatorItem = 0;
		if (userRatingFrequencies.row(u).sum() > 0 && itemRatingFrequencies.row(j).sum() > 0 && userAverages.get(u) > 0
				&& itemAverages.get(j) > 0) {
			// Go through all the possible rating values
			for (int r = 0; r < ratingScale.size(); ++r) {
				int ratingValue = (int) Math.round(ratingScale.get(r));
				// user component
				int tmpUser = 0;
				double frequencyInt = userRatingFrequencies.get(u, ratingValue - 1);
				int frequency = (int) frequencyInt;
				tmpUser = frequency + 1 + isAvgRating(userAverages.get(u), ratingValue);
				enumeratorUser += tmpUser * ratingValue;
				denominatorUser += tmpUser;

				// item component
				int tmpItem = 0;
				frequency = 0;
				frequencyInt = itemRatingFrequencies.get(j, ratingValue - 1);
				frequency = (int) frequencyInt;
				tmpItem = frequency + 1 + isAvgRating(itemAverages.get(j), ratingValue);
				enumeratorItem += tmpItem * ratingValue;
				denominatorItem += tmpItem;
			}

			double w_u = userWeights.get(u);
			double w_i = itemWeights.get(j);
			float pred_ui_user = enumeratorUser / denominatorUser;
			float pred_ui_item = enumeratorItem / denominatorItem;
			estimate = (float) w_u * pred_ui_user + (float) w_i * pred_ui_item;

		} else {
			// if the user or item weren't known in the training phase...
			if (userRatingFrequencies.row(u).sum() == 0 || userAverages.get(u) == 0) {
				double iavg = itemAverages.get(j);
				if (iavg != 0) {
					return iavg;
				} else {

					return globalMean;
				}
			}
			if (itemRatingFrequencies.row(j).sum() == 0 || itemAverages.get(j) == 0) {
				double uavg = userAverages.get(u);
				if (uavg != 0) {
					return uavg;
				} else {
					// Some heuristic -> a bit above the average rating
					return globalMean;
				}
			}
		}
		return estimate;
	}
}
