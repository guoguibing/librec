package net.librec.spark

import net.librec.recommender.item.RecommendedList
import net.librec.spark.data.DataConverter
import net.librec.spark.rdd.SimilarityFunctions._
import net.librec.spark.rdd.SplitterFunctions._
import net.librec.spark.rdd.StatisticalFunctions
import net.librec.spark.recommender.cf.UserKNN

/**
  * Librec Context Test Case
  *
  * @author WangYuFeng
  */
object SparkTest {

  def main(args: Array[String]): Unit = {
    val inPath = args(0)
    val dep = args(1)
    val conf = new LibrecConf().setAppName("librec application")
    conf.setInt("rec.similarity.shrinkage", 10)
    val lc = new LibrecContext(conf)
    val data = new DataConverter(lc).convertText(inPath)
    val algoData = data.splitByRatio(Array(0.8, 0.2), "rating", seed = 1)
    val similarity = StatisticalFunctions.toIndexedSparseVectors(data).computeSimilarity(Correlation.BCOS, conf)
    val userKNN = new UserKNN(knn = 200, isRanking = false, trainData = algoData(0), similarityData = similarity)
    userKNN.train()
    val predictArr = algoData(1).map(rats => (rats.user, rats.item)).collect()
    val predictResult = userKNN.predict(predictArr).collect()
    val groundTruthList = new RecommendedList(predictResult.length, false)
    algoData(1).collect().foreach(rat => groundTruthList.addIndependently(rat.user, rat.item, rat.rate))
    val recommendedList = new RecommendedList(predictResult.length)
    predictResult.foreach(rat => recommendedList.addIndependently(rat.user, rat.item, rat.rate))
    println("---Evaluator MSE: " + Evaluator.eval(Measure.MSE, groundTruthList, recommendedList, conf))
    println("---Evaluator MAE: " + Evaluator.eval(Measure.MAE, groundTruthList, recommendedList, conf))
    lc.stop()
  }
}
