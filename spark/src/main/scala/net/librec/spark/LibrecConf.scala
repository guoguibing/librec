package net.librec.spark

import net.librec.conf.Configuration
import org.apache.spark.SparkConf

import scala.collection.JavaConverters._

/**
  * Configuration holder which is representing
  * properties passed from user to Librec.
  *
  * @author WangYuFeng
  */
class LibrecConf extends Configuration with Serializable {

  /**
    * Set a spark environment configuration variable.
    * @param key key of configuration
    * @param value value of configuration
    * @return LibrecConf
    */
  def setSparkEnv(key: String, value: String): LibrecConf = {
    if (key == null) {
      throw new NullPointerException("null key")
    }
    if (value == null) {
      throw new NullPointerException("null value for " + key)
    }
    getProps.setProperty(key, value)
    this
  }

  /**
    * The master URL to connect to, such as "local" to run locally with one thread, "local[4]" to
    * run locally with 4 cores, or "spark://master:7077" to run on a Spark standalone cluster.
    */
  def setMaster(master: String):LibrecConf = {
    set("spark.master", master)
    this
  }

  /** Set a name for your application. Shown in the Spark web UI. */
  def setAppName(name: String):LibrecConf = {
    set("spark.app.name", name)
    this
  }

  /** Copy this object to spark configuration */
  def toSparkConf: SparkConf = {
    val cloned = new SparkConf()
    iterator().asScala.foreach {
      e =>
        cloned.set(e.getKey, e.getValue)
    }
    cloned
  }

}

object LibrecConf {

  implicit class librecConfTypeConversion(val sparkconf: SparkConf) {
    def librecToSparkConf: LibrecConf = sparkconf.asInstanceOf[LibrecConf]
  }

}
