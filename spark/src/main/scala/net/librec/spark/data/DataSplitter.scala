package net.librec.spark.data

import org.apache.spark.rdd.RDD
import net.librec.spark.LibrecConf
import net.librec.spark.math.structure.IndexedVector

/**
  * A <tt>DataSplitter</tt> is an interface to split
  * input data.
  *
  * @author WangYuFeng
  */
class DataSplitter(conf: LibrecConf, data: RDD[IndexedVector]) {
  /**
    * Split dataset into train set, test set by ratio.
    *
    * @return train set and test set
    */
  def splitByRatio(): Array[RDD[IndexedVector]] = {
    val trainRatio = conf.getDouble("data.splitter.trainset.ratio", 0.8)
    data.randomSplit(Array(trainRatio, 1 - trainRatio))
  }

}
