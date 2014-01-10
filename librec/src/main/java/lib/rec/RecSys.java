package lib.rec;

import happy.coding.io.Configer;
import happy.coding.io.FileIO;
import happy.coding.io.Logs;
import happy.coding.io.Strings;
import happy.coding.system.Dates;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import lib.rec.Recommender.Measure;
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
import lib.rec.ext.BaseMF;
import lib.rec.ext.BaseNM;
import lib.rec.ext.DMF;
import lib.rec.ext.DNM;
import lib.rec.ext.DRM;
import lib.rec.ext.DRMPlus;
import lib.rec.ext.Hybrid;
import no.uib.cipr.matrix.sparse.CompRowMatrix;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Table;

/**
 * Main Class for Matrix-based Recommender Systems
 * 
 * @author guoguibing
 * 
 */
public class RecSys {

	// configuration 
	private static Configer cf;
	private static String algorithm;

	// params for multiple runs at once
	public static int paramIdx;
	public static boolean isMultRun = false;

	// rating matrix 
	private static CompRowMatrix rateMatrix = null;
	// user {raw id, internal id} map
	private final static BiMap<String, Integer> userIds = HashBiMap.create();
	// item {raw id, internal id} map
	private final static BiMap<String, Integer> itemIds = HashBiMap.create();
	// rating scales
	private final static List<Double> scales = new ArrayList<>();

	public static void main(String[] args) throws Exception {
		// config logger
		Logs.config(FileIO.getResource("log4j.properties"), false);

		// get configuration file
		cf = new Configer("librec.conf");

		// debug info
		debugInfo();

		// prepare data
		readData(cf.getPath("dataset.ratings"));

		// config general recommender 
		Recommender.cf = cf;
		Recommender.rateMatrix = rateMatrix;
		Recommender.numUsers = userIds.size();
		Recommender.numItems = itemIds.size();
		Recommender.scales = scales;

		// required: only one parameter varying for multiple run
		Recommender.params = RecUtils.buildParams(cf);

		// run algorithms
		if (Recommender.params.size() > 0) {
			// multiple run
			for (Entry<String, List<Double>> en : Recommender.params.entrySet()) {
				for (int i = 0, im = en.getValue().size(); i < im; i++) {
					RecSys.paramIdx = i;
					runAlgorithm();

					// useful for some methods which do not use the parameters defined in Recommender.params
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

		for (int i = 0; i < kFold; i++) {
			Recommender algo = getRecommender(ds.getKthFold(i + 1), i + 1);

			algos[i] = algo;
			ts[i] = new Thread(algo);
			ts[i].start();
		}

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
	private static Recommender getRecommender(CompRowMatrix[] data, int fold) throws Exception {

		CompRowMatrix trainMatrix = data[0], testMatrix = data[1];
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
			return new SocialMF(trainMatrix, testMatrix, fold);
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

	private static void readData(String path) throws Exception {

		Table<String, String, Double> dataTable = HashBasedTable.create();
		BufferedReader br = FileIO.getReader(path);
		String line = null;
		while ((line = br.readLine()) != null) {
			String[] data = line.split("[ \t,]");

			String user = data[0];
			String item = data[1];
			Double rate = Double.valueOf(data[2]);

			if (!scales.contains(rate))
				scales.add(rate); // rating scales

			dataTable.put(user, item, rate);
			if (!userIds.containsKey(user))
				userIds.put(user, userIds.size()); // inner user id starts from 0

			if (!itemIds.containsKey(item))
				itemIds.put(item, itemIds.size()); // inner item id starts from 0
		}
		br.close();

		Collections.sort(scales);
		Logs.debug("User amount: {}, item amount: {}", userIds.size(), itemIds.size());
		Logs.debug("Rating Scales: {{}}", Strings.toString(scales, ", "));

		// if min-rate = 0.0, add a small value
		double epsilon = scales.get(0).doubleValue() == 0.0 ? 1e-5 : 0;

		// build rating matrix
		int numRows = userIds.size();
		int numCols = itemIds.size();
		int[][] nz = new int[numRows][];

		BiMap<Integer, String> idUsers = userIds.inverse();

		for (int uid = 0; uid < nz.length; uid++) {
			String user = idUsers.get(uid);

			nz[uid] = new int[dataTable.row(user).size()];

			List<Integer> items = new ArrayList<>();
			for (String item : dataTable.row(user).keySet()) {
				int iid = itemIds.get(item);
				items.add(iid);
			}
			Collections.sort(items);

			for (int c = 0, cm = items.size(); c < cm; c++)
				nz[uid][c] = items.get(c);
		}

		rateMatrix = new CompRowMatrix(numRows, numCols, nz);
		for (int i = 0; i < numRows; i++) {
			String user = idUsers.get(i);

			Map<String, Double> itemRates = dataTable.row(user);
			for (Entry<String, Double> en : itemRates.entrySet()) {
				int j = itemIds.get(en.getKey());
				double rate = en.getValue();
				rateMatrix.set(i, j, rate + epsilon);
			}
		}

		// release memory of data table
		dataTable = null;
	}

	/**
	 * print out debug information
	 */
	private static void debugInfo() {
		String datasetInfo = String.format(
				"Dataset: %s, %s",
				Strings.last(cf.getPath("dataset.ratings"), 38),
				cf.isOn("is.cross.validation") ? "kFold: " + cf.getInt("num.kfold") : "ratio: "
						+ (float) cf.getDouble("val.ratio"));
		Logs.info(datasetInfo);
	}
}
