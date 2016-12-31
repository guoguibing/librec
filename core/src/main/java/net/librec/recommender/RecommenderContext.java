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
package net.librec.recommender;

import java.util.HashMap;
import java.util.Map;

import net.librec.common.AbstractContext;
import net.librec.conf.Configuration;
import net.librec.data.DataModel;
import net.librec.similarity.RecommenderSimilarity;

/**
 * RecommenderContext
 *
 * @author WangYuFeng
 */
public class RecommenderContext extends AbstractContext {

    protected DataModel dataModel;

    protected RecommenderSimilarity similarity;

    protected Map<String, RecommenderSimilarity> similarities;

    public RecommenderContext(Configuration conf) {
        this.conf = conf;
    }

    public RecommenderContext(Configuration conf, DataModel dataModel) {
        this.conf = conf;
        this.dataModel = dataModel;
    }

    public RecommenderContext(Configuration conf, DataModel dataModel, RecommenderSimilarity similarity) {
        this.conf = conf;
        this.dataModel = dataModel;
        this.similarity = similarity;
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

    /**
     * @return the similarities
     */
    public Map<String, RecommenderSimilarity> getSimilarities() {
        return similarities;
    }

    /**
     * @param similarityKey
     *            the similarities to add
     * @param similarity
     *            the similarities to add
     */
    public void addSimilarities(String similarityKey, RecommenderSimilarity similarity) {
        if(this.similarities == null){
            this.similarities = new HashMap<String, RecommenderSimilarity>();
        }
        this.similarities.put(similarityKey, similarity);
    }
}
