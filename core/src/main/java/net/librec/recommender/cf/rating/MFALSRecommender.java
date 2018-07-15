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
import net.librec.math.structure.*;
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
        DenseMatrix identify = BuildEyeMatrix(numFactors);
        for (int iter = 1; iter <= numIterations; iter++) {
            // fix item matrix M, solve user matrix U
            for (int userIdx = 0; userIdx < numUsers; userIdx++) {
                // number of items rated by user userIdx
                SequentialSparseVector userRatingVec = trainMatrix.row(userIdx);
                int numItemOfUser = userRatingVec.size();
                DenseMatrix M = new DenseMatrix(numItemOfUser, numFactors);
                DenseVector uservector = new VectorBasedDenseVector(numItemOfUser);
                int index = 0;
                for (Vector.VectorEntry ve : userRatingVec) {
                    int itemIdx = ve.index();
                    double rating = ve.get();
                    M.set(index, itemFactors.row(itemIdx));
                    uservector.set(index, rating);
                    index += 1;
                }

                DenseMatrix A = M.transpose().times(M).plus(identify.times(regUser).times(numItemOfUser));

                userFactors.set(userIdx, A.inverse().times(M.transpose().times(uservector)));
            }
            // fix user matrix U, solve item matrix M
            for (int itemIdx = 0; itemIdx < numItems; itemIdx++) {
                // latent factor of users that have rated item itemIdx
                // number of users rate item j
                SequentialSparseVector itemRatingVec = trainMatrix.column(itemIdx);
                int numusers = itemRatingVec.size();
                DenseMatrix U = new DenseMatrix(numusers, numFactors);
                DenseVector itemvector = new VectorBasedDenseVector(numusers);
                int index = 0;
                for (Vector.VectorEntry ve : itemRatingVec) {
                    int userIdx = ve.index();
                    double rating = ve.get();
                    U.set(index, userFactors.row(userIdx));
                    itemvector.set(index, rating);
                    index += 1;
                }

                DenseMatrix A = U.transpose().times(U).plus(identify.times(regItem).times(numusers));
                itemFactors.set(itemIdx, A.inverse().times(U.transpose().times(itemvector)));
            }
        }
    }

    protected DenseMatrix BuildEyeMatrix(int numDim) throws LibrecException {
        double[][] values = new double[numDim][numDim];
        for (int i=0; i<numDim; i++) {
            values[i][i] = 1.0;
        }
        return new DenseMatrix(values);
    }

}
