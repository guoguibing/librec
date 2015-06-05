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

import java.util.HashMap;
import java.util.Map;

import librec.data.DenseMatrix;
import librec.data.MatrixEntry;
import librec.data.SparseMatrix;
import librec.intf.SocialRecommender;

/**
 * Hao Ma, Haixuan Yang, Michael R. Lyu and Irwin King, <strong>SoRec: Social recommendation using probabilistic matrix
 * factorization</strong>, ACM CIKM 2008.
 * 
 * @author guoguibing
 * 
 */
public class SoRec extends SocialRecommender {

	private DenseMatrix Z;
	private float regC, regZ;

	private Map<Integer, Integer> inDegrees, outDegrees;

	public SoRec(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);

		initByNorm = false;
	}

	@Override
	protected void initModel() throws Exception {
		super.initModel();

		Z = new DenseMatrix(numUsers, numFactors);
		Z.init();
		
		regC = algoOptions.getFloat("-c");
		regZ = algoOptions.getFloat("-z");

		inDegrees = new HashMap<>();
		outDegrees = new HashMap<>();

		for (int u = 0; u < numUsers; u++) {
			int in = socialMatrix.columnSize(u);
			int out = socialMatrix.rowSize(u);

			inDegrees.put(u, in);
			outDegrees.put(u, out);
		}
	}

	@Override
	protected void buildModel() throws Exception {
		for (int iter = 1; iter <= numIters; iter++) {
			loss = 0;

			DenseMatrix PS = new DenseMatrix(numUsers, numFactors);
			DenseMatrix QS = new DenseMatrix(numItems, numFactors);
			DenseMatrix ZS = new DenseMatrix(numUsers, numFactors);

			// ratings
			for (MatrixEntry me : trainMatrix) {
				int u = me.row();
				int j = me.column();
				double ruj = me.get();

				double pred = predict(u, j, false);
				double euj = g(pred) - normalize(ruj);

				loss += euj * euj;

				for (int f = 0; f < numFactors; f++) {
					double puf = P.get(u, f);
					double qjf = Q.get(j, f);

					PS.add(u, f, gd(pred) * euj * qjf + regU * puf);
					QS.add(j, f, gd(pred) * euj * puf + regI * qjf);

					loss += regU * puf * puf + regI * qjf * qjf;
				}
			}

			// friends
			for (MatrixEntry me : socialMatrix) {
				int u = me.row();
				int v = me.column();
				double tuv = me.get(); // tuv ~ cik in the original paper
				if (tuv <= 0)
					continue;

				double pred = DenseMatrix.rowMult(P, u, Z, v);

				int vminus = inDegrees.get(v); // ~ d-(k)
				int uplus = outDegrees.get(u); // ~ d+(i)
				double weight = Math.sqrt(vminus / (uplus + vminus + 0.0));

				double euv = g(pred) - weight * tuv; // weight * tuv ~ cik*
				loss += regC * euv * euv;

				for (int f = 0; f < numFactors; f++) {
					double puf = P.get(u, f);
					double zvf = Z.get(v, f);

					PS.add(u, f, regC * gd(pred) * euv * zvf);
					ZS.add(v, f, regC * gd(pred) * euv * puf + regZ * zvf);

					loss += regZ * zvf * zvf;
				}
			}

			P = P.add(PS.scale(-lRate));
			Q = Q.add(QS.scale(-lRate));
			Z = Z.add(ZS.scale(-lRate));

			loss *= 0.5;

			if (isConverged(iter))
				break;
		}
	}

	@Override
	protected double predict(int u, int j, boolean bounded) {
		double pred = DenseMatrix.rowMult(P, u, Q, j);

		if (bounded)
			return denormalize(g(pred));

		return pred;
	}

}
