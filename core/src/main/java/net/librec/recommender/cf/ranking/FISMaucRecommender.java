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
import net.librec.recommender.MatrixFactorizationRecommender;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Kabbur et al., <strong>FISM: Factored Item Similarity Models for Top-N Recommender Systems</strong>, KDD 2013.
 *
 * @author SunYatong
 */
@ModelData({"isRanking", "fismauc", "P", "Q", "itemBiases", "userBiases"})
public class FISMaucRecommender extends MatrixFactorizationRecommender {

    /**
     * Guava cache configuration
     */
    protected static String cacheSpec;
    /**
     * user-items cache, item-users cache
     */
    protected LoadingCache<Integer, List<Integer>> userItemsCache;
    /**
     * hyper-parameters
     */
    private float rho, alpha, beta, gamma;
    /**
     * learning rate
     */
    private double lRate;
    /**
     * items and users biases vector
     */
    private DenseVector itemBiases;
    /**
     * two low-rank item matrices, an item-item similarity was learned as a product of these two matrices
     */
    private DenseMatrix P, Q;

    @Override
    protected void setup() throws LibrecException {

        super.setup();

        P = new DenseMatrix(numItems, numFactors);
        Q = new DenseMatrix(numItems, numFactors);
        P.init(0, 0.01);
        Q.init(0, 0.01);
        itemBiases = new DenseVector(numItems);
        itemBiases.init(0, 0.01);
        rho = conf.getFloat("rec.recommender.rho");//3-15
        alpha = conf.getFloat("rec.recommender.alpha", 0.5f);
        beta = conf.getFloat("rec.recommender.beta", 0.6f);
        gamma = conf.getFloat("rec.recommender.gamma", 0.1f);
        lRate = conf.getDouble("rec.iteration.learnrate", 0.0001);
        cacheSpec = conf.get("guava.cache.spec", "maximumSize=200,expireAfterAccess=2m");

        userItemsCache = trainMatrix.rowColumnsCache(cacheSpec);
    }

    @Override
    protected void trainModel() throws LibrecException {

        for (int iter = 1; iter <= numIterations; iter++) {
            loss = 0;
            // for all u in C
            for (int u = 0; u < numUsers; u++) {
                SparseVector Ru = trainMatrix.row(u);
                int Ru_p_size = Ru.size();
                if (Ru_p_size == 0 || Ru_p_size == 1) {
                    Ru_p_size = 2;
                }
                // for all i in Ru+
                for (int i : Ru.getIndex()) {
                    // x <- 0
                    DenseVector x = new DenseVector(numFactors);
                    x.init(0);
                    // t <- (n - 1)^(-alpha) Σ pj    (j!=i)
                    DenseVector t = new DenseVector(numFactors);
                    t.init(0);
                    for (int j : Ru.getIndex()) {
                        if (i != j) {
                            t = t.add(P.row(j));
                        }
                    }
                    t = t.scale(Math.pow(Ru_p_size - 1, -alpha));
                    for (int tindex = 0; tindex < numFactors; tindex++) {
                        if (Double.isNaN(t.get(tindex))) {
                            System.out.println("user:" + u + ", item:" + i + ", Ru_p_size:" + Ru_p_size);
                        }
                    }
                    // Z <- SampleZeros(rho)
                    int sampleSize = (int) (rho * Ru_p_size);
                    // make a random sample of negative feedback for Ru-
                    List<Integer> negative_indices = null;
                    try {
                        negative_indices = Randoms.randInts(sampleSize, 0, numItems);
                        Iterator<Integer> iterator = negative_indices.iterator(); // Thanks for Zhaohua hong's fix
                        while(iterator.hasNext()){
                            int index = iterator.next();
                            if(Ru.get(index)>0.1){
                                iterator.remove();
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    // for all j in Z
                    for (int j : negative_indices) {
                        double bi = itemBiases.get(i);
                        double bj = itemBiases.get(j);

                        // update pui  puj  rui  ruj
                        double rui = Ru.get(i);
                        double pui = bi + Q.row(i).inner(t);
                        double puj = bj + Q.row(j).inner(t);
                        double ruj = 0.0;
                        double e = (rui - ruj) - (pui - puj);
                        loss += e * e;

                        // update bi  bj
                        itemBiases.add(i, lRate * (e - gamma * bi));
                        itemBiases.add(j, lRate * (e - gamma * bj));

                        // update qi qj
                        DenseVector delta_qi = t.scale(e).minus(Q.row(i).scale(beta));
                        DenseVector qi = Q.row(i).add(delta_qi.scale(lRate));
                        Q.setRow(i, qi);
                        DenseVector delta_qj = t.scale(e).minus(Q.row(j).scale(beta));
                        DenseVector qj = Q.row(j).minus(delta_qj.scale(lRate));
                        Q.setRow(j, qj);

                        // update x
                        x = x.add(qi.minus(qj).scale(e));
                    }
                    // for all j in Ru+\{i}
                    for (int j : Ru.getIndex()) {
                        if (j != i) {
                            // update pj
                            DenseVector delta_pj = x.scale(Math.pow(rho, -1) * Math.pow(Ru_p_size - 1, -alpha)).minus(P.row(j).scale(beta));
                            P.setRow(j, P.row(j).add(delta_pj.scale(lRate)));
                        }
                    }
                }
            }
            for (int i = 0; i < numItems; i++) {
                double bi = itemBiases.get(i);
                loss += gamma * bi * bi;
                loss += beta * Q.row(i).inner(Q.row(i));
                loss += beta * P.row(i).inner(P.row(i));
            }
            loss *= 0.5;
            if (isConverged(iter) && earlyStop) {
                break;
            }
            updateLRate(iter);
        }
    }

    @Override
    protected double predict(int u, int j) throws LibrecException {
        double pred = itemBiases.get(j);
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
        double wu = count - 1 > 0 ? Math.pow(count - 1, -alpha) : 0;
        return pred + wu * sum;
    }

}
