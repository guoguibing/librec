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
import happy.coding.io.net.EMailer;
import happy.coding.system.Dates;
import happy.coding.system.Systems;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import librec.baseline.ConstantGuess;
import librec.baseline.GlobalAverage;
import librec.baseline.ItemAverage;
import librec.baseline.MostPopular;
import librec.baseline.RandomGuess;
import librec.baseline.UserAverage;
import librec.data.DataDAO;
import librec.data.DataSplitter;
import librec.data.SparseMatrix;
import librec.ext.AR;
import librec.ext.Hybrid;
import librec.ext.NMF;
import librec.ext.PD;
import librec.ext.PRankD;
import librec.ext.SlopeOne;
import librec.intf.Recommender;
import librec.intf.Recommender.Measure;
import librec.ranking.BPR;
import librec.ranking.CLiMF;
import librec.ranking.FISMauc;
import librec.ranking.FISMrmse;
import librec.ranking.GBPR;
import librec.ranking.RankALS;
import librec.ranking.RankSGD;
import librec.ranking.SBPR;
import librec.ranking.SLIM;
import librec.ranking.WRMF;
import librec.rating.BPMF;
import librec.rating.BiasedMF;
import librec.rating.ItemKNN;
import librec.rating.PMF;
import librec.rating.RSTE;
import librec.rating.RegSVD;
import librec.rating.SVDPlusPlus;
import librec.rating.SoRec;
import librec.rating.SoReg;
import librec.rating.SocialMF;
import librec.rating.TimeSVDPlusPlus;
import librec.rating.TrustMF;
import librec.rating.TrustSVD;
import librec.rating.UserKNN;

/**
 * Main Class of the LibRec Library
 * 
 * @author guoguibing
 * 
 */
public class LibRec {
	// version: MAJOR version (significant changes), followed by MINOR version (small changes, bug fixes)
	private static String version = "1.2";

	// configuration
	private static Configer cf;
	private static String configFile = "librec.conf";
	private static String algorithm;

	// rate DAO object
	private static DataDAO rateDao;

	// rating matrix
	private static SparseMatrix rateMatrix = null;

	/**
	 * entry of the LibRec library
	 * 
	 * @param args
	 *            command line arguments
	 */
	public static void main(String[] args) {

		try {
			// process librec arguments
			cmdLine(args);

			// get configuration file
			cf = new Configer(configFile);

			// debug info
			debugInfo();

			// prepare data
			rateDao = new DataDAO(cf.getPath("dataset.training"));
			rateMatrix = rateDao.readData(cf.getDouble("val.binary.threshold"));

			// config general recommender
			Recommender.cf = cf;
			Recommender.rateMatrix = rateMatrix;
			Recommender.rateDao = rateDao;

			// run algorithms
			runAlgorithm();

			// collect results to folder "Results"
			String destPath = FileIO.makeDirectory("Results");
			String results = destPath + algorithm + "@" + Dates.now() + ".txt";
			FileIO.copyFile("results.txt", results);

			// send notification
			notifyMe(results);

		} catch (Exception e) {
			// capture exception to log file
			Logs.error(e.getMessage());

			e.printStackTrace();
		}
	}

	/**
	 * process arguments specified at the command line
	 * 
	 * @param args
	 *            command line arguments
	 */
	private static void cmdLine(String[] args) throws Exception {
		// read arguments
		for (int i = 0; i < args.length; i += 2) {
			if (args[i].equals("-c")) {
				// configuration file
				configFile = args[i + 1];

			} else if (args[i].equals("-v")) {
				// print out short version information
				System.out.println("LibRec version " + version);
				System.exit(0);

			} else if (args[i].equals("--version")) {
				// print out full version information
				printMe();
				System.exit(0);

			} else if (args[i].equals("--dataset-spec")) {
				// print out data set specification
				cf = new Configer(configFile);

				DataDAO rateDao = new DataDAO(cf.getPath("dataset.training"));
				rateDao.printSpecs();

				String socialSet = cf.getPath("dataset.social");
				if (!socialSet.equals("-1")) {
					DataDAO socDao = rateDao == null ? new DataDAO(socialSet) : new DataDAO(socialSet,
							rateDao.getUserIds());
					socDao.printSpecs();
				}

				String testSet = cf.getPath("dataset.testing");
				if (!testSet.equals("-1")) {
					DataDAO testDao = rateDao == null ? new DataDAO(testSet) : new DataDAO(testSet,
							rateDao.getUserIds(), rateDao.getItemIds());
					testDao.printSpecs();
				}

				System.exit(0);
			}
		}
	}

