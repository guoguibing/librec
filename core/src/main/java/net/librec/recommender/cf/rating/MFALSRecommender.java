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
package net.librec.recommender.cf.rating;

import net.librec.annotation.ModelData;
import net.librec.common.LibrecException;
import net.librec.math.structure.DenseMatrix;
import net.librec.math.structure.DenseVector;
import net.librec.math.structure.DiagMatrix;
import net.librec.recommender.MatrixFactorizationRecommender;

/**
 * The class implementing the Alternating Least Squares algorithm
 * <p>
 * The origin paper: Yunhong Zhou, Dennis Wilkinson, Robert Schreiber and Rong
 * Pan. Large-Scale Parallel Collaborative Filtering for the Netflix Prize.
 * Proceedings of the 4th international conference on Algorithmic Aspects in
 * Information and Management. Shanghai, China pp. 337-348, 2008.
 * http://www.hpl.hp.com/personal/Robert_Schreiber/papers/2008%20AAIM%20Netflix/
 * netflix_aaim08(submitted).pdf
 *
 * @author wubin (Email: wubin@gs.zzu.edu.cn)
 */
@ModelData({"isRating", "biasedMF", "userFactors", "itemFactors"})
public class MFALSRecommender extends MatrixFactorizationRecommender {
    @Override
    protected void trainModel() throws LibrecException {
        DiagMatrix identify = DiagMatrix.eye(numFactors);
        for (int iter = 1; iter <= numIterations; iter++) {
            // fix item matrix M, solve user matrix U
            for (int userIdx = 0; userIdx < userFactors.numRows(); userIdx++) {
                // number of items rated by user userIdx
                int numitems = trainMatrix.rowSize(userIdx);
                DenseMatrix M = new DenseMatrix(numitems, numFactors);
                int index = 0;
                for (int itemIdx = 0; itemIdx < itemFactors.numRows(); itemIdx++) {
                    if (trainMatrix.get(userIdx, itemIdx) > 0) {
                        M.setRow(index++, itemFactors.row(itemIdx));
                    }
                }

                // step 1:
                DenseMatrix A = M.transpose().mult(M).add(identify.scale(regUser).scale(numitems));
                // step 2:
                // ratings of this userIdx
                DenseVector uservector = new DenseVector(numitems);
                int index1 = 0;
                for (int itemIdx = 0; itemIdx < trainMatrix.numColumns(); itemIdx++) {
                    Double realRating = trainMatrix.get(userIdx, itemIdx);
                    if (realRating > 0) {
                        uservector.set(index1++, realRating);
                    }
                }
                // step 3: the updated user matrix wrt user j
                userFactors.setRow(userIdx, A.inv().mult(M.transpose().mult(uservector)));
            }
            // fix user matrix U, solve item matrix M
            for (int itemIdx = 0; itemIdx < itemFactors.numRows(); itemIdx++) {
                // latent factor of users that have rated item itemIdx
                // number of users rate item j
                int numusers = trainMatrix.columnSize(itemIdx);
                DenseMatrix U = new DenseMatrix(numusers, numFactors);
                int index = 0;
                for (int userIdx = 0; userIdx < userFactors.numRows(); userIdx++) {
                    if (trainMatrix.get(userIdx, itemIdx) > 0) {
                        U.setRow(index++, userFactors.row(userIdx));
                    }
                }
                if (U.numRows() == 0)
                    continue;
                // step 1:
                DenseMatrix A = U.transpose().mult(U).add(identify.scale(regItem).scale(numusers));
                // step 2:
                // ratings of this item
                DenseVector itemvector = new DenseVector(numusers);
                int index1 = 0;
                for (int userIdx = 0; userIdx < trainMatrix.numRows(); userIdx++) {
                    Double realRating = trainMatrix.get(userIdx, itemIdx);
                    if (realRating > 0) {
                        itemvector.set(index1++, realRating);
                    }
                }
                // step 3: the updated item matrix wrt item j
                itemFactors.setRow(itemIdx, A.inv().mult(U.transpose().mult(itemvector)));
            }
        }
    }

}
