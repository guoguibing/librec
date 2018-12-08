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
package net.librec.spark.recommender

import net.librec.recommender.item.RecommendedList
import net.librec.spark.data.DataConverter
import net.librec.spark.rdd.SimilarityFunctions._
import net.librec.spark.rdd.SplitterFunctions._
import net.librec.spark.rdd.StatisticalFunctions
import net.librec.spark.recommender.cf.UserKNN
import net.librec.spark.{BaseTestSuite, _}

import scala.collection.mutable.ArrayBuffer

/**
  * UserKNNJavaTestCase.
  *
  * @author WangYuFeng
  */
class UserKNNTestCase extends BaseTestSuite {

  test("UserKNNRankingTestCase") {
    val conf = new LibrecConf().setMaster(master).setAppName(appName)
    conf.setInt("rec.similarity.shrinkage", 10)
    val lc = new LibrecContext(conf)
    val data = new DataConverter(lc).convertText("E:/workspace/my_workspace/librec/librec/data/spark/ratings.txt")
    val algoData = data.splitByRatio(Array(0.8, 0.2), "rating", seed = 1)
    val similarity = StatisticalFunctions.toIndexedSparseVectors(data).computeSimilarity(Correlation.BCOS, conf)
    val userKNN = new UserKNN(knn = 200, isRanking = true, trainData = algoData(0), similarityData = similarity)
    userKNN.train()
    val predictArr = algoData(1).map(rats => (rats.user, rats.item)).collect()
    val predictResult = userKNN.predict(predictArr).collect()
    val groundTruthList = new RecommendedList(predictResult.length, true)
    algoData(1).collect().foreach(rat => groundTruthList.addIndependently(rat.user, rat.item, rat.rate))
    val recommendedList = new RecommendedList(predictResult.length)
    predictResult.foreach(rat => recommendedList.addIndependently(rat.user, rat.item, rat.rate))
    conf.setInt("rec.recommender.ranking.topn", 5)
    println("---Evaluator AP: " + Evaluator.eval(Measure.AP, groundTruthList, recommendedList, conf))
    conf.setInt("rec.recommender.ranking.topn", 100)
    println("---Evaluator IDCG: " + Evaluator.eval(Measure.IDCG, groundTruthList, recommendedList, conf))
    lc.stop()
  }


  test("UserKNNRatingTestCase") {
    val conf = new LibrecConf().setMaster(master).setAppName(appName)
    conf.setInt("rec.similarity.shrinkage", 10)
    val lc = new LibrecContext(conf)
    val data = new DataConverter(lc).convertText("E:/workspace/my_workspace/librec/librec/data/spark/ratings.txt")
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
