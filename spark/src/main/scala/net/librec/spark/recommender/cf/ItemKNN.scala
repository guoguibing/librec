package net.librec.spark.recommender.cf

import org.apache.spark.rdd.RDD
import net.librec.spark.data.Rating
import net.librec.spark.math.structure.IndexedVector
import net.librec.spark.rdd.StatisticalFunctions
import net.librec.spark.rdd.StatisticalFunctions._
import net.librec.spark.recommender.Recommender

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * ItemKNNRecommender
  *
  * @param knn
  * @param isRanking
  * @param trainData
  * @param similarityData RDD[(item, (simiItem, score))]
  *
  * @author WangYuFeng and Zhang Chaoli
  */
class ItemKNN(val knn: Int,
              val isRanking: Boolean,
              val trainData: RDD[Rating],
              val similarityData: RDD[(Int, (Int, Double))]
             ) extends Recommender[Array[(Int, Int)]] {
  /**
    * Global mean of ratings
    */
  var globalMean = 0.0
  /**
    * Mean of ratings each item
    */
  var itemMeans = mutable.Map.empty[Int, Double]
  /**
    * All training sample denoted by Vectors
    */
  var trainVectors: RDD[IndexedVector] = _

  /**
    * @see net.librec.spark.recommender.Recommender#train()
    */
  override def train(): Unit = {
    trainVectors = StatisticalFunctions.toIndexedSparseVectors(trainData, "item")
    globalMean = trainVectors.mean()
    itemMeans = trainVectors.indexedMeans()
  }

  /**
    * @see net.librec.spark.recommender.Recommender#predict(T)
    */
  override def predict(userItems: Array[(Int, Int)]): RDD[Rating] = {
    val sc = similarityData.sparkContext
    val predUsersBC = sc.broadcast(userItems.map(_._1).toSet)
    val predItemsBC = sc.broadcast(userItems.map(_._2).toSet)

    // get all <predItem, simiItem> pairs of predItems
    val predItemSimiItemPairs = similarityData.filter { similarity =>
      val predItems = predItemsBC.value
      if (predItems.contains(similarity._1)) {
        if (similarity._2._2 > 0) true else false
      } else false
    }.map { case (predItem, (simiItem, score)) => (simiItem, (predItem, score)) }

    // get all <predUser, ratedItem> pairs of predUsers
    val predUserRatedItemPairs = trainData.filter { rating =>
      val predUsers = predUsersBC.value
      if (predUsers.contains(rating.user)) true else false
    }.map(rat => (rat.item, (rat.user, rat.rate)))

    if (isRanking) {
      predItemSimiItemPairs.join(predUserRatedItemPairs)
        .map { case (simiItem, ((predItem, score), (predUser, rate))) =>
          (predUser + "_" + predItem, score)
        }.aggregateByKey(ArrayBuffer[Double]())(_ += _, _ ++ _)
        .map { case (predUserItem, scores) =>
          val predUserItemArr = predUserItem.split("_", 2)
          Rating(predUserItemArr(0).toInt, predUserItemArr(1).toInt, scores.sortWith(_ < _).take(knn).sum)
        }
    } else {
      val itemMeansBC = sc.broadcast(itemMeans.toMap)
      predItemSimiItemPairs.join(predUserRatedItemPairs)
        .mapPartitions { iter =>
          val itemMeans = itemMeansBC.value
          iter.map { case (simiItem, ((predItem, score), (predUser, rate))) =>
            val mean = itemMeans.getOrElse(simiItem, 0.0)
            (predUser + "_" + predItem, (rate, score, mean))
          }
        }.aggregateByKey(ArrayBuffer[(Double, Double, Double)]())(_ += _, _ ++ _)
        .mapPartitions { iter =>
          val itemMeans = itemMeansBC.value
          iter.map { case (predUserItem, rateScores) =>
            val predUserItemArr = predUserItem.split("_", 2)
            val (predUser, predItem) = (predUserItemArr(0).toInt, predUserItemArr(1).toInt)
            var sum, ws = 0.0
            rateScores.sortWith((rateScore1, rateScore2) => rateScore1._2.compareTo(rateScore2._2) > 0).take(knn).foreach { case (rate, score, mean) =>
              sum += score * (rate - mean)
              ws += Math.abs(score)
            }
            val predRate = if (ws > 0) itemMeans.getOrElse(predItem, 0.0) + sum / ws else globalMean
            Rating(predUser, predItem, predRate)
          }
        }
    }

  }

  /**
    * Predict specific ratings for some users on related items.
    *
    * @param userItems some users and related items
    * @return predictive ratings
    */
  def predict(userItems: RDD[Rating]): RDD[Rating] = {
    val userItemArr = userItems.map(rating => (rating.user, rating.item)).collect()
    predict(userItemArr)
  }

}