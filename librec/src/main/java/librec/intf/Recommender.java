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

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import librec.data.AddConfiguration;
import librec.data.Configuration;
import librec.data.DataDAO;
import librec.data.DataSplitter;
import librec.data.MatrixEntry;
import librec.data.SparseMatrix;
import librec.data.SparseVector;
import librec.data.SymmMatrix;
import librec.util.Dates;
import librec.util.Debug;
import librec.util.FileConfiger;
import librec.util.FileIO;
import librec.util.LineConfiger;
import librec.util.Lists;
import librec.util.Logs;
import librec.util.Measures;
import librec.util.Randoms;
import librec.util.Sims;
import librec.util.Stats;

import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.cache.LoadingCache;

/**
 * General recommenders
 * 
 * @author Guibing Guo
 */
@Configuration
public abstract class Recommender implements Runnable {

	/************************************ Static parameters for all recommenders ***********************************/
	// configer
	public static FileConfiger cf;
	// matrix of rating data
	public static SparseMatrix rateMatrix, timeMatrix;

	// default temporary file directory
	public static String tempDirPath;

	// params used for multiple runs
	public static Map<String, List<Float>> params = new HashMap<>();

	// Guava cache configuration
	protected static String cacheSpec;

	// number of cpu cores used for parallelization
	protected static int numCPUs;

	// verbose
	protected static boolean verbose = true;

	// line configer for item ranking, evaluation
	protected static LineConfiger rankOptions, algoOptions;

	// is ranking/rating prediction
	public static boolean isRankingPred;
	// threshold to binarize ratings
	public static float binThold;
	// the ratio of validation data split from training data
	public static float validationRatio;
	// is diversity-based measures used
	protected static boolean isDiverseUsed;
	// early-stop criteria
	protected static Measure earlyStopMeasure = null;
	// is save model
	protected static boolean isSaveModel = false;
	// is split data by date
	protected static boolean isSplitByDate;
	// view of rating predictions
	public static String view;

	// rate DAO object
	public static DataDAO rateDao;

	// number of users, items, ratings
	protected static int numUsers, numItems, numRates;
	// number of recommended items
	protected static int numRecs, numIgnore;

	// a list of rating scales
	protected static List<Double> ratingScale;
	// number of rating levels
	protected static int numLevels;
	// Maximum, minimum values of rating scales
	protected static double maxRate, minRate;

	// ratings' timestamps
	public static SparseMatrix testTimeMatrix;
	// minimum, maximum timestamp
	protected static long minTimestamp, maxTimestamp;

	// init mean and standard deviation
	protected static double initMean, initStd;
	// small value for initialization
	protected static double smallValue = 0.01;

	// number of nearest neighbors
	protected static int knn;
	// similarity measure
	protected static String similarityMeasure;
	// number of shrinkage
	protected static int similarityShrinkage;

	/**
	 * An indicator of initialization of static fields. This enables us to control when static fields are initialized,
	 * while "static block" will be always initialized or executed. The latter could cause unexpected exceptions when
	 * multiple runs (with different configuration files) are conducted sequentially, because some static settings will
	 * not be override in such a "staic block".
	 */
	public static boolean resetStatics = true;

	/************************************ Recommender-specific parameters ****************************************/
	// algorithm's name
	public String algoName;
	// current fold
	protected int fold;
	// fold information
	protected String foldInfo;
	// is output recommendation results 
	protected boolean isResultsOut = true;

	// user-vector cache, item-vector cache
	protected LoadingCache<Integer, SparseVector> userCache, itemCache;

	// user-items cache, item-users cache
	protected LoadingCache<Integer, List<Integer>> userItemsCache, itemUsersCache;

	// rating matrix for training, validation and test
	protected SparseMatrix trainMatrix, validationMatrix, testMatrix;

	// upper symmetric matrix of item-item correlations
	protected SymmMatrix corrs;

	// performance measures
	public Map<Measure, Double> measures;
	// global average of training rates
	protected double globalMean;

