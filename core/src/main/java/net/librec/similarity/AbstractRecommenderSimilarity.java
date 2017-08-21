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
package net.librec.similarity;

import net.librec.conf.Configuration;
import net.librec.data.DataModel;
import net.librec.data.convertor.appender.SocialDataAppender;
import net.librec.math.structure.SparseMatrix;
import net.librec.math.structure.SparseVector;
import net.librec.math.structure.SymmMatrix;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Calculate Recommender Similarity, such as cosine, Pearson, Jaccard
 * similarity, etc.
 *
 * @author zhanghaidong
 */

public abstract class AbstractRecommenderSimilarity implements RecommenderSimilarity {

    /**
     * Configuration
     */
    protected Configuration conf;
    /**
     * Similarity Matrix
     */
    protected SymmMatrix similarityMatrix;

    /**
     * Build social similarity matrix with trainMatrix in dataModel.
     *
     * @param dataModel
     *            the input data model
     */
    @Override
    public void buildSimilarityMatrix(DataModel dataModel) {
        conf = dataModel.getContext().getConf();
        String similarityKey = conf.get("rec.recommender.similarity.key", "user");
        if(StringUtils.isNotBlank(similarityKey)){
            if (StringUtils.equals(similarityKey, "social")) {
                buildSocialSimilarityMatrix(dataModel);
            } else {
                // calculate the similarity between users, or the similarity between
                // items.
                boolean isUser = StringUtils.equals(similarityKey, "user") ? true : false;
                SparseMatrix trainMatrix = dataModel.getDataSplitter().getTrainData();
                int numUsers = trainMatrix.numRows();
                int numItems = trainMatrix.numColumns();
                int count = isUser ? numUsers : numItems;
                
                similarityMatrix = new SymmMatrix(count);
                
                for (int i = 0; i < count; i++) {
                    SparseVector thisVector = isUser ? trainMatrix.row(i) : trainMatrix.column(i);
                    if (thisVector.getCount() == 0) {
                        continue;
                    }
                    // user/item itself exclusive
                    for (int j = i + 1; j < count; j++) {
                        SparseVector thatVector = isUser ? trainMatrix.row(j) : trainMatrix.column(j);
                        if (thatVector.getCount() == 0) {
                            continue;
                        }
                        
                        double sim = getCorrelation(thisVector, thatVector);
                        if (!Double.isNaN(sim) && sim != 0) {
                            similarityMatrix.set(i, j, sim);
                        }
                    }
                }
            }
        }

    }

    /**
     * Build social similarity matrix with trainMatrix
     * and socialMatrix in dataModel.
     * 
     * @param dataModel
     *            the input data model
     */
    public void buildSocialSimilarityMatrix(DataModel dataModel) {
        SparseMatrix trainMatrix = dataModel.getDataSplitter().getTrainData();
        SparseMatrix socialMatrix = ((SocialDataAppender) dataModel.getDataAppender()).getUserAppender();
        int numUsers = trainMatrix.numRows();

        similarityMatrix = new SymmMatrix(numUsers);

        for (int userIdx = 0; userIdx < numUsers; userIdx++) {
            SparseVector userVector = trainMatrix.row(userIdx);
            if (userVector.getCount() == 0) {
                continue;
            }
            List<Integer> socialList = socialMatrix.getRows(userIdx);
            for (int socialIdx : socialList) {
                SparseVector socialVector = trainMatrix.row(socialIdx);
                if (socialVector.getCount() == 0) {
                    continue;
                }

                double sim = getCorrelation(userVector, socialVector);
                if (!Double.isNaN(sim)) {
                    similarityMatrix.set(userIdx, socialIdx, sim);
                }
            }
        }
    }

    /**
     * Find the common rated items by this user and that user, or the common
     * users have rated this item or that item. And then return the similarity.
     *
     * @param thisVector:
     *            the rated items by this user, or users that have rated this
     *            item .
     * @param thatVector:
     *            the rated items by that user, or users that have rated that
     *            item.
     * @return similarity
     */
    public double getCorrelation(SparseVector thisVector, SparseVector thatVector) {
        // compute similarity
        List<Double> thisList = new ArrayList<Double>();
        List<Double> thatList = new ArrayList<Double>();

        for (Integer idx : thatVector.getIndex()) {
            if (thisVector.contains(idx)) {
                thisList.add(thisVector.get(idx));
                thatList.add(thatVector.get(idx));
            }
        }
        double sim = getSimilarity(thisList, thatList);

        // shrink to account for vector size
        if (!Double.isNaN(sim)) {
            int n = thisList.size();
            int shrinkage = conf.getInt("rec.similarity.shrinkage", 0);
            if (shrinkage > 0)
                sim *= n / (n + shrinkage + 0.0);
        }

        return sim;
    }

    /**
     * Calculate the similarity between thisList and thatList.
     *
     * @param thisList
     *            this list
     * @param thatList
     *            that list
     * @return similarity
     */
    protected abstract double getSimilarity(List<? extends Number> thisList, List<? extends Number> thatList);

    /**
     * Return the similarity matrix.
     *
     * @return the similarity matrix
     */
    @Override
    public SymmMatrix getSimilarityMatrix() {
        return similarityMatrix;
    }

}
