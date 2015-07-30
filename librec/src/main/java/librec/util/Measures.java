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

package librec.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reimplemenmt <code>MyMediaLite.Eval.Measures</code> namespace.
 * 
 * @author guoguibing
 * 
 */
public class Measures {

	/**
	 * Compute the average precision (AP) of a list of ranked items
	 * 
	 * @param <T>
	 * 
	 * @param rankedList
	 *            a list of ranked item IDs, the highest-ranking item first
	 * @param groudTruth
	 *            a collection of positive/correct item IDs
	 * @return the AP for the given list
	 */
	public static <T> double AP(List<T> rankedList, List<T> groundTruth) {
		int hits = 0;
		double sum_precs = 0;

		for (int n = 0, m = rankedList.size(); n < m; n++) {
			T item = rankedList.get(n);

			if (groundTruth.contains(item)) {
				hits++;
				sum_precs += hits / (n + 1.0); // prec@n
			}
		}

		if (hits > 0)
			return sum_precs / groundTruth.size();
		else
			return 0.0;
	}

	/**
	 * Compute the precision at N of a list of ranked items at several N
	 * 
	 * @param <T>
	 * 
	 * @param rankedList
	 *            a list of ranked item IDs, the highest-ranking item first
	 * @param groundTruth
	 *            a collection of positive/correct item IDs
	 * @param ns
	 *            the cutoff positions in the list
	 * @return {N: prec@N}: the precision at N for the given data at the
	 *         different positions N
	 */
	public static <T> Map<Integer, Double> PrecAt(List<T> rankedList, List<T> groundTruth, List<Integer> ns) {
		Map<Integer, Double> prec_at_n = new HashMap<>();
		for (int n : ns)
			prec_at_n.put(n, PrecAt(rankedList, groundTruth, n));

		return prec_at_n;
	}

	/**
	 * Compute the precision at N of a list of ranked items
	 * 
	 * @param <T>
	 * 
	 * @param rankedList
	 *            a list of ranked item IDs, the highest-ranking item first
	 * @param groundTruth
	 *            a collection of positive/correct item IDs
	 * @param n
	 *            the cutoff position in the list
	 * @return the precision at N for the given data
	 */
	public static <T> double PrecAt(List<T> rankedList, List<T> groundTruth, int n) {
		return HitsAt(rankedList, groundTruth, n) / (n + 0.0);
	}

	/**
	 * Compute the precision at N of a list of ranked items at several N
	 * 
	 * @param <T>
	 * 
	 * @param rankedList
	 *            a list of ranked item IDs, the highest-ranking item first
	 * @param groundTruth
	 *            a collection of positive/correct item IDs
	 * @param ns
	 *            the cutoff positions in the list
	 * @return {N: recall@N}: the precision at N for the given data at the
	 *         different positions N
	 */
	public static <T> Map<Integer, Double> RecallAt(List<T> rankedList, List<T> groundTruth, List<Integer> ns) {
		Map<Integer, Double> recall_at_n = new HashMap<>();
		for (int n : ns)
			recall_at_n.put(n, RecallAt(rankedList, groundTruth, n));

		return recall_at_n;
	}

	/**
	 * Compute the precision at N of a list of ranked items
	 * 
	 * @param <T>
	 * 
	 * @param rankedList
	 *            a list of ranked item IDs, the highest-ranking item first
	 * @param groundTruth
	 *            a collection of positive/correct item IDs
	 * @param n
	 *            the cutoff position in the list
	 * @return the recall at N for the given data
	 */
	public static <T> Double RecallAt(List<T> rankedList, List<T> groundTruth, int n) {
		return HitsAt(rankedList, groundTruth, n) / (groundTruth.size() + 0.0);
	}

	/**
	 * Compute the number of hits until position N of a list of ranked items
	 * 
	 * @param <T>
	 * 
	 * @param rankedList
	 *            a list of ranked item IDs, the highest-ranking item first
	 * @param groundTruth
	 *            a collection of positive/correct item IDs
	 * @param n
	 *            the cutoff position in the list
	 * @return the hits at N for the given data
	 */
	public static <T> int HitsAt(List<T> rankedList, List<T> groundTruth, int n) {
		int hits = 0;

		for (int i = 0, k = rankedList.size(); i < k; i++) {
			T item = rankedList.get(i);

			if (!groundTruth.contains(item))
				continue;

			if (i < n)
				hits++;
			else
				break;
		}

		return hits;
	}

