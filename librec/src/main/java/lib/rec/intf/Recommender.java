package lib.rec.intf;

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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import lib.rec.data.DataDAO;
import lib.rec.data.MatrixEntry;
import lib.rec.data.SparseMatrix;
import lib.rec.data.SparseVector;
import lib.rec.data.SymmMatrix;

import com.google.common.base.Stopwatch;

/**
 * Interface class for general recommendation system.
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
		globalMean = Stats.sum(trainMatrix.getData()) / numRates;

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
		String result = getEvalInfo(measures, isRankingPred);
		sw.stop();
		long testTime = sw.elapsed(TimeUnit.MILLISECONDS) - trainTime;

		// collecting results
		measures.put(Measure.TrainTime, (double) trainTime);
		measures.put(Measure.TestTime, (double) testTime);

		String evalInfo = algoName + foldStr + ": " + result + "\tTime: "
				+ Dates.parse(measures.get(Measure.TrainTime).longValue()) + ", "
				+ Dates.parse(measures.get(Measure.TestTime).longValue());
		if (fold > 0)
			Logs.debug(evalInfo);
	}

	public static String getEvalInfo(Map<Measure, Double> measures, boolean isRankingPred) {
		String evalInfo = null;
		if (isRankingPred) {
			if (isDiverseUsed)
				evalInfo = String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%2d", measures.get(Measure.D5),
						measures.get(Measure.D10), measures.get(Measure.MAE), measures.get(Measure.RMSE),
						measures.get(Measure.Pre5), measures.get(Measure.Pre10), measures.get(Measure.Rec5),
						measures.get(Measure.Rec10), measures.get(Measure.AUC), measures.get(Measure.MAP),
						measures.get(Measure.NDCG), measures.get(Measure.MRR), numIgnore);
			else
				evalInfo = String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%2d", measures.get(Measure.MAE),
						measures.get(Measure.RMSE), measures.get(Measure.Pre5), measures.get(Measure.Pre10),
						measures.get(Measure.Rec5), measures.get(Measure.Rec10), measures.get(Measure.AUC),
						measures.get(Measure.MAP), measures.get(Measure.NDCG), measures.get(Measure.MRR), numIgnore);
		} else
			evalInfo = String.format("%.3f,%.3f", measures.get(Measure.MAE), measures.get(Measure.RMSE));
					// measures.get(Measure.NMAE), measures.get(Measure.ASYMM));

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

		int numCount = isUser ? numUsers : numItems;
		SymmMatrix corrs = new SymmMatrix(numCount);

		for (int i = 0; i < numCount; i++) {
			SparseVector iv = isUser ? trainMatrix.row(i) : trainMatrix.col(i);
			if (iv.getUsed() == 0)
				continue;

			for (int j = i + 1; j < numCount; j++) {
				SparseVector jv = isUser ? trainMatrix.row(j) : trainMatrix.col(j);

				double sim = compCorr(iv, jv);

				if (sim != 0.0)
					corrs.set(i, j, sim);
			}
		}

		return corrs;
	}

	/**
	 * Compute the correlation between two vectors
	 * 
	 * @param iv
	 *            vector i
	 * @param jv
	 *            vector j
	 * @return the correlation between vectors i and j
	 */
	protected double compCorr(SparseVector iv, SparseVector jv) {

		// compute similarity
		List<Double> is = new ArrayList<>();
		List<Double> js = new ArrayList<>();

		for (Integer item : jv.getIndex()) {
			if (iv.contains(item)) {
				is.add(iv.get(item));
				js.add(jv.get(item));
			}
		}

		double sim = 0;
		switch (cf.getString("similarity").toLowerCase()) {
		case "cos":
			sim = Sims.cos(is, js);
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

		if (Double.isNaN(sim))
			sim = 0.0; // 0: no correlation

		// shrink to account for vector size
		int n = is.size();
		int shrinkage = cf.getInt("num.shrinkage");
		if (shrinkage > 0)
			sim *= n / (n + shrinkage + 0.0);

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

		// candidate items: here only training items
		Set<Integer> candItems = new HashSet<>();
		for (Integer j : trainMatrix.getColumnIndices())
			candItems.add(j);

		if (verbose)
			Logs.debug("{}{} has candidate items: {}", algoName, foldInfo, candItems.size());

		// ignore items: most popular items
		if (numIgnore > 0) {
			List<Integer> ignoreItems = new ArrayList<>();

			Map<Integer, Integer> itemDegs = new HashMap<>();
			for (int j : candItems)
				itemDegs.put(j, trainMatrix.colSize(j));
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

			// get positive items from testing data
			SparseVector tv = testMatrix.row(u);
			List<Integer> correctItems = new ArrayList<>();
			for (Integer j : tv.getIndex()) {
				// intersect with the candidate items
				if (candItems.contains(j))
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

			// get rated items from training data
			SparseVector rv = trainMatrix.row(u);
			List<Integer> ratedItems = new ArrayList<>();
			for (Integer j : rv.getIndex()) {
				if (candItems.contains(j))
					ratedItems.add(j);
			}
			// number of candidate items for this user
			int numCand = candItems.size() - ratedItems.size();

			// predicted items: no repeated items that has been rated by user u
			List<Integer> rankedItems = ranking(u, candItems, ratedItems);
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
	 * predicted rating values
	 * 
	 * @param u
	 *            user id
	 * 
	 * @param j
	 *            item id
	 * @return a ranking score for user u on item j
	 */
	protected double ranking(int u, int j) {
		return predict(u, j, true);
	}

	/**
	 * compute ranking scores for a list of candidate items
	 * 
	 * @param u
	 *            user id
	 * @param candItems
	 *            candidate items
	 * @param ignoreItems
	 *            items to be ignored from candidate items
	 * @return a list of ranked items
	 */
	protected List<Integer> ranking(int u, Collection<Integer> candItems, Collection<Integer> ignoreItems) {

		List<Integer> items = new ArrayList<>();

		Map<Integer, Double> itemScores = new HashMap<>();
		for (Integer j : candItems) {
			if (!ignoreItems.contains(j)) {
				double rank = ranking(u, j);
				if (!Double.isNaN(rank))
					itemScores.put(j, rank);
			}
		}

		if (itemScores.size() > 0) {

			List<KeyValPair<Integer>> sorted = Lists.sortMap(itemScores, true);
			List<KeyValPair<Integer>> recomd = (numRecs < 0 || sorted.size() <= numRecs) ? sorted : sorted.subList(0,
					numRecs);

			for (KeyValPair<Integer> kv : recomd)
				items.add(kv.getKey());
		}

		return items;
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
			SparseVector iv = trainMatrix.col(i);

			for (int jd = id + 1; jd < cutoff; jd++) {
				int j = rankedItems.get(jd);

				double corr = corrs.get(i, j);
				if (corr == 0) {
					// if not found
					corr = compCorr(iv, trainMatrix.col(j));
					if (corr != 0)
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
	 * logistic function g(x)
	 */
	protected double g(double x) {
		return 1.0 / (1 + Math.exp(-x));
	}

	/**
	 * gradient value of logistic function g(x)
	 */
	protected double gd(double x) {
		return g(x) * g(-x); // also = g(x)(1-g(x))
	}

	/**
	 * useful to print out specific recommender's settings
	 */
	@Override
	public String toString() {
		return "";
	}
}
