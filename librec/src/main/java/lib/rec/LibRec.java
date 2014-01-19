package lib.rec;

import happy.coding.io.Configer;
import happy.coding.io.FileIO;
import happy.coding.io.Logs;
import happy.coding.io.Strings;
import happy.coding.system.Dates;
import happy.coding.system.Debug;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import lib.rec.baseline.ConstantGuess;
import lib.rec.baseline.GlobalAverage;
import lib.rec.baseline.ItemAverage;
import lib.rec.baseline.ItemKNN;
import lib.rec.baseline.MostPopular;
import lib.rec.baseline.RandomGuess;
import lib.rec.baseline.UserAverage;
import lib.rec.baseline.UserKNN;
import lib.rec.core.BiasedMF;
import lib.rec.core.CLiMF;
import lib.rec.core.PMF;
import lib.rec.core.RegSVD;
import lib.rec.core.SVDPlusPlus;
import lib.rec.core.SlopeOne;
import lib.rec.core.SocialMF;
import lib.rec.data.DataDAO;
import lib.rec.data.DataSplitter;
import lib.rec.data.SparseMat;
import lib.rec.ext.BaseMF;
import lib.rec.ext.BaseNM;
import lib.rec.ext.DMF;
import lib.rec.ext.DNM;
import lib.rec.ext.DRM;
import lib.rec.ext.DRMPlus;
import lib.rec.ext.Hybrid;
import lib.rec.intf.Recommender;
import lib.rec.intf.Recommender.Measure;

/**
 * Main Class for Matrix-based Recommender Systems
 * 
 * @author guoguibing
 * 
 */
public class LibRec {

	// configuration
	private static Configer cf;
	private static String algorithm;

	// params for multiple runs at once
	public static int paramIdx;
	public static boolean isMultRun = false;

	// rating matrix
	private static SparseMat rateMatrix = null;

	public static void main(String[] args) throws Exception {
		// config logger
		Logs.config(FileIO.getResource("log4j.properties"), false);

		// get configuration file
		cf = new Configer("librec.conf");

		// debug info
		debugInfo();

		// prepare data
		DataDAO rateDao = new DataDAO(cf.getPath("dataset.ratings"));

		if (Debug.ON) {
			rateDao.printSpecs();
			return;
		}

		rateMatrix = rateDao.readData();

		// config general recommender
		Recommender.cf = cf;
		Recommender.rateMatrix = rateMatrix;
		Recommender.rateDao = rateDao;

		// required: only one parameter varying for multiple run
		Recommender.params = RecUtils.buildParams(cf);

		// run algorithms
		if (Recommender.params.size() > 0) {
			// multiple run
			for (Entry<String, List<Double>> en : Recommender.params.entrySet()) {
				for (int i = 0, im = en.getValue().size(); i < im; i++) {
					LibRec.paramIdx = i;
					runAlgorithm();

					// useful for some methods which do not use the parameters
					// defined in Recommender.params
					if (!isMultRun)
						break;
				}
			}

		} else {
			// single run
			runAlgorithm();
		}

		// collect results
		FileIO.notifyMe(algorithm, cf.getString("notify.email.to"), cf.isOn("is.email.notify"));
	}

	private static void runAlgorithm() throws Exception {
		if (cf.isOn("is.cross.validation"))
			runCrossValidation();
		else
			runRatio();
	}

	/**
	 * interface to run cross validation approach
	 */
	private static void runCrossValidation() throws Exception {

		int kFold = cf.getInt("num.kfold");
		DataSplitter ds = new DataSplitter(rateMatrix, kFold);

		Thread[] ts = new Thread[kFold];
		Recommender[] algos = new Recommender[kFold];

		boolean isPara = cf.isOn("is.parallel.folds");

		for (int i = 0; i < kFold; i++) {
			Recommender algo = getRecommender(ds.getKthFold(i + 1), i + 1);

			algos[i] = algo;
			ts[i] = new Thread(algo);
			ts[i].start();

			if (!isPara)
				ts[i].join();
		}

		if (isPara)
			for (Thread t : ts)
				t.join();

		// average performance of k-fold
		Map<Measure, Double> avgMeasure = new HashMap<>();
		for (Recommender algo : algos) {
			for (Entry<Measure, Double> en : algo.measures.entrySet()) {
				Measure m = en.getKey();
				double val = avgMeasure.containsKey(m) ? avgMeasure.get(m) : 0.0;
				avgMeasure.put(m, val + en.getValue() / kFold);
			}
		}

		printEvalInfo(algos[0], avgMeasure);
	}

