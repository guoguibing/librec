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
import net.librec.math.structure.*;
import net.librec.math.structure.Vector.VectorEntry;
import net.librec.recommender.MatrixFactorizationRecommender;

import java.util.ArrayList;
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
    private VectorBasedDenseVector supportVector;

    private double sumSupport;

    @Override
    protected void setup() throws LibrecException {
        super.setup();

        isSupportWeight = conf.getBoolean("rec.rankals.support.weight", true);

        supportVector = new VectorBasedDenseVector(numItems);
        sumSupport = 0;
        for (int itemIdx = 0; itemIdx < numItems; itemIdx++) {
            double supportValue = isSupportWeight ? trainMatrix.column(itemIdx).getNumEntries() : 1;
            supportVector.set(itemIdx, supportValue);
            sumSupport += supportValue;
        }
    }

    @Override
    protected void trainModel() throws LibrecException {
        for (int iter = 1; iter < numIterations; iter++) {

            // P step: update user vectors
            DenseVector sum_sq = new VectorBasedDenseVector(numFactors);
            DenseMatrix sum_sqq = new DenseMatrix(numFactors, numFactors);

            for (int j = 0; j < numItems; j++) {
                DenseVector qj = itemFactors.row(j);
                double sj = supportVector.get(j);
                sum_sq = sum_sq.plus(qj.times(sj));
                sum_sqq = sum_sqq.plus(qj.outer(qj).times(sj));
            }

            List<Integer> cus = nonEmptyRows(trainMatrix); // list of users with$c_ui=1$
            for (int u : cus) {
                // for each user
                DenseMatrix sum_cqq = new DenseMatrix(numFactors, numFactors);
                DenseVector sum_cq = new VectorBasedDenseVector(numFactors);
                DenseVector sum_cqr = new VectorBasedDenseVector(numFactors);
                DenseVector sum_sqr = new VectorBasedDenseVector(numFactors);

                SequentialSparseVector Ru = trainMatrix.row(u);
                double sum_c = Ru.getNumEntries();
                double sum_sr = 0, sum_cr = 0;

                for (VectorEntry ve : Ru) {
                    int i = ve.index();
                    double rui = ve.get();
                    // double cui = 1;
                    DenseVector qi = itemFactors.row(i);

                    sum_cqq = sum_cqq.plus(qi.outer(qi));
                    sum_cq = sum_cq.plus(qi);
                    sum_cqr = sum_cqr.plus(qi.times(rui));

                    // ratings of unrated items will be 0
                    double si = supportVector.get(i);
                    sum_sr += si * rui;
                    sum_cr += rui;
                    sum_sqr = sum_sqr.plus(qi.times(si * rui));
                }

                DenseMatrix M = sum_cqq.times(sumSupport).minus(sum_cq.outer(sum_sq)).minus(sum_sq.outer(sum_cq))
                        .plus(sum_sqq.times(sum_c));

                DenseVector y = sum_cqr.times(sumSupport).minus(sum_cq.times(sum_sr)).minus(sum_sq.times(sum_cr))
                        .plus(sum_sqr.times(sum_c));

                DenseVector pu = M.inverse().times(y);
                userFactors.row(u).assign((index, value) -> {
                    return pu.get(index);
                });
            }

            // Q step: update item vectors
            Map<Integer, Double> m_sum_sr = new HashMap<>();
            Map<Integer, Double> m_sum_cr = new HashMap<>();
            Map<Integer, Double> m_sum_c = new HashMap<>();
            Map<Integer, DenseVector> m_sum_cq = new HashMap<>();

            for (int u : cus) {
                SequentialSparseVector Ru = trainMatrix.row(u);

                double sum_sr = 0, sum_cr = 0, sum_c = Ru.getNumEntries();
                DenseVector sum_cq = new VectorBasedDenseVector(numFactors);

                for (VectorEntry ve : Ru) {
                    int j = ve.index();
                    double ruj = ve.get();
                    double sj = supportVector.get(j);

                    sum_sr += sj * ruj;
                    sum_cr += ruj;
                    sum_cq = sum_cq.plus(itemFactors.row(j));
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
                DenseVector sum_p_p_cq = new VectorBasedDenseVector(numFactors);
                DenseVector sum_cpr = new VectorBasedDenseVector(numFactors);
                DenseVector sum_c_sr_p = new VectorBasedDenseVector(numFactors);
                DenseVector sum_cr_p = new VectorBasedDenseVector(numFactors);
                DenseVector sum_p_r_c = new VectorBasedDenseVector(numFactors);

                double si = supportVector.get(i);
                DenseVector itemVector = new VectorBasedDenseVector(trainMatrix.column(i));

                for (int u : cus) {
                    DenseVector pu = userFactors.row(u);
                    // double rui = trainMatrix.get(u, i);
                    double rui = itemVector.get(u);

                    DenseMatrix pp = pu.outer(pu);
                    sum_cpp = sum_cpp.plus(pp);
                    sum_p_p_cq = sum_p_p_cq.plus(pp.times(m_sum_cq.get(u)));
                    sum_p_p_c = sum_p_p_c.plus(pp.times(m_sum_c.get(u)));
                    sum_cr_p = sum_cr_p.plus(pu.times(m_sum_cr.get(u)));

                    if (rui > 0) {
                        sum_cpr = sum_cpr.plus(pu.times(rui));
                        sum_c_sr_p = sum_c_sr_p.plus(pu.times(m_sum_sr.get(u)));
                        sum_p_r_c = sum_p_r_c.plus(pu.times(rui * m_sum_c.get(u)));
                    }
                }
                DenseMatrix subtract = sum_cpp.times(si + 1);
                DenseMatrix M = sum_cpp.times(sumSupport).plus(sum_p_p_c.times(si)).minus(subtract);
                DenseVector y = sum_cpp.times(sum_sq).plus(sum_cpr.times(sumSupport)).minus(sum_c_sr_p)
                        .plus(sum_p_p_cq.times(si)).minus(sum_cr_p.times(si)).plus(sum_p_r_c.times(si));
                DenseVector qi = M.inverse().times(y.minus(subtract.times(itemFactors.row(i))));
                itemFactors.row(i).assign((index, value) -> {
                    return qi.get(index);
                });
            }
        }
    }

    /**
     * @return a list of rows which have at least one non-empty entry
     */
    public List<Integer> nonEmptyRows(SequentialAccessSparseMatrix matrix) {
        if (matrix == null) {
            LOG.error("The matrix passed in is null.");
            return null;
        }

        List<Integer> list = new ArrayList<>(matrix.rowSize());

        for (int userId = 0; userId < matrix.rowSize(); userId++) {
            if (matrix.row(userId).getNumEntries() > 0) {
                list.add(userId);
            }
        }

        return list;
    }
}
