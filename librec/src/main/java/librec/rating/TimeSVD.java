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
package librec.rating;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class TimeSVD {

	public double LRATE_UF_INITIAL = 0.007;
	public double LRATE_MF_INITIAL = 0.007;
	public double LRATE_MW_INITIAL = 1e-3;

	public double LRATE_UB_INITIAL = 3e-3;
	public double LRATE_MB_INITIAL = 2e-3;
	public double LRATE_UDB_INITIAL = 2.5e-3;
	public double LRATE_UBA_INITIAL = 1e-5;
	public double LRATE_MBB_INITIAL = 5e-5;
	public double LRATE_MBS_INITIAL = 5.108e-3;
	public double LRATE_MBDS_INITIAL = 2e-3;
	public double LRATE_UFA_INITIAL = 1e-5;
	public double LRATE_MFB_INITIAL = 2e-3;
	public double K_UB = 0.01;
	public double K_MB = 3.454e-2;
	public double K_UDB = 5e-3;
	public double K_UBA = 50;
	public double K_MBB = 0.1;
	public double K_MBS = 0.01;
	public double K_MBDS = 1.56e-3;
	public double K_UFA = 50;
	public double K_MFB = 1e-3;
	public double K_UF = 8.223e-2;
	public double K_MF = 8.610e-3;
	public double K_MW = 0.05;

	public int NUM_MOVIE_BINS = 30;
	public int MOVIE_BIN_SIZE = NUM_DATES / NUM_MOVIE_BINS + 1;
	public double BETA = 0.4;
	public double a = 6.76;
	public double log_a = Math.log(a);
	public final int HIGHEST_DAY_FREQ = 2651;
	public final int NUM_A_TIERS = 10;

	public Random rand = new Random(0);

	// Current lrates
	public double LRATE_UF;
	public double LRATE_MF;
	public double LRATE_MW;
	public double LRATE_UB;
	public double LRATE_MB;
	public double LRATE_UDB;
	public double LRATE_UBA;
	public double LRATE_MBB;
	public double LRATE_MBS;
	public double LRATE_MBDS;
	public double LRATE_UFA;
	public double LRATE_MFB;

	// Learning related stuff
	public double[][] userFeatures;
	public double[][] movieFeatures;
	public double[] userBias;
	public double[] movieBias;
	public double[][] mw;
	public double[][] sum_mw;
	public double[] userRatingCount;
	public double[] norm;
	public double[][] movieBiasBins;
	public double[] userBiasAlpha;
	public double[] dateMean;
	public ArrayList<Double>[] userDateBias; // Holds dates weights
	public ArrayList<Integer>[] userDateBiasIndex; // Holds dates
	public double[] movieBiasScale;
	public ArrayList<Double>[] movieBiasDateScale;
	public ArrayList<Integer>[] movieBiasDateScaleIndex;
	public double[][] userFeaturesAlpha;
	public double[][] movieFrequencyBias;

	public static final int NUM_EPOCHS_SPAN = 5;
	public static final double MIN_ERROR_DIFF = 0.00001;

	public double GLOBAL_MEAN = 3.6033;
	public static final double CINEMATCH_BASELINE = 0.9514;

	public static final int NUM_USERS = 458293;
	public static final int NUM_MOVIES = 17770;
	public static final int NUM_DATES = 2243;
	public static final int NUM_POINTS = 102416306;
	public static final int NUM_1_POINTS = 94362233;
	public static final int NUM_2_POINTS = 1965045;
	public static final int NUM_3_POINTS = 1964391;
	public static final int NUM_4_POINTS = 1374739;
	public static final int NUM_5_POINTS = 2749898;
	public static final int NUM_TRAINING_POINTS = NUM_1_POINTS + NUM_2_POINTS
			+ NUM_3_POINTS;
	public static final int NUM_TRAINING_PROBE_POINTS = NUM_1_POINTS
			+ NUM_2_POINTS + NUM_3_POINTS + NUM_4_POINTS;

	public static final String INPUT_DATA = "all.dta";
	public static final String INPUT_INDEX = "all.idx";
	public static final String INPUT_QUAL = "qual.dta";
	public static final String LOGFILE = "log.txt";

	public int NUM_FEATURES;
	public int TEST_PARAM;

	// Data that is stored in memory
	public int[] users = new int[NUM_TRAINING_PROBE_POINTS];//userIDs
	public int[] userIndex = new int[NUM_USERS];
	public short[] movies = new short[NUM_TRAINING_PROBE_POINTS];
	public short[] dates = new short[NUM_TRAINING_PROBE_POINTS];
	public byte[] ratings = new byte[NUM_TRAINING_PROBE_POINTS];
	public int[][] probeData = new int[NUM_4_POINTS][4];
	public int[][] qualData = new int[NUM_5_POINTS][3];

	public TimeSVD(int numFeatures) {
		this.NUM_FEATURES = numFeatures;

		// Initialize things that are specific to the training session.
		initializeVars();
	}

	public TimeSVD(int numFeatures, int testParam) {
		this.NUM_FEATURES = numFeatures;

		// Initialize things that are specific to the training session.
		initializeVars();
	}

	public TimeSVD(int numFeatures, double LRATE_BIAS, double LRATE_FEATURES,
			double LRATE_MW, double K_BIAS, double K_FEATURES, double K_MW) {
		// Set the constants to the specified values.
		this.NUM_FEATURES = numFeatures;
		this.LRATE_MW_INITIAL = LRATE_MW;

		// Initialize things that are specific to the training session.
		initializeVars();
	}

	@SuppressWarnings("unchecked")
	private void initializeVars() {
		userFeatures = new double[NUM_USERS][NUM_FEATURES];
		movieFeatures = new double[NUM_MOVIES][NUM_FEATURES];
		userBias = new double[NUM_USERS];
		movieBias = new double[NUM_MOVIES];
		mw = new double[NUM_USERS][NUM_FEATURES];
		sum_mw = new double[NUM_USERS][NUM_FEATURES];
		userRatingCount = new double[NUM_USERS];
		norm = new double[NUM_USERS];
		movieBiasBins = new double[NUM_MOVIES][NUM_MOVIE_BINS];
		userBiasAlpha = new double[NUM_USERS];
		dateMean = new double[NUM_USERS];
		userDateBias = new ArrayList[NUM_USERS];
		userDateBiasIndex = new ArrayList[NUM_USERS];
		movieBiasScale = new double[NUM_USERS];
		movieBiasDateScale = new ArrayList[NUM_USERS];
		movieBiasDateScaleIndex = new ArrayList[NUM_USERS];
		userFeaturesAlpha = new double[NUM_USERS][NUM_FEATURES];
		movieFrequencyBias = new double[NUM_MOVIES][NUM_A_TIERS];

		LRATE_UF = LRATE_UF_INITIAL;
		LRATE_MF = LRATE_MF_INITIAL;
		LRATE_MW = LRATE_MW_INITIAL;
		LRATE_UB = LRATE_UB_INITIAL;
		LRATE_MB = LRATE_MB_INITIAL;
		LRATE_UDB = LRATE_UDB_INITIAL;
		LRATE_UBA = LRATE_UBA_INITIAL;
		LRATE_MBB = LRATE_MBB_INITIAL;
		LRATE_MBS = LRATE_MBS_INITIAL;
		LRATE_MBDS = LRATE_MBDS_INITIAL;
		LRATE_UFA = LRATE_UFA_INITIAL;
		LRATE_MFB = LRATE_MFB_INITIAL;

		rand = new Random(0);

		// Initialize weights.
		for (int i = 0; i < userFeatures.length; i++) {
			for (int j = 0; j < userFeatures[i].length; j++) {
				userFeatures[i][j] = (rand.nextDouble() - 0.5) / 50;
			}
		}
		for (int i = 0; i < movieFeatures.length; i++) {
			for (int j = 0; j < movieFeatures[i].length; j++) {
				movieFeatures[i][j] = (rand.nextDouble() - 0.5) / 50;
			}
		}
		// User bias (specific to day)
		for (int i = 0; i < userDateBias.length; i++) {
			userDateBias[i] = new ArrayList<Double>();
		}
		for (int i = 0; i < userDateBias.length; i++) {
			userDateBiasIndex[i] = new ArrayList<Integer>();
		}
		// Movie bias (specific to day)
		for (int i = 0; i < userDateBias.length; i++) {
			movieBiasDateScale[i] = new ArrayList<Double>();
		}
		for (int i = 0; i < userDateBias.length; i++) {
			movieBiasDateScaleIndex[i] = new ArrayList<Integer>();
		}
		// C should be around 1
		for (int i = 0; i < movieBiasScale.length; i++) {
			movieBiasScale[i] = 1;
		}
	}
	
	private void setVarsToNull() {
		userFeatures = null;
		movieFeatures = null;
		userBias = null;
		movieBias = null;
		mw = null;
		sum_mw = null;
		userRatingCount = null;
		norm = null;
		movieBiasBins = null;
		userBiasAlpha = null;
		dateMean = null;
		userDateBias = null;
		userDateBiasIndex = null;
		movieBiasScale = null;
		movieBiasDateScale = null;
		movieBiasDateScaleIndex = null;
		userFeaturesAlpha = null;
		movieFrequencyBias = null;
	}

	public void train() throws NumberFormatException, IOException {
		System.out.println(timestampLine(String.format("Training %d features.",
				NUM_FEATURES)));

		// Read in input
		readInput();

		// Set up logfile.
		BufferedWriter logWriter = new BufferedWriter(new FileWriter(LOGFILE, true));
		logWriter.write("\n");

		// TRAIN WITH TRAINING SET ONLY (no probe)
		precompute(NUM_TRAINING_POINTS);
		double previousRmse = calcProbeRmse();
		logRmse(logWriter, previousRmse, 0);
		int numEpochsToTrain = 0;
		for (int i = 1; true; i++) {
			double rmse = trainWithNumPoints(NUM_TRAINING_POINTS);
			logRmse(logWriter, rmse, i);

			// Slow down learning rate as we're getting close to the answer.
			LRATE_UF *= .9;
			LRATE_MF *= .9;
			LRATE_MW *= .9;

			// If probe error has been going up, we should stop.
			double rmseDiff = previousRmse - rmse;
			if (rmseDiff < MIN_ERROR_DIFF) {
				System.out
						.println(timestampLine("Probe error has started"
								+ " to go up significantly; memorizing number of epochs to train."));
				generateProbeOutput();
				numEpochsToTrain = i;
				break;
			}
			
			previousRmse = rmse;
		}

		// TRAIN WITH PROBE.
		setVarsToNull();
		initializeVars();
		precompute(NUM_TRAINING_PROBE_POINTS);
		logEpoch(logWriter, 0);
		for (int i = 1; i <= numEpochsToTrain + NUM_EPOCHS_SPAN; i++) {
			// Train with training set AND probe.
			trainWithNumPoints(NUM_TRAINING_PROBE_POINTS);
			logEpoch(logWriter, i);

			// Slow down learning rate as we're getting close to the answer.
			LRATE_UF *= .9;
			LRATE_MF *= .9;
			LRATE_MW *= .9;

			if (i == numEpochsToTrain + NUM_EPOCHS_SPAN) {
				generateOutput();
			}
		}

		saveBestParams();
		logWriter.close();

		System.out.println("Done!");
	}

	public double trainWithNumPoints(int numPoints) throws IOException {
		int user;
		short movie, date;
		byte rating;
		int prevUser = -1;
		double err, uf, mf, ufa;
		short m;
		double[] tmp_sum = new double[this.NUM_FEATURES];
		int binNum;
		double dateDev, timeDev;
		double bi, bi_bin, cu, udb, delta;
		Double cut;
		int ind;
		int f_ui;
		Integer freq;
		Map<Integer, Integer> dateToFreq = new HashMap<Integer, Integer>();
		int d;

		for (int j = 0; j < numPoints; j++) {
			user = users[j];
			movie = movies[j];
			date = dates[j];
			rating = ratings[j];
			binNum = date / MOVIE_BIN_SIZE;
			dateDev = date - dateMean[user];
			timeDev = Math.signum(dateDev) * Math.pow(Math.abs(dateDev), BETA);

			// Precomputation:
			// First calculate f_ui by getting day to frequency map for this user.
			if (user != prevUser) {
				dateToFreq = new HashMap<Integer, Integer>();
				// Traverse this user's data and construct dateToFreq
				for (int l = j; l < numPoints && users[l] == user; l++) {
					d = dates[l];
					freq = dateToFreq.get(d);
					if (freq == null) {
						freq = 0;
					}
					freq++;
					dateToFreq.put(d, freq);
				}
				// Pre-calc for SVD++
				// Reset tmp_sum
				for (int k = 0; k < tmp_sum.length; k++) {
					tmp_sum[k] = 0;
				}
				// Reset sum_mw and calculate sums
				for (int k = 0; k < NUM_FEATURES; k++) {
					sum_mw[user][k] = 0;
				}
				for (int l = j; l < numPoints && users[l] == user; l++) {
					m = movies[l];
					for (int k = 0; k < NUM_FEATURES; k++) {
						sum_mw[user][k] += mw[m][k];
					}
				}
			}
			prevUser = user;

			// Calculate the error.
			err = rating - predictRating(movie, user, date, dateToFreq);

			// Cache old values
			bi = movieBias[movie];
			bi_bin = movieBiasBins[movie][binNum];
			cu = movieBiasScale[user];
			ind = movieBiasDateScaleIndex[user].indexOf((int) date);
			if (ind == -1) {
				cut = 0.0;
			} else {
				cut = movieBiasDateScale[user].get(ind);
			}

			// Train biases.
			// User bias
			userBias[user] += LRATE_UB * (err - K_UB * userBias[user]);
			// Long term user bias
			userBiasAlpha[user] += LRATE_UBA
					* (err * timeDev - K_UBA * userBiasAlpha[user]);
			// Short term user bias
			ind = userDateBiasIndex[user].indexOf((int) date);
			if (ind == -1) {
				udb = 0.0;
				delta = LRATE_UDB * (err - K_UDB * udb);
				userDateBiasIndex[user].add((int) date);
				userDateBias[user].add(udb + delta);
			} else {
				udb = userDateBias[user].get(ind);
				delta = LRATE_UDB * (err - K_UDB * udb);
				userDateBias[user].set(ind, udb + delta);
			}
			// Movie bias
			movieBias[movie] += LRATE_MB * (err * (cu + cut) - K_MB * bi);
			// Movie bias over time
			movieBiasBins[movie][binNum] += LRATE_MBB
					* (err * (cu + cut) - K_MBB * bi_bin);
			// Movie bias scales (plus time version)
			movieBiasScale[user] += LRATE_MBS
					* (err * (bi + bi_bin) - K_MBS * (cu - 1));
			ind = movieBiasDateScaleIndex[user].indexOf((int) date);
			if (ind == -1) {
				cut = 0.0;
				delta = LRATE_MBDS * (err * (bi + bi_bin) - K_MBDS * cut);
				movieBiasDateScaleIndex[user].add((int) date);
				movieBiasDateScale[user].add(cut + delta);
			} else {
				cut = movieBiasDateScale[user].get(ind);
				delta = LRATE_MBDS * (err * (bi + bi_bin) - K_MBDS * cut);
				movieBiasDateScale[user].set(ind, cut + delta);
			}
			// Frequency of user rating bias for the movie
			freq = dateToFreq.get((int) date);
			if (freq == null) {
				freq = 1;
			}
			f_ui = (int) (Math.log(freq) / log_a);
			movieFrequencyBias[movie][f_ui] += LRATE_MFB * err - K_MFB
					* movieFrequencyBias[movie][f_ui];

			// Train all features.
			for (int k = 0; k < NUM_FEATURES; k++) {
				uf = userFeatures[user][k];
				mf = movieFeatures[movie][k];
				ufa = userFeaturesAlpha[user][k];

				userFeatures[user][k] += LRATE_UF * (err * mf - K_UF * uf);
				movieFeatures[movie][k] += LRATE_MF
						* (err * (uf + ufa * timeDev + norm[user] * sum_mw[user][k]) - K_MF
								* mf);

				// Update user features alpha
				userFeaturesAlpha[user][k] += LRATE_UFA
						* (err * mf * timeDev - K_UFA * ufa);

				// Sum mw gradients, don't train yet.
				tmp_sum[k] += err * norm[user] * mf;
			}

			// Update movie weights if we have a new user
			if (j + 1 == numPoints || users[j + 1] != user) {
				for (int l = j; l >= 0 && users[l] == user; l--) {
					m = movies[l];
					for (int k = 0; k < NUM_FEATURES; k++) {
						mw[m][k] += LRATE_MW * (tmp_sum[k] - K_MW * mw[m][k]);
					}
				}
			}
		}

		// Recalculate sum_mw
		for (int j = 0; j < NUM_USERS; j++) {
			for (int k = 0; k < NUM_FEATURES; k++) {
				sum_mw[j][k] = 0;
			}
		}
		for (int j = 0; j < numPoints; j++) {
			user = users[j];
			movie = movies[j];
			for (int k = 0; k < NUM_FEATURES; k++) {
				sum_mw[user][k] += mw[movie][k];
			}
		}

		// Calculate probe error and return.
		return calcProbeRmse();
	}

	@SuppressWarnings("resource")
	public void precompute(int numPoints) throws NumberFormatException,
			IOException {
		// If we are precomputing with probe, we need to re-read the data in the
		// correct order.
		if (numPoints == NUM_TRAINING_PROBE_POINTS) {
			// Read input into memory
			InputStream fis = new FileInputStream(INPUT_DATA);
			BufferedReader br = new BufferedReader(new InputStreamReader(fis,
					Charset.forName("UTF-8")));
			InputStream fisIdx = new FileInputStream(INPUT_INDEX);
			BufferedReader brIdx = new BufferedReader(new InputStreamReader(fisIdx,
					Charset.forName("UTF-8")));

			// Read INPUT_INDEX
			System.out.println(timestampLine("Loading data index..."));
			byte[] dataIndices = new byte[NUM_POINTS];
			String line;
			byte index;
			int lineNum = 0;
			while ((line = brIdx.readLine()) != null) {
				index = Byte.parseByte(line);
				dataIndices[lineNum] = index;
				lineNum++;
			}

			// Read INPUT_DATA
			System.out.println(timestampLine("Loading data..."));
			String[] parts;
			int user;
			short movie, date;
			byte rating;
			lineNum = 0;
			int trainingDataIndex = 0;
			while ((line = br.readLine()) != null) {
				parts = line.split(" ");
				user = Integer.parseInt(parts[0]) - 1;
				movie = (short) (Short.parseShort(parts[1]) - 1);
				date = (short) (Short.parseShort(parts[2]) - 1);
				rating = (byte) (Byte.parseByte(parts[3]));
				if (dataIndices[lineNum] == 1 || dataIndices[lineNum] == 2
						|| dataIndices[lineNum] == 3 || dataIndices[lineNum] == 4) {

					users[trainingDataIndex] = user;
					movies[trainingDataIndex] = movie;
					dates[trainingDataIndex] = date;
					ratings[trainingDataIndex] = rating;
					trainingDataIndex++;
				}
				lineNum++;
				if (lineNum % 10000000 == 0) {
					System.out.println(timestampLine(lineNum + " / " + NUM_POINTS));
				}
			}
		}

		// Calculate the global rating mean
		long ratingSum = 0;
		for (int i = 0; i < numPoints; i++) {
			ratingSum += ratings[i];
		}
		GLOBAL_MEAN = ((double) ratingSum) / numPoints;

		int prevUser = -1;
		int user;
		// Index the beginning of data for each user
		for (int i = 0; i < numPoints; i++) {
			user = users[i];
			if (user != prevUser) {
				userIndex[user] = i;
			}
			prevUser = user;
		}
		// Count number of ratings for each user
		for (int i = 0; i < numPoints; i++) {
			user = users[i];
			userRatingCount[user]++;
		}
		// Calculate norms
		for (int i = 0; i < norm.length; i++) {
			if (userRatingCount[i] == 0) {
				norm[i] = 1;
			} else {
				norm[i] = 1 / Math.sqrt(userRatingCount[i]);
			}
		}
		// Calculate average date of user ratings.
		for (int i = 0; i < numPoints; i++) {
			user = users[i];
			dateMean[user] += dates[i];
		}
		for (int i = 0; i < dateMean.length; i++) {
			if (userRatingCount[i] != 0) {
				dateMean[i] /= userRatingCount[i];
			}
		}
		System.out.println(timestampLine("Finished precomputation.\n"));
	}

	public double predictRating(int movie, int user, int date,
			Map<Integer, Integer> dateToFreq) {

		int binNum = date / MOVIE_BIN_SIZE;
		double dateDev = date - dateMean[user];
		double timeDev = Math.signum(dateDev) * Math.pow(Math.abs(dateDev), BETA);
		// User bias (specific to day)
		double udb = 0;
		int ind = userDateBiasIndex[user].indexOf((int) date);
		if (ind != -1) {
			udb = userDateBias[user].get(ind);
		}
		// Movie bias (specific to day)
		double cut = 0;
		ind = movieBiasDateScaleIndex[user].indexOf((int) date);
		if (ind != -1) {
			cut = movieBiasDateScale[user].get(ind);
		}
		// Compute function for frequency of user rating
		Integer freq = dateToFreq.get((int) date);
		if (freq == null) {
			freq = 1;
		}
		int f_ui = (int) (Math.log(freq) / log_a);

		// Compute ratings
		double ratingSum = GLOBAL_MEAN;

		// Add in biases.
		// User biases
		ratingSum += userBias[user];
		ratingSum += userBiasAlpha[user] * timeDev;
		ratingSum += udb;
		// Movie biases
		ratingSum += (movieBias[movie] + movieBiasBins[movie][binNum])
				* (movieBiasScale[user] + cut);
		ratingSum += movieFrequencyBias[movie][f_ui];

		// Take dot product of feature vectors.
		for (int i = 0; i < NUM_FEATURES; i++) {
			ratingSum += (userFeatures[user][i] + userFeaturesAlpha[user][i]
					* timeDev + sum_mw[user][i] * norm[user])
					* movieFeatures[movie][i];
		}
		return ratingSum;
	}

	public String timestampLine(String logline) {
		String currentDate = new SimpleDateFormat("h:mm:ss a").format(new Date());
		return currentDate + ": " + logline;
	}

	@SuppressWarnings("unused")
	private double addAndClip(double n, double addThis) {
		n += addThis;
		if (n > 5) {
			return 5;
		} else if (n < 1) {
			return 1;
		}
		return n;
	}

	private double calcProbeRmse() throws IOException {
		int user, prevUser = -1;
		short movie, date;
		byte rating;
		Map<Integer, Integer> dateToFreq = new HashMap<Integer, Integer>();
		Integer freq;

		// Test the model in probe set.
		double rmse = 0;
		for (int j = 0; j < probeData.length; j++) {
			user = probeData[j][0];
			movie = (short) probeData[j][1];
			date = (short) probeData[j][2];
			rating = (byte) probeData[j][3];

			if (user != prevUser) {
				dateToFreq = new HashMap<Integer, Integer>();
				// Traverse this user's data and construct dateToFreq
				for (int l = j; l < probeData.length && probeData[l][0] == user; l++) {
					freq = dateToFreq.get((int) date);
					if (freq == null) {
						freq = 0;
					}
					freq++;
					dateToFreq.put((int) date, freq);
				}
			}
			prevUser = user;

			rmse += Math
					.pow(rating - predictRating(movie, user, date, dateToFreq), 2);
		}
		rmse = Math.sqrt(rmse / NUM_4_POINTS);

		return rmse;
	}

	private void logRmse(BufferedWriter logWriter, double rmse, int i)
			throws IOException {
		// Print + log some stats.
		double predictedPercent = (1 - rmse / CINEMATCH_BASELINE) * 100;
		String currentDate = new SimpleDateFormat("h:mm:ss a").format(new Date());
		String logline = currentDate
				+ String.format(": epoch %d probe RMSE %.5f (%.2f%%) ", i, rmse,
						predictedPercent);
		System.out.println(logline);
		logWriter.write(logline + "\n");
	}

	private void logEpoch(BufferedWriter logWriter, int i) throws IOException {
		// Print + log some stats.
		String currentDate = new SimpleDateFormat("h:mm:ss a").format(new Date());
		String logline = currentDate + String.format(": epoch %d", i);
		System.out.println(logline);
		logWriter.write(logline + "\n");
	}

	// Reads input with 1 2 3 data, and then appends probe onto the end.
	@SuppressWarnings("resource")
	private void readInput() throws NumberFormatException, IOException {
		// Read input into memory
		InputStream fis = new FileInputStream(INPUT_DATA);
		BufferedReader br = new BufferedReader(new InputStreamReader(fis,
				Charset.forName("UTF-8")));
		InputStream fisIdx = new FileInputStream(INPUT_INDEX);
		BufferedReader brIdx = new BufferedReader(new InputStreamReader(fisIdx,
				Charset.forName("UTF-8")));

		// Read INPUT_INDEX
		System.out.println(timestampLine("Loading data index..."));
		byte[] dataIndices = new byte[NUM_POINTS];
		String line;
		byte index;
		int lineNum = 0;
		while ((line = brIdx.readLine()) != null) {
			index = Byte.parseByte(line);
			dataIndices[lineNum] = index;
			lineNum++;
		}

		// Read INPUT_DATA
		System.out.println(timestampLine("Loading data..."));
		String[] parts;
		int user;
		short movie, date;
		byte rating;
		lineNum = 0;
		int trainingDataIndex = 0, probeDataIndex = 0, qualDataIndex = 0;
		while ((line = br.readLine()) != null) {
			parts = line.split(" ");
			user = Integer.parseInt(parts[0]) - 1;
			movie = (short) (Short.parseShort(parts[1]) - 1);
			date = (short) (Short.parseShort(parts[2]) - 1);
			rating = (byte) (Byte.parseByte(parts[3]));
			if (dataIndices[lineNum] == 1 || dataIndices[lineNum] == 2
					|| dataIndices[lineNum] == 3) {

				users[trainingDataIndex] = user;
				movies[trainingDataIndex] = movie;
				dates[trainingDataIndex] = date;
				ratings[trainingDataIndex] = rating;
				trainingDataIndex++;
			} else if (dataIndices[lineNum] == 4) {
				probeData[probeDataIndex][0] = user;
				probeData[probeDataIndex][1] = movie;
				probeData[probeDataIndex][2] = date;
				probeData[probeDataIndex][3] = rating;

				probeDataIndex++;
			} else if (dataIndices[lineNum] == 5) {
				qualData[qualDataIndex][0] = user;
				qualData[qualDataIndex][1] = movie;
				qualData[qualDataIndex][2] = date;

				qualDataIndex++;
			}
			lineNum++;
			if (lineNum % 10000000 == 0) {
				System.out.println(timestampLine(lineNum + " / " + NUM_POINTS));
			}
		}
		
		System.out.println(timestampLine("Done loading data."));
	}

	private void saveBestParams() throws IOException {
		// Save params
		// Save bestUserFeatures
		FileOutputStream fileOut = new FileOutputStream("userFeatures");
		ObjectOutputStream objOut = new ObjectOutputStream(fileOut);
		objOut.writeObject(userFeatures);
		objOut.close();
		fileOut.close();
		// Save bestMovieFeatures
		fileOut = new FileOutputStream("movieFeatures");
		objOut = new ObjectOutputStream(fileOut);
		objOut.writeObject(movieFeatures);
		objOut.close();
		fileOut.close();
		// Save bestUserBias
		fileOut = new FileOutputStream("userBias");
		objOut = new ObjectOutputStream(fileOut);
		objOut.writeObject(userBias);
		objOut.close();
		fileOut.close();
		// Save bestMovieBias
		fileOut = new FileOutputStream("movieBias");
		objOut = new ObjectOutputStream(fileOut);
		objOut.writeObject(movieBias);
		objOut.close();
		fileOut.close();
		// Save best_mw
		fileOut = new FileOutputStream("mw");
		objOut = new ObjectOutputStream(fileOut);
		objOut.writeObject(mw);
		objOut.close();
		fileOut.close();
		// Save best_sum_mw
		fileOut = new FileOutputStream("sum_mw");
		objOut = new ObjectOutputStream(fileOut);
		objOut.writeObject(sum_mw);
		objOut.close();
		fileOut.close();
	}

	private void generateProbeOutput() throws IOException {
		FileWriter fstream = new FileWriter("TimeSVD_123_no_probe_training");
		BufferedWriter out = new BufferedWriter(fstream);
		int movie, user, date, prevUser = -1;
		Map<Integer, Integer> dateToFreq = new HashMap<Integer, Integer>();
		Integer freq;
		double predictedRating;
		for (int j = 0; j < NUM_TRAINING_POINTS; j++) {
			user = users[j];
			movie = (short) movies[j];
			date = (short) dates[j];

			if (user != prevUser) {
				dateToFreq = new HashMap<Integer, Integer>();
				// Traverse this user's data and construct dateToFreq
				for (int l = j; l < qualData.length && qualData[l][0] == user; l++) {
					freq = dateToFreq.get((int) date);
					if (freq == null) {
						freq = 0;
					}
					freq++;
					dateToFreq.put((int) date, freq);
				}
			}
			prevUser = user;
			
			predictedRating = predictRating(movie, user, date, dateToFreq);
			out.write(String.format("%d %d %.4f\n", user, movie, predictedRating));
		}
		out.close();
		
		prevUser = -1;
		fstream = new FileWriter("TimeSVD_4_no_probe_training");
		out = new BufferedWriter(fstream);
		// Test the model in probe set.
		for (int j = 0; j < probeData.length; j++) {
			user = probeData[j][0];
			movie = (short) probeData[j][1];
			date = (short) probeData[j][2];

			if (user != prevUser) {
				dateToFreq = new HashMap<Integer, Integer>();
				// Traverse this user's data and construct dateToFreq
				for (int l = j; l < qualData.length && qualData[l][0] == user; l++) {
					freq = dateToFreq.get((int) date);
					if (freq == null) {
						freq = 0;
					}
					freq++;
					dateToFreq.put((int) date, freq);
				}
			}
			prevUser = user;
			
			predictedRating = predictRating(movie, user, date, dateToFreq);
			out.write(String.format("%d %d %.4f\n", user, movie, predictedRating));
		}
		out.close();
	}
	
	private void generateOutput() throws IOException {
		FileWriter fstream = new FileWriter("TimeSVD_1234_with_probe_training");
		BufferedWriter out = new BufferedWriter(fstream);
		int movie, user, date, prevUser = -1;
		Map<Integer, Integer> dateToFreq = new HashMap<Integer, Integer>();
		Integer freq;
		double predictedRating;
		for (int i = 0; i < NUM_TRAINING_PROBE_POINTS; i++) {
			user = users[i];
			movie = movies[i];
			date = dates[i];

			if (user != prevUser) {
				dateToFreq = new HashMap<Integer, Integer>();
				// Traverse this user's data and construct dateToFreq
				for (int l = i; l < qualData.length && qualData[l][0] == user; l++) {
					freq = dateToFreq.get((int) date);
					if (freq == null) {
						freq = 0;
					}
					freq++;
					dateToFreq.put((int) date, freq);
				}
			}
			prevUser = user;
			
			predictedRating = predictRating(movie, user, date, dateToFreq);
			out.write(String.format("%d %d %.4f\n", user, movie, predictedRating));
		}
		out.close();
		
		prevUser = -1;
		fstream = new FileWriter("TimeSVD_5_with_probe_training");
		out = new BufferedWriter(fstream);
		for (int i = 0; i < qualData.length; i++) {
			user = qualData[i][0];
			movie = qualData[i][1];
			date = qualData[i][2];

			if (user != prevUser) {
				dateToFreq = new HashMap<Integer, Integer>();
				// Traverse this user's data and construct dateToFreq
				for (int l = i; l < qualData.length && qualData[l][0] == user; l++) {
					freq = dateToFreq.get((int) date);
					if (freq == null) {
						freq = 0;
					}
					freq++;
					dateToFreq.put((int) date, freq);
				}
			}
			prevUser = user;
			
			predictedRating = predictRating(movie, user, date, dateToFreq);
			out.write(String.format("%d %d %.4f\n", user, movie, predictedRating));
		}
		out.close();
	}
	

	public static void main(String[] args) throws NumberFormatException,
			IOException {

		TimeSVD trainer;
		if (args.length == 1) {
			trainer = new TimeSVD(Integer.parseInt(args[0]));
		} else if (args.length == 2) {
			trainer = new TimeSVD(Integer.parseInt(args[0]),
					Integer.parseInt(args[1]));
		} else if (args.length == 7) {
			trainer = new TimeSVD(Integer.parseInt(args[0]),
					Double.parseDouble(args[1]), Double.parseDouble(args[2]),
					Double.parseDouble(args[3]), Double.parseDouble(args[4]),
					Double.parseDouble(args[5]), Double.parseDouble(args[6]));
		} else {
			System.exit(1);
			return;
		}
		trainer.train();
	}
}

