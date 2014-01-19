package lib.rec.ext;

import happy.coding.io.FileIO;
import happy.coding.math.Randoms;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lib.rec.data.DenseVec;
import lib.rec.data.SparseMat;
import lib.rec.data.UpperSymmMat;
import lib.rec.intf.IterativeRecommender;
import no.uib.cipr.matrix.MatrixEntry;
import no.uib.cipr.matrix.sparse.SparseVector;

public class BaseNM extends IterativeRecommender {

	protected UpperSymmMat itemCorrs;
	protected boolean isPosOnly;
	protected double minSim;

	protected String dirPath;
	protected boolean isMem;

	public BaseNM(SparseMat trainMatrix, SparseMat testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);

		algoName = "BaseNM";

		isPosOnly = cf.isOn("is.similarity.pos");
		minSim = isPosOnly ? 0.0 : Double.MIN_VALUE;

		dirPath = "Fold " + fold;
		isMem = numItems < 100_000;
	}

	private void initItemCorrsMem() {
		// item correlation matrix
		itemCorrs = new UpperSymmMat(numItems);

		// ignore items without any training ratings: can greatly reduce memory usage
		Set<Integer> items = new HashSet<>();
		for (int i = 0; i < numItems; i++)
			if (trainMatrix.col(i).getUsed() == 0)
				items.add(i);

		for (int i = 0; i < numItems; i++) {
			if (items.contains(i))
				continue;

			itemCorrs.set(i, i, 0.0);

			for (int j = i + 1; j < numItems; j++) {
				if (items.contains(j))
					continue;

				double val = isPosOnly ? Randoms.uniform(0.0, 0.01) : Randoms.gaussian(initMean, initStd);
				itemCorrs.set(i, j, val);
			}
		}
	}

	private void initItemCorrsDisk() throws Exception {
		// ignore items without any training ratings
		Set<Integer> items = new HashSet<>();
		for (int i = 0; i < numItems; i++)
			if (trainMatrix.col(i).getUsed() == 0)
				items.add(i);

		// create fold
		FileIO.deleteDirectory(dirPath);
		dirPath = FileIO.makeDirectory(dirPath);

		for (int i = 0; i < numItems; i++) {
			if (items.contains(i))
				continue;

			StringBuilder sb = new StringBuilder();
			// Different from memory-based: scan all items 
			for (int j = i + 1; j < numItems; j++) {
				if (items.contains(j))
					continue;

				double val = isPosOnly ? Randoms.uniform(0.0, 0.01) : Randoms.gaussian(initMean, initStd);
				sb.append(j + " " + val + "\n");
			}
			// output to disk
			if (sb.length() > 0)
				FileIO.writeString(dirPath + i + ".txt", sb.toString());
		}
	}

	@Override
	protected void initModel() {

		// user, item biases
		userBiases = new DenseVec(numUsers);
		itemBiases = new DenseVec(numItems);

		userBiases.init(initMean, initStd);
		itemBiases.init(initMean, initStd);

		if (isMem)
			initItemCorrsMem();
		else
			try {
				initItemCorrsDisk();
			} catch (Exception e) {
				e.printStackTrace();
			}
	}

	protected void buildModelMem() {
		for (int iter = 1; iter <= maxIters; iter++) {

			loss = 0;
			errs = 0;
			for (MatrixEntry me : trainMatrix) {

				int u = me.row(); // user
				int j = me.column(); // item

				double ruj = me.get();
				if (ruj <= 0.0)
					continue;

				// a set of similar items
				SparseVector uv = trainMatrix.row(u, j);
				List<Integer> items = new ArrayList<>();
				for (int i : uv.getIndex()) {
					if (itemCorrs.get(j, i) > minSim)
						items.add(i);
				}
				double w = Math.sqrt(items.size());

				// obtain the prediction
				double bu = userBiases.get(u), bj = itemBiases.get(j);
				double pred = globalMean + bu + bj;

				double sum_sji = 0;
				for (int i : items) {
					double sji = itemCorrs.get(j, i);
					double rui = uv.get(i);
					double bui = globalMean + bu + itemBiases.get(i);

					pred += sji * (rui - bui) / w;
					sum_sji += sji / w;
				}

				double euj = ruj - pred;
				errs += euj * euj;
				loss += euj * euj;

				// update similarity frist since bu and bj are used here
				for (int i : items) {
					double sji = itemCorrs.get(j, i);
					double rui = uv.get(i);
					double bui = globalMean + bu + itemBiases.get(i);

					double delta = lRate * (euj * (rui - bui) / w - regU * sji);
					itemCorrs.add(j, i, delta);

					loss += regU * sji * sji;
				}

				// update factors
				double sgd = euj * (1 - sum_sji) - regU * bu;
				userBiases.add(u, lRate * sgd);
				loss += regU * bu * bu;

				sgd = euj * (1 - sum_sji) - regI * bj;
				itemBiases.add(j, lRate * sgd);
				loss += regI * bj * bj;

			}

			errs *= 0.5;
			loss *= 0.5;

			if (postEachIter(iter))
				break;

		}// end of training
	}

	protected void buildModelDisk() throws Exception {
		for (int iter = 1; iter <= maxIters; iter++) {

			loss = 0;
			errs = 0;
			for (MatrixEntry me : trainMatrix) {

				int u = me.row(); // user
				int j = me.column(); // item

				double ruj = me.get();
				if (ruj <= 0.0)
					continue;

				// a set of similar items
				SparseVector cv = getCorrVector(j);

				SparseVector uv = trainMatrix.row(u, j);
				List<Integer> items = new ArrayList<>();

				Map<Integer, SparseVector> itemVectors = new HashMap<>();
				for (int i : uv.getIndex()) {
					SparseVector sv = null;
					double sji = i > j ? cv.get(i) : (sv = getCorrVector(i)).get(j);
					if (sji != 0 && sji > minSim) {
						items.add(i);

						if (sv != null)
							itemVectors.put(i, sv);
					}
				}
				double w = Math.sqrt(items.size());

				// obtain the prediction
				double bu = userBiases.get(u), bj = itemBiases.get(j);
				double pred = globalMean + bu + bj;

				double sum_sji = 0;
				for (int i : items) {
					double sji = i > j ? cv.get(i) : itemVectors.get(i).get(j);
					double rui = uv.get(i);
					double bui = globalMean + bu + itemBiases.get(i);

					pred += sji * (rui - bui) / w;
					sum_sji += sji / w;
				}

				double euj = ruj - pred;
				errs += euj * euj;
				loss += euj * euj;

				// update similarity frist since bu and bj are used here
				for (int i : items) {
					SparseVector sv = null;
					double sji = i > j ? cv.get(i) : (sv = itemVectors.get(i)).get(j);
					double rui = uv.get(i);
					double bui = globalMean + bu + itemBiases.get(i);

					double delta = lRate * (euj * (rui - bui) / w - regU * sji);

					if (i > j)
						cv.add(i, delta);
					else {
						sv.add(j, delta);
						updateCorrVector(i, sv);
					}

					loss += regU * sji * sji;
				}

				// update correlation vector to disk
				updateCorrVector(j, cv);

				// update factors
				double sgd = euj * (1 - sum_sji) - regU * bu;
				userBiases.add(u, lRate * sgd);
				loss += regU * bu * bu;

				sgd = euj * (1 - sum_sji) - regI * bj;
				itemBiases.add(j, lRate * sgd);
				loss += regI * bj * bj;
			}

			errs *= 0.5;
			loss *= 0.5;

			if (postEachIter(iter))
				break;

		}// end of training
	}

	protected void updateCorrVector(int j, SparseVector corrVec) throws Exception {
		StringBuilder sb = new StringBuilder();

		for (int i : corrVec.getIndex()) {
			double val = corrVec.get(i);
			if (val != 0)
				sb.append(i + " " + val + "\n");
		}

		FileIO.writeString(dirPath + j + ".txt", sb.toString());
	}

	protected SparseVector getCorrVector(int j) {
		SparseVector iv = new SparseVector(numItems);

		// read data
		BufferedReader br = null;
		try {
			br = FileIO.getReader(dirPath + j + ".txt");
			String line = null;
			while ((line = br.readLine()) != null) {
				String[] data = line.split("[ \t,]");

				int i = Integer.parseInt(data[0]);
				double val = Double.parseDouble(data[1]);

				iv.set(i, val);
			}

			br.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

		return iv;
	}

	@Override
	protected void buildModel() {

		if (isMem)
			buildModelMem();
		else
			try {
				buildModelDisk();
			} catch (Exception e) {
				e.printStackTrace();
			}
	}

	@Override
	protected double predict(int u, int j) {
		double bu = userBiases.get(u);
		double pred = globalMean + bu + itemBiases.get(j);

		SparseVector cv = null;
		if (!isMem)
			cv = getCorrVector(j);

		// get a number of similar items except item j
		SparseVector uv = trainMatrix.row(u, j);
		int[] items = uv.getIndex();

		int k = 0;
		double sum = 0;
		for (int i : items) {
			double sji = isMem ? itemCorrs.get(j, i) : cv.get(i);

			if (sji != 0 && sji > minSim) {
				double rui = trainMatrix.get(u, i);
				double bui = globalMean + bu + itemBiases.get(i);

				sum += sji * (rui - bui);
				k++;
			}
		}

		if (k > 0)
			pred += sum / Math.sqrt(k);

		return pred;
	}

	@Override
	public String toString() {
		return super.toString() + "," + isPosOnly;
	}
}
