package net.librec.spark.recommender

import net.librec.recommender.item.RecommendedList
import net.librec.spark.{BaseTestSuite, _}
import net.librec.spark.data.DataConverter
import net.librec.spark.rdd.SimilarityFunctions._
import net.librec.spark.rdd.SplitterFunctions._
import net.librec.spark.rdd.StatisticalFunctions
import net.librec.spark.recommender.cf.ItemKNN

/**
  * ItemKNNTestCase.
  *
  * @author Zhang Chaoli
  */
class ItemKNNTestCase extends BaseTestSuite {

  test("ItemKNNRankingTestCase") {
    val conf = new LibrecConf().setMaster(master).setAppName(appName)
    val lc = new LibrecContext(conf)
    val data = new DataConverter(lc).convertText("/Users/clzhang/Documents/IntelliJIDEA_program/librec/librec_3.0.0_matrix/data/spark/ratings.txt")
    val algoData = data.splitByRatio(Array(0.8, 0.2), "rating", 1000)
    val (trainData, testData) = (algoData(0), algoData(1))

    val similarity = StatisticalFunctions.toIndexedSparseVectors(data, "item").computeSimilarity(Correlation.BCOS, conf)
    val itemKNN = new ItemKNN(knn = 10, isRanking = true, trainData = trainData, similarityData = similarity)
    itemKNN.train()
    val predictArr = testData.map(rats => (rats.user, rats.item)).collect()
    val predictResult = itemKNN.predict(predictArr).collect()
    println("---predictResult: " + predictResult.foreach(println))

    val groundTruthList = new RecommendedList(predictResult.length, true)
    testData.collect().foreach(rat => groundTruthList.addIndependently(rat.user, rat.item, rat.rate))
    val recommendedList = new RecommendedList(predictResult.length)
    predictResult.foreach(rat => recommendedList.addIndependently(rat.user, rat.item, rat.rate))
    conf.setInt("rec.recommender.ranking.topn", 5)
    println("---Evaluator AP: " + Evaluator.eval(Measure.AP, groundTruthList, recommendedList, conf))
    conf.setInt("rec.recommender.ranking.topn", 100)
    println("---Evaluator IDCG: " + Evaluator.eval(Measure.IDCG, groundTruthList, recommendedList, conf))

    lc.stop()
  }

  test("ItemKNNRatingTestCase") {
    val conf = new LibrecConf().setMaster(master).setAppName(appName)
    val lc = new LibrecContext(conf)
    val trainData = new DataConverter(lc).convertText("/Users/clzhang/Documents/IntelliJIDEA_program/librec/librec_2.0.0/data/spark/rating/ratings.txt")

    val itemNum = 10 // TODO: not indexed
    val testData = trainData.map(rats => (rats.user, rats.item.toString))
        .reduceByKey(_ + "," + _)
        .flatMap { case (user, itemsStr) =>
          var items = itemsStr.split(",").map(_.toInt).toSet
          val predItems = (0 until itemNum).toSet -- items
          predItems.map((user, _))
        }

    val similarity = StatisticalFunctions.toIndexedSparseVectors(trainData, "item").computeSimilarity(Correlation.BCOS, conf)
    val itemKNN = new ItemKNN(knn = 10, isRanking = false, trainData = trainData, similarityData = similarity)
    itemKNN.train()
    val predictArr = testData.collect()
    val predictResult = itemKNN.predict(predictArr).collect()
    println("---predictResult: " + predictResult.foreach(println))

//    val groundTruthList = new RecommendedList(predictResult.length, true)
//    testData.collect().foreach(rat => groundTruthList.addIndependently(rat.user, rat.item, rat.rate))
//    val recommendedList = new RecommendedList(predictResult.length)
//    predictResult.foreach(rat => recommendedList.addIndependently(rat.user, rat.item, rat.rate))
//    conf.setInt("rec.recommender.ranking.topn", 5)
//    println("---Evaluator AP: " + Evaluator.eval(Measure.AP, groundTruthList, recommendedList, conf))
//    conf.setInt("rec.recommender.ranking.topn", 100)
//    println("---Evaluator IDCG: " + Evaluator.eval(Measure.IDCG, groundTruthList, recommendedList, conf))

    lc.stop()
  }

}
