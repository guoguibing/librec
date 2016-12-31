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
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;
import net.librec.annotation.ModelData;
import net.librec.common.LibrecException;
import net.librec.math.algorithm.Randoms;
import net.librec.math.structure.DenseMatrix;
import net.librec.math.structure.DenseVector;
import net.librec.math.structure.SparseVector;
import net.librec.math.structure.VectorEntry;
import net.librec.recommender.MatrixFactorizationRecommender;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Kabbur et al., <strong>FISM: Factored Item Similarity Models for Top-N Recommender Systems</strong>, KDD 2013.
 */
@ModelData({"isRanking", "fismrmse", "P", "Q", "itemBiases", "userBiases"})
public class FISMrmseRecommender extends MatrixFactorizationRecommender {

    private int rho;
    private float alpha;
    private double learnRate;
    private int trainMatrixSize;

    /**
     * bias regularization
     */
    private double regBias;

    /**
     * items and users biases vector
     */
    private DenseVector itemBiases, userBiases;

    /**
     * two low-rank item matrices, an item-item similarity was learned as a product of these two matrices
     */
    private DenseMatrix P, Q;

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

        P = new DenseMatrix(numItems, numFactors);
        Q = new DenseMatrix(numItems, numFactors);
        P.init();
        Q.init();

        itemBiases = new DenseVector(numItems);
        userBiases = new DenseVector(numUsers);
        itemBiases.init();
        userBiases.init();

        trainMatrixSize = trainMatrix.size();
        rho = conf.getInt("rec.fismrmse.rho");
        alpha = conf.getFloat("rec.fismrmse.alpha");
        regBias = 0.1f;

        cacheSpec = conf.get("guava.cache.spec", "maximumSize=200,expireAfterAccess=2m");
        userItemsCache = trainMatrix.rowColumnsCache(cacheSpec);
    }

    @Override
    protected void trainModel() throws LibrecException {
        int sampleSize = rho * trainMatrixSize;
        int totalSize = numUsers * numItems;

        for (int iter = 1; iter <= numIterations; iter++) {

            double loss = 0;

            // temporal data
            DenseMatrix PS = new DenseMatrix(numItems, numFactors);
            DenseMatrix QS = new DenseMatrix(numItems, numFactors);

            // new training data by sampling negative values
            Table<Integer, Integer, Double> R = trainMatrix.getDataTable();

            // make a random sample of negative feedback (total - nnz)
            List<Integer> indices = null;
            try {
                indices = Randoms.randInts(sampleSize, 0, totalSize - trainMatrixSize);
            } catch (Exception e) {
                e.printStackTrace();
            }

            int index = 0, count = 0;
            boolean isDone = false;
            for (int u = 0; u < numUsers; u++) {
                for (int j = 0; j < numItems; j++) {
                    double ruj = trainMatrix.get(u, j);
                    if (ruj != 0)
                        continue; // rated items

                    if (count++ == indices.get(index)) {
                        R.put(u, j, 0.0);
                        index++;
                        if (index >= indices.size()) {
                            isDone = true;
                            break;
                        }
                    }
                }
                if (isDone)
                    break;
            }

            // update throughout each user-item-rating (u, j, ruj) cell
            for (Cell<Integer, Integer, Double> cell : R.cellSet()) {
                int u = cell.getRowKey();
                int j = cell.getColumnKey();
                double ruj = cell.getValue();

                // for efficiency, use the below code to predict ruj instead of
                // simply using "predict(u,j)"
                SparseVector Ru = trainMatrix.row(u);
                double bu = userBiases.get(u), bj = itemBiases.get(j);

                double sum_ij = 0;
                int cnt = 0;
                for (VectorEntry ve : Ru) {
                    int i = ve.index();
                    // for training, i and j should be equal as j may be rated
                    // or unrated
                    if (i != j) {
                        sum_ij += DenseMatrix.rowMult(P, i, Q, j);
                        cnt++;
                    }
                }

                double wu = cnt > 0 ? Math.pow(cnt, -alpha) : 0;
                double puj = bu + bj + wu * sum_ij;

                double euj = puj - ruj;

                loss += euj * euj;

                // update bu
                userBiases.add(u, -learnRate * (euj + regBias * bu));

                // update bj
                itemBiases.add(j, -learnRate * (euj + regBias * bj));

                loss += regBias * bu * bu + regBias * bj * bj;

                // update qjf
                for (int f = 0; f < numFactors; f++) {
                    double qjf = Q.get(j, f);

                    double sum_i = 0;
                    for (VectorEntry ve : Ru) {
                        int i = ve.index();
                        if (i != j) {
                            sum_i += P.get(i, f);
                        }
                    }

                    double delta = euj * wu * sum_i + regItem * qjf;
                    QS.add(j, f, -learnRate * delta);

                    loss += regItem * qjf * qjf;
                }

                // update pif
                for (VectorEntry ve : Ru) {
                    int i = ve.index();
                    if (i != j) {
                        for (int f = 0; f < numFactors; f++) {
                            double pif = P.get(i, f);
                            double delta = euj * wu * Q.get(j, f) + regItem * pif;
                            PS.add(i, f, -learnRate * delta);

                            loss += regItem * pif * pif;
                        }
                    }
                }
            }

            P = P.add(PS);
            Q = Q.add(QS);

            loss *= 0.5;

//            if (isConverged(iter)  && earlyStop)
//                break;
        }
    }

    @Override
    protected double predict(int u, int j) throws LibrecException {
        double pred = userBiases.get(u) + itemBiases.get(j);

        double sum = 0;
        int count = 0;

        List<Integer> ratedItems = null;

        try {
            ratedItems = userItemsCache.get(u);
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        for (int i : ratedItems) {
            // for test, i and j will be always unequal as j is unrated
            if (i != j) {
                sum += DenseMatrix.rowMult(P, i, Q, j);
                count++;
            }
        }

        double wu = count > 0 ? Math.pow(count, -alpha) : 0;

        return pred + wu * sum;
    }

}
