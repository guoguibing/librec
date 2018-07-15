package net.librec.spark.data

import net.librec.spark.BaseTestSuite

class DataConvertorTestCase extends BaseTestSuite{
  override val appName: String = "DataConverterTestCase"
  var dataConverter:DataConvertor = _

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    dataConverter = new DataConvertor(lc)
  }
  
  test("convertText"){
    val rdd = dataConverter.convertText("file:///E:/workspace/my_workspace/librec/librec/data/spark/rating/ratings.txt")
    println(rdd.partitions.length)
    rdd.mapPartitionsWithIndex((index, ratings) =>{
      var result = List[String]()
      while (ratings.hasNext){
        val x = ratings.next()
        println(index + "-" + x.user + "-" + x.item + "-" + x.rate)
        result.::(index + "-" + x.user + "-" + x.item + "-" + x.rate)
      }
      result.iterator
    }).collect()
    val rdd1 = lc.sparkContext.parallelize(1 to 20)
    rdd1.map(v=> v+100).mapPartitionsWithIndex((index, ratings) =>{
      var result = List[String]()
      while (ratings.hasNext){
        val x = ratings.next()
        println(index + "-" + x)
        result.::(index + "-" + x)
      }
      result.iterator
    }).collect()
  }

}