	/**
	 * Compute the normalized cumulative gain (NDCG) of a list of ranked items
	 * 
	 * @param <T>
	 * 
	 * @param rankedList
	 *            a list of ranked item IDs, the highest-ranking item first
	 * @param groundTruth
	 *            a collection of positive/correct item IDs
	 * @return the NDCG for the given data
	 */
	public static <T> double nDCG(List<T> rankedList, List<T> groundTruth) {
		double dcg = 0;
		double idcg = IDCG(groundTruth.size());

		for (int i = 0, n = rankedList.size(); i < n; i++) {
			T item_id = rankedList.get(i);

			if (!groundTruth.contains(item_id))
				continue;

			// compute NDCG part
			int rank = i + 1;
			dcg += 1 / Maths.log(rank + 1, 2);
		}

		return dcg / idcg;
	}

	/**
	 * Compute the ideal DCG given the number of positive items
	 * 
	 * @param n
	 *            the number of positive items
	 * @return the ideal DCG
	 */
	public static double IDCG(int n) {
		double idcg = 0;
		for (int i = 0; i < n; i++)
			idcg += 1 / Maths.log(i + 2, 2);
		return idcg;
	}

	/**
	 * Compute the reciprocal rank of a list of ranked items
	 * 
	 * @param <T>
	 * 
	 * @param rankedList
	 *            a list of ranked item IDs, the highest-ranking item first
	 * @param groundTruth
	 *            a collection of positive/correct item IDs
	 * @return the mean reciprocal rank for the given data<
	 */
	public static <T> double RR(List<T> rankedList, List<T> groundTruth) {

		for (int i = 0, n = rankedList.size(); i < n; i++) {

			T item = rankedList.get(i);
			if (groundTruth.contains(item))
				return 1 / (i + 1.0);
		}

		return 0;
	}

	/**
	 * Compute the area under the ROC curve (AUC) of a list of ranked items
	 * 
	 * @param <T>
	 * 
	 * @param rankedList
	 *            a list of ranked item IDs, the highest-ranking item first
	 * @param groundTruth
	 *            a collection of positive/correct item IDs
	 * @param num_dropped_items
	 *            the number of relevant items that were not ranked (considered
	 *            to be ranked below all ranked_items)
	 * @return the AUC for the given data
	 * @throws Exception
	 */
	public static <T> double AUC(List<T> rankedList, List<T> groundTruth, int num_dropped_items) {

		int num_rele_items = Lists.overlapSize(groundTruth, rankedList);
		int num_eval_items = rankedList.size() + num_dropped_items;
		int num_eval_pairs = (num_eval_items - num_rele_items) * num_rele_items;
		if (num_eval_pairs < 0) {
			Logs.error("num_eval_pairs cannot be less than 0");
			System.exit(-1);
		}

		if (num_eval_pairs == 0)
			return 0.5;

		int num_correct_pairs = 0;
		int hits = 0;
		for (T item_id : rankedList)
			if (!groundTruth.contains(item_id))
				num_correct_pairs += hits;
			else
				hits++;

		int num_miss_items = Lists.exceptSize(groundTruth, rankedList);
		num_correct_pairs += hits * (num_dropped_items - num_miss_items);

		return (num_correct_pairs + 0.0) / num_eval_pairs;
	}

	/**
	 * Asymmetric loss function: the asymmetric loss captures the fact that
	 * recommending bad movies as good movies is worse than recommending good
	 * movies as bad.
	 * 
	 * @param rate
	 *            real rating
	 * @param pred
	 *            predicted rating
	 * @param minRate
	 *            mininmum rating scale
	 * @param maxRate
	 *            maximum rating scale
	 * @return Asymmetric loss value
	 */
	/*
	 * Example of asymmetric loss matrix:
	 * 
	 * {0, 0, 0, 7.5, 10, 12.5}, {0, 0, 0, 4, 6, 8}, {0, 0, 0, 1.5, 3, 4.5}, {3,
	 * 2, 1, 0, 0, 0}, {4, 3, 2, 0, 0, 0}, {5, 4, 3, 0, 0, 0}
	 */
	public static double ASYMMLoss(double rate, double pred, double minRate, double maxRate) {

		// median value
		double med = (minRate + maxRate) / 2.0;
		double loss = 0;

		if (rate <= med && pred <= med) {
			loss = 0;
		} else if (rate > med && pred <= med) {
			// good movies recommended as bad
			loss = rate - pred;
		} else if (rate <= med && pred > med) {
			// bad movies recommended as good, more penalty
			loss = (pred - rate) * (1 + (med - rate + 1) * 0.5);
		} else {
			loss = 0;
		}

		return loss;
	}

}
