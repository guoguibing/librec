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

package librec.main;

import happy.coding.io.Configer;
import happy.coding.io.FileIO;
import happy.coding.io.Logs;
import happy.coding.io.Strings;
import happy.coding.system.Dates;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import librec.baseline.ConstantGuess;
import librec.baseline.GlobalAverage;
import librec.baseline.ItemAverage;
import librec.baseline.MostPopular;
import librec.baseline.RandomGuess;
import librec.baseline.UserAverage;
import librec.core.BPMF;
import librec.core.BiasedMF;
import librec.core.CLiMF;
import librec.core.ItemKNN;
import librec.core.PMF;
import librec.core.RegSVD;
import librec.core.SVDPlusPlus;
import librec.core.SocialMF;
import librec.core.TrustMF;
import librec.core.UserKNN;
import librec.core.WRMF;
import librec.data.DataDAO;
import librec.data.DataSplitter;
import librec.data.SparseMatrix;
import librec.ext.Hybrid;
import librec.ext.NMF;
import librec.ext.SlopeOne;
import librec.intf.Recommender;
import librec.intf.Recommender.Measure;

/**
 * Main Class of the LibRec Library
 * 
 * @author guoguibing
 * 
 */
public class LibRec {
	// version: MAJOR version (significant changes), followed by MINOR version
	// (small changes, bug fixes)
	private static String version = "1.0";

	// configuration
	private static Configer cf;
	private static String algorithm;

	// params for multiple runs at once
	public static int paramIdx;
	public static boolean isMultRun = false;

	// rate DAO object
	private static DataDAO rateDao;

	// rating matrix
	private static SparseMatrix rateMatrix = null;

	public static void main(String[] args) throws Exception {
		// Logs.debug(LibRec.readme());

		// get configuration file
		cf = new Configer("librec.conf");

		// debug info
		debugInfo();

		// prepare data
		rateDao = new DataDAO(cf.getPath("dataset.training"));
		rateMatrix = rateDao.readData();

		// config general recommender
		Recommender.cf = cf;
		Recommender.rateMatrix = rateMatrix;
		Recommender.rateDao = rateDao;

		// run algorithms
		runAlgorithm();

		// collect results
		FileIO.notifyMe(algorithm, cf.getString("notify.email.to"), cf.isOn("is.email.notify"));
	}

	private static void runAlgorithm() throws Exception {
		String testPath = cf.getPath("dataset.testing");

		if (!testPath.equals("-1"))
			runTestFile(testPath);
		else if (cf.isOn("is.cross.validation"))
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
	 * Interface to run ratio-validation approach
	 */
	private static void runRatio() throws Exception {

		DataSplitter ds = new DataSplitter(rateMatrix);
		double ratio = cf.getDouble("val.ratio");

		Recommender algo = getRecommender(ds.getRatio(ratio), -1);
		algo.execute();

		printEvalInfo(algo, algo.measures);
	}

	/**
	 * Interface to run testing using data from an input file
	 * 
	 */
	private static void runTestFile(String path) throws Exception {

		DataDAO testDao = new DataDAO(path, rateDao.getUserIds(), rateDao.getItemIds());
		SparseMatrix testMatrix = testDao.readData(false);

		Recommender algo = getRecommender(new SparseMatrix[] { rateMatrix, testMatrix }, -1);
		algo.execute();

		printEvalInfo(algo, algo.measures);
	}

	/**
	 * print out the evaluation information for a specific algorithm
	 */
	private static void printEvalInfo(Recommender algo, Map<Measure, Double> ms) {

		String result = Recommender.getEvalInfo(ms);
		String time = Dates.parse(ms.get(Measure.TrainTime).longValue()) + ","
				+ Dates.parse(ms.get(Measure.TestTime).longValue());
		String evalInfo = String.format("%s,%s,%s,%s", algo.algoName, result, algo.toString(), time);

		Logs.info(evalInfo);
	}

	/**
	 * @return a recommender to be run
	 */
	private static Recommender getRecommender(SparseMatrix[] data, int fold) throws Exception {

		SparseMatrix trainMatrix = data[0], testMatrix = data[1];
		algorithm = cf.getString("recommender");

		switch (algorithm.toLowerCase()) {
		/* baselines */
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

			/* cores */
		case "userknn":
			return new UserKNN(trainMatrix, testMatrix, fold);
		case "itemknn":
			return new ItemKNN(trainMatrix, testMatrix, fold);
		case "regsvd":
			return new RegSVD(trainMatrix, testMatrix, fold);
		case "biasedmf":
			return new BiasedMF(trainMatrix, testMatrix, fold);
		case "svd++":
			return new SVDPlusPlus(trainMatrix, testMatrix, fold);
		case "pmf":
			return new PMF(trainMatrix, testMatrix, fold);
		case "bpmf":
			return new BPMF(trainMatrix, testMatrix, fold);
		case "climf":
			return new CLiMF(trainMatrix, testMatrix, fold);
		case "socialmf":
			return new SocialMF(trainMatrix, testMatrix, fold);
		case "trustmf":
			return new TrustMF(trainMatrix, testMatrix, fold);
		case "wrmf":
			return new WRMF(trainMatrix, testMatrix, fold);

			/* extension */
		case "nmf":
			return new NMF(trainMatrix, testMatrix, fold);
		case "hybrid":
			return new Hybrid(trainMatrix, testMatrix, fold);
		case "slopeone":
			return new SlopeOne(trainMatrix, testMatrix, fold);

		default:
			throw new Exception("No recommender is specified!");
		}
	}

	/**
	 * Print out debug information
	 */
	private static void debugInfo() {
		String cv = "kFold: " + cf.getInt("num.kfold")
				+ (cf.isOn("is.parallel.folds") ? " [Parallel]" : " [Singleton]");
		String cvInfo = cf.isOn("is.cross.validation") ? cv : "ratio: " + (float) cf.getDouble("val.ratio");

		String testPath = cf.getPath("dataset.testing");
		boolean isTestingFlie = !testPath.equals("-1");
		String mode = isTestingFlie ? String.format("Testing:: %s.", Strings.last(testPath, 38)) : cvInfo;

		if (!Recommender.isRankingPred) {
			String view = cf.getString("rating.pred.view");
			switch (view.toLowerCase()) {
			case "cold-start":
				mode += ", " + view;
				break;
			case "trust-degree":
				mode += String.format(", %s [%d, %d]",
						new Object[] { view, cf.getInt("min.trust.degree"), cf.getInt("max.trust.degree") });
				break;
			case "all":
			default:
				break;
			}
		}
		String debugInfo = String.format("Training: %s, %s", Strings.last(cf.getPath("dataset.training"), 38), mode);
		Logs.info(debugInfo);
	}

	/**
	 * Print out software information
	 */
	public static String readme() {
		return "\nLibRec " + version + " Copyright (C) 2014 Guibing Guo \n\n"

		/* Description */
		+ "LibRec is free software: you can redistribute it and/or modify \n"
				+ "it under the terms of the GNU General Public License as published by \n"
				+ "the Free Software Foundation, either version 3 of the License, \n"
				+ "or (at your option) any later version. \n\n"

				/* Usage */
				+ "LibRec is distributed in the hope that it will be useful, \n"
				+ "but WITHOUT ANY WARRANTY; without even the implied warranty of \n"
				+ "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the \n"
				+ "GNU General Public License for more details. \n\n"

				/* licence */
				+ "You should have received a copy of the GNU General Public License \n"
				+ "along with LibRec. If not, see <http://www.gnu.org/licenses/>.";
	}
}
