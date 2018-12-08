package net.librec.spark

import net.librec.common.LibrecException
import net.librec.eval.AbstractRecommenderEvaluator
import net.librec.eval.ranking._
import net.librec.eval.rating._
import net.librec.recommender.item.RecommendedList

/**
  * Recommender Evaluator
  *
  * @author WangYuFeng
  */
object Measure extends Enumeration{
  type Measure = Value
  val AUC,AP,HR,IDCG,NDCG,PRECISION,RECALL,RR,Novelty,Entropy,RMSE,MSE,MAE,MPE = Value
}

import net.librec.spark.Measure._
object Evaluator {
  type PairRecommendedList = (RecommendedList, RecommendedList)

  /**
    * The map between Measure and Recommender Evaluator
    */
  private var cache = Map[Measure, AbstractRecommenderEvaluator]()

  /**
    * Evaluate the recommended result
    *
    * @param measure
    * @param groundTruthList
    * @param recommendedList
    * @param conf
    * @return evaluate result
    */
  def eval(measure: Measure, groundTruthList: RecommendedList, recommendedList: RecommendedList, conf: LibrecConf = new LibrecConf()): Double = {
    if (!cache.contains(measure)) {
      measure match {
        case AUC => cache += (AUC ->new AUCEvaluator())
        case AP => cache += (AP ->new AveragePrecisionEvaluator())
        case HR => cache += (HR ->new HitRateEvaluator())
        case IDCG => cache += (IDCG ->new IdealDCGEvaluator())
        case NDCG => cache += (NDCG ->new NormalizedDCGEvaluator())
        case PRECISION => cache += (PRECISION ->new PrecisionEvaluator())
        case RECALL => cache += (RECALL ->new RecallEvaluator())
        case RR => cache += (RR ->new ReciprocalRankEvaluator())
        case Novelty => cache += (Novelty ->new NoveltyEvaluator())
        case Entropy => cache += (Entropy ->new EntropyEvaluator())
        case RMSE => cache += (RMSE ->new RMSEEvaluator())
        case MSE => cache += (MSE ->new MSEEvaluator())
        case MAE => cache += (MAE ->new MAEEvaluator())
        case MPE => cache += (MPE ->new MPEEvaluator())
        case _ => throw new LibrecException("No matching Measure, please refer to net.librec.spark.Measure!")
      }
    }
    cache(measure).evaluateIndependently(conf, groundTruthList, recommendedList)
  }
}
