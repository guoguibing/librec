package net.librec.spark

/**
  * Librec Context Test Case
  * @author WangYuFeng
  */
class LibrecContextTestCase extends BaseTestSuite{
  override val appName: String = "LibrecContextTestCase"
  test("LibrecContext") {
    lc.convertData("file:///E:/workspace/my_workspace/librec/librec/data/spark/rating/ratings.txt").foreach(println)
  }
}
