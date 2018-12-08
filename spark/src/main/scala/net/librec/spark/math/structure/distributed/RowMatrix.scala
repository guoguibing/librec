package net.librec.spark.math.structure.distributed

import net.librec.math.structure.{AbstractMatrix, DenseMatrix}
import net.librec.spark.math.structure.Vector
import org.apache.spark.rdd.RDD
import net.librec.spark.Logging

/**
  * Represents a row-oriented distributed Matrix with no meaningful row indices.
  *
  * @param rows rows stored as an RDD[Vector]
  * @param nRows number of rows. A non-positive value means unknown, and then the number of rows will
  *              be determined by the number of records in the RDD `rows`.
  * @param nCols number of columns. A non-positive value means unknown, and then the number of
  *              columns will be determined by the size of the first row.
  */
class RowMatrix(val rows: RDD[Vector],
                private var nRows: Long,
                private var nCols: Int) extends DistributedMatrix with Logging {
  /** Gets or computes the number of rows. */
  override def numRows(): Long = {
    if (nRows <= 0L) {
      nRows = rows.count()
      if (nRows == 0L) {
        sys.error("Cannot determine the number of rows because it is not specified in the " +
          "constructor and the rows RDD is empty.")
      }
    }
    nRows
  }

  /** Gets or computes the number of columns. */
  override def numCols(): Long = {
    if (nCols <= 0) {
      try {
        // Calling `first` will throw an exception if `rows` is empty.
        nCols = rows.first().size
      } catch {
        case err: UnsupportedOperationException =>
          sys.error("Cannot determine the number of cols because it is not specified in the " +
            "constructor and the rows RDD is empty.")
      }
    }
    nCols
  }

  /** Collect the distributed matrix on the driver as a `Matrix`. */
  override def toLocalMatrix: AbstractMatrix = {
    val m = numRows().toInt
    val n = numCols().toInt
    val mat = new DenseMatrix(m, n)
    var i = 0
    rows.collect().foreach { vector =>
      vector.foreachActive { case (j, v) =>
        mat.set(i, j, v)
      }
      i += 1
    }
    mat
  }
}
