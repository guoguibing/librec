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
package net.librec.spark.data

import org.apache.spark.rdd.RDD
import net.librec.spark.LibrecContext

/**
  * A <tt>DataConverter</tt> is a class to convert
  * data file(s) from one source format to a target format.
  *
  * @author WangYuFeng
  */
class DataConverter(context: LibrecContext) {

  val conf = context.conf

  /**
    * Read data file(s) to RDD[Rating]
    *
    * @param path local or hdfs path of data file(s)
    * @param sep separator for each line of data file(s)
    * @return
    */
  def convertText(path: String, sep: String = " "): RDD[Rating] = {
    val data = context.sparkContext.textFile(path)
    data.map(line => {
      val values = line.split(sep)
      Rating(values(0).toInt, values(1).toInt, values(2).toDouble)
    })
  }

}
