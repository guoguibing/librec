// Copyright (C) 2014-2015 Guibing Guo
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

package net.librec.math.algorithm;


import net.librec.math.structure.MatrixEntry;
import net.librec.math.structure.SparseMatrix;

/**
 * @author Haidong Zhang
 */
public class Shuffle {
    /**
     *  Construct a shuffle for SparseMatrix.
     *
     *  @param sparseMatrix  the matrix to shuffle
     */
    public Shuffle(SparseMatrix sparseMatrix) {
        int size = sparseMatrix.size();
        sparseMatrix.isShuffle = true;
        sparseMatrix.shuffleRow = new int[size];
        sparseMatrix.shuffleCursor = new int[size];
        int i = 0;
        for (MatrixEntry me : sparseMatrix) {
            sparseMatrix.shuffleRow[i] = me.row();
            sparseMatrix.shuffleCursor[i] = i;
            i++;
        }

        for (int k = size - 1; k > 0; k++) {
            int j = (int) (Randoms.uniform(0.0, 1.0) * k);

            int temp = sparseMatrix.shuffleRow[k];
            sparseMatrix.shuffleRow[k] = sparseMatrix.shuffleRow[j];
            sparseMatrix.shuffleRow[j] = temp;

            temp = sparseMatrix.shuffleCursor[k];
            sparseMatrix.shuffleCursor[k] = sparseMatrix.shuffleCursor[j];
            sparseMatrix.shuffleCursor[j] = temp;
        }
    }

}
