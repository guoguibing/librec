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
package net.librec.recommender.rec.ext;

import net.librec.annotation.ModelData;
import net.librec.common.LibrecException;
import net.librec.math.algorithm.Randoms;
import net.librec.math.algorithm.Sims;
import net.librec.math.structure.DenseVector;
import net.librec.math.structure.SparseVector;
import net.librec.math.structure.SymmMatrix;
import net.librec.math.structure.VectorEntry;
import net.librec.recommender.rec.cf.ranking.RankSGDRecommender;
import net.librec.util.Lists;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Neil Hurley, <strong>Personalised ranking with diversity</strong>, RecSys 2013.
 * <p>
 * <p>
 * Related Work:
 * <ul>
 * <li>Jahrer and Toscher, Collaborative Filtering Ensemble for Ranking, JMLR, 2012 (KDD Cup 2011 Track 2).</li>
 * </ul>
 * </p>
 *
 * @author guoguibing and Keqiang Wang
 */
@ModelData({"isRanking", "prankd", "userFactors", "itemFactors","trainMatrix"})
public class PRankDRecommender extends RankSGDRecommender {
    /**
     * item importance
     */
    private DenseVector itemWeights;

    /**
     * item correlations
     */
    private SymmMatrix itemCorrs;

    /**
     * similarity filter
     */
    private float alpha;

    /**
     * similarity measure
     */
    protected static String similarityMeasure;

    /**
     * initialization
     *
     * @throws LibrecException
     */
    @Override
    protected void setup() throws LibrecException {
        super.setup();

        similarityMeasure = "cos-binary";

        // compute item sampling probability
        Map<Integer, Double> itemProbsMap = new HashMap<>();
        double maxUsersCount = 0;

        itemWeights = new DenseVector(numItems);
        for (int itemIdx = 0; itemIdx < numItems; itemIdx++) {
            int usersCount = trainMatrix.columnSize(itemIdx);

            maxUsersCount = maxUsersCount < usersCount ? usersCount : maxUsersCount;
            itemWeights.set(itemIdx, usersCount);
            // sample items based on popularity
            double prob = (usersCount + 0.0) / numRates;
            if (prob > 0)
                itemProbsMap.put(itemIdx, prob);
        }
        itemProbs = Lists.sortMap(itemProbsMap);

        // compute item relative importance
        for (int itemIdx = 0; itemIdx < numItems; itemIdx++) {
            itemWeights.set(itemIdx, itemWeights.get(itemIdx) / maxUsersCount);
        }

        alpha = conf.getFloat("-alpha");

        // compute item correlations by cosine similarity
        itemCorrs = buildCorrs(false);
    }

    /**
     * train model
     *
     * @throws LibrecException
     * @throws ExecutionException
     */
    @Override
    protected void trainModel() throws LibrecException {
        for (int iter = 1; iter <= numIterations; iter++) {

            // for each rated user-item (u,i) pair
            for (int userIdx : trainMatrix.rows()) {

                SparseVector itemRatingsVector = trainMatrix.row(userIdx);
                for (VectorEntry itemRatingEntry : itemRatingsVector) {
                    // each rated item i
                    int posItemIdx = itemRatingEntry.index();
                    double posRating = itemRatingEntry.get();

                    int negItemIdx = -1;
                    while (true) {
                        // draw an item j with probability proportional to popularity
                        double sum = 0, randValue = Randoms.random();
                        for (Map.Entry<Integer, Double> mapEntry : itemProbs) {
                            int tempNegItemIdx = mapEntry.getKey();
                            double prob = mapEntry.getValue();

                            sum += prob;
                            if (sum >= randValue) {
                                negItemIdx = tempNegItemIdx;
                                break;
                            }
                        }

                        // ensure that it is unrated by user u
                        if (!itemRatingsVector.contains(negItemIdx))
                            break;
                    }
                    double negRating = 0;

                    // compute predictions
                    double posPredictRating = predict(userIdx, posItemIdx), negPredictRating = predict(userIdx, negItemIdx);

                    double distance = Math.sqrt(1 - itemCorrs.get(posItemIdx, negItemIdx));
                    double itemWeightValue = itemWeights.get(negItemIdx);

                    double error = itemWeightValue * (posPredictRating - negPredictRating - distance * (posRating - negRating));

                    // update vectors
                    double learnFactor = learnRate * error;
                    for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                        double userFactorValue = userFactors.get(userIdx, factorIdx);
                        double posItemFactorValue = itemFactors.get(posItemIdx, factorIdx);
                        double negItemFactorValue = itemFactors.get(negItemIdx, factorIdx);

                        userFactors.add(userIdx, factorIdx, -learnFactor * (posItemFactorValue - negItemFactorValue));
                        itemFactors.add(posItemIdx, factorIdx, -learnFactor * userFactorValue);
                        itemFactors.add(negItemIdx, factorIdx, learnFactor * userFactorValue);
                    }
                }
            }
        }
    }


    /**
     * @param x     input value
     * @param mu    mean of normal distribution
     * @param sigma standard deviation of normation distribution
     * @return a gaussian value with mean {@code mu} and standard deviation {@code sigma};
     */
    protected double gaussian(double x, double mu, double sigma) {
        return Math.exp(-0.5 * Math.pow(x - mu, 2) / (sigma * sigma));
    }

    /**
     * build user-user or item-item correlation matrix from training data
     *
     * @param isUser whether it is user-user correlation matrix
     * @return a upper symmetric matrix with user-user or item-item coefficients
     */
    protected SymmMatrix buildCorrs(boolean isUser) {
        int count = isUser ? numUsers : numItems;
        SymmMatrix corrs = new SymmMatrix(count);

        for (int index = 0; index < count; index++) {
            SparseVector vector = isUser ? trainMatrix.row(index) : trainMatrix.column(index);
            if (vector.getCount() == 0)
                continue;
            // user/item itself exclusive
            for (int comparedIndex = index + 1; comparedIndex < count; comparedIndex++) {
                SparseVector comparedVector = isUser ? trainMatrix.row(comparedIndex) : trainMatrix.column(comparedIndex);

                double similarity = correlation(vector, comparedVector);

                if (!Double.isNaN(similarity))
                    corrs.set(index, comparedIndex, similarity);
            }
        }
        return corrs;
    }

    /**
     * Compute the correlation between two vectors using method specified by configuration key "similarity"
     *
     * @param vector         vector
     * @param comparedVector vector
     * @return the correlation between vectors vector and comparedVector
     */
    protected double correlation(SparseVector vector, SparseVector comparedVector) {
        return correlation(vector, comparedVector, similarityMeasure);
    }

    /**
     * Compute the correlation between two vectors for a specific method
     *
     * @param vector         vector
     * @param comparedVector vector
     * @param method         similarity method
     * @return the correlation between vectors vector and comparedVector; return NaN if the correlation is not computable.
     */
    protected double correlation(SparseVector vector, SparseVector comparedVector, String method) {

        // compute similarity
        List<Double> vectorList = new ArrayList<>();
        List<Double> comparedVectorList = new ArrayList<>();

        for (Integer idx : comparedVector.getIndex()) {
            if (vector.contains(idx)) {
                vectorList.add(vector.get(idx));
                comparedVectorList.add(comparedVector.get(idx));
            }
        }

        double similarity = 0;
        switch (method.toLowerCase()) {
            case "cos":
                // for ratings along the overlappings
                similarity = Sims.cos(vectorList, comparedVectorList);
                break;
            case "cos-binary":
                // for ratings along all the vectors (including one-sided 0s)
                similarity = vector.inner(comparedVector) / (Math.sqrt(vector.inner(vector)) * Math.sqrt(comparedVector.inner(comparedVector)));
                break;
            case "msd":
                similarity = Sims.msd(vectorList, comparedVectorList);
                break;
            case "cpc":
                similarity = Sims.cpc(vectorList, comparedVectorList, (minRate + maxRate) / 2.0);
                break;
            case "exjaccard":
                similarity = Sims.exJaccard(vectorList, comparedVectorList);
                break;
            case "pcc":
            default:
                similarity = Sims.pcc(vectorList, comparedVectorList);
                break;
        }

        // shrink to account for vector size
        if (!Double.isNaN(similarity)) {
            int size = vectorList.size();
            int shrinkage = conf.getInt("num.shrinkage");
            if (shrinkage > 0)
                similarity *= size / (size + shrinkage + 0.0);
        }

        return similarity;
    }
}

