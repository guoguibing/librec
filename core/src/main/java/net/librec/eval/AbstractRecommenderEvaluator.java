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

import net.librec.conf.Configuration;
import net.librec.math.structure.SparseMatrix;
import net.librec.math.structure.SymmMatrix;
import net.librec.recommender.RecommenderContext;
import net.librec.recommender.item.RecommendedList;

/**
 * Abstract Recommender Evaluator
 * 
 * @author WangYuFeng
 */
public abstract class AbstractRecommenderEvaluator implements RecommenderEvaluator {

	/**
	 * topN
	 */
	protected int topN;
	/**
	 * conf
	 */
	protected Configuration conf;
	/**
	 * similarityMatrix
	 */
	protected SymmMatrix similarityMatrix;

	/**
	 * evaluate
	 * 
	 * @param context
	 * @param recommendedList
	 * @return
	 */
	public double evaluate(RecommenderContext context, RecommendedList recommendedList) {
		SparseMatrix testMatrix = context.getDataModel().getDataSplitter().getTestData();
		conf = context.getConf();
		if (conf.getBoolean("rec.similarity.category")) {
			similarityMatrix = context.getSimilarity().getSimilarityMatrix();
		}
		return evaluate(testMatrix, recommendedList);
	}

	/**
	 * evaluate
	 * 
	 * @param testMatrix
	 * @param recommendedList
	 * @return
	 */
	public abstract double evaluate(SparseMatrix testMatrix, RecommendedList recommendedList);

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.librec.eval.RecommenderEvaluator#setTopN(int)
	 */
	@Override
	public void setTopN(int topN) {
		this.topN = topN;
	}

	/**
	 * @return the conf
	 */
	public Configuration getConf() {
		return conf;
	}

}
