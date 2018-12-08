package net.librec.spark

import net.librec.recommender.item.RecommendedList
import net.librec.spark.{Evaluator, Measure}

/**
  * Librec Context Test Case
  *
  * @author WangYuFeng
  */
class EvaluatorTestCase extends BaseTestSuite{
  override val appName: String = "EvaluatorTestCase"

  test("EvaluatorTestCase") {
    val groundTruthList: RecommendedList = new RecommendedList(2)
    groundTruthList.addIndependently(1,2,0.0)
    groundTruthList.addIndependently(2,1,4.0)
    val recommendedList: RecommendedList = new RecommendedList(2)
    recommendedList.addIndependently(1,2,0.1)
    recommendedList.addIndependently(2,1,5.2)
    println(Evaluator.eval(Measure.AUC,groundTruthList,recommendedList))
  }
}
