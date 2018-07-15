package net.librec.spark

import net.librec.common.LibrecException
import net.librec.math.structure.SequentialSparseVector
import net.librec.similarity._

/**
  * Recommender Similarity
  *
  * @author WangYuFeng
  */
object Correlation extends Enumeration {
  type Correlation = Value
  val BCOS, COS, CPC, MSESIM, MSD, PCC, KRCC, DICE, JACCARD, EXJACCARD = Value
}

import net.librec.spark.Correlation._

object Similarity {
  type PairSparseVector = (SequentialSparseVector, SequentialSparseVector)

  /**
    * The map between Measure and Recommender Similarity
    */
  private var cache = Map[Correlation, AbstractRecommenderSimilarity]()

  /**
    * Find the common rated items by this user and that user, or the common
    * users have rated this item or that item. And then return the similarity.
    *
    * @param correlation the method of calculate similarity between thisVector and thatVector.
    * @param thisVector the rated items by this user, or users that have rated this item.
    * @param thatVector the rated items by that user, or users that have rated that item.
    * @param conf
    * @return similarity
    */
  def getCorrelation(correlation: Correlation, thisVector: SequentialSparseVector, thatVector: SequentialSparseVector, conf: LibrecConf = new LibrecConf()): Double = {
    if (!cache.contains(correlation)) {
      correlation match {
        case COS => cache += (COS -> new CosineSimilarity())
        case BCOS => cache += (BCOS -> new BinaryCosineSimilarity())
        case CPC => cache += (CPC -> new CPCSimilarity())
        case MSESIM => cache += (MSESIM -> new MSESimilarity())
        case MSD => cache += (MSD -> new MSDSimilarity())
        case PCC => cache += (PCC -> new PCCSimilarity())
        case KRCC => cache += (KRCC -> new KRCCSimilarity())
        case DICE => cache += (DICE -> new DiceCoefficientSimilarity())
        case JACCARD => cache += (JACCARD -> new JaccardSimilarity())
        case EXJACCARD => cache += (EXJACCARD -> new ExJaccardSimilarity())
        case _ => throw new LibrecException("No matching Correlation, please refer to net.librec.spark.Correlation!")
      }
    }
    cache(correlation).getCorrelationIndependently(conf, thisVector, thatVector)
  }

}
