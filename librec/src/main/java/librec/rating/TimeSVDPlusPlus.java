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

import happy.coding.io.FileIO;
import happy.coding.io.Logs;
import happy.coding.io.Strings;
import happy.coding.math.Randoms;

import java.io.BufferedReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import librec.data.DenseMatrix;
import librec.data.DenseVector;
import librec.data.MatrixEntry;
import librec.data.RatingContext;
import librec.data.SparseMatrix;
import librec.intf.ContextRecommender;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

/**
 * Koren, <strong>Collaborative Filtering with Temporal Dynamics</strong>, KDD 2009.
 * 
 * 
 * <p>
 * Thank Bin Wu for sharing a version of timeSVD++ source code.
 * </p>
 * 
 * @author guoguibing
 * 
 */
public class TimeSVDPlusPlus extends ContextRecommender {

	// the span of days of rating timestamps
	private static int numDays;

	// {user, mean date}
	private static Map<Integer, Double> userMeanDate;

	// minimum/maximum rating timestamp
	private static long minTimestamp, maxTimestamp;

	// time decay factor
	private static float beta;

	// number of bins over all the items
	private static int numBins;

	// item's implicit influence
	private DenseMatrix Y;

	// {item, bin(t)} bias matrix
	private DenseMatrix Bit;

	// {user, day, bias} table
	private Table<Integer, Integer, Double> But;

	// user bias parameters
	private DenseVector userAlpha;

	// {user, feature} alpha matrix
	private DenseMatrix Auk;

	// {user, {feature, day, value} } map
	private Map<Integer, Table<Integer, Integer, Double>> Pukt;

	// time unit may depend on data sets, e.g. in MovieLens, it is unix seconds   
	private final static TimeUnit secs = TimeUnit.SECONDS;

	// read context information
	static {
		try {
			readContext();

			setup();

		} catch (Exception e) {
			Logs.error(e.getMessage());
			e.printStackTrace();
		}
	}

	public TimeSVDPlusPlus(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);

