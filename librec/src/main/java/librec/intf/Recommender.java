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

package librec.intf;

import happy.coding.io.Configer;
import happy.coding.io.KeyValPair;
import happy.coding.io.Lists;
import happy.coding.io.Logs;
import happy.coding.math.Measures;
import happy.coding.math.Randoms;
import happy.coding.math.Sims;
import happy.coding.math.Stats;
import happy.coding.system.Dates;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import librec.data.DataDAO;
import librec.data.MatrixEntry;
import librec.data.SparseMatrix;
import librec.data.SparseVector;
import librec.data.SymmMatrix;
import librec.data.VectorEntry;

import com.google.common.base.Stopwatch;

/**
 * General recommenders
 * 
 * @author Guibing Guo
 */
public abstract class Recommender implements Runnable {

	/************************************ Static parameters for all recommenders ***********************************/
	// configer
	public static Configer cf;
	// matrix of rating data
	public static SparseMatrix rateMatrix;

	// params used for multiple runs
	public static Map<String, List<Double>> params;

	// verbose
	protected static boolean verbose;
	// is ranking/rating prediction
	public static boolean isRankingPred;
	// is diversity-based measures used
	protected static boolean isDiverseUsed;
	// view of rating predictions
	protected static String view;

	// rate DAO object
	public static DataDAO rateDao;

	// number of users, items, ratings
	protected static int numUsers, numItems, numRates;
	// number of recommended items
	protected static int numRecs, numIgnore;

	// a list of rating scalses
	protected static List<Double> scales;
	// Maximum, minimum values of rating scales
	protected static double maxRate, minRate;
	// init mean and standard deviation
	protected static double initMean, initStd;

	/************************************ Recommender-specific parameters ****************************************/
	// algorithm's name
	public String algoName;
	// current fold
	protected int fold;
	// fold information
	protected String foldInfo;

	// rating matrix for training and testing
	protected SparseMatrix trainMatrix, testMatrix;

	// upper symmetric matrix of item-item correlations
	protected SymmMatrix corrs;

	// performance measures
	public Map<Measure, Double> measures;
	// global average of training rates
	protected double globalMean;

	public enum Measure {
		MAE, RMSE, NMAE, ASYMM, D5, D10, Pre5, Pre10, Rec5, Rec10, MAP, MRR, NDCG, AUC, TrainTime, TestTime
	}

	/**
	 * Constructor for Recommender
	 * 
	 * @param trainMatrix
	 *            train matrix
	 * @param testMatrix
	 *            test matrix
	 */
	public Recommender(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		this.trainMatrix = trainMatrix;
		this.testMatrix = testMatrix;
		this.fold = fold;

		// config recommender
		if (cf == null || rateMatrix == null) {
			Logs.error("Recommender is not well configed");
			System.exit(-1);
		}

		// global mean
		numRates = trainMatrix.size();
		globalMean = trainMatrix.sum() / numRates;

		// fold info
		foldInfo = fold > 0 ? " fold [" + fold + "]" : "";

		// compute item-item correlations
		if (isRankingPred && isDiverseUsed)
			corrs = new SymmMatrix(numItems);

		// static initialization, only done once
		if (scales == null) {
			initMean = 0.0;
			initStd = 0.1;

			scales = rateDao.getScales();
			minRate = scales.get(0);
			maxRate = scales.get(scales.size() - 1);

			numUsers = rateDao.numUsers();
			numItems = rateDao.numItems();

			verbose = cf.isOn("is.verbose");
			isRankingPred = cf.isOn("is.ranking.pred");
			isDiverseUsed = cf.isOn("is.diverse.used");
			view = cf.getString("rating.pred.view").toLowerCase();

			// -1 to use as many as possible or disable
			numRecs = cf.getInt("num.reclist.len");
			numIgnore = cf.getInt("num.ignor.items");

			// initial random seed
			int seed = cf.getInt("num.rand.seed");
			Randoms.seed(seed <= 0 ? System.currentTimeMillis() : seed);
		}
	}

	public void run() {
		execute();
	}

