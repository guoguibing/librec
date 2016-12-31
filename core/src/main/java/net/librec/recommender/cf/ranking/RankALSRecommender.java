/**
 * Copyright (C) 2016 LibRec
 * <p>
 * This file is part of LibRec.
 * LibRec is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * LibRec is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with LibRec. If not, see <http://www.gnu.org/licenses/>.
 */
package net.librec.recommender.cf.ranking;

import net.librec.annotation.ModelData;
import net.librec.common.LibrecException;
import net.librec.math.structure.DenseMatrix;
import net.librec.math.structure.DenseVector;
import net.librec.math.structure.SparseVector;
import net.librec.math.structure.VectorEntry;
import net.librec.recommender.MatrixFactorizationRecommender;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Takacs and Tikk,
 * <strong>
 * Alternating Least Squares for Personalized Ranking
 * </strong>, RecSys 2012.
 *
 * @author guoguibing and Keqiang Wang
 */
@ModelData({"isRanking", "rankals", "userFactors", "itemFactors", "trainMatrix"})
public class RankALSRecommender extends MatrixFactorizationRecommender {
    // whether support based weighting is used ($s_i=|U_i|$) or not ($s_i=1$)
    private boolean isSupportWeight;

    private DenseVector supportVector;

    private double sumSupport;

    @Override
    protected void setup() throws LibrecException {
        super.setup();

        isSupportWeight =conf.getBoolean("rec.rankals.support.weight", true);

        supportVector = new DenseVector(numItems);
        sumSupport = 0;
        for (int itemIdx = 0; itemIdx < numItems; itemIdx++) {
            double supportValue = isSupportWeight ? trainMatrix.columnSize(itemIdx) : 1;
            supportVector.set(itemIdx, supportValue);
            sumSupport += supportValue;
        }
    }

    @Override
    protected void trainModel() throws LibrecException {
        for (int iter = 1; iter < numIterations; iter++) {

            // P step: update user vectors
            DenseVector sum_sq = new DenseVector(numFactors);
            DenseMatrix sum_sqq = new DenseMatrix(numFactors, numFactors);

            for (int j = 0; j < numItems; j++) {
                DenseVector qj = itemFactors.row(j);
                double sj = supportVector.get(j);

                sum_sq = sum_sq.add(qj.scale(sj));
                sum_sqq = sum_sqq.add(qj.outer(qj).scale(sj));
            }

            List<Integer> cus = trainMatrix.rows(); // list of users with$c_ui=1$
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
                    DenseVector qi = itemFactors.row(i);

                    sum_cqq = sum_cqq.add(qi.outer(qi));
                    sum_cq = sum_cq.add(qi);
                    sum_cqr = sum_cqr.add(qi.scale(rui));

                    // ratings of unrated items will be 0
                    double si = supportVector.get(i);
                    sum_sr += si * rui;
                    sum_cr += rui;
                    sum_sqr = sum_sqr.add(qi.scale(si * rui));
                }

                DenseMatrix M = sum_cqq.scale(sumSupport).minus(sum_cq.outer(sum_sq)).minus(sum_sq.outer(sum_cq))
                        .add(sum_sqq.scale(sum_c));

                DenseVector y = sum_cqr.scale(sumSupport).minus(sum_cq.scale(sum_sr)).minus(sum_sq.scale(sum_cr))
                        .add(sum_sqr.scale(sum_c));

                DenseVector pu = M.inv().mult(y);
                userFactors.setRow(u, pu);
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
                    double sj = supportVector.get(j);

                    sum_sr += sj * ruj;
                    sum_cr += ruj;
                    sum_cq = sum_cq.add(itemFactors.row(j));
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

                double si = supportVector.get(i);

                for (int u : cus) {
                    DenseVector pu = userFactors.row(u);
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

                DenseMatrix M = sum_cpp.scale(sumSupport).add(sum_p_p_c.scale(si));
                DenseVector y = sum_cpp.mult(sum_sq).add(sum_cpr.scale(sumSupport)).minus(sum_c_sr_p)
                        .add(sum_p_p_cq.scale(si)).minus(sum_cr_p.scale(si)).add(sum_p_r_c.scale(si));
                DenseVector qi = M.inv().mult(y);
                itemFactors.setRow(i, qi);
            }
        }
    }
}
