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

package librec.ext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import librec.data.SparseMatrix;
import librec.data.SparseVector;
import librec.intf.Recommender;
import librec.util.Lists;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

/**
 * Zhou et al., <strong>Solving the apparent diversity-accuracy dilemma of recommender systems</strong>, Proceedings of
 * the National Academy of Sciences, 2010.
 * 
 * @author guoguibing
 * 
 */
public class Hybrid extends Recommender {

	Table<Integer, Integer, Double> userItemRanks = HashBasedTable.create();
	Table<Integer, Integer, Double> heatScores = HashBasedTable.create();
	Table<Integer, Integer, Double> probScores = HashBasedTable.create();
	protected float lambda;

	Map<Integer, Integer> userDegrees = new HashMap<>();
	Map<Integer, Integer> itemDegrees = new HashMap<>();
	double maxProb = Double.MIN_VALUE, maxHeat = Double.MIN_VALUE;

	public Hybrid(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);

		algoName = "Hybrid (HeatS+ProbS)";
		isRankingPred = true;

		lambda = algoOptions.getFloat("-lambda");
	}

	@Override
	protected void initModel() throws Exception {
		for (int j = 0; j < numItems; j++)
			itemDegrees.put(j, trainMatrix.columnSize(j));
	}

	protected double ranking_basic(int u, int j) {

		// Note that in ranking, we first check a user u, and then check the
		// ranking score of each candidate items
		if (!heatScores.containsRow(u)) {
			// new user
			heatScores.clear();
			probScores.clear();

			SparseVector uv = trainMatrix.row(u);
			List<Integer> items = Lists.toList(uv.getIndex());

			// distribute resources to users, including user u
			Map<Integer, Double> userResources = new HashMap<>();
			for (int v = 0; v < numUsers; v++) {
				SparseVector vv = trainMatrix.row(v);
				double sum = 0.0;
				int kj = vv.getCount();
				for (int item : vv.getIndex())
					sum += items.contains(item) ? 1.0 : 0.0;

				userResources.put(v, kj > 0 ? sum / kj : 0.0);
			}

			// redistribute resources to items
			maxHeat = Double.MIN_VALUE;
			for (int i = 0; i < numItems; i++) {
				SparseVector iv = trainMatrix.column(i);
				double sum = 0;
				int kj = iv.getCount();
				for (int user : iv.getIndex())
					sum += userResources.get(user);

				double score = kj > 0 ? sum / kj : 0.0;
				heatScores.put(u, i, score);

				if (score > maxHeat)
					maxHeat = score;
			}

			// prob scores
			userResources.clear();
			for (int v = 0; v < numUsers; v++) {
				SparseVector vv = trainMatrix.row(v);
				double sum = 0.0;
				for (int item : vv.getIndex())
					sum += items.contains(item) ? 1.0 / itemDegrees.get(item) : 0.0;

				userResources.put(v, sum);
			}

			maxProb = Double.MIN_VALUE;
			for (int i = 0; i < numItems; i++) {
				SparseVector iv = trainMatrix.column(i);
				double score = 0;
				for (int user : iv.getIndex())
					score += userResources.get(user) / userDegrees.get(user);

				probScores.put(u, i, score);

				if (score > maxProb)
					maxProb = score;
			}
		}

		return heatScores.contains(u, j) ? heatScores.get(u, j) / maxHeat * (1 - lambda) + probScores.get(u, j)
				/ maxProb * lambda : 0.0;
	}

	protected double ranking(int u, int j) {

		// Note that in ranking, we first check a user u, and then check the
		// ranking score of each candidate items
		if (!userItemRanks.containsRow(u)) {
			// new user
			userItemRanks.clear();

			SparseVector uv = trainMatrix.row(u);
			List<Integer> items = Lists.toList(uv.getIndex());

			// distribute resources to users, including user u
			Map<Integer, Double> userResources = new HashMap<>();
			for (int v = 0; v < numUsers; v++) {
				SparseVector vv = trainMatrix.row(v);
				double sum = 0;
				int kj = vv.getCount();
				for (int item : vv.getIndex()) {
					if (items.contains(item))
						sum += 1.0 / Math.pow(itemDegrees.get(item), lambda);
				}

				if (kj > 0)
					userResources.put(v, sum / kj);
			}

			// redistribute resources to items
			for (int i = 0; i < numItems; i++) {
				if (items.contains(i))
					continue;

				SparseVector iv = trainMatrix.column(i);
				double sum = 0;
				for (int user : iv.getIndex())
					sum += userResources.containsKey(user) ? userResources.get(user) : 0.0;

				double score = sum / Math.pow(itemDegrees.get(i), 1 - lambda);
				userItemRanks.put(u, i, score);
			}
		}

		return userItemRanks.contains(u, j) ? userItemRanks.get(u, j) : 0.0;
	}

	/**
	 * for validity purpose
	 */
	protected double ProbS(int u, int j) {

		if (!userItemRanks.containsRow(u)) {

			userItemRanks.clear();

			SparseVector uv = trainMatrix.row(u);
			List<Integer> items = Lists.toList(uv.getIndex());

			// distribute resources to users, including user u
			Map<Integer, Double> userResources = new HashMap<>();
			for (int v = 0; v < numUsers; v++) {
				SparseVector vv = trainMatrix.row(v);
				double sum = 0;
				for (int item : vv.getIndex()) {
					if (items.contains(item))
						sum += 1.0 / itemDegrees.get(item);
				}

				userResources.put(v, sum);
			}

			// redistribute resources to items
			for (int i = 0; i < numItems; i++) {
				if (items.contains(i))
					continue;

				SparseVector iv = trainMatrix.column(i);
				double sum = 0;
				for (int user : iv.getIndex())
					sum += userResources.get(user) / userDegrees.get(user);

				double score = sum;
				userItemRanks.put(u, i, score);
			}
		}

		return userItemRanks.contains(u, j) ? userItemRanks.get(u, j) : 0.0;
	}

	@Override
	public String toString() {
		return super.toString() + "," + lambda;
	}
}