		algoName = "timeSVD++";
	}

	@Override
	protected void initModel() throws Exception {
		super.initModel();

		userBias = new DenseVector(numUsers);
		userBias.init();

		itemBias = new DenseVector(numItems);
		itemBias.init();

		userAlpha = new DenseVector(numUsers);
		userAlpha.init();

		Bit = new DenseMatrix(numItems, numBins);
		Bit.init();

		Y = new DenseMatrix(numItems, numFactors);
		Y.init();

		Auk = new DenseMatrix(numUsers, numFactors);
		Auk.init();

		But = HashBasedTable.create();
		Pukt = new HashMap<>();
	}

	@Override
	protected void buildModel() throws Exception {
		for (int iter = 1; iter <= numIters; iter++) {
			errs = 0;
			loss = 0;

			for (MatrixEntry me : trainMatrix) {
				int u = me.row();
				int i = me.column();
				double rui = me.get();
				if (rui <= 0)
					continue;

				long timestamp = ratingContexts.get(u, i).getTimestamp();
				// day t
				int t = days(timestamp, minTimestamp);
				int bin = bin(t);
				double dev_ut = dev(u, t);

				double bi = itemBias.get(i);
				double bit = Bit.get(i, bin);
				double bu = userBias.get(u);
				double but = 0;
				// late initialization
				if (!But.contains(u, t))
					But.put(u, t, Randoms.random());
				but = But.get(u, t);

				double au = userAlpha.get(u); // alpha_u

				double pui = globalMean + bi + bit; // mu + bi(t)
				pui += bu + au * dev_ut + but; // bu(t)

				// qi * yj
				List<Integer> Ru = trainMatrix.getColumns(u);
				double sum_y = 0;
				for (int j : Ru) {
					sum_y += DenseMatrix.rowMult(Y, j, Q, i);
				}
				if (Ru.size() > 0)
					pui += sum_y * Math.pow(Ru.size(), -0.5);

				// qi * pu(t)
				if (!Pukt.containsKey(u)) {
					Table<Integer, Integer, Double> data = HashBasedTable.create();
					Pukt.put(u, data);
				}

				Table<Integer, Integer, Double> pkt = Pukt.get(u);
				for (int k = 0; k < numFactors; k++) {
					double qik = Q.get(i, k);
					if (!pkt.contains(k, t)) {
						// late initialization
						pkt.put(k, t, Randoms.random());
					}
					double puk = P.get(u, k) + Auk.get(u, k) * dev_ut + pkt.get(k, t);

					pui += puk * qik;
				}

				double eui = pui - rui;
				errs += eui * eui;
				loss += eui * eui;

				// update bi
				double sgd = eui + regB * bi;
				itemBias.add(i, -lRate * sgd);
				loss += regB * bi * bi;

				// update bi,bin(t)
				sgd = eui + regB * bit;
				Bit.add(i, t, -lRate * sgd);
				loss += regB * bit * bit;

				// update bu
				sgd = eui + regB * bu;
				userBias.add(u, -lRate * sgd);
				loss += regB * bu * bu;

				// update au
				sgd = eui * dev_ut + regB * au;
				userAlpha.add(u, -lRate * sgd);
				loss += regB * au * au;

				// update but
				sgd = eui + regB * but;
				double delta = but - lRate * sgd;
				But.put(u, t, delta);
				loss += regB * but * but;

				// TODO: add codes here to update other variables
			}

			if (isConverged(iter))
				break;
		}
	}

	@Override
	protected double predict(int u, int i) {
		// retrieve the test rating timestamp
		long timestamp = ratingContexts.get(u, i).getTimestamp();
		int t = days(timestamp, minTimestamp);
		int bin = bin(t);
		double dev_ut = dev(u, t);

		double pred = globalMean;

		// bi(t): timeSVD++ adopts eq. (6) rather than eq. (12)
		pred += itemBias.get(i) + Bit.get(i, bin);

		// bu(t): eq. (9)
		pred += userBias.get(u) + userAlpha.get(u) * dev_ut + But.get(u, t);

		// qi * yj
		List<Integer> Ru = trainMatrix.getColumns(u);
		double sum_y = 0;
		for (int j : Ru) {
			sum_y += DenseMatrix.rowMult(Y, j, Q, i);
		}
		if (Ru.size() > 0)
			pred += sum_y * Math.pow(Ru.size(), -0.5);

		// qi * pu(t)
		if (!Pukt.containsKey(u)) {
			Table<Integer, Integer, Double> data = HashBasedTable.create();
			Pukt.put(u, data);
		}

		Table<Integer, Integer, Double> pkt = Pukt.get(u);
		for (int k = 0; k < numFactors; k++) {
			double qik = Q.get(i, k);
			// eq. (13)
			double puk = P.get(u, k) + Auk.get(u, k) * dev_ut + (pkt.contains(k, t) ? pkt.get(k, t) : 0);

			pred += puk * qik;
		}

		return pred;
	}

	@Override
	public String toString() {
		return super.toString() + "," + Strings.toString(new Object[] { beta, numBins });
	}

	/**
	 * Read rating timestamps
	 */
	protected static void readContext() throws Exception {
		String contextPath = cf.getPath("dataset.social");
		Logs.debug("Context dataset: {}", Strings.last(contextPath, 38));

		ratingContexts = HashBasedTable.create();
		BufferedReader br = FileIO.getReader(contextPath);
		String line = null;
		RatingContext rc = null;

		minTimestamp = Long.MAX_VALUE;
		maxTimestamp = Long.MIN_VALUE;

		while ((line = br.readLine()) != null) {
			String[] data = line.split("[ \t,]");
			String user = data[0];
			String item = data[1];

			// convert to million seconds
			long timestamp = secs.toMillis(Long.parseLong(data[3]));

			int userId = rateDao.getUserId(user);
			int itemId = rateDao.getItemId(item);

			rc = new RatingContext(userId, itemId);
			rc.setTimestamp(timestamp);

			ratingContexts.put(userId, itemId, rc);

			if (minTimestamp > timestamp)
				minTimestamp = timestamp;
			if (maxTimestamp < timestamp)
				maxTimestamp = timestamp;
		}

		numDays = days(maxTimestamp, minTimestamp);
	}

	/**
	 * setup for the timeSVD++ model
	 */
	protected static void setup() {
		beta = cf.getFloat("timeSVD++.beta");
		numBins = cf.getInt("timeSVD++.item.bins");

		// compute user's mean of rating timestamps
		userMeanDate = new HashMap<>();
		for (int u = 0; u < numUsers; u++) {
			Map<Integer, RatingContext> rcs = ratingContexts.row(u);

			int sum = 0;
			for (RatingContext rc : rcs.values()) {
				sum += days(rc.getTimestamp(), minTimestamp);
			}

			if (sum > 0)
				userMeanDate.put(u, (sum + 0.0) / rcs.size());
		}
	}

	/***************************************************************** Functional Methods *******************************************/
	/**
	 * @return the time deviation for a specific timestamp t w.r.t the mean date tu
	 */
	protected double dev(int u, int t) {
		double tu = userMeanDate.get(u);

		// date difference in days
		double diff = t - tu;

		return Math.signum(diff) * Math.pow(diff, beta);
	}

	/**
	 * @return the bin number (starting from 0) for a specific timestamp t;
	 */
	protected static int bin(int day) {
		return day * numBins / numDays;
	}

	/**
	 * @return number of days for a given time difference
	 */
	protected static int days(long diff) {
		return (int) TimeUnit.MILLISECONDS.toDays(diff);
	}

	/**
	 * @return number of days between two timestamps
	 */
	protected static int days(long t1, long t2) {
		return days(Math.abs(t1 - t2));
	}
}