	/**
	 * Recommendation measures
	 * 
	 */
	public enum Measure {
		/* prediction-based measures */
		MAE, RMSE, NMAE, rMAE, rRMSE, MPE, Perplexity,
		/* ranking-based measures */
		D5, D10, Pre5, Pre10, Rec5, Rec10, MAP, MRR, NDCG, AUC,
		/* execution time */
		TrainTime, TestTime,
		/* loss value */
		Loss
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

		// config recommender
		if (cf == null || rateMatrix == null) {
			Logs.error("Recommender is not well configed");
			System.exit(-1);
		}

		// static initialization (reset), only done once
		if (resetStatics) {
			// change the indicator
			resetStatics = false;

			ratingScale = rateDao.getRatingScale();
			minRate = ratingScale.get(0);
			maxRate = ratingScale.get(ratingScale.size() - 1);
			numLevels = ratingScale.size();

			numUsers = rateDao.numUsers();
			numItems = rateDao.numItems();

			// ratings' timestamps
			minTimestamp = rateDao.getMinTimestamp();
			maxTimestamp = rateDao.getMaxTimestamp();
			if (testTimeMatrix == null)
				testTimeMatrix = timeMatrix;

			initMean = 0.0;
			initStd = 0.1;

			cacheSpec = cf.getString("guava.cache.spec", "maximumSize=200,expireAfterAccess=2m");

			rankOptions = cf.getParamOptions("item.ranking");
			isRankingPred = rankOptions.isMainOn();
			isDiverseUsed = rankOptions.contains("-diverse");
			numRecs = rankOptions.getInt("-topN", -1);
			numIgnore = rankOptions.getInt("-ignore", -1);

			LineConfiger evalOptions = cf.getParamOptions("evaluation.setup");
			view = evalOptions.getString("--test-view", "all");
			validationRatio = evalOptions.getFloat("-v", 0.0f);
			isSplitByDate = evalOptions.contains("--by-date");

			String earlyStop = evalOptions.getString("--early-stop");
			if (earlyStop != null) {
				for (Measure m : Measure.values()) {
					if (m.name().equalsIgnoreCase(earlyStop)) {
						earlyStopMeasure = m;
						break;
					}
				}
			}

			int numProcessors = Runtime.getRuntime().availableProcessors();
			numCPUs = evalOptions.getInt("-cpu", numProcessors);
			Randoms.seed(evalOptions.getLong("--rand-seed", System.currentTimeMillis())); // initial random seed

			// output options
			LineConfiger outputOptions = cf.getParamOptions("output.setup");
			if (outputOptions != null) {
				verbose = outputOptions.isOn("-verbose", true);
				isSaveModel = outputOptions.contains("--save-model");
			}

			knn = cf.getInt("num.neighbors", 20);
			similarityMeasure = cf.getString("similarity", "PCC");
			similarityShrinkage = cf.getInt("num.shrinkage", 30);
		}

		// training, validation, test data
		if (validationRatio > 0 && validationRatio < 1) {
			DataSplitter ds = new DataSplitter(trainMatrix);
			double ratio = 1 - validationRatio;

			SparseMatrix[] trainSubsets = isSplitByDate ? ds.getRatioByRatingDate(ratio, timeMatrix) : ds
					.getRatioByRating(ratio);
			this.trainMatrix = trainSubsets[0];
			this.validationMatrix = trainSubsets[1];
		} else {
			this.trainMatrix = trainMatrix;
		}

		this.testMatrix = testMatrix;

		// fold info
		this.fold = fold;
		foldInfo = fold > 0 ? " fold [" + fold + "]" : "";

		// whether to write out results
		LineConfiger outputOptions = cf.getParamOptions("output.setup");
		if (outputOptions != null) {
			isResultsOut = outputOptions.isMainOn();
		}

		// global mean
		numRates = trainMatrix.size();
		globalMean = trainMatrix.sum() / numRates;

		// class name as the default algorithm name
		algoName = this.getClass().getSimpleName();
		// get parameters of an algorithm
		algoOptions = getModelParams(algoName);

		// compute item-item correlations
		if (isRankingPred && isDiverseUsed)
			corrs = new SymmMatrix(numItems);
	}

