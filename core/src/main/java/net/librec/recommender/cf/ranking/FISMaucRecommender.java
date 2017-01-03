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
package net.librec.recommender.cf.ranking;

import com.google.common.cache.LoadingCache;
import net.librec.annotation.ModelData;
import net.librec.common.LibrecException;
import net.librec.math.algorithm.Randoms;
import net.librec.math.structure.DenseMatrix;
import net.librec.math.structure.DenseVector;
import net.librec.math.structure.SparseVector;
import net.librec.math.structure.VectorEntry;
import net.librec.recommender.MatrixFactorizationRecommender;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Kabbur et al., <strong>FISM: Factored Item Similarity Models for Top-N Recommender Systems</strong>, KDD 2013.
 */
@ModelData({"isRanking", "fismauc", "P", "Q", "itemBiases"})
public class FISMaucRecommender extends MatrixFactorizationRecommender {
    private int rho;
    private float alpha;

    /**
     * bias regularization
     */
    private double regBias;

    /**
     * items biases vector
     */
    private DenseVector itemBiases;

    /**
     * another item factors
     */
    private DenseMatrix anotherItemFactors;


    /**
     * user-items cache, item-users cache
     */
    protected LoadingCache<Integer, List<Integer>> userItemsCache;

    /**
     * Guava cache configuration
     */
    protected static String cacheSpec;

    @Override
    protected void setup() throws LibrecException {
        super.setup();

        itemBiases = new DenseVector(numItems);
        itemBiases.init();

        anotherItemFactors = new DenseMatrix(numItems, numFactors);
        anotherItemFactors.init(initMean, initStd);

        isRanking = true;
        rho = conf.getInt("rec.fismauc.rho");
        alpha = conf.getFloat("rec.fismauc.alpha");
        regBias = 0.1f;

        cacheSpec = conf.get("guava.cache.spec", "maximumSize=200,expireAfterAccess=2m");
        userItemsCache = trainMatrix.rowColumnsCache(cacheSpec);
    }

    @Override
    protected void trainModel() throws LibrecException {

        for (int iter = 1; iter <= numIterations; iter++) {

            loss = 0.0d;

            for (int u : trainMatrix.rows()) {
                SparseVector Ru = trainMatrix.row(u);
                int[] ratedItems = Ru.getIndex();

                for (VectorEntry ve : Ru) {
                    int i = ve.index();
                    double rui = ve.get();

                    List<Integer> js = new ArrayList<>();
                    int len = 0;
                    while (len < rho) {
                        int j = Randoms.uniform(numItems);
                        if (Ru.contains(j) || js.contains(j)) {
                            continue;
                        }

                        js.add(j);
                        len++;
                    }

                    double wu = Ru.getCount() - 1 > 0 ? Math.pow(Ru.getCount() - 1, -alpha) : 0;
                    double[] x = new double[numFactors];

                    for (int j : js) {
                        double sum_i = 0, sum_j = 0;
                        for (int k : ratedItems) {
                            if (i != k) {
                                sum_i += DenseMatrix.rowMult(anotherItemFactors, k, itemFactors, i);
                            }

                            sum_j += DenseMatrix.rowMult(anotherItemFactors, k, itemFactors, j);
                        }

                        double bi = itemBiases.get(i), bj = itemBiases.get(j);

                        double pui = bi + wu * sum_i;
                        double puj = bj + Math.pow(Ru.getCount(), -alpha) * sum_j;
                        double ruj = 0;

                        double eij = (rui - ruj) - (pui - puj);

                        loss += eij * eij;

                        // update bi
                        itemBiases.add(i, learnRate * (eij - regBias * bi));

                        // update bj
                        itemBiases.add(j, -learnRate * (eij - regBias * bj));

                        loss += regBias * bi * bi - regBias * bj * bj;

                        // update qif, qjf
                        for (int f = 0; f < numFactors; f++) {
                            double qif = itemFactors.get(i, f), qjf = itemFactors.get(j, f);

                            double sum_k = 0;
                            for (int k : ratedItems) {
                                if (k != i) {
                                    sum_k += anotherItemFactors.get(k, f);
                                }
                            }

                            double delta_i = eij * wu * sum_k - regItem * qif;
                            itemFactors.add(i, f, learnRate * delta_i);

                            double delta_j = eij * wu * sum_k - regItem * qjf;
                            itemFactors.add(j, f, -learnRate * delta_j);

                            x[f] += eij * (qif - qjf);

                            loss += regItem * qif * qif - regItem * qjf * qjf;
                        }
                    }

                    // update for each rated item
                    for (int j : ratedItems) {
                        if (j != i) {
                            for (int f = 0; f < numFactors; f++) {
                                double pjf = anotherItemFactors.get(j, f);
                                double delta = wu * x[f] / rho - regItem * pjf;

                                anotherItemFactors.add(j, f, learnRate * delta);

                                loss += regItem * pjf * pjf;
                            }
                        }
                    }
                }
            }

            loss *= 0.5d;
            if (isConverged(iter) && earlyStop) {
                break;
            }
            updateLRate(iter);
        }
    }

    @Override
    protected double predict(int userIdx, int itemIdx) throws LibrecException {

        double sum = 0;
        int count = 0;

        List<Integer> ratedItems = null;

        try {
            ratedItems = userItemsCache.get(userIdx);
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        for (int ratedItemIdx : ratedItems) {
            // for test, i and j will be always unequal as j is unrated
            if (itemIdx != ratedItemIdx) {
                sum += DenseMatrix.rowMult(anotherItemFactors, ratedItemIdx, itemFactors, itemIdx);
                count++;
            }
        }

        double wu = count > 0 ? Math.pow(count, -alpha) : 0;

        return itemBiases.get(itemIdx) + wu * sum;
    }
}
