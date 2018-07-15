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
package net.librec.spark.rdd

import net.librec.math.structure.SequentialSparseVector
import net.librec.spark.Correlation.Correlation
import net.librec.spark.math.structure.IndexedVector
import net.librec.spark.{LibrecConf, Similarity}
import org.apache.spark.rdd.RDD

import scala.collection.mutable.ArrayBuffer

class SimilarityFunctions(rdd: RDD[IndexedVector]) {

  /**
    * Calculate Recommender Similarity, such as cosine, Pearson, Jaccard
    * similarity, etc.
    *
    * @param correlation the method of calculate similarity between thisVector and thatVector.
    * @param conf
    * @return all recommender similarity
    */
  def computeSimilarity(correlation: Correlation, conf: LibrecConf): RDD[(Int, (Int, Double))] = {
    val vectorRDD = rdd.mapPartitions({iter =>
      val localVectors = ArrayBuffer[(Int, SequentialSparseVector)]()
      while (iter.hasNext) {
        val indexedVector = iter.next()
        localVectors.+=((indexedVector.getIndex, indexedVector.asLocalVector.asInstanceOf[SequentialSparseVector]))
      }
      localVectors.iterator
    }, true)
    val ratingVectors = rdd.sparkContext.broadcast(vectorRDD.collect())
    vectorRDD.mapPartitions({ iter =>
      val vectors = ratingVectors.value
      val similarities = ArrayBuffer[(Int, (Int, Double))]()
      while (iter.hasNext) {
        val thisVector = iter.next()
        vectors.map { thatVector =>
          if (thisVector._1 < thatVector._1) {
            val sim = Similarity.getCorrelation(correlation, thisVector._2, thatVector._2, conf)
            if (sim == sim && sim != 0.0D) { // Return the specified number is Not-a-Number (NaN) value and unequals 0.0D
              similarities.+=((thisVector._1, (thatVector._1, sim)))
            }
          }
        }
      }
      similarities.iterator
    }, true)
  }

}

object SimilarityFunctions {
  implicit def addSimilarityFunctions(rdd: RDD[IndexedVector]) = new SimilarityFunctions(rdd)
}
