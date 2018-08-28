package net.librec.spark.rdd

import java.util.Random

import net.librec.spark.BaseTestSuite
import org.apache.spark.ml.tuning.{CrossValidator, CrossValidatorModel, TrainValidationSplit}
import org.apache.spark.rdd.RDD
import org.apache.spark.util.random.{BernoulliCellSampler, XORShiftRandom}
import net.librec.spark.rdd.SplitterFunctions._
import net.librec.spark.data.{DataConverter, Rating}


class SpliterFunctionsTestCase extends BaseTestSuite{
  override val appName: String = "EvaluatorTestCase"

  test("SpliterFunctionsTestCase") {
    val dataConverter = new DataConverter(lc)
    val rdd = dataConverter.convertText("file:///E:/workspace/my_workspace/librec/librec/data/spark/rating/ratings.txt")
    /*
    val datas = rdd.randomSplit(Array(0.1,0.2,0.3))
    var i = 0
    for(x <- datas){
      x.foreach(v=>println(i+"--"+v.user))
      i += 1
    }*/
    val weights = Array(0.7,0.1,0.2)
    val seed = new Random().nextLong()
    val sum = weights.sum
    val normalizedCumWeights = weights.map(_ / sum).scanLeft(0.0d)(_ + _)
    val rdd1 = rdd.groupBy(rat => rat.user)
    val res = normalizedCumWeights.sliding(weights.length-1).map { x =>
      rdd1.asInstanceOf[RDD[(Int, Iterable[Rating])]].mapPartitionsWithIndex({ (index,p)=>
        val sampler = new BernoulliCellSampler[Rating](x(0),x(1))
//        println(x(0)+"-------"+x(1))
        sampler.setSeed(seed + index)
        val itr = p.flatMap(ratings=> sampler.sample(ratings._2.iterator))
//        itr.foreach(println)
        //      p.flatMap(case (user,ratings)=> sampler.sample(ratings))
        itr
      }, preservesPartitioning = true)
    }.toArray.asInstanceOf[Array[RDD[Rating]]]
    var i = 0
    for(x <- res){
      x.foreach(v=>println(i+"--"+v.user))
      i += 1
    }
//    Array(0.1,0.2,0.3,0.4).map(_/1).scanLeft(0.0d)(_ + _).sliding(2).foreach(x =>println(x.mkString(",")))
//    rdd.groupBy(rat=> rat.user)
//    val ratings:Iterator[Rating] = Array(Rating(1,1,1),Rating(2,2,2),Rating(3,3,3)).iterator
//    Array(0.3,0.7).map(_/1).scanLeft(0.0d)(_ + _).sliding(2).map{x =>
////      println(x(0),x(1))
//      val sampler = new BernoulliCellSampler[Rating](x(0), x(1))
//      sampler.setSeed(Random.nextLong())
//      sampler.sample(ratings)
//    }.foreach(x =>println(x.length))
//    val p = Iterator((1,Iterable((1,1),(1,2),(1,3))),(2,Iterable((2,1),(2,2),(2,3))))
//    p.zipWithIndex.map{case (value,index) => println(value._2)}
  }


  test("SpliterFunctionsTestCase-radio") {
    val data = new DataConverter(lc).convertText("file:///E:/workspace/my_workspace/librec/librec/data/spark/rating/ratings.txt")
//    data.splitByRatio(Array(0.8,0.2),"userfixed").map(x => x.foreach(println))
//    val x = data.splitByLOO("item")
//    for ((elem, count) <- x.zipWithIndex){
//      elem.foreach(println(count,_))
//    }
    val x = data.splitByGivenN(1,"item")
  }
}
