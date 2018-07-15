package net.librec.spark.recommender

import net.librec.spark.data.Rating
import org.apache.spark.rdd.RDD

/**
  * Recommender
  *
  * @author WangYuFeng
  */
trait Recommender[T] extends Serializable {
  /**
    * Train recommender model
    */
  def train(): Unit

  /**
    * Predict specific ratings for some users on related items.
    *
    * @param userItems some users and related items
    * @return predictive ratings
    */
  def predict(userItems: T): RDD[Rating]
}