	/**
	 * execution method of a recommender
	 * 
	 */
	public void execute() {

		Stopwatch sw = Stopwatch.createStarted();
		initModel();

		// print out algorithm's settings: to indicate starting building models
		String algoInfo = toString();
		if (!algoInfo.isEmpty())
			Logs.debug(algoName + ": " + algoInfo);

		buildModel();
		long trainTime = sw.elapsed(TimeUnit.MILLISECONDS);

		// evaluation
		String foldStr = fold > 0 ? " fold [" + fold + "]" : "";
		if (verbose)
			Logs.debug("{}{}: evaluate testing data ... ", algoName, foldStr);
		measures = isRankingPred ? evalRankings() : evalRatings();
		String result = getEvalInfo(measures);
		sw.stop();
		long testTime = sw.elapsed(TimeUnit.MILLISECONDS) - trainTime;

		// collecting results
		measures.put(Measure.TrainTime, (double) trainTime);
		measures.put(Measure.TestTime, (double) testTime);

		String evalInfo = algoName + foldStr + ": " + result + "\tTime: "
				+ Dates.parse(measures.get(Measure.TrainTime).longValue()) + ", "
				+ Dates.parse(measures.get(Measure.TestTime).longValue());
		if (!isRankingPred)
			evalInfo += "\tView: " + view;

		if (fold > 0)
			Logs.debug(evalInfo);
	}

	/**
	 * @return the evaluation information of a recommend
	 */
	public static String getEvalInfo(Map<Measure, Double> measures) {
		String evalInfo = null;
		if (isRankingPred) {
			// Note: MAE and RMSE are computed, but not used here 
			// ....  if you need them, add it back in the same manner as other metrics
			if (isDiverseUsed)
				evalInfo = String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%2d", measures.get(Measure.D5),
						measures.get(Measure.D10), measures.get(Measure.Pre5), measures.get(Measure.Pre10),
						measures.get(Measure.Rec5), measures.get(Measure.Rec10), measures.get(Measure.AUC),
						measures.get(Measure.MAP), measures.get(Measure.NDCG), measures.get(Measure.MRR), numIgnore);
			else
				evalInfo = String.format("%s,%s,%s,%s,%s,%s,%s,%s,%2d", measures.get(Measure.Pre5),
						measures.get(Measure.Pre10), measures.get(Measure.Rec5), measures.get(Measure.Rec10),
						measures.get(Measure.AUC), measures.get(Measure.MAP), measures.get(Measure.NDCG),
						measures.get(Measure.MRR), numIgnore);
		} else
			evalInfo = String.format("%.3f,%.3f,%.3f,%.3f", measures.get(Measure.MAE), measures.get(Measure.RMSE),
					measures.get(Measure.NMAE), measures.get(Measure.ASYMM));

