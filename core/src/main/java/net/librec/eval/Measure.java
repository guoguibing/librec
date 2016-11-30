/**
 * Copyright (C) 2016 LibRec
 * 
 * This file is part of LibRec.
 * LibRec is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LibRec is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LibRec. If not, see <http://www.gnu.org/licenses/>.
 */
package net.librec.eval;

import java.util.ArrayList;
import java.util.List;

import net.librec.eval.ranking.AUCEvaluator;
import net.librec.eval.ranking.AveragePrecisionEvaluator;
import net.librec.eval.ranking.IdealDCGEvaluator;
import net.librec.eval.ranking.NormalizedDCGEvaluator;
import net.librec.eval.ranking.PrecisionEvaluator;
import net.librec.eval.ranking.RecallEvaluator;
import net.librec.eval.ranking.ReciprocalRankEvaluator;
import net.librec.eval.rating.MAEEvaluator;
import net.librec.eval.rating.MPEEvaluator;
import net.librec.eval.rating.MSEEvaluator;
import net.librec.eval.rating.RMSEEvaluator;

/**
 * Measure
 * 
 * @author WangYuFeng
 */
public enum Measure {

	AUC(AUCEvaluator.class), 
	AP(AveragePrecisionEvaluator.class), 
	IDCG(IdealDCGEvaluator.class), 
	NDCG(NormalizedDCGEvaluator.class), 
	PRECISION(PrecisionEvaluator.class), 
	RECALL(RecallEvaluator.class), 
	RR(ReciprocalRankEvaluator.class), 
	RMSE(RMSEEvaluator.class), 
	MSE(MSEEvaluator.class), 
	MAE(MAEEvaluator.class), 
	MPE(MPEEvaluator.class);

	private Class<? extends RecommenderEvaluator> evaluatorClass;

	private Measure(Class<? extends RecommenderEvaluator> evaluatorClass) {
		this.evaluatorClass = evaluatorClass;
	}

	public static List<MeasureValue> getMeasureEnumList(boolean isRanking, int topN) {
		if (isRanking) {
			return getRankingEnumList(topN);
		} else {
			return getRatingEnumList();
		}
	}

	/**
	 * get Ranking Default EnumList
	 * 
	 * @return
	 */
	private static List<MeasureValue> getRankingEnumList(int topN) {
		List<MeasureValue> rankingEnumList = new ArrayList<MeasureValue>();
		rankingEnumList.add(new MeasureValue(AUC));
		rankingEnumList.add(new MeasureValue(AP));
		rankingEnumList.add(new MeasureValue(IDCG));
		rankingEnumList.add(new MeasureValue(NDCG));
		rankingEnumList.add(new MeasureValue(RR));
		if (topN <= 0) {
			rankingEnumList.add(new MeasureValue(PRECISION, 5));
			rankingEnumList.add(new MeasureValue(PRECISION, 10));
			rankingEnumList.add(new MeasureValue(RECALL, 5));
			rankingEnumList.add(new MeasureValue(RECALL, 10));
		} else {
			rankingEnumList.add(new MeasureValue(PRECISION, topN));
			rankingEnumList.add(new MeasureValue(RECALL, topN));
		}
		return rankingEnumList;
	}

	/**
	 * get Rating Default EnumList
	 */
	private static List<MeasureValue> getRatingEnumList() {
		List<MeasureValue> ratingEnumList = new ArrayList<MeasureValue>();
		ratingEnumList.add(new MeasureValue(RMSE));
		ratingEnumList.add(new MeasureValue(MSE));
		ratingEnumList.add(new MeasureValue(MAE));
		ratingEnumList.add(new MeasureValue(MPE));
		return ratingEnumList;
	}

	/**
	 * @return the topN
	 */
	public Class<? extends RecommenderEvaluator> getEvaluatorClass() {
		return evaluatorClass;
	}

	public static class MeasureValue {
		private Measure measure;
		private Integer topN;

		public MeasureValue(Measure measure) {
			this.measure = measure;
		}

		public MeasureValue(Measure measure, Integer topN) {
			this.measure = measure;
			this.topN = topN;
		}

		/**
		 * @return the measure
		 */
		public Measure getMeasure() {
			return measure;
		}

		/**
		 * @param measure
		 *            the measure to set
		 */
		public void setMeasure(Measure measure) {
			this.measure = measure;
		}

		/**
		 * @return the topN
		 */
		public Integer getTopN() {
			return topN;
		}

		/**
		 * @param topN
		 *            the topN to set
		 */
		public void setTopN(Integer topN) {
			this.topN = topN;
		}

	}

}
