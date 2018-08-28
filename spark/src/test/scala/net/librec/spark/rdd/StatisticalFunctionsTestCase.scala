package net.librec.spark.rdd

import net.librec.spark.BaseTestSuite
import net.librec.spark.data.DataConverter
import net.librec.spark.math.structure.IndexedVector
import net.librec.spark.rdd.StatisticalFunctions._
import org.apache.spark.rdd.RDD


class StatisticalFunctionsTestCase extends BaseTestSuite{
  override val appName: String = "StatisticalFunctionsTestCase"

  test("StatisticalFunctionsTestCase-radio") {
    val data = new DataConverter(lc).convertText("file:///E:/workspace/my_workspace/librec/librec/data/spark/rating/ratings.txt")
    val x:RDD[IndexedVector] = StatisticalFunctions.toIndexedSparseVectors(data)
    x.mean()
  }
}
