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

package librec.ranking;

import happy.coding.io.Logs;
import happy.coding.io.Strings;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import librec.data.Configuration;
import librec.data.DenseMatrix;
import librec.data.DenseVector;
import librec.data.SparseMatrix;
import librec.data.SparseVector;
import librec.data.VectorEntry;
import librec.intf.IterativeRecommender;

/**
 * Takacs and Tikk, <strong>Alternating Least Squares for Personalized Ranking</strong>, RecSys 2012.
 * 
 * @author guoguibing
 * 
 */
@Configuration("binThold, factors, isSupportWeight, numIters")
public class RankALS extends IterativeRecommender {

	// whether support based weighting is used ($s_i=|U_i|$) or not ($s_i=1$)
	private boolean isSupportWeight;

	private DenseVector s;

	private double sum_s;

	public RankALS(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);

		isRankingPred = true;
		checkBinary();
	}

	@Override
	protected void initModel() throws Exception {
		super.initModel();

		isSupportWeight = algoOptions.isOn("-sw");

		s = new DenseVector(numItems);
		sum_s = 0;
		for (int i = 0; i < numItems; i++) {
			double si = isSupportWeight ? trainMatrix.columnSize(i) : 1;
			s.set(i, si);
			sum_s += si;
		}

	}

	@Override
	protected void buildModel() throws Exception {
		for (int iter = 1; iter < numIters; iter++) {

			if (verbose)
				Logs.debug("{}{} runs at iter = {}/{}", algoName, foldInfo, iter, numIters);

			// P step: update user vectors
			DenseVector sum_sq = new DenseVector(numFactors);
			DenseMatrix sum_sqq = new DenseMatrix(numFactors, numFactors);

			for (int j = 0; j < numItems; j++) {
				DenseVector qj = Q.row(j);
				double sj = s.get(j);

				sum_sq = sum_sq.add(qj.scale(sj));
				sum_sqq = sum_sqq.add(qj.outer(qj).scale(sj));
			}

			List<Integer> cus = trainMatrix.rows(); // list of users with
													// $c_ui=1$
			for (int u : cus) {
				// for each user
				DenseMatrix sum_cqq = new DenseMatrix(numFactors, numFactors);
				DenseVector sum_cq = new DenseVector(numFactors);
				DenseVector sum_cqr = new DenseVector(numFactors);
				DenseVector sum_sqr = new DenseVector(numFactors);

				SparseVector Ru = trainMatrix.row(u);
				double sum_c = Ru.getCount();
				double sum_sr = 0, sum_cr = 0;

				for (VectorEntry ve : Ru) {
					int i = ve.index();
					double rui = ve.get();
					// double cui = 1;
					DenseVector qi = Q.row(i);

					sum_cqq = sum_cqq.add(qi.outer(qi));
					sum_cq = sum_cq.add(qi);
					sum_cqr = sum_cqr.add(qi.scale(rui));

					// ratings of unrated items will be 0
					double si = s.get(i);
					sum_sr += si * rui;
					sum_cr += rui;
					sum_sqr = sum_sqr.add(qi.scale(si * rui));
				}

				DenseMatrix M = sum_cqq.scale(sum_s).minus(sum_cq.outer(sum_sq)).minus(sum_sq.outer(sum_cq))
						.add(sum_sqq.scale(sum_c));

				DenseVector y = sum_cqr.scale(sum_s).minus(sum_cq.scale(sum_sr)).minus(sum_sq.scale(sum_cr))
						.add(sum_sqr.scale(sum_c));

				DenseVector pu = M.inv().mult(y);
				P.setRow(u, pu);
			}

			// Q step: update item vectors
			Map<Integer, Double> m_sum_sr = new HashMap<>();
			Map<Integer, Double> m_sum_cr = new HashMap<>();
			Map<Integer, Double> m_sum_c = new HashMap<>();
			Map<Integer, DenseVector> m_sum_cq = new HashMap<>();

			for (int u : cus) {
				SparseVector Ru = trainMatrix.row(u);

				double sum_sr = 0, sum_cr = 0, sum_c = Ru.getCount();
				DenseVector sum_cq = new DenseVector(numFactors);

				for (VectorEntry ve : Ru) {
					int j = ve.index();
					double ruj = ve.get();
					double sj = s.get(j);

					sum_sr += sj * ruj;
					sum_cr += ruj;
					sum_cq = sum_cq.add(Q.row(j));
				}

				m_sum_sr.put(u, sum_sr);
				m_sum_cr.put(u, sum_cr);
				m_sum_c.put(u, sum_c);
				m_sum_cq.put(u, sum_cq);
			}

			for (int i = 0; i < numItems; i++) {
				// for each item
				DenseMatrix sum_cpp = new DenseMatrix(numFactors, numFactors);
				DenseMatrix sum_p_p_c = new DenseMatrix(numFactors, numFactors);
				DenseVector sum_p_p_cq = new DenseVector(numFactors);
				DenseVector sum_cpr = new DenseVector(numFactors);
				DenseVector sum_c_sr_p = new DenseVector(numFactors);
				DenseVector sum_cr_p = new DenseVector(numFactors);
				DenseVector sum_p_r_c = new DenseVector(numFactors);

				double si = s.get(i);

				for (int u : cus) {
					DenseVector pu = P.row(u);
					double rui = trainMatrix.get(u, i);

					DenseMatrix pp = pu.outer(pu);
					sum_cpp = sum_cpp.add(pp);
					sum_p_p_cq = sum_p_p_cq.add(pp.mult(m_sum_cq.get(u)));
					sum_p_p_c = sum_p_p_c.add(pp.scale(m_sum_c.get(u)));
					sum_cr_p = sum_cr_p.add(pu.scale(m_sum_cr.get(u)));

					if (rui > 0) {
						sum_cpr = sum_cpr.add(pu.scale(rui));
						sum_c_sr_p = sum_c_sr_p.add(pu.scale(m_sum_sr.get(u)));
						sum_p_r_c = sum_p_r_c.add(pu.scale(rui * m_sum_c.get(u)));
					}
				}

				DenseMatrix M = sum_cpp.scale(sum_s).add(sum_p_p_c.scale(si));
				DenseVector y = sum_cpp.mult(sum_sq).add(sum_cpr.scale(sum_s)).minus(sum_c_sr_p)
						.add(sum_p_p_cq.scale(si)).minus(sum_cr_p.scale(si)).add(sum_p_r_c.scale(si));
				DenseVector qi = M.inv().mult(y);
				Q.setRow(i, qi);
			}
		}
	}

	@Override
	public String toString() {
		return Strings.toString(new Object[] { binThold, numFactors, isSupportWeight, numIters });
	}
}
