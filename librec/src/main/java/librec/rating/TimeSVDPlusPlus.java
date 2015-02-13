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
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import happy.coding.io.FileIO;
import happy.coding.io.Logs;
import happy.coding.io.Strings;
import happy.coding.math.Randoms;
import librec.data.DenseMatrix;
import librec.data.DenseVector;
import librec.data.MatrixEntry;
import librec.data.RatingContext;
import librec.data.SparseMatrix;
import librec.data.SparseVector;
import librec.data.VectorEntry;
import librec.intf.ContextRecommender;

/**
 * Koren, <strong>Collaborative Filtering with Temporal Dynamics</strong>, KDD 2009.
 * 
 * 
 * <p>Thank Bin Wu for sharing a version of timeSVD++ source code.</p> 
 * 
 * @author guoguibing
 * 
 */
public class TimeSVDPlusPlus extends ContextRecommender {

	// million seconds per day
	private static long MS_PER_DAY = 24 * 60 * 60 * 1000 * 1000;

	// the span of days of rating timestamps
	private static int numDays;
	// {user, mean date}
	private static Map<Integer, Long> dateMean;

	// minimum/maximum rating timestamp
	private static long min, max;

	private static float beta;

	// number of bins for all the items
	private static int numBins;

	// item's implicit influence
	private DenseMatrix Y;

	// {item, bin(t)} bias matrix
	private DenseMatrix Bit;

	// {user, day, bias} table
	private Table<Integer, Integer, Double> But;

	// user bias parameters
	private DenseVector userAlpha;

	// {user, day, bias} table
	private Table<Integer, Integer, Double> Cut;
	// user scaling
	private DenseVector userScaling;

	// {user, feature} alpha matrix
	private DenseMatrix Auf;

	// {user, {feature, day, value} } map
	private Map<Integer, Table<Integer, Integer, Double>> Puft;