	/**
	 * interface to run ratio-validation approach
	 */
	private static void runRatio() throws Exception {

		DataSplitter ds = new DataSplitter(rateMatrix);
		double ratio = cf.getDouble("val.ratio");

		Recommender algo = getRecommender(ds.getRatio(ratio), -1);
		algo.execute();

		printEvalInfo(algo, algo.measures);
	}

	/**
	 * print out the evaluation information for a specific algorithm
	 */
	private static void printEvalInfo(Recommender algo, Map<Measure, Double> ms) {

		String result = Recommender.getEvalInfo(ms, Recommender.isRankingPred);
		String time = Dates.parse(ms.get(Measure.TrainTime).longValue()) + ","
				+ Dates.parse(ms.get(Measure.TestTime).longValue());
		String evalInfo = String.format("%s,%s,%s,%s", algo.algoName, result, algo.toString(), time);

		Logs.info(evalInfo);
	}

	/**
	 * @return a recommender to be run
	 */
	private static Recommender getRecommender(SparseMat[] data, int fold) throws Exception {

		SparseMat trainMatrix = data[0], testMatrix = data[1];
		String socialPath = cf.getPath("dataset.social");

		algorithm = cf.getString("recommender");

		switch (algorithm.toLowerCase()) {
		case "globalavg":
			return new GlobalAverage(trainMatrix, testMatrix, fold);
		case "useravg":
			return new UserAverage(trainMatrix, testMatrix, fold);
		case "itemavg":
			return new ItemAverage(trainMatrix, testMatrix, fold);
		case "random":
			return new RandomGuess(trainMatrix, testMatrix, fold);
		case "constant":
			return new ConstantGuess(trainMatrix, testMatrix, fold);
		case "mostpop":
			return new MostPopular(trainMatrix, testMatrix, fold);
		case "userknn":
			return new UserKNN(trainMatrix, testMatrix, fold);
		case "itemknn":
			return new ItemKNN(trainMatrix, testMatrix, fold);
		case "slopeone":
			return new SlopeOne(trainMatrix, testMatrix, fold);
		case "regsvd":
			return new RegSVD(trainMatrix, testMatrix, fold);
		case "biasedmf":
			return new BiasedMF(trainMatrix, testMatrix, fold);
		case "svd++":
			return new SVDPlusPlus(trainMatrix, testMatrix, fold);
		case "pmf":
			return new PMF(trainMatrix, testMatrix, fold);
		case "climf":
			return new CLiMF(trainMatrix, testMatrix, fold);
		case "socialmf":
			return new SocialMF(trainMatrix, testMatrix, fold, socialPath);
		case "aaai-basemf":
			return new BaseMF(trainMatrix, testMatrix, fold);
		case "aaai-dmf":
			return new DMF(trainMatrix, testMatrix, fold);
		case "aaai-basenm":
			return new BaseNM(trainMatrix, testMatrix, fold);
		case "aaai-dnm":
			return new DNM(trainMatrix, testMatrix, fold);
		case "aaai-drm":
			return new DRM(trainMatrix, testMatrix, fold);
		case "aaai-drmplus":
			return new DRMPlus(trainMatrix, testMatrix, fold);
		case "hybrid":
			return new Hybrid(trainMatrix, testMatrix, fold);
		default:
			throw new Exception("No recommender is specified!");
		}
	}

	/**
	 * print out debug information
	 */
	private static void debugInfo() {
		String cv = "kFold: " + cf.getInt("num.kfold")
				+ (cf.isOn("is.parallel.folds") ? " [Parallelism]" : " [Singleton]");
		String datasetInfo = String.format("Dataset: %s, %s", Strings.last(cf.getPath("dataset.ratings"), 38),
				cf.isOn("is.cross.validation") ? cv : "ratio: " + (float) cf.getDouble("val.ratio"));
		Logs.info(datasetInfo);
	}
}
