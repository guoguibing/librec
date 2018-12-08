package net.librec.spark.math.structure

import net.librec.math.structure
import net.librec.spark.data.Rating

/**
  * IndexedVector
  *
  * @param index
  * @param vector
  *
  * @author WangYuFeng
  */
abstract case class IndexedVector(index: Int, vector: Vector) extends Vector with Serializable {

  /**
    * get index
    * @return
    */
  def getIndex: Int = index

  /**
    * get values
    * @return
    */
  def values: Array[Double] = vector.values

  override def toString: String = {
    "Index: " + getIndex + " " + super.toString
  }
}

/**
  * IndexedDenseVector
  *
  * @param index
  * @param vector
  */
class IndexedDenseVector(index: Int, vector: DenseVector) extends IndexedVector(index, vector) {
  /**
    * get ID
    *
    * @return userID or itemID
    */
  override def getIndex: Int = index

  /**
    * Size of the vector.
    */
  override def size: Int = vector.size

  /**
    * Converts the instance to a double array.
    */
  override def toArray: Array[Double] = vector.toArray

  /**
    * Convert this vector to the new mllib-local representation.
    */
  override def asLocalVector: structure.Vector = vector.asLocalVector

  /**
    * @see net.librec.spark.math.structure#foreachActive((Int, Double) => Unit)
    */
  override def foreachActive(f: (Int, Double) => Unit): Unit = vector.foreachActive(f: (Int, Double) => Unit)
}

/**
  * IndexedSparseVector
  *
  * @param index
  * @param vector
  */
class IndexedSparseVector(index: Int, vector: SparseVector) extends IndexedVector(index, vector) {

  def this(userOrItem: String, ratingsToConvert: Iterable[Rating], size: Int) = {
    this(IndexedSparseVector.getIndexFromRatings(userOrItem, ratingsToConvert), IndexedSparseVector.parseToSparseVector(ratingsToConvert, size))
  }

  /**
    * get ID
    *
    * @return userID or itemID
    */
  override def getIndex: Int = index

  /**
    * Size of the vector.
    */
  override def size: Int = vector.size

  /**
    * Converts the instance to a double array.
    */
  override def toArray: Array[Double] = vector.toArray

  /**
    * Convert this vector to the new mllib-local representation.
    */
  override def asLocalVector: structure.Vector = vector.asLocalVector

  /**
    * @see net.librec.spark.math.structure#foreachActive((Int, Double) => Unit)
    */
  override def foreachActive(f: (Int, Double) => Unit): Unit = vector.foreachActive(f: (Int, Double) => Unit)
}


object IndexedSparseVector {
  /**
    * Parse ratings to SparseVector.
    *
    * @param ratings
    * @param size
    * @return
    */
  def parseToSparseVector(ratings: Iterable[Rating], size: Int): SparseVector = {
    val sorted = ratings.map(rat => (rat.item, rat.rate)).toList.sortBy(_._1)
    val products = sorted.map(rat => rat._1).toArray
    val rats = sorted.map(rat => rat._2).toArray
    new SparseVector(size, products, rats)
  }

  /**
    * Get user's or item's ID from ratings. Check if all ratings are from the same user.
    *
    * @param userOrItem       user or item
    * @param ratingsToConvert convert to ratings
    * @return id
    */
  protected def getIndexFromRatings(userOrItem: String, ratingsToConvert: Iterable[Rating]): Int = {
    if (ratingsToConvert.nonEmpty) {
      if (userOrItem.equals("user")) ratingsToConvert.head.user else if (userOrItem.equals("item")) ratingsToConvert.head.user else 0
    }
    else 0
  }

  def apply(index: Int, size: Int, indices: Array[Int], ratings: Array[Double]): IndexedSparseVector = new IndexedSparseVector(index, new SparseVector(size, indices, ratings))

}