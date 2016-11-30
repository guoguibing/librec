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
package net.librec.recommender;

import net.librec.conf.Configuration;
import net.librec.data.DataModel;
import net.librec.similarity.RecommenderSimilarity;

/**
 * RecommenderContext
 * 
 * @author WangYuFeng
 */
public class RecommenderContext {

	protected final Configuration conf;

	protected final DataModel dataModel;

	protected RecommenderSimilarity similarity;

	public RecommenderContext(Configuration conf, DataModel dataModel) {
		this.conf = conf;
		this.dataModel = dataModel;
	}

	public RecommenderContext(Configuration conf, DataModel dataModel, RecommenderSimilarity similarity) {
		this.conf = conf;
		this.dataModel = dataModel;
		this.similarity = similarity;
	}

	public Configuration getConf() {
		return conf;
	}

	public DataModel getDataModel() {
		return dataModel;
	}

	public RecommenderSimilarity getSimilarity() {
		return similarity;
	}

	public void setSimilarity(RecommenderSimilarity similarity) {
		this.similarity = similarity;
	}

}