		return evalInfo;
	}

	/**
	 * initilize recommender model
	 */
	protected void initModel() {
	}

	/**
	 * build user-user or item-item correlation matrix from training data
	 * 
	 * @param isUser
	 *            whether it is user-user correlation matrix
	 * 
	 * @return a upper symmetric matrix with user-user or item-item coefficients
	 * 
	 */
	protected SymmMatrix buildCorrs(boolean isUser) {
		Logs.debug("Build {} similarity matrix ...", isUser ? "user" : "item");

		int count = isUser ? numUsers : numItems;
		SymmMatrix corrs = new SymmMatrix(count);

		for (int i = 0; i < count; i++) {
			SparseVector iv = isUser ? trainMatrix.row(i) : trainMatrix.column(i);
			if (iv.getCount() == 0)
				continue;
			// user/item itself exclusive
			for (int j = i + 1; j < count; j++) {
				SparseVector jv = isUser ? trainMatrix.row(j) : trainMatrix.column(j);

				double sim = correlation(iv, jv);

				if (!Double.isNaN(sim))
					corrs.set(i, j, sim);
			}
		}

		return corrs;
	}

	/**
	 * Compute the correlation between two vectors using method specified by
	 * configuration key "similarity"
	 * 
	 * @param iv
	 *            vector i
	 * @param jv
	 *            vector j
	 * @return the correlation between vectors i and j
	 */
	protected double correlation(SparseVector iv, SparseVector jv) {
		return correlation(iv, jv, cf.getString("similarity"));
	}

	/**
	 * Compute the correlation between two vectors for a specific method
	 * 
	 * @param iv
	 *            vector i
	 * @param jv
	 *            vector j
	 * @param method
	 *            similarity method
	 * @return the correlation between vectors i and j; return NaN if the
	 *         correlation is not computable.
	 */
	protected double correlation(SparseVector iv, SparseVector jv, String method) {

		// compute similarity
		List<Double> is = new ArrayList<>();
		List<Double> js = new ArrayList<>();

		for (Integer idx : jv.getIndex()) {
			if (iv.contains(idx)) {
				is.add(iv.get(idx));
				js.add(jv.get(idx));
			}
		}

		double sim = 0;
		switch (method.toLowerCase()) {
		case "cos":
			// for real ratings
			sim = Sims.cos(is, js);
			break;
		case "cos-binary":
			// for binary ratings
			sim = iv.inner(jv) / (Math.sqrt(iv.inner(iv)) * Math.sqrt(jv.inner(jv)));
			break;
		case "msd":
			sim = Sims.msd(is, js);
			break;
		case "cpc":
			sim = Sims.cpc(is, js, (minRate + maxRate) / 2.0);
			break;
		case "exjaccard":
			sim = Sims.exJaccard(is, js);
			break;
		case "pcc":
		default:
			sim = Sims.pcc(is, js);
			break;
		}

		// shrink to account for vector size
		if (!Double.isNaN(sim)) {
			int n = is.size();
			int shrinkage = cf.getInt("num.shrinkage");
			if (shrinkage > 0)
				sim *= n / (n + shrinkage + 0.0);
		}

		return sim;
	}

	/**
	 * Learning method: override this method to build a model, for a model-based
	 * method. Default implementation is useful for memory-based methods.
	 * 
	 */
	protected void buildModel() {
	}

	/**
	 * determine whether the rating of a user-item (u, j) is used to predicted
	 * 
	 */
	protected boolean isTestable(int u, int j) {
		switch (view) {
		case "cold-start":
			return trainMatrix.rowSize(u) < 5 ? true : false;
		case "all":
		default:
			return true;
		}
	}

	/**
	 * @return the evaluation results of rating predictions
	 */
	private Map<Measure, Double> evalRatings() {

		Map<Measure, Double> measures = new HashMap<>();

		double sum_maes = 0, sum_mses = 0, sum_asyms = 0;
		int numCount = 0;
		for (MatrixEntry me : testMatrix) {
			double rate = me.get();
			if (rate <= 0)
				continue;

			int u = me.row();
			int j = me.column();

			if (!isTestable(u, j))
				continue;

			double pred = predict(u, j, true);
			if (Double.isNaN(pred))
				continue;

			double err = rate - pred;

			sum_maes += Math.abs(err);
			sum_mses += err * err;
			sum_asyms += Measures.ASYMMLoss(rate, pred, minRate, maxRate);
			numCount++;
		}

		double mae = sum_maes / numCount;
		double rmse = Math.sqrt(sum_mses / numCount);
		double asymm = sum_asyms / numCount;

		measures.put(Measure.MAE, mae);
		// normalized MAE: useful for direct comparison between two systems
		// using different rating scales.
		measures.put(Measure.NMAE, mae / (maxRate - minRate));
		measures.put(Measure.RMSE, rmse);
		measures.put(Measure.ASYMM, asymm);

		return measures;
	}

	/**
	 * @return the evaluation results of ranking predictions
	 */
	private Map<Measure, Double> evalRankings() {

		Map<Measure, Double> measures = new HashMap<>();

		List<Double> ds5 = new ArrayList<>();
		List<Double> ds10 = new ArrayList<>();

		List<Double> precs5 = new ArrayList<>();
		List<Double> precs10 = new ArrayList<>();
		List<Double> recalls5 = new ArrayList<>();
		List<Double> recalls10 = new ArrayList<>();
		List<Double> aps = new ArrayList<>();
		List<Double> rrs = new ArrayList<>();
		List<Double> aucs = new ArrayList<>();
		List<Double> ndcgs = new ArrayList<>();

		List<Double> maes = new ArrayList<>();
		List<Double> rmses = new ArrayList<>();

		// candidate items for all users: here only training items
		List<Integer> candItems = trainMatrix.columns();

		if (verbose)
			Logs.debug("{}{} has candidate items: {}", algoName, foldInfo, candItems.size());

		// ignore items for all users: most popular items
		if (numIgnore > 0) {
			List<Integer> ignoreItems = new ArrayList<>();

			Map<Integer, Integer> itemDegs = new HashMap<>();
			for (int j : candItems)
				itemDegs.put(j, trainMatrix.columnSize(j));
			List<KeyValPair<Integer>> sortedDegrees = Lists.sortMap(itemDegs, true);
			int k = 0;
			for (KeyValPair<Integer> deg : sortedDegrees) {
				ignoreItems.add(deg.getKey());
				if (++k >= numIgnore)
					break;
			}

			// remove ignore items from candidate items
			candItems.removeAll(ignoreItems);
		}

		// for each test user
		for (int u = 0, um = testMatrix.numRows(); u < um; u++) {

			// make a copy: candidate items for each user
			List<Integer> pCandItems = new ArrayList<>(candItems);

			// get positive items from testing data
			SparseVector tv = testMatrix.row(u);
			List<Integer> correctItems = new ArrayList<>();

			// get overall MAE and RMSE -- not preferred for ranking
			for (Integer j : tv.getIndex()) {
				// intersect with the candidate items
				if (pCandItems.contains(j))
					correctItems.add(j);

				double pred = predict(u, j, true);
				if (!Double.isNaN(pred)) {
					double rate = tv.get(j);
					double euj = rate - pred;

					maes.add(Math.abs(euj));
					rmses.add(euj * euj);
				}
			}
			if (correctItems.size() == 0)
				continue; // no testing data for user u

			// remove rated items from candidate items
			SparseVector rv = trainMatrix.row(u);
			for (VectorEntry ve : rv)
				pCandItems.remove((Integer) ve.index());

			// number of candidate items for this user
			int numCand = pCandItems.size();

			// predict the ranking scores of all candidate items
			Map<Integer, Double> itemScores = ranking(u, pCandItems);

			// order the ranking scores from highest to lowest
			List<Integer> rankedItems = new ArrayList<>();
			if (itemScores.size() > 0) {

				List<KeyValPair<Integer>> sorted = Lists.sortMap(itemScores, true);
				List<KeyValPair<Integer>> recomd = (numRecs < 0 || sorted.size() <= numRecs) ? sorted : sorted.subList(
						0, numRecs);

				for (KeyValPair<Integer> kv : recomd)
					rankedItems.add(kv.getKey());
			}

			if (rankedItems.size() == 0)
				continue; // no recommendations available for user u

			int numDropped = numCand - rankedItems.size();
			double AUC = Measures.AUC(rankedItems, correctItems, numDropped);
			double AP = Measures.AP(rankedItems, correctItems);
			double nDCG = Measures.nDCG(rankedItems, correctItems);
			double RR = Measures.RR(rankedItems, correctItems);

			if (isDiverseUsed) {
				double d5 = diverseAt(rankedItems, 5);
				double d10 = diverseAt(rankedItems, 10);

				ds5.add(d5);
				ds10.add(d10);
			}

			List<Integer> cutoffs = Arrays.asList(5, 10);
			Map<Integer, Double> precs = Measures.PrecAt(rankedItems, correctItems, cutoffs);
			Map<Integer, Double> recalls = Measures.RecallAt(rankedItems, correctItems, cutoffs);

			precs5.add(precs.get(5));
			precs10.add(precs.get(10));
			recalls5.add(recalls.get(5));
			recalls10.add(recalls.get(10));

			aucs.add(AUC);
			aps.add(AP);
			rrs.add(RR);
			ndcgs.add(nDCG);
		}

		measures.put(Measure.D5, isDiverseUsed ? Stats.mean(ds5) : 0.0);
		measures.put(Measure.D10, isDiverseUsed ? Stats.mean(ds10) : 0.0);
		measures.put(Measure.Pre5, Stats.mean(precs5));
		measures.put(Measure.Pre10, Stats.mean(precs10));
		measures.put(Measure.Rec5, Stats.mean(recalls5));
		measures.put(Measure.Rec10, Stats.mean(recalls10));
		measures.put(Measure.AUC, Stats.mean(aucs));
		measures.put(Measure.NDCG, Stats.mean(ndcgs));
		measures.put(Measure.MAP, Stats.mean(aps));
		measures.put(Measure.MRR, Stats.mean(rrs));

		measures.put(Measure.MAE, Stats.mean(maes));
		measures.put(Measure.RMSE, Stats.mean(rmses));

		return measures;
	}

	/**
	 * predict a specific rating for user u on item j. It is useful for
	 * evalution which requires predictions are bounded.
	 * 
	 * @param u
	 *            user id
	 * @param j
	 *            item id
	 * @param bound
	 *            whether to bound the prediction
	 * @return prediction
	 */
	protected double predict(int u, int j, boolean bound) {
		double pred = predict(u, j);

		if (bound) {
			if (pred > maxRate)
				pred = maxRate;
			if (pred < minRate)
				pred = minRate;
		}

		return pred;
	}

	/**
	 * Convert a rating (given by user {@code u} on item {@code j}) into a
	 * binary value, useful for the task of item ranking
	 * 
	 * @param u
	 *            user id
	 * @param j
	 *            item id
	 * @param ruj
	 *            rating value
	 * 
	 * @return a binarized rating value
	 */
	protected double binary(int u, int j, double ruj) {
		return ruj > 0 ? 1 : 0;
	}

	/**
	 * 
	 * Binarize a given matrix by applying the {@link binary(int, int, double)}
	 * method. Note that the given matrix will be changed directly.
	 * 
	 * @param mat
	 *            a given matrix to be binarized
	 */
	protected void binary(SparseMatrix mat) {
		for (MatrixEntry me : mat) {
			int u = me.row();
			int j = me.column();
			double ruj = me.get();

			me.set(binary(u, j, ruj));
		}
	}

	/**
	 * predict a specific rating for user u on item j, note that the prediction
	 * is not bounded. It is useful for building models with no need to bound
	 * predictions.
	 * 
	 * @param u
	 *            user id
	 * @param j
	 *            item id
	 * @return raw prediction without bounded
	 */
	protected double predict(int u, int j) {
		return globalMean;
	}

	/**
	 * predict a ranking score for user u on item j: default case using the
	 * unbounded predicted rating values
	 * 
	 * @param u
	 *            user id
	 * 
	 * @param j
	 *            item id
	 * @return a ranking score for user u on item j
	 */
	protected double ranking(int u, int j) {
		return predict(u, j, false);
	}

	/**
	 * compute ranking scores for a list of candidate items
	 * 
	 * @param u
	 *            user id
	 * @param candItems
	 *            candidate items
	 * @return a map of {item, ranking scores}
	 */
	protected Map<Integer, Double> ranking(int u, Collection<Integer> candItems) {

		Map<Integer, Double> itemRanks = new HashMap<>();
		for (Integer j : candItems) {
			double rank = ranking(u, j);
			if (!Double.isNaN(rank))
				itemRanks.put(j, rank);
		}

		return itemRanks;
	}

	/**
	 * 
	 * @param rankedItems
	 *            the list of ranked items to be recommended
	 * @param cutoff
	 *            cutoff in the list
	 * @param corrs
	 *            correlations between items
	 * @return diversity at a specific cutoff position
	 */
	protected double diverseAt(List<Integer> rankedItems, int cutoff) {

		int num = 0;
		double sum = 0.0;
		for (int id = 0; id < cutoff; id++) {
			int i = rankedItems.get(id);
			SparseVector iv = trainMatrix.column(i);

			for (int jd = id + 1; jd < cutoff; jd++) {
				int j = rankedItems.get(jd);

				double corr = corrs.get(i, j);
				if (corr == 0) {
					// if not found
					corr = correlation(iv, trainMatrix.column(j));
					if (!Double.isNaN(corr))
						corrs.set(i, j, corr);
				}

				if (!Double.isNaN(corr)) {
					sum += (1 - corr);
					num++;
				}
			}
		}

		return 0.5 * (sum / num);
	}

	/**
	 * Below are a set of mathematical functions. As many recommenders often
	 * adopts them, for conveniency's sake, we put these functions in the base
	 * Recommender class, though they belong to Math class.
	 * 
	 */

	/**
	 * logistic function g(x)
	 */
	protected double g(double x) {
		return 1.0 / (1 + Math.exp(-x));
	}

	/**
	 * gradient value of logistic function g(x)
	 */
	protected double gd(double x) {
		return g(x) * g(-x);
	}

	/**
	 * @param x
	 *            input value
	 * @param mu
	 *            mean of normal distribution
	 * @param sigma
	 *            standard deviation of normation distribution
	 * 
	 * @return a gaussian value with mean {@code mu} and standard deviation
	 *         {@code sigma};
	 */
	protected double gaussian(double x, double mu, double sigma) {
		return Math.exp(-0.5 * Math.pow(x - mu, 2) / (sigma * sigma));
	}

	/**
	 * normalize a rating to the region (0, 1)
	 */
	protected double normalize(double rate) {
		return (rate - minRate) / (maxRate - minRate);
	}

	/**
	 * 
	 * denormalize a prediction to the region (minRate, maxRate)
	 */
	protected double denormalize(double pred) {
		return minRate + pred * (maxRate - minRate);
	}

	/**
	 * useful to print out specific recommender's settings
	 */
	@Override
	public String toString() {
		return "";
	}
}
