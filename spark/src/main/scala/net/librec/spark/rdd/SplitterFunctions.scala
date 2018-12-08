package net.librec.spark.rdd

import java.util.Random

import net.librec.math.algorithm.Randoms
import org.apache.spark.rdd.RDD
import org.apache.spark.util.random.BernoulliCellSampler
import net.librec.spark.Logging
import net.librec.spark.data.Rating

import scala.collection.mutable.ArrayBuffer

/**
  * Spliter Functions
  *
  * @param rdd
  */
class SplitterFunctions(rdd: RDD[Rating]) extends Logging {

  private val sc = rdd.sparkContext

  /**
    * Split the data by ratio.
    *
    * @param weights
    * @param splitterType
    * @param seed
    * @return
    */
  def splitByRatio(weights: Array[Double], splitterType: String, seed: Long = new Random().nextLong()): Array[RDD[Rating]] = {
    require(weights.forall(_ >= 0),
      s"Weights must be nonnegative, but got ${weights.mkString("[", ",", "]")}")
    require(weights.sum > 0,
      s"Sum of weights must be positive, but got ${weights.mkString("[", ",", "]")}")
    require(weights.length != 2 || weights.length != 3,
      s"Length of weights must be 2 or 3, but got ${weights.length}")
    val sum = weights.sum
    val normalizedCumWeights = weights.map(_ / sum).scanLeft(0.0d)(_ + _)
    splitterType match {
      case "rating" =>
        normalizedCumWeights.sliding(2).map { x =>
          rdd.mapPartitionsWithIndex({ (index, partition) =>
            val sampler = new BernoulliCellSampler[Rating](x(0), x(1))
            sampler.setSeed(seed + index)
            sampler.sample(partition)
          }, preservesPartitioning = true)
        }.toArray
      case "user" =>
        val userRDD = rdd.groupBy(rat => rat.user)
        normalizedCumWeights.sliding(2).map { x =>
          userRDD.mapPartitionsWithIndex({ (index, p) =>
            val sampler = new BernoulliCellSampler[Rating](x(0), x(1))
            sampler.setSeed(seed + index)
            p.flatMap(ratings => sampler.sample(ratings._2.iterator))
          }, preservesPartitioning = true)
        }.toArray
      case "userfixed" =>
        val userRDD = rdd.groupBy(rat => rat.user)
        normalizedCumWeights.sliding(2).map { x =>
          userRDD.mapPartitionsWithIndex({ (index, p) =>
            val sampler = new UserFixedSampler[Rating](x(0), x(1))
            sampler.setSeed(seed + index)
            p.flatMap(ratings => sampler.sample(ratings._2.iterator))
          }, preservesPartitioning = true)
        }.toArray
    }
  }

  /**
    * Split the data by LOO.
    *
    * @param splitterType
    * @param seed
    * @return
    */
  def splitByLOO(splitterType: String, seed: Long = new Random().nextLong()): Array[RDD[Rating]] = {
    (splitterType match {
      case "user" =>
        rdd.groupBy(rat => rat.user)
      case "item" =>
        rdd.groupBy(rat => rat.item)
    }).map { case (_, rats) =>
      Randoms.seed(seed)
      val ratings = rats.toList
      val randId = (ratings.length * Randoms.uniform).toInt
      val looRating = ratings(randId)
      (ratings.dropWhile { x => x.equals(looRating) }, looRating)
    }.aggregate((ArrayBuffer[Rating](), ArrayBuffer[Rating]()))(
      seqOp = (merge: (ArrayBuffer[Rating], ArrayBuffer[Rating]), value: (Iterable[Rating], Rating)) => (merge._1.++(value._1), merge._2.+:(value._2)),
      combOp = (comb1: (ArrayBuffer[Rating], ArrayBuffer[Rating]), comb2: (ArrayBuffer[Rating], ArrayBuffer[Rating])) => (comb1._1.++(comb2._1), comb1._2.++(comb2._2))
    ).productIterator.map(iter => sc.parallelize(iter.asInstanceOf[Iterable[Rating]].toSeq)).toArray
  }

  /**
    * Split the data by GivenN.
    *
    * @param numGiven
    * @param splitterType
    * @param seed
    * @return
    */
  def splitByGivenN(numGiven: Int, splitterType: String, seed: Long = new Random().nextLong()): Array[RDD[Rating]] = {
    (splitterType match {
      case "user" =>
        rdd.groupBy(rat => rat.user)
      case "item" =>
        rdd.groupBy(rat => rat.item)
    }).map { case (_, rats) =>
      Randoms.seed(seed)
      val ratings = rats.toList
      val givenIdxs = Randoms.nextIntArray(numGiven, ratings.length)
      val givenNIter = givenIdxs.map(idx => ratings(idx))
      (ratings.diff(givenNIter), givenNIter.toIterable)
    }.aggregate((ArrayBuffer[Rating](), ArrayBuffer[Rating]()))(
      seqOp = (merge: (ArrayBuffer[Rating], ArrayBuffer[Rating]), value: (Iterable[Rating], Iterable[Rating])) => (merge._1.++(value._1), merge._2.++(value._2)),
      combOp = (comb1: (ArrayBuffer[Rating], ArrayBuffer[Rating]), comb2: (ArrayBuffer[Rating], ArrayBuffer[Rating])) => (comb1._1.++(comb2._1), comb1._2.++(comb2._2))
    ).productIterator.map(iter => sc.parallelize(iter.asInstanceOf[Iterable[Rating]].toSeq)).toArray
  }
}

class UserFixedSampler[T](lb: Double, ub: Double) extends BernoulliCellSampler[T](lb, ub) {
  private val rng: Random = new Random()
  /**
    * Number of certain ratio
    */
  private var count = 0

  /**
    * Sample items
    *
    * @param items item list
    * @return filtered item list
    */
  override def sample(items: Iterator[T]): Iterator[T] = {
    val itemList = items.toList
    val k: Int = Math.floor(itemList.length * (ub - lb)).toInt
    itemList.filter(_ => sample() > 0 && count <= k).iterator
  }

  /**
    * Sample by random
    *
    * @return sample state
    */
  override def sample(): Int = {
    if (ub - lb > 0.0) {
      val x = rng.nextDouble()
      if ((x >= lb) && (x < ub)) {
        count += 1
        1
      } else 0
    } else 0
  }
}

object SplitterFunctions {
  implicit def addSplitterFunctions(rdd: RDD[Rating]): SplitterFunctions = new SplitterFunctions(rdd)
}