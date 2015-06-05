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

import java.util.List;

import librec.data.DenseMatrix;
import librec.data.DenseVector;
import librec.data.MatrixEntry;
import librec.data.SparseMatrix;
import librec.intf.SocialRecommender;

/**
 * Guo et al., <strong>TrustSVD: Collaborative Filtering with Both the Explicit and Implicit Influence of User Trust and
 * of Item Ratings</strong>, AAAI 2015.
 * 
 * @author guoguibing 
 * 
 */
public class TrustSVD extends SocialRecommender {

	private DenseMatrix W, Y;
	private DenseVector wlr_j, wlr_tc, wlr_tr;

	public TrustSVD(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);
	}

	@Override
	protected void initModel() throws Exception {
		super.initModel();

		userBias = new DenseVector(numUsers);
		itemBias = new DenseVector(numItems);

		W = new DenseMatrix(numUsers, numFactors);
		Y = new DenseMatrix(numItems, numFactors);

		if (initByNorm) {
			userBias.init(initMean, initStd);
			itemBias.init(initMean, initStd);
			W.init(initMean, initStd);
			Y.init(initMean, initStd);
		} else {
			userBias.init();
			itemBias.init();
			W.init();
			Y.init();
		}

		wlr_tc = new DenseVector(numUsers);
		wlr_tr = new DenseVector(numUsers);
		wlr_j = new DenseVector(numItems);

		userItemsCache = trainMatrix.rowColumnsCache(cacheSpec);
		userFriendsCache = socialMatrix.rowColumnsCache(cacheSpec);

		for (int u = 0; u < numUsers; u++) {
			int count = socialMatrix.columnSize(u);
			wlr_tc.set(u, count > 0 ? 1.0 / Math.sqrt(count) : 1.0);

			count = socialMatrix.rowSize(u);
			wlr_tr.set(u, count > 0 ? 1.0 / Math.sqrt(count) : 1.0);
		}

		for (int j = 0; j < numItems; j++) {
			int count = trainMatrix.columnSize(j);
			wlr_j.set(j, count > 0 ? 1.0 / Math.sqrt(count) : 1.0);
		}
	}

	@Override
	protected void buildModel() throws Exception {
		for (int iter = 1; iter <= numIters; iter++) {
			loss = 0;

			DenseMatrix PS = new DenseMatrix(numUsers, numFactors);
			DenseMatrix WS = new DenseMatrix(numUsers, numFactors);

			for (MatrixEntry me : trainMatrix) {
				int u = me.row(); // user
				int j = me.column(); // item
				double ruj = me.get(); // rating

				// To speed up, directly access the prediction instead of invoking "pred = predict(u,j)"
				double bu = userBias.get(u);
				double bj = itemBias.get(j);
				double pred = globalMean + bu + bj + DenseMatrix.rowMult(P, u, Q, j);

				// Y
				List<Integer> nu = userItemsCache.get(u);
				if (nu.size() > 0) {
					double sum = 0;
					for (int i : nu)
						sum += DenseMatrix.rowMult(Y, i, Q, j);

					pred += sum / Math.sqrt(nu.size());
				}

				// W
				List<Integer> tu = userFriendsCache.get(u);
				if (tu.size() > 0) {
					double sum = 0.0;
					for (int v : tu)
						sum += DenseMatrix.rowMult(W, v, Q, j);

					pred += sum / Math.sqrt(tu.size());
				}

				double euj = pred - ruj;

				loss += euj * euj;

				double w_nu = Math.sqrt(nu.size());
				double w_tu = Math.sqrt(tu.size());

				// update factors
				double reg_u = 1.0 / w_nu;
				double reg_j = wlr_j.get(j);

				double sgd = euj + regB * reg_u * bu;
				userBias.add(u, -lRate * sgd);

				sgd = euj + regB * reg_j * bj;
				itemBias.add(j, -lRate * sgd);

				loss += regB * reg_u * bu * bu;
				loss += regB * reg_j * bj * bj;

				double[] sum_ys = new double[numFactors]; 
				for (int f = 0; f < numFactors; f++) {
					double sum = 0;
					for (int i : nu)
						sum += Y.get(i, f);

					sum_ys[f] = w_nu > 0 ? sum / w_nu : sum;
				}

				double[] sum_ts = new double[numFactors];
				for (int f = 0; f < numFactors; f++) {
					double sum = 0;
					for (int v : tu)
						sum += W.get(v, f);

					sum_ts[f] = w_tu > 0 ? sum / w_tu : sum;
				}

				for (int f = 0; f < numFactors; f++) {
					double puf = P.get(u, f);
					double qjf = Q.get(j, f);

					double delta_u = euj * qjf + regU * reg_u * puf;
					double delta_j = euj * (puf + sum_ys[f] + sum_ts[f]) + regI * reg_j * qjf;

					PS.add(u, f, delta_u);
					Q.add(j, f, -lRate * delta_j);

					loss += regU * reg_u * puf * puf + regI * reg_j * qjf * qjf;

					for (int i : nu) {
						double yif = Y.get(i, f);

						double reg_yi = wlr_j.get(i);
						double delta_y = euj * qjf / w_nu + regI * reg_yi * yif;
						Y.add(i, f, -lRate * delta_y);

						loss += regI * reg_yi * yif * yif;
					}

					// update wvf
					for (int v : tu) {
						double wvf = W.get(v, f);

						double reg_v = wlr_tc.get(v);
						double delta_t = euj * qjf / w_tu + regU * reg_v * wvf;
						WS.add(v, f, delta_t);

						loss += regU * reg_v * wvf * wvf;
					}
				}
			}

			for (MatrixEntry me : socialMatrix) {
				int u = me.row();
				int v = me.column();
				double tuv = me.get();
				if (tuv == 0)
					continue;

				double pred = DenseMatrix.rowMult(P, u, W, v);
				double eut = pred - tuv;

				loss += regS * eut * eut;

				double csgd = regS * eut;
				double reg_u = wlr_tr.get(u);

				for (int f = 0; f < numFactors; f++) {
					double puf = P.get(u, f);
					double wvf = W.get(v, f);

					PS.add(u, f, csgd * wvf + regS * reg_u * puf);
					WS.add(v, f, csgd * puf);

					loss += regS * reg_u * puf * puf;
				}
			}

			P = P.add(PS.scale(-lRate));
			W = W.add(WS.scale(-lRate));

			loss *= 0.5;

			if (isConverged(iter))
				break;

		}// end of training
	}

	@Override
	protected double predict(int u, int j) throws Exception {
		double pred = globalMean + userBias.get(u) + itemBias.get(j) + DenseMatrix.rowMult(P, u, Q, j);

		// Y
		List<Integer> nu = userItemsCache.get(u);
		if (nu.size() > 0) {
			double sum = 0;
			for (int i : nu)
				sum += DenseMatrix.rowMult(Y, i, Q, j);

			pred += sum / Math.sqrt(nu.size());
		}

		// W
		List<Integer> tu = userFriendsCache.get(u);
		if (tu.size() > 0) {
			double sum = 0.0;
			for (int v : tu)
				sum += DenseMatrix.rowMult(W, v, Q, j);

			pred += sum / Math.sqrt(tu.size());
		}

		return pred;
	}
}