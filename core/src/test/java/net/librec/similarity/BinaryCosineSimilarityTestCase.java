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
package net.librec.similarity;

import net.librec.BaseTestCase;
import net.librec.data.DataModel;
import net.librec.data.model.TextDataModel;
import net.librec.math.algorithm.Randoms;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Binary Cosine Similarity Test Case corresponds to BinaryCosineSimilarity
 * {@link net.librec.similarity.BinaryCosineSimilarity}
 *
 * @author SunYatong
 */
public class BinaryCosineSimilarityTestCase extends BaseTestCase {

    private DataModel dataModel;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        conf.set("data.appender.class", "social");
        conf.set("data.appender.path", "filmtrust/trust/trust.txt");
        dataModel = new TextDataModel(conf);
    }

    @Test
    public void test1BuildUserSimilarityMatrix() throws Exception {
        dataModel.buildDataModel();
        conf.set("rec.recommender.similarity.key", "user");
        RecommenderSimilarity similarity = new BinaryCosineSimilarity();
        similarity.buildSimilarityMatrix(dataModel);

        assertEquals(1508, similarity.getSimilarityMatrix().getDim());
    }

    @Test
    public void test2BuildItemSimilarityMatrix() throws Exception {
        dataModel.buildDataModel();
        conf.set("rec.recommender.similarity.key", "item");
        RecommenderSimilarity similarity = new BinaryCosineSimilarity();
        similarity.buildSimilarityMatrix(dataModel);

        assertEquals(2071, similarity.getSimilarityMatrix().getDim());
    }

    @Test
    public void test3BuildSocialSimilarityMatrix() throws Exception {
        dataModel.buildDataModel();
        conf.set("rec.recommender.similarity.key", "social");
        RecommenderSimilarity similarity = new BinaryCosineSimilarity();
        similarity.buildSimilarityMatrix(dataModel);

        assertEquals(1508, similarity.getSimilarityMatrix().getDim());
    }

    @Test
    public void test4SimilarityMatrix() throws Exception {
        conf.set("data.input.path", "test/datamodeltest/matrix4by4.txt");
        conf.set("rec.recommender.similarity.key", "user");
        Randoms.seed(conf.getInt("rec.random.seed"));
        TextDataModel textDataModel = new TextDataModel(conf);
        textDataModel.buildDataModel();
        RecommenderSimilarity similarity = new BinaryCosineSimilarity();
        similarity.buildSimilarityMatrix(textDataModel);

        for (int i=0; i<similarity.getSimilarityMatrix().getDim(); i++) {
            for (int j=0; j<similarity.getSimilarityMatrix().getDim(); j++) {
                System.out.println("row:" + i + " col:" + j + " value:" + similarity.getSimilarityMatrix().get(i, j));
            }
        }

        assertEquals(4, similarity.getSimilarityMatrix().getDim());
        assertEquals(0.5477, similarity.getSimilarityMatrix().get(0,1), 0.001);
        assertEquals(0, similarity.getSimilarityMatrix().get(0, 2), 0.001);
        assertEquals(0.6485, similarity.getSimilarityMatrix().get(0, 3), 0.001);
        assertEquals(0.624, similarity.getSimilarityMatrix().get(1, 3), 0.001);
    }
}