	public void run() {
		try {
			execute();
		} catch (Exception e) {
			// capture error message
			Logs.error(e.getMessage());

			e.printStackTrace();
		}
	}

	/**
	 * execution method of a recommender
	 * 
	 */
	public void execute() throws Exception {

		Stopwatch sw = Stopwatch.createStarted();
		if (Debug.ON) {
			// learn a recommender model
			initModel();

			// show algorithm's configuration
			printAlgoConfig();

			// build the model
			buildModel();

			// post-processing after building a model, e.g., release intermediate memory to avoid memory leak
			postModel();
		} else {
			/**
			 * load a learned model: this code will not be executed unless "Debug.OFF" mainly for the purpose of
			 * exemplifying how to use the saved models
			 */
			loadModel();
		}
		long trainTime = sw.elapsed(TimeUnit.MILLISECONDS);

		// validation
		if (validationRatio > 0 && validationRatio < 1) {
			validateModel();

			trainTime = sw.elapsed(TimeUnit.MILLISECONDS);
		}

		// evaluation
		if (verbose)
			Logs.debug("{}{} evaluate test data ... ", algoName, foldInfo);
		measures = isRankingPred ? evalRankings() : evalRatings();
		String measurements = getEvalInfo(measures);
		sw.stop();
		long testTime = sw.elapsed(TimeUnit.MILLISECONDS) - trainTime;

		// collecting results
		measures.put(Measure.TrainTime, (double) trainTime);
		measures.put(Measure.TestTime, (double) testTime);

		String evalInfo = algoName + foldInfo + ": " + measurements + "\tTime: "
				+ Dates.parse(measures.get(Measure.TrainTime).longValue()) + ", "
				+ Dates.parse(measures.get(Measure.TestTime).longValue());
		if (!isRankingPred)
			evalInfo += "\tView: " + view;

		if (fold > 0)
			Logs.debug(evalInfo);

		if (isSaveModel)
			saveModel();
	}

	private void printAlgoConfig() {
		String algoInfo = toString();

		Class<? extends Recommender> cl = this.getClass();
		// basic annotation
		String algoConfig = cl.getAnnotation(Configuration.class).value();

		// additional algorithm-specific configuration
		if (cl.isAnnotationPresent(AddConfiguration.class)) {
			AddConfiguration add = cl.getAnnotation(AddConfiguration.class);

			String before = add.before();
			if (!Strings.isNullOrEmpty(before))
				algoConfig = before + ", " + algoConfig;

			String after = add.after();
			if (!Strings.isNullOrEmpty(after))
				algoConfig += ", " + after;
		}

		if (!algoInfo.isEmpty()) {
			if (!algoConfig.isEmpty())
				Logs.debug("{}: [{}] = [{}]", algoName, algoConfig, algoInfo);
			else
				Logs.debug("{}: {}", algoName, algoInfo);
		}
	}

	/**
	 * validate model with held-out validation data
	 */
	protected void validateModel() {
	}

	/**
	 * @return the evaluation information of a recommend
	 */
	public static String getEvalInfo(Map<Measure, Double> measures) {
		String evalInfo = null;
		if (isRankingPred) {
			if (isDiverseUsed)
				evalInfo = String.format("%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f",
						measures.get(Measure.Pre5), measures.get(Measure.Pre10), measures.get(Measure.Rec5),
						measures.get(Measure.Rec10), measures.get(Measure.AUC), measures.get(Measure.MAP),
						measures.get(Measure.NDCG), measures.get(Measure.MRR), measures.get(Measure.D5),
						measures.get(Measure.D10));
			else
				evalInfo = String.format("%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f", measures.get(Measure.Pre5),
						measures.get(Measure.Pre10), measures.get(Measure.Rec5), measures.get(Measure.Rec10),
						measures.get(Measure.AUC), measures.get(Measure.MAP), measures.get(Measure.NDCG),
						measures.get(Measure.MRR));

		} else {
			evalInfo = String.format("%.6f,%.6f,%.6f,%.6f,%.6f,%.6f", measures.get(Measure.MAE),
					measures.get(Measure.RMSE), measures.get(Measure.NMAE), measures.get(Measure.rMAE),
					measures.get(Measure.rRMSE), measures.get(Measure.MPE));

			// for some graphic models
			if (measures.containsKey(Measure.Perplexity)) {
				evalInfo += String.format(",%.6f", measures.get(Measure.Perplexity));
			}
		}

		return evalInfo;
	}

