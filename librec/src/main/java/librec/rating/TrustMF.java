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

import librec.data.DenseMatrix;
import librec.data.DenseVector;
import librec.data.MatrixEntry;
import librec.data.SparseMatrix;
import librec.intf.SocialRecommender;

/**
 * Yang et al., <strong>Social Collaborative Filtering by Trust</strong>, IJCAI 2013.
 * 
 * @author guoguibing
 * 
 */
public class TrustMF extends SocialRecommender {

	protected DenseMatrix Br, Wr, Vr;
	protected DenseMatrix Be, We, Ve;

	protected String model;

	public TrustMF(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);

		model = algoOptions.getString("-m");
		algoName = "TrustMF (" + model + ")";
	}

	protected void initTr() {
		Vr = new DenseMatrix(numItems, numFactors);
		Br = new DenseMatrix(numUsers, numFactors);
		Wr = new DenseMatrix(numUsers, numFactors);

		Vr.init();
		Br.init();
		Wr.init();
	}

	protected void initTe() {
		Ve = new DenseMatrix(numItems, numFactors);
		Be = new DenseMatrix(numUsers, numFactors);
		We = new DenseMatrix(numUsers, numFactors);

		Ve.init();
		Be.init();
		We.init();
	}

	@Override
	protected void initModel() throws Exception {
		switch (model) {
		case "Tr":
			initTr();
			break;

		case "Te":
			initTe();
			break;

		case "T":
		default:
			initTr();
			initTe();
			break;
		}
	}

	@Override
	protected void buildModel() throws Exception {
		switch (model) {
		case "Tr":
			TrusterMF();
			break;
		case "Te":
			TrusteeMF();
			break;
		case "T":
		default:
			TrusterMF();
			TrusteeMF();
			break;
		}
	}

	/**
	 * Build TrusterMF model: Br*Vr
	 */
	protected void TrusterMF() throws Exception {
		for (int iter = 1; iter <= numIters; iter++) {
			loss = 0;

			// gradients of B, V, W
			DenseMatrix BS = new DenseMatrix(numUsers, numFactors);
			DenseMatrix WS = new DenseMatrix(numUsers, numFactors);
			DenseMatrix VS = new DenseMatrix(numItems, numFactors);

			// rate matrix
			for (MatrixEntry me : trainMatrix) {

				int u = me.row();
				int j = me.column();
				double ruj = me.get();

				double pred = predict(u, j, false);
				double euj = g(pred) - normalize(ruj);

				loss += euj * euj;

				double csgd = gd(pred) * euj;

				for (int f = 0; f < numFactors; f++) {
					BS.add(u, f, csgd * Vr.get(j, f) + regU * Br.get(u, f));
					VS.add(j, f, csgd * Br.get(u, f) + regI * Vr.get(j, f));

					loss += regU * Br.get(u, f) * Br.get(u, f);
					loss += regI * Vr.get(j, f) * Vr.get(j, f);
				}
			}

			// social matrix
			for (MatrixEntry me : socialMatrix) {

				int u = me.row();
				int k = me.column();
				double tuk = me.get();

				if (tuk > 0) {
					double pred = DenseMatrix.rowMult(Br, u, Wr, k);
					double euj = g(pred) - tuk;

					loss += regS * euj * euj;

					double csgd = gd(pred) * euj;

					for (int f = 0; f < numFactors; f++) {
						BS.add(u, f, regS * csgd * Wr.get(k, f) + regU * Br.get(u, f));
						WS.add(k, f, regS * csgd * Br.get(u, f) + regU * Wr.get(k, f));

						loss += regU * Br.get(u, f) * Br.get(u, f);
						loss += regU * Wr.get(u, f) * Wr.get(u, f);
					}
				}
			}

			Br = Br.add(BS.scale(-lRate));
			Vr = Vr.add(VS.scale(-lRate));
			Wr = Wr.add(WS.scale(-lRate));

			loss *= 0.5;

			if (isConverged(iter))
				break;
		}
	}

	/**
	 * Build TrusteeMF model: We*Ve
	 */
	protected void TrusteeMF() throws Exception {
		for (int iter = 1; iter <= numIters; iter++) {
			loss = 0;

			// gradients of B, V, W
			DenseMatrix BS = new DenseMatrix(numUsers, numFactors);
			DenseMatrix WS = new DenseMatrix(numUsers, numFactors);
			DenseMatrix VS = new DenseMatrix(numItems, numFactors);

			// rate matrix
			for (MatrixEntry me : trainMatrix) {

				int u = me.row();
				int j = me.column();
				double ruj = me.get();

				double pred = predict(u, j, false);
				double euj = g(pred) - normalize(ruj);

				loss += euj * euj;

				double csgd = gd(pred) * euj;

				for (int f = 0; f < numFactors; f++) {
					WS.add(u, f, csgd * Ve.get(j, f) + regU * We.get(u, f));
					VS.add(j, f, csgd * We.get(u, f) + regI * Ve.get(j, f));

					loss += regU * We.get(u, f) * We.get(u, f);
					loss += regI * Ve.get(j, f) * Ve.get(j, f);
				}
			}

			// social matrix
			for (MatrixEntry me : socialMatrix) {

				int k = me.row();
				int u = me.column();
				double tku = me.get();

				if (tku > 0) {
					double pred = DenseMatrix.rowMult(Be, k, We, u);
					double euj = g(pred) - tku;

					loss += regS * euj * euj;

					double csgd = gd(pred) * euj;

					for (int f = 0; f < numFactors; f++) {
						WS.add(u, f, regS * csgd * Be.get(k, f) + regU * We.get(u, f));
						BS.add(k, f, regS * csgd * We.get(u, f) + regU * Be.get(k, f));

						loss += regU * We.get(u, f) * We.get(u, f);
						loss += regU * Be.get(k, f) * Be.get(k, f);
					}
				}
			}

			Be = Be.add(BS.scale(-lRate));
			Ve = Ve.add(VS.scale(-lRate));
			We = We.add(WS.scale(-lRate));

			loss *= 0.5;

			if (isConverged(iter))
				break;
		}
	}

	/**
	 * This is the method used by the paper authors
	 */
	protected void updateLRate(int iter) {
		if (iter == 10)
			lRate *= 0.6;
		else if (iter == 30)
			lRate *= 0.333;
		else if (iter == 100)
			lRate *= 0.5;
	}

	protected double predict(int u, int j, boolean bounded) {

		double pred = 0.0;
		switch (model) {
		case "Tr":
			pred = DenseMatrix.rowMult(Br, u, Vr, j);
			break;
		case "Te":
			pred = DenseMatrix.rowMult(We, u, Ve, j);
			break;
		case "T":
		default:
			DenseVector uv = Br.row(u).add(We.row(u, false));
			DenseVector jv = Vr.row(j).add(Ve.row(j, false));

			pred = uv.scale(0.5).inner(jv.scale(0.5));
			break;
		}

		if (bounded)
			return denormalize(g(pred));

		return pred;
	}
}
