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
  * UserKNNRecommender
  *
  * @param knn
  * @param isRanking
  * @param trainData
  * @param similarityData RDD[(user, (simiUser, score))]
  *
  * @author WangYuFeng and Zhang Chaoli
  */
class UserKNN(val knn: Int,
              val isRanking: Boolean,
              val trainData: RDD[Rating],
              val similarityData: RDD[(Int, (Int, Double))]
             ) extends Recommender[Array[(Int, Int)]] {

  /**
    * Global mean of ratings
    */
  var globalMean = 0.0
  /**
    * Mean of ratings each user
    */
  var userMeans = mutable.Map.empty[Int, Double]
  /**
    * All training sample denoted by Vectors
    */
  var trainVectors: RDD[IndexedVector] = _

  /**
    * @see net.librec.spark.recommender.Recommender#train()
    */
  override def train(): Unit = {
    trainVectors = StatisticalFunctions.toIndexedSparseVectors(trainData)
    globalMean = trainVectors.mean()
    userMeans = trainVectors.indexedMeans()
  }

  /**
    * @see net.librec.spark.recommender.Recommender#predict(T)
    */
  override def predict(userItems: Array[(Int, Int)]): RDD[Rating] = {
    val sc = similarityData.sparkContext
    val predUsersBC = sc.broadcast(userItems.map(_._1).toSet)
    val predItemsBC = sc.broadcast(userItems.map(_._2).toSet)

    // get all <predUser, simiUser> pairs of predUsers
    val predUserSimiUserPairs = similarityData.filter { similarity =>
      val predUsers = predUsersBC.value
      if (predUsers.contains(similarity._1)) true else false
    }.map { case (predUser, (simiUser, score)) => (simiUser, (predUser, score)) }

    // get all <predItem, ratingUser> pairs of predItems
    val predItemRatingUserPairs = trainData.filter { rating =>
      val predItems = predItemsBC.value
      if (predItems.contains(rating.item)) true else false
    }.map(rat => (rat.user, (rat.item, rat.rate)))

    if (isRanking) {
      predUserSimiUserPairs.join(predItemRatingUserPairs)
        .map { case (simiRatingUser, ((predUser, score), (predItem, rate))) =>
          (predUser + "_" + predItem, score)
        }.aggregateByKey(ArrayBuffer[Double]())(_ += _, _ ++ _)
        .map { case (predUserItem, scores) =>
          val predUserItemArr = predUserItem.split("_", 2)
          Rating(predUserItemArr(0).toInt, predUserItemArr(1).toInt, scores.sortWith(_ < _).take(knn).sum)
        }
    } else {
      val userMeansBC = sc.broadcast(userMeans.toMap)
      predUserSimiUserPairs.join(predItemRatingUserPairs)
        .mapPartitions { iter =>
          val userMeans = userMeansBC.value
          iter.map { case (simiRatingUser, ((predUser, score), (predItem, rate))) =>
            val mean = userMeans.getOrElse(simiRatingUser, 0.0)
            (predUser + "_" + predItem, (rate, score, mean))
          }
        }.aggregateByKey(ArrayBuffer[(Double, Double, Double)]())(_ += _, _ ++ _)
        .mapPartitions { iter =>
          val userMeans = userMeansBC.value
          iter.map { case (predUserItem, rateScores) =>
            val predUserItemArr = predUserItem.split("_", 2)
            val (predUser, predItem) = (predUserItemArr(0).toInt, predUserItemArr(1).toInt)
            var sum, ws = 0.0
            rateScores.sortWith((rateScore1, rateScore2) => rateScore1._2.compareTo(rateScore2._2) > 0).take(knn).foreach { case (rate, score, mean) =>
              sum += score * (rate - mean)
              ws += Math.abs(score)
            }
            val predRate = if (ws > 0) userMeans.getOrElse(predUser, 0.0) + sum / ws else globalMean
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