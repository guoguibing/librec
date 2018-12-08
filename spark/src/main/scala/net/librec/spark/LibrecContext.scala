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
package net.librec.spark

import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
import net.librec.spark.data.{DataConverter, Rating}
import org.apache.commons.lang.StringUtils

/**
  * Create new LibrecContext based on provided Librec configuration
  *
  * @param conf Librec configuration
  * @author WangYuFeng
  */
class LibrecContext(val conf: LibrecConf) {
  val sparkContext = SparkContext.getOrCreate(LibrecContext.updatedConf(conf))

  /**
    * Read data file(s) to RDD[Rating]
    *
    * @param path local or hdfs path of data file(s)
    * @return
    */
  def convertData(path: String): RDD[Rating] = {
    val converter = new DataConverter(this)
    converter.convertText(path)
  }

  /**
    * Adds a JAR dependency for all tasks to be executed on this `SparkContext` in the future.
    *
    * If a jar is added during execution, it will not be available until the next TaskSet starts.
    *
    * @param path can be either a local file, a file in HDFS (or other Hadoop-supported filesystems),
    *             an HTTP, HTTPS or FTP URI, or local:/path for a file on every worker node.
    */
  def addJars(path: String): Unit = {
    if (StringUtils.isNotBlank(path)) {
      val jarPaths = path.split(",")
      jarPaths.foreach(p => sparkContext.addJar(p.trim))
    }
  }

  /**
    * Return a copy of this LibrecContext's configuration. The configuration ''cannot'' be
    * changed at runtime.
    */
  def getConf: LibrecConf = conf

  /**
    * Shut down the SparkContext.
    */
  def stop(): Unit = sparkContext.stop()
}

object LibrecContext {
  def updatedConf(conf: LibrecConf): SparkConf = conf.toSparkConf

}