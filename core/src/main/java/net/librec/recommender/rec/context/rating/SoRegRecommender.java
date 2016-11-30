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
package net.librec.recommender.rec.context.rating;

import static net.librec.math.algorithm.Maths.g;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import net.librec.annotation.ModelData;
import net.librec.common.LibrecException;
import net.librec.math.algorithm.Sims;
import net.librec.math.structure.DenseMatrix;
import net.librec.math.structure.MatrixEntry;
import net.librec.math.structure.SparseVector;
import net.librec.recommender.item.RecommendedList;
import net.librec.recommender.rec.SocialRecommender;

/**
 * Hao Ma, Dengyong Zhou, Chao Liu, Michael R. Lyu and Irwin King, <strong>Recommender systems with social
 * regularization</strong>, WSDM 2011.<br>
 *
 * <p>
 * In the original paper, this method is named as "SR2_pcc". For consistency, we rename it as "SoReg" as used by some
 * other papers such as: Tang et al., <strong>Exploiting Local and Global Social Context for Recommendation</strong>,
 * IJCAI 2013.
 * </p>
 *
 * @author guoguibing and Keqiang Wang
 */
@ModelData({"isRating", "soreg", "userFactors", "itemFactors"})
public class SoRegRecommender extends SocialRecommender {
    protected double learnRate;
    private Table<Integer, Integer, Double> userCorrs;
    private float beta;

    @Override
    public void setup() throws LibrecException {
        super.setup();
        learnRate = conf.getDouble("rec.iteration.learnrate", 0.01);
        userCorrs = HashBasedTable.create();
        beta = conf.getFloat("-beta");
    }

    @Override
    protected void trainModel() throws LibrecException {
        for (int iter = 1; iter <= numIterations; iter++) {
            // temp data
            DenseMatrix tempUserFactors = new DenseMatrix(numUsers, numFactors);
            DenseMatrix tempItemFactors = new DenseMatrix(numItems, numFactors);

            // ratings
            for (MatrixEntry matrixEntry : trainMatrix) {
                int userIdx = matrixEntry.row();
                int itemIdx = matrixEntry.column();
                double rating = matrixEntry.get();

                double predictRating = predict(userIdx, itemIdx);
                double error = predictRating - rating;


                for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                    tempUserFactors.add(userIdx, factorIdx, error * itemFactors.get(itemIdx, factorIdx) + regUser * userFactors.get(userIdx, factorIdx));
                    tempItemFactors.add(itemIdx, factorIdx, error * userFactors.get(userIdx, factorIdx) + regItem * itemFactors.get(itemIdx, factorIdx));
                }
            }

            // friends
            for (int userIdx = 0; userIdx < numUsers; userIdx++) {
                // out links: F+
                SparseVector userOutLinks = socialMatrix.row(userIdx);

                for (int userOutIdx : userOutLinks.getIndex()) {
                    double userOutSim = similarity(userIdx, userOutIdx);
                    if (!Double.isNaN(userOutSim)) {
                        for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                            double errorOut = userFactors.get(userIdx, factorIdx) - userFactors.get(userOutIdx, factorIdx);
                            tempUserFactors.add(userIdx, factorIdx, beta * userOutSim * errorOut);

                        }
                    }
                }

                // in links: F-
                SparseVector userInLinks = socialMatrix.column(userIdx);
                for (int userInIdx : userInLinks.getIndex()) {
                    double userInSim = similarity(userIdx, userInIdx);
                    if (!Double.isNaN(userInSim)) {
                        for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                            double errorIn = userFactors.get(userIdx, factorIdx) - userFactors.get(userInIdx, factorIdx);
                            tempUserFactors.add(userIdx, factorIdx, beta * userInSim * errorIn);
                        }
                    }
                }

            } // end of for loop
            userFactors = userFactors.add(tempUserFactors.scale(-learnRate));
            itemFactors = itemFactors.add(tempItemFactors.scale(-learnRate));

        }
    }



    /**
     * compute similarity between users 1 and 2
     */
    protected double similarity(Integer userIdx1, Integer userIdx2) {
        if (userCorrs.contains(userIdx1, userIdx2))
            return userCorrs.get(userIdx1, userIdx2);

        if (userCorrs.contains(userIdx2, userIdx1))
            return userCorrs.get(userIdx2, userIdx1);

        double sim = Double.NaN;

        if (userIdx1 < trainMatrix.numRows() && userIdx2 < trainMatrix.numRows()) {
            SparseVector uv = trainMatrix.row(userIdx1);
            if (uv.getCount() > 0) {
                SparseVector vv = trainMatrix.row(userIdx2);
                sim = correlation(uv, vv, "pcc"); // could change to other measures

                if (!Double.isNaN(sim))
                    sim = (1.0 + sim) / 2;
            }
        }

        userCorrs.put(userIdx1, userIdx2, sim);

        return sim;
    }

    /**
     * Compute the correlation between two vectors for a specific method
     *
     * @param vector1 vector 1
     * @param vector2 vector 2
     * @param method  similarity method
     * @return the correlation between vectors 1 and 2; return NaN if the correlation is not computable.
     */
    protected double correlation(SparseVector vector1, SparseVector vector2, String method) {

        // compute similarity
        List<Double> vectorList1 = new ArrayList<>();
        List<Double> vectorList2 = new ArrayList<>();

        for (Integer idx : vector2.getIndex()) {
            if (vector1.contains(idx)) {
                vectorList1.add(vector1.get(idx));
                vectorList2.add(vector2.get(idx));
            }
        }

        double sim;
        switch (method.toLowerCase()) {
            case "cos":
                // for ratings along the overlappings
                sim = Sims.cos(vectorList1, vectorList2);
                break;
            case "cos-binary":
                // for ratings along all the vectors (including one-sided 0s)
                sim = vector1.inner(vector2) / (Math.sqrt(vector1.inner(vector1)) * Math.sqrt(vector2.inner(vector2)));
                break;
            case "msd":
                sim = Sims.msd(vectorList1, vectorList2);
                break;
            case "cpc":
                sim = Sims.cpc(vectorList1, vectorList2, (minRate + maxRate) / 2.0);
                break;
            case "exjaccard":
                sim = Sims.exJaccard(vectorList1, vectorList2);
                break;
            case "pcc":
            default:
                sim = Sims.pcc(vectorList1, vectorList2);
                break;
        }

        // shrink to account for vector size
        if (!Double.isNaN(sim)) {
            int n = vectorList1.size();
            int shrinkage = conf.getInt("num.shrinkage");
            if (shrinkage > 0)
                sim *= n / (n + shrinkage + 0.0);
        }

        return sim;
    }
}
