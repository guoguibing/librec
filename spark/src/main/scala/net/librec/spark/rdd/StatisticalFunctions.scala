package net.librec.spark.rdd

import net.librec.spark.Logging
import net.librec.spark.data.Rating
import net.librec.spark.math.structure
import net.librec.spark.math.structure.{IndexedSparseVector, IndexedVector, SparseVector}
import org.apache.spark.rdd.RDD

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * Some statistical functions of RDD[IndexedVector]
  *
  * @param rdd
  */
class StatisticalFunctions(rdd: RDD[IndexedVector]) extends Logging {

  def this(ratings: RDD[Rating])(implicit d: DummyImplicit) = this(StatisticalFunctions.toIndexedSparseVectors(ratings))

  /**
    * Sample mean vector.
    */
  def sum(): Double = rdd.aggregate(0.0)(
    seqOp = (merge: Double, value: structure.Vector) => merge + (value match {
      case vector: IndexedSparseVector => vector.values.sum
      case _ => value.toArray.sum
    }),
    combOp = (comb1: Double, comb2: Double) => comb1 + comb2
  )

  /**
    * Sample mean vector.
    */
  def mean(): Double = {
    val (sum, count) = rdd.aggregate((0.0, 0))(
      seqOp = (merge: (Double, Int), value: structure.Vector) => {
        value match {
          case vector: IndexedSparseVector =>
            val values = vector.values
            (merge._1 + values.sum, merge._2 + values.length)
          case _ =>
            val values = value.toArray
            (merge._1 + values.sum, merge._2 + values.length)
        }
      },
      combOp = (comb1: (Double, Int), comb2: (Double, Int)) => (comb1._1 + comb2._1, comb1._2 + comb2._2)
    )
    sum / count
  }

  /**
    * Sample mean vector.
    */
  def indexedMeans(): mutable.Map[Int, Double] = rdd.aggregate(mutable.Map.empty[Int, Double])(
    seqOp = (merge: mutable.Map[Int, Double], value: IndexedVector) => merge += (value.getIndex ->
      (value match {
        case vector: IndexedSparseVector =>
          val values = vector.values
          if (values.length > 0) values.sum / values.length else 0.0
        case _ =>
          val values = value.toArray
          if (values.length > 0) values.sum / values.length else 0.0
      })),
    combOp = (comb1: mutable.Map[Int, Double], comb2: mutable.Map[Int, Double]) => comb1 ++ comb2
  )

}

object StatisticalFunctions {
  implicit def addStatisticalFunctions(rdd: RDD[IndexedVector]): StatisticalFunctions = new StatisticalFunctions(rdd)

  /**
    * Convert RDD[Rating] to RDD[IndexedVector] according to ratingType.
    *
    * @param ratings
    * @param ratingType
    * @return
    */
  def toIndexedSparseVectors(ratings: RDD[Rating], ratingType: String = "user"): RDD[IndexedVector] = {
    val vectorSize = if (ratingType == "user") {
      ratings.max()(Ordering.by(_.item)).item + 1
    } else {
      ratings.max()(Ordering.by(_.user)).user + 1
    }
    val ratingMap = if (ratingType == "user") {
      ratings.map(rat => (rat.user, (rat.item, rat.rate)))
    } else {
      ratings.map(rat => (rat.item, (rat.user, rat.rate)))
    }
    ratingMap.groupByKey().mapPartitions( { iter =>
      val indexedVectors = ArrayBuffer[IndexedSparseVector]()
      while (iter.hasNext) {
        val keyRatings = iter.next()
        val vectors = keyRatings._2.filter(_._2 > 0.0).toList.sortBy(_._1).unzip
        indexedVectors.+=(IndexedSparseVector(keyRatings._1, vectorSize, vectors._1.toArray, vectors._2.toArray))
      }
      indexedVectors.iterator
    }, true)
  }

  /**
    * Convert RDD[Rating] to RDD[(Int, SparseVector)] according to rating type.
    *
    * @param ratings
    * @param ratingType
    * @return
    */
  def toSparseVectors(ratings: RDD[Rating], ratingType: String = "user"): RDD[(Int, SparseVector)] = {
    val vectorSize = if (ratingType == "user") {
      ratings.max()(Ordering.by(_.user)).user + 1
    } else {
      ratings.max()(Ordering.by(_.item)).item + 1
    }
    val ratingMap = if (ratingType == "user") {
      ratings.map(rat => (rat.user, (rat.item, rat.rate)))
    } else {
      ratings.map(rat => (rat.item, (rat.user, rat.rate)))
    }
    ratingMap.groupByKey().mapPartitions( { iter =>
      val sparseVectors = ArrayBuffer[(Int, SparseVector)]()
      while (iter.hasNext) {
        val keyRatings = iter.next()
        val vectors = keyRatings._2.filter(_._2 > 0.0).toList.sortBy(_._1).unzip
        sparseVectors.+=((keyRatings._1, new SparseVector(vectorSize, vectors._1.toArray, vectors._2.toArray)))
      }
      sparseVectors.iterator
    }, true)
  }

}