package net.librec.spark.math.structure.distributed

import net.librec.math.structure.AbstractMatrix
import org.apache.spark.mllib.linalg.distributed.IndexedRow
import org.apache.spark.rdd.RDD
import net.librec.spark.math.structure.IndexedVector

/**
  * IndexedMatrix
  *
  * @author WangYuFeng
  */
class IndexedMatrix(val vectors: RDD[IndexedVector],
                    private var nRows: Long,
                    private var nCols: Int) extends DistributedMatrix {

  /** Alternative constructor leaving matrix dimensions to be determined automatically. */
  def this(vectors: RDD[IndexedVector]) = this(vectors, 0L, 0)

  /**
    * @see net.librec.spark.math.structure.distributed#numCols()
    */
  override def numCols(): Long = {
    if (nCols <= 0) {
      // Calling `first` will throw an exception if `rows` is empty.
      nCols = vectors.first().size
    }
    nCols
  }

  /**
    * @see net.librec.spark.math.structure.distributed#numRows()
    */
  override def numRows(): Long = {
    if (nRows <= 0L) {
      // Reduce will throw an exception if `vectors` is empty.
      nRows = vectors.map(_.getIndex).reduce(math.max) + 1L
    }
    nRows
  }

  /**
    * @see net.librec.spark.math.structure.distributed#toLocalMatrix
    */
  override def toLocalMatrix: AbstractMatrix = ???
}
