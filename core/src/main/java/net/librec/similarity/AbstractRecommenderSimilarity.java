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
import net.librec.math.structure.SequentialAccessSparseMatrix;
import net.librec.math.structure.SequentialSparseVector;
import net.librec.math.structure.SymmMatrix;
import net.librec.math.structure.Vector;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Calculate Recommender Similarity, such as cosine, Pearson, Jaccard
 * similarity, etc.
 *
 * @author zhanghaidong and Keqiang Wang (email: sei.wkq2008@gmail.com)
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
     * @param dataModel the input data model
     */
    @Override
    public void buildSimilarityMatrix(DataModel dataModel) {
        conf = dataModel.getContext().getConf();
        String similarityKey = conf.get("rec.recommender.similarity.key", "user");
        if (StringUtils.isNotBlank(similarityKey)) {
            if (StringUtils.equals(similarityKey, "social")) {
                buildSocialSimilarityMatrix(dataModel);
            } else {
                // calculate the similarity between users, or the similarity between items.
                boolean isUser = StringUtils.equals(similarityKey, "user");
                SequentialAccessSparseMatrix trainMatrix = dataModel.getDataSplitter().getTrainData();
                int numUsers = trainMatrix.rowSize();
                int numItems = trainMatrix.columnSize();
                int count = isUser ? numUsers : numItems;

                similarityMatrix = new SymmMatrix(count);
                List<Integer> indexList = new ArrayList<>();
                for (int index = 0; index < count; index++) {
                    indexList.add(index);
                }

                indexList.parallelStream().forEach((Integer thisIndex) -> {
                    SequentialSparseVector thisVector = isUser ? trainMatrix.row(thisIndex) : trainMatrix.column(thisIndex);
                    if (thisVector.getNumEntries() != 0) {
                        // user/item itself exclusive
                        for (int thatIndex = thisIndex + 1; thatIndex < count; thatIndex++) {
                            SequentialSparseVector thatVector = isUser ? trainMatrix.row(thatIndex) : trainMatrix.column(thatIndex);
                            if (thatVector.getNumEntries() == 0) {
                                continue;
                            }

                            double sim = getCorrelation(thisVector, thatVector);
                            if (!Double.isNaN(sim) && sim != 0.0) {
                                similarityMatrix.set(thisIndex, thatIndex, sim);
                            }
                        }
                    }
                });
            }
        }
    }

    /**
     * Build social similarity matrix with trainMatrix
     * and socialMatrix in dataModel.
     *
     * @param dataModel the input data model
     */
    public void buildSocialSimilarityMatrix(DataModel dataModel) {
        SequentialAccessSparseMatrix trainMatrix = dataModel.getDataSplitter().getTrainData();
        SequentialAccessSparseMatrix socialMatrix = ((SocialDataAppender) dataModel.getDataAppender()).getUserAppender();
        int numUsers = trainMatrix.rowSize();

        similarityMatrix = new SymmMatrix(numUsers);

        for (int userIdx = 0; userIdx < numUsers; userIdx++) {
            SequentialSparseVector userVector = trainMatrix.row(userIdx);
            if (userVector.getNumEntries() == 0) {
                continue;
            }
            SequentialSparseVector socialVector = socialMatrix.row(userIdx);
            for (Vector.VectorEntry vectorEntry : socialVector) {
                int socialUserIndex = vectorEntry.index();
                SequentialSparseVector socialUserVector = trainMatrix.row(socialUserIndex);
                if (socialUserVector.getNumEntries() == 0) {
                    continue;
                }

                double sim = getCorrelation(userVector, socialVector);
                if (!Double.isNaN(sim)) {
                    similarityMatrix.set(userIdx, socialUserIndex, sim);
                }
            }
        }
    }

    /**
     * Find the common rated items by this user and that user, or the common
     * users have rated this item or that item. And then return the similarity.
     *
     * @param thisVector: the rated items by this user, or users that have rated this
     *                    item.
     * @param thatVector: the rated items by that user, or users that have rated that
     *                    item.
     * @return similarity
     */
    public double getCorrelation(SequentialSparseVector thisVector, SequentialSparseVector thatVector) {
        // compute similarity
        List<Double> thisList = new ArrayList<>();
        List<Double> thatList = new ArrayList<>();

        int thisPosition = 0, thatPosition = 0;
        int thisSize = thisVector.getNumEntries(), thatSize = thatVector.getNumEntries();
        int thisIndex, thatIndex;
        while (thisPosition < thisSize && thatPosition < thatSize) {
            thisIndex = thisVector.getIndexAtPosition(thisPosition);
            thatIndex = thatVector.getIndexAtPosition(thatPosition);
            if (thisIndex == thatIndex) {
                thisList.add(thisVector.getAtPosition(thisPosition));
                thatList.add(thatVector.getAtPosition(thatPosition));
                thisPosition++;
                thatPosition++;
            } else if (thisIndex > thatIndex) {
                thatPosition++;
            } else {
                thisPosition++;
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
     * Find the common rated items by this user and that user, or the common
     * users have rated this item or that item. And then return the similarity.
     *
     * @param thisVector: the rated items by this user, or users that have rated this
     *                    item .
     * @param thatVector: the rated items by that user, or users that have rated that
     *                    item.
     * @return similarity
     */
    public double getCorrelationIndependently(Configuration conf, SequentialSparseVector thisVector, SequentialSparseVector thatVector) {
        this.conf = conf;
        return getCorrelation(thisVector, thatVector);
    }

    /**
     * Calculate the similarity between thisList and thatList.
     *
     * @param thisList this list
     * @param thatList that list
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