	private static void runAlgorithm() throws Exception {
		String testPath = cf.getPath("dataset.testing");

		if (!testPath.equals("-1"))
			runTestFile(testPath);
		else if (cf.isOn("is.cross.validation"))
			runCrossValidation();
		else if (cf.getDouble("val.ratio") > 0)
			runRatio();
		else if (cf.getInt("num.given") > 0)
			runGiven();
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
	 * Interface to run (Given N)-validation approach
	 */
	private static void runGiven() throws Exception {

		DataSplitter ds = new DataSplitter(rateMatrix);
		int n = cf.getInt("num.given.n");
		double ratio = cf.getDouble("val.given.ratio");

		Recommender algo = getRecommender(ds.getGiven(n > 0 ? n : ratio), -1);
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
		// we add quota symbol to indicate the textual format of time 
		String time = String.format("'%s','%s'", Dates.parse(ms.get(Measure.TrainTime).longValue()),
				Dates.parse(ms.get(Measure.TestTime).longValue()));
		String evalInfo = String.format("%s,%s,%s,%s", algo.algoName, result, algo.toString(), time);

		Logs.info(evalInfo);
	}

	/**
	 * Send a notification of completeness
	 * 
	 * @param attachment
	 *            email attachment
	 */
	private static void notifyMe(String attachment) throws Exception {
		if (!cf.isOn("is.email.notify"))
			return;

		EMailer notifier = new EMailer();
		Properties props = notifier.getProps();

		props.setProperty("mail.debug", "false");

		String host = cf.getString("mail.smtp.host");
		String port = cf.getString("mail.smtp.port");
		props.setProperty("mail.smtp.host", host);
		props.setProperty("mail.smtp.port", port);
		props.setProperty("mail.smtp.auth", cf.getString("mail.smtp.auth"));

		props.put("mail.smtp.socketFactory.port", port);
		props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");

		final String user = cf.getString("mail.smtp.user");
		final String pwd = cf.getString("mail.smtp.password");
		props.setProperty("mail.smtp.user", user);
		props.setProperty("mail.smtp.password", pwd);

		props.setProperty("mail.from", user);
		props.setProperty("mail.to", cf.getString("mail.to"));

		props.setProperty("mail.subject", FileIO.getCurrentFolder() + "." + algorithm + " [" + Systems.getIP() + "]");
		props.setProperty("mail.text", "Program was finished @" + Dates.now());

		String msg = "Program [" + algorithm + "] has been finished !";
		notifier.send(msg, attachment);
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

			/* rating prediction */
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
		case "timesvd++":
			return new TimeSVDPlusPlus(trainMatrix, testMatrix, fold);
		case "pmf":
			return new PMF(trainMatrix, testMatrix, fold);
		case "bpmf":
			return new BPMF(trainMatrix, testMatrix, fold);
		case "socialmf":
			return new SocialMF(trainMatrix, testMatrix, fold);
		case "trustmf":
			return new TrustMF(trainMatrix, testMatrix, fold);
		case "sorec":
			return new SoRec(trainMatrix, testMatrix, fold);
		case "soreg":
			return new SoReg(trainMatrix, testMatrix, fold);
		case "rste":
			return new RSTE(trainMatrix, testMatrix, fold);
		case "trustsvd":
			return new TrustSVD(trainMatrix, testMatrix, fold);

			/* item ranking */
		case "climf":
			return new CLiMF(trainMatrix, testMatrix, fold);
		case "fismrmse":
			return new FISMrmse(trainMatrix, testMatrix, fold);
		case "fism":
		case "fismauc":
			return new FISMauc(trainMatrix, testMatrix, fold);
		case "rankals":
			return new RankALS(trainMatrix, testMatrix, fold);
		case "ranksgd":
			return new RankSGD(trainMatrix, testMatrix, fold);
		case "wrmf":
			return new WRMF(trainMatrix, testMatrix, fold);
		case "bpr":
			return new BPR(trainMatrix, testMatrix, fold);
		case "gbpr":
			return new GBPR(trainMatrix, testMatrix, fold);
		case "sbpr":
			return new SBPR(trainMatrix, testMatrix, fold);
		case "slim":
			return new SLIM(trainMatrix, testMatrix, fold);

			/* extension */
		case "nmf":
			return new NMF(trainMatrix, testMatrix, fold);
		case "hybrid":
			return new Hybrid(trainMatrix, testMatrix, fold);
		case "slopeone":
			return new SlopeOne(trainMatrix, testMatrix, fold);
		case "pd":
			return new PD(trainMatrix, testMatrix, fold);
		case "ar":
			return new AR(trainMatrix, testMatrix, fold);
		case "prankd":
			return new PRankD(trainMatrix, testMatrix, fold);

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

		float ratio = cf.getFloat("val.ratio");
		int givenN = cf.getInt("num.given.n");
		float givenRatio = cf.getFloat("val.given.ratio");

		String cvInfo = cf.isOn("is.cross.validation") ? cv : (ratio > 0 ? "ratio: " + ratio : "given: "
				+ (givenN > 0 ? givenN : givenRatio));

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
	public static void printMe() {
		String readme = "\nLibRec version " + version + ", copyright (C) 2014-2015 Guibing Guo \n\n"

		/* Description */
		+ "LibRec is a free software: you can redistribute it and/or modify \n"
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

		System.out.println(readme);
	}
}
