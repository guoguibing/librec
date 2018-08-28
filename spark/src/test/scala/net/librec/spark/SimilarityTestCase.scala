package net.librec.spark

import net.librec.spark.data.DataConverter
import net.librec.spark.rdd.SimilarityFunctions._
import net.librec.spark.rdd.StatisticalFunctions

/**
  * Librec Context Test Case
  *
  * @author WangYuFeng
  */
class SimilarityTestCase extends BaseTestSuite{
  override val appName: String = "SimilarityTestCase"
  var dataConverter:DataConverter = _

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    dataConverter = new DataConverter(lc)
  }

  test("SimilarityTestCase") {
//    IndexedRow
//
//    val a = lc.sparkContext.parallelize(List((1,5),(2,6),(3,7)))
//    a.join(a).collect().foreach(print)
    val rdd = dataConverter.convertText("file:///E:/workspace/my_workspace/librec/librec/data/spark/rating/ratings.txt")
    val similarity = StatisticalFunctions.toIndexedSparseVectors(rdd).computeSimilarity(Correlation.BCOS, conf).foreach(println)
  }
}