	/**
	 * initilize recommender model
	 */
	protected void initModel() throws Exception {
	}

	protected LineConfiger getModelParams(String algoName) {
		return cf.contains(algoName) ? cf.getParamOptions(algoName) : null;
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
	 * Compute the correlation between two vectors using method specified by configuration key "similarity"
	 * 
	 * @param iv
	 *            vector i
	 * @param jv
	 *            vector j
	 * @return the correlation between vectors i and j
	 */
	protected double correlation(SparseVector iv, SparseVector jv) {
		return correlation(iv, jv, similarityMeasure);
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
	 * @return the correlation between vectors i and j; return NaN if the correlation is not computable.
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
			// for ratings along the overlappings
			sim = Sims.cos(is, js);
			break;
		case "cos-binary":
			// for ratings along all the vectors (including one-sided 0s)
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
	 * Learning method: override this method to build a model, for a model-based method. Default implementation is
	 * useful for memory-based methods.
	 * 
	 */
	protected void buildModel() throws Exception {
	}

	/**
	 * After learning model: release some intermediate data to avoid memory leak
	 */
	protected void postModel() throws Exception {
	}

	/**
	 * Serializing a learned model (i.e., variable data) to files.
	 */
	protected void saveModel() throws Exception {
	}

	/**
	 * Deserializing a learned model (i.e., variable data) from files.
	 */
	protected void loadModel() throws Exception {
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
	protected Map<Measure, Double> evalRatings() throws Exception {

		List<String> preds = null;
		String toFile = null;
		if (isResultsOut) {
			preds = new ArrayList<String>(1500);
			preds.add("# userId itemId rating prediction"); // optional: file header
			toFile = tempDirPath + algoName + "-rating-predictions" + foldInfo + ".txt"; // the output-file name
			FileIO.deleteFile(toFile); // delete possibly old files
		}

		double sum_maes = 0, sum_mses = 0, sum_r_maes = 0, sum_r_rmses = 0, sum_perps = 0;
		int numCount = 0, numPEs = 0;
		for (MatrixEntry me : testMatrix) {
			double rate = me.get();

			int u = me.row();
			int j = me.column();

			if (!isTestable(u, j))
				continue;

			double pred = predict(u, j, true);
			if (Double.isNaN(pred))
				continue;

			// perplexity: for some graphic model
			double perp = perplexity(u, j, pred);
			sum_perps += perp;

			// rounding prediction to the closest rating level
			double rPred = Math.round(pred / minRate) * minRate;

			double err = Math.abs(rate - pred); // absolute predictive error
			double r_err = Math.abs(rate - rPred);

			sum_maes += err;
			sum_mses += err * err;

			sum_r_maes += r_err;
			sum_r_rmses += r_err * r_err;

			numCount++;

			if (r_err > 1e-5)
				numPEs++;

			// output predictions
			if (isResultsOut) {
				// restore back to the original user/item id
				preds.add(rateDao.getUserId(u) + " " + rateDao.getItemId(j) + " " + rate + " " + (float) pred);
				if (preds.size() >= 1000) {
					FileIO.writeList(toFile, preds, true);
					preds.clear();
				}
			}
		}

		if (isResultsOut && preds.size() > 0) {
			FileIO.writeList(toFile, preds, true);
			Logs.debug("{}{} has writeen rating predictions to {}", algoName, foldInfo, toFile);
		}

		double mae = sum_maes / numCount;
		double rmse = Math.sqrt(sum_mses / numCount);

		double r_mae = sum_r_maes / numCount;
		double r_rmse = Math.sqrt(sum_r_rmses / numCount);

		Map<Measure, Double> measures = new HashMap<>();
		measures.put(Measure.MAE, mae);
		// normalized MAE: useful for direct comparison among different data sets with distinct rating scales
		measures.put(Measure.NMAE, mae / (maxRate - minRate));
		measures.put(Measure.RMSE, rmse);

		// MAE and RMSE after rounding predictions to the closest rating levels
		measures.put(Measure.rMAE, r_mae);
		measures.put(Measure.rRMSE, r_rmse);

		// measure zero-one loss
		measures.put(Measure.MPE, (numPEs + 0.0) / numCount);

		// perplexity
		if (sum_perps > 0) {
			measures.put(Measure.Perplexity, Math.exp(sum_perps / numCount));
		}

		return measures;
	}

	/**
	 * @return the evaluation results of ranking predictions
	 */
	protected Map<Measure, Double> evalRankings() throws Exception {

		int capacity = Lists.initSize(testMatrix.numRows());

		// initialization capacity to speed up
		List<Double> ds5 = new ArrayList<>(isDiverseUsed ? capacity : 0);
		List<Double> ds10 = new ArrayList<>(isDiverseUsed ? capacity : 0);

		List<Double> precs5 = new ArrayList<>(capacity);
		List<Double> precs10 = new ArrayList<>(capacity);
		List<Double> recalls5 = new ArrayList<>(capacity);
		List<Double> recalls10 = new ArrayList<>(capacity);
		List<Double> aps = new ArrayList<>(capacity);
		List<Double> rrs = new ArrayList<>(capacity);
		List<Double> aucs = new ArrayList<>(capacity);
		List<Double> ndcgs = new ArrayList<>(capacity);

		// candidate items for all users: here only training items
		// use HashSet instead of ArrayList to speedup removeAll() and contains() operations: HashSet: O(1); ArrayList: O(log n).
		Set<Integer> candItems = new HashSet<>(trainMatrix.columns());

		List<String> preds = null;
		String toFile = null;
		int numTopNRanks = numRecs < 0 ? 10 : numRecs;
		if (isResultsOut) {
			preds = new ArrayList<String>(1500);
			preds.add("# userId: recommendations in (itemId, ranking score) pairs, where a correct recommendation is denoted by symbol *."); // optional: file header
			toFile = tempDirPath
					+ String.format("%s-top-%d-items%s.txt", new Object[] { algoName, numTopNRanks, foldInfo }); // the output-file name
			FileIO.deleteFile(toFile); // delete possibly old files
		}

		if (verbose)
			Logs.debug("{}{} has candidate items: {}", algoName, foldInfo, candItems.size());

		// ignore items for all users: most popular items
		if (numIgnore > 0) {
			List<Map.Entry<Integer, Integer>> itemDegs = new ArrayList<>();
			for (Integer j : candItems) {
				itemDegs.add(new SimpleImmutableEntry<Integer, Integer>(j, trainMatrix.columnSize(j)));
			}
			Lists.sortList(itemDegs, true);
			int k = 0;
			for (Map.Entry<Integer, Integer> deg : itemDegs) {

				// ignore these items from candidate items
				candItems.remove(deg.getKey());
				if (++k >= numIgnore)
					break;
			}
		}

		// for each test user
		for (int u = 0, um = testMatrix.numRows(); u < um; u++) {

			if (verbose && ((u + 1) % 100 == 0))
				Logs.debug("{}{} evaluates progress: {} / {}", algoName, foldInfo, u + 1, um);

			// number of candidate items for all users
			int numCands = candItems.size();

			// get positive items from test matrix
			List<Integer> testItems = testMatrix.getColumns(u);
			List<Integer> correctItems = new ArrayList<>();

			// intersect with the candidate items
			for (Integer j : testItems) {
				if (candItems.contains(j))
					correctItems.add(j);
			}

			if (correctItems.size() == 0)
				continue; // no testing data for user u

			// remove rated items from candidate items
			List<Integer> ratedItems = trainMatrix.getColumns(u);

			// predict the ranking scores (unordered) of all candidate items
			List<Map.Entry<Integer, Double>> itemScores = new ArrayList<>(Lists.initSize(candItems));
			for (final Integer j : candItems) {
				// item j is not rated 
				if (!ratedItems.contains(j)) {
					final double rank = ranking(u, j);
					if (!Double.isNaN(rank)) {
						itemScores.add(new SimpleImmutableEntry<Integer, Double>(j, rank));
					}
				} else {
					numCands--;
				}
			}

			if (itemScores.size() == 0)
				continue; // no recommendations available for user u

			// order the ranking scores from highest to lowest: List to preserve orders
			Lists.sortList(itemScores, true);
			List<Map.Entry<Integer, Double>> recomd = (numRecs <= 0 || itemScores.size() <= numRecs) ? itemScores
					: itemScores.subList(0, numRecs);

			List<Integer> rankedItems = new ArrayList<>();
			StringBuilder sb = new StringBuilder();
			int count = 0;
			for (Map.Entry<Integer, Double> kv : recomd) {
				Integer item = kv.getKey();
				rankedItems.add(item);

				if (isResultsOut && count < numTopNRanks) {
					// restore back to the original item id
					sb.append("(").append(rateDao.getItemId(item));

					if (testItems.contains(item))
						sb.append("*"); // indicating correct recommendation

					sb.append(", ").append(kv.getValue().floatValue()).append(")");

					count++;
					
					if (count < numTopNRanks)
						sb.append(", ");
				}
			}

			int numDropped = numCands - rankedItems.size();
			double AUC = Measures.AUC(rankedItems, correctItems, numDropped);
			double AP = Measures.AP(rankedItems, correctItems);
			double nDCG = Measures.nDCG(rankedItems, correctItems);
			double RR = Measures.RR(rankedItems, correctItems);

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

			// diversity
			if (isDiverseUsed) {
				double d5 = diverseAt(rankedItems, 5);
				double d10 = diverseAt(rankedItems, 10);

				ds5.add(d5);
				ds10.add(d10);
			}

			// output predictions
			if (isResultsOut) {
				// restore back to the original user id
				preds.add(rateDao.getUserId(u) + ": " + sb.toString());
				if (preds.size() >= 1000) {
					FileIO.writeList(toFile, preds, true);
					preds.clear();
				}
			}
		}

		// write results out first
		if (isResultsOut && preds.size() > 0) {
			FileIO.writeList(toFile, preds, true);
			Logs.debug("{}{} has writeen item recommendations to {}", algoName, foldInfo, toFile);
		}

		// measure the performance
		Map<Measure, Double> measures = new HashMap<>();
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

		return measures;
	}

	/**
	 * predict a specific rating for user u on item j. It is useful for evalution which requires predictions are
	 * bounded.
	 * 
	 * @param u
	 *            user id
	 * @param j
	 *            item id
	 * @param bound
	 *            whether to bound the prediction
	 * @return prediction
	 */
	protected double predict(int u, int j, boolean bound) throws Exception {
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
	 * predict a specific rating for user u on item j, note that the prediction is not bounded. It is useful for
	 * building models with no need to bound predictions.
	 * 
	 * @param u
	 *            user id
	 * @param j
	 *            item id
	 * @return raw prediction without bounded
	 */
	protected double predict(int u, int j) throws Exception {
		return globalMean;
	}

	protected double perplexity(int u, int j, double r) throws Exception {
		return 0;
	}

	/**
	 * predict a ranking score for user u on item j: default case using the unbounded predicted rating values
	 * 
	 * @param u
	 *            user id
	 * 
	 * @param j
	 *            item id
	 * @return a ranking score for user u on item j
	 */
	protected double ranking(int u, int j) throws Exception {
		return predict(u, j, false);
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
	 * Below are a set of mathematical functions. As many recommenders often adopts them, for conveniency's sake, we put
	 * these functions in the base Recommender class, though they belong to Math class.
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
	 * @return a gaussian value with mean {@code mu} and standard deviation {@code sigma};
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
	 * Check if ratings have been binarized; useful for methods that require binarized ratings;
	 */
	protected void checkBinary() {
		if (binThold < 0) {
			Logs.error("val.binary.threshold={}, ratings must be binarized first! Try set a non-negative value.",
					binThold);
			System.exit(-1);
		}
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