	// read context information
	static {
		try {
			readContext();

			preset();

		} catch (Exception e) {
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

		userScaling = new DenseVector(numUsers); // cu
		userScaling.init();

		Y = new DenseMatrix(numItems, numFactors);
		Y.init();

		Auf = new DenseMatrix(numUsers, numFactors);
		Auf.init();

		But = HashBasedTable.create();
		Cut = HashBasedTable.create();
		Puft = new HashMap<>();
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

				long t = ratingContexts.get(u, i).getTimestamp();
				int bin = bin(t);
				int day = day(t);
				double dev = dev(u, t);

				double bi = itemBias.get(i);
				double bit = Bit.get(i, bin);
				double bu = userBias.get(u);
				double but = But.get(u, day);
				double au = userAlpha.get(u); // alpha_u
				double cu = userScaling.get(u);
				double cut = Cut.get(u, day);

				double pui = globalMean + (bi + bit) * (cu + cut); // mu + bi(t)
				pui += bu + au * dev(u, t) + but; // bu(t)

				// qi*yi
				SparseVector Ru = trainMatrix.row(u);
				double sum_y = 0;
				for (VectorEntry ve : Ru) {
					int k = ve.index();
					sum_y += DenseMatrix.rowMult(Y, k, Q, i);
				}
				if (Ru.getCount() > 0)
					pui += sum_y / Math.pow(Ru.getCount(), -0.5);

				// qi*pu(t)
				if (!Puft.containsKey(u)) {
					Table<Integer, Integer, Double> data = HashBasedTable.create();
					Puft.put(u, data);
				}

				Table<Integer, Integer, Double> data = Puft.get(u);
				for (int f = 0; f < numFactors; f++) {
					double qif = Q.get(i, f);
					if (!data.contains(f, day)) {
						// late initialization
						data.put(f, day, Randoms.random());
					}
					double puf = P.get(u, f) + Auf.get(u, f) * dev + data.get(f, day);

					pui += puf * qif;
				}

				double eui = pui - rui;
				errs += eui * eui;
				loss += eui * eui;

				// update bu
				double sgd = eui + regB * bu;
				userBias.add(u, -lRate * sgd);
				
				// TODO: add codes here to update other variables
			}

			if (isConverged(iter))
				break;
		}
	}

	@Override
	protected double predict(int u, int j) {
		// retrieve the test rating timestamp
		long t = ratingContexts.get(u, j).getTimestamp();
		int bin = bin(t);
		int day = day(t);
		double dev = dev(u, t);

		double pred = globalMean;

		// bi(t)
		pred += (itemBias.get(j) + Bit.get(j, bin)) * (userScaling.get(u) + Cut.get(u, day));

		// bu(t)
		pred += userBias.get(u) + userAlpha.get(u) * dev + But.get(u, day);

		// qi*yi
		SparseVector Ru = trainMatrix.row(u);
		double sum_y = 0;
		for (VectorEntry ve : Ru) {
			int k = ve.index();
			sum_y += DenseMatrix.rowMult(Y, k, Q, j);
		}
		if (Ru.getCount() > 0)
			pred += sum_y / Math.pow(Ru.getCount(), -0.5);

		// qi*pu(t)
		if (!Puft.containsKey(u)) {
			Table<Integer, Integer, Double> data = HashBasedTable.create();
			Puft.put(u, data);
		}

		Table<Integer, Integer, Double> data = Puft.get(u);
		for (int f = 0; f < numFactors; f++) {
			double qjf = Q.get(j, f);
			double puf = P.get(u, f) + Auf.get(u, f) * dev + (data.contains(f, day) ? data.get(f, day) : 0);

			pred += puf * qjf;
		}

		return pred;
	}

	@Override
	public String toString() {
		return super.toString() + "," + Strings.toString(new Object[] { beta, numBins });
	}

	/**
	 * Read rating timestamp
	 * 
	 */
	protected static void readContext() throws Exception {
		String contextPath = cf.getPath("dataset.social");
		Logs.debug("Context dataset: {}", Strings.last(contextPath, 38));

		ratingContexts = HashBasedTable.create();
		BufferedReader br = FileIO.getReader(contextPath);
		String line = null;
		RatingContext rc = null;

		min = Long.MAX_VALUE;
		max = Long.MIN_VALUE;
		while ((line = br.readLine()) != null) {
			String[] data = line.split("[ \t,]");
			String user = data[0];
			String item = data[1];
			long timestamp = Long.parseLong(data[3]);

			int userId = rateDao.getUserId(user);
			int itemId = rateDao.getItemId(item);

			rc = new RatingContext(userId, itemId);
			rc.setTimestamp(timestamp);

			ratingContexts.put(userId, itemId, rc);

			if (min > timestamp)
				min = timestamp;
			if (max < timestamp)
				max = timestamp;
		}

		numDays = days(max - min);
	}

	/**
	 * preset for the timeSVD++ model
	 */
	protected static void preset() {
		beta = cf.getFloat("timeSVD++.beta");
		numBins = cf.getInt("timeSVD++.item.bins");

		// compute user's mean of rating timestamps
		dateMean = new HashMap<>();
		for (int u = 0; u < numUsers; u++) {
			Map<Integer, RatingContext> rcs = ratingContexts.row(u);

			long sum = 0;
			for (RatingContext rc : rcs.values()) {
				sum += rc.getTimestamp();
			}

			dateMean.put(u, sum / rcs.size());
		}
	}

	/***************************************************************** Functional Methods *******************************************/
	/**
	 * @return the time deviation for a specific timestamp t w.r.t the mean date tu
	 */
	protected double dev(int u, long t) {
		long tu = dateMean.get(u);

		// date difference in millionseconds;
		long diff = t - tu;

		// date difference in days
		int days = days(Math.abs(diff));

		return Math.signum(diff) * Math.pow(days, beta);
	}

	/**
	 * @return the bin number (starting from 0) for a specific timestamp t;
	 */
	protected static int bin(long t) {
		return day(t) * numBins / numDays;
	}

	/**
	 * @return the number of days since the earliest one
	 */
	protected static int day(long t) {
		return days(t - min);
	}

	/**
	 * @return the number of days for a timestamp difference
	 */
	private static int days(long diff) {
		return (int) (diff / MS_PER_DAY);
	}
}
