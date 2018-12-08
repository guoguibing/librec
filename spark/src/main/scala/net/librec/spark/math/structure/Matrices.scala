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
package net.librec.spark.math.structure


/**
  * Trait for a local matrix.
  *
  * @author WangYuFeng
  */
sealed trait Matrix extends Serializable {

  /** Number of rows. */
  def numRows: Int

  /** Number of columns. */
  def numCols: Int

  /** Flag that keeps track whether the matrix is transposed or not. False by default. */
  val isTransposed: Boolean = false

  /**
    * Transpose the Matrix. Returns a new `Matrix` instance sharing the same underlying data.
    */
  def transpose: Matrix

  /**
    * Returns an iterator of column vectors.
    * This operation could be expensive, depending on the underlying storage.
    */
  def colIter: Iterator[Vector]

  /**
    * Returns an iterator of row vectors.
    * This operation could be expensive, depending on the underlying storage.
    */
  def rowIter: Iterator[Vector] = this.transpose.colIter

}

/**
  * DenseMatrix
  *
  * @param numRows
  * @param numCols
  * @param values
  * @param isTransposed
  */
class DenseMatrix(val numRows: Int,
                  val numCols: Int,
                  val values: Array[Double],
                  override val isTransposed: Boolean) extends Matrix {
  /**
    * Transpose the Matrix. Returns a new `Matrix` instance sharing the same underlying data.
    */
  override def transpose: DenseMatrix = new DenseMatrix(numCols, numRows, values, !isTransposed)

  /**
    * Returns an iterator of column vectors.
    * This operation could be expensive, depending on the underlying storage.
    */
  override def colIter: Iterator[Vector] = {
    if (isTransposed) {
      Iterator.tabulate(numCols) { j =>
        val col = new Array[Double](numRows)
        //        blas.dcopy(numRows, values, j, numCols, col, 0, 1)
        new DenseVector(col)
      }
    } else {
      Iterator.tabulate(numCols) { j =>
        new DenseVector(values.slice(j * numRows, (j + 1) * numRows))
      }
    }
  }
}