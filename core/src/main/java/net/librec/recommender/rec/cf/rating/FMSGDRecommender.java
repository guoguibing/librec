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

package net.librec.recommender.rec.cf.rating;

import com.google.common.cache.LoadingCache;
import net.librec.annotation.ModelData;
import net.librec.common.LibrecException;
import net.librec.math.algorithm.Randoms;
import net.librec.math.structure.MatrixEntry;
import net.librec.math.structure.SparseVector;
import net.librec.math.structure.VectorEntry;
import net.librec.recommender.rec.FactorizationMachineRecommender;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static net.librec.math.algorithm.Maths.g;

/**
 * Factorization Machine Recommender through  stochastic gradient descend
 *
 */

@ModelData({"isRanking", "fmsgd", "rateFMData"})
public class FMSGDRecommender extends FactorizationMachineRecommender {

    protected static String cacheSpec;
    // user-vector cache, item-vector cache
    protected LoadingCache<Integer, SparseVector> userCache, itemCache;

/**
 * Stochastic Gradient Descent with Square Loss and BPR Loss
 * Rendle, Steffen. "Factorization Machines" Proceedings of the 10th IEEE International Conference on Data Mining (ICDM 2010)opment in Information Retrieval. ACM, 2011.
 * Rendle, Steffen, "Factorization Machines with libFM." ACM Transactions on Intelligent Systems and Technology, 2012.
 *
 * @author Jiaxi Tang
 *
 */


    private HashMap<Integer, ArrayList<Integer>> posCache;
    private HashMap<Integer, ArrayList<Integer>> negCache;

    private double learnRate;

    @Override
    protected void setup() throws LibrecException {
        super.setup();
        learnRate = conf.getFloat("rec.iteration.learnrate", 0.01f);

        /** START: prepare positive and negative cache for ranking problems **/
        // NOTE: factorization machine for ranking will use posCache and negCache instead of trainMatrix
        if (isRanking){
            // initialization
            posCache = new HashMap<>();
            negCache = new HashMap<>();

            // construct cache
            for (MatrixEntry me: trainMatrix) {
                int row = me.row();
                double rate = me.get();

                int[] inds = getUserItemIndex(row);
                int u = inds[0];

                if (rate >= 1){  // positive
                    if (posCache.containsKey(u)){
                        posCache.get(u).add(row);
                    } else {
                        ArrayList<Integer> array = new ArrayList<>();
                        array.add(row);
                        posCache.put(u, array);
                    }
                }
                else{  // negative
                    if (negCache.containsKey(u)){
                        negCache.get(u).add(row);
                    } else {
                        ArrayList<Integer> array = new ArrayList<>();
                        array.add(row);
                        negCache.put(u, array);
                    }
                }
            }

            // put all of the negative instance in the test matrix for training, because evaluation only base on positive instance.
            for (MatrixEntry me: testMatrix){
                int row = me.row();
                double rate = me.get();
                int[] inds = getUserItemIndex(row);
                int u = inds[0];

                if (rate >= 1){  // positive
                    continue;
                }
                else{  // negative
                    if (negCache.containsKey(u)){
                        negCache.get(u).add(row);
                    } else {
                        ArrayList<Integer> array = new ArrayList<>();
                        array.add(row);
                        negCache.put(u, array);
                    }
                }
            }
        }
        /** END: prepare positive and negative cache for ranking problems **/
    }

    @Override
    protected void trainModel() throws LibrecException {
        if (isRanking) {
            userCache = rateFMData.rowCache(cacheSpec);
            buildRankingModel();
        } else {
            buildRatingModel();
        }
    }


    private void buildRatingModel() throws LibrecException {
        for (int iter = 0; iter < numIterations; iter ++){
            double loss = 0.0;

            for (MatrixEntry me: trainMatrix) {
                int row = me.row();
                SparseVector x = rateFMData.row(row);

                double rate = me.get();
                double pred = predict(x);

                double err = pred - rate;
                loss += err * err;
                double gradLoss = err;

                // global bias
                loss += regW0 * w0 * w0;

                double hW0 = 1;
                double gradW0 = gradLoss * hW0 + regW0 * w0;

                // update w0
                w0 +=  -learnRate * gradW0;

                // 1-way interactions
                for (int l = 0; l < p; l ++){
                    if (!x.contains(l))
                        continue;
                    double oldWl = W.get(l);
                    double hWl = x.get(l);
                    double gradWl = gradLoss * hWl + regW * oldWl;
                    W.add(l, -learnRate * gradWl);

                    loss += regW * oldWl * oldWl;

                    // 2-way interactions
                    for (int f = 0; f < k; f ++){
                        double oldVlf = V.get(l, f);
                        double hVlf = 0;
                        double xl = x.get(l);
                        for (int j = 0; j < p; j ++){
                            if (j != l && x.contains(j))
                                hVlf += xl * V.get(j, f) * x.get(j);
                        }
                        double gradVlf = gradLoss * hVlf + regF * oldVlf;
                        V.add(l, f, -learnRate * gradVlf);
                        loss += regF * oldVlf * oldVlf;
                    }
                }
            }

            loss *= 0.5;

//            if (isConverged(iter))
//                break;
        }
    }

    private void buildRankingModel() throws LibrecException {
        for (int iter = 1; iter <= numIterations; iter++) {

            double loss = 0.0;

            for (int s = 0, smax = numUsers * 100; s < smax; s++) {
                // randomly draw xi and xj
                int u = 0;
                SparseVector xi, xj;
                do {
                    xi = positiveSampling();
                    int[] inds = getUserItemIndex(xi);

                    u = inds[0];

                    xj = negativeSampling(u);
                } while (xj == null);

                // update parameters
                double xui = predict(xi);
                double xuj = predict(xj);
                double xuij = xui - xuj;

                double vals = -Math.log(g(xuij));
                loss += vals;

                double dloss_xuij = g(xuij) - 1;

                // global bias: no need to update w0, cause dxuij_w0 = 0

                // 1-way interactions
                for (int l = 0; l < p; l ++){
                    if (!(xi.contains(l) || xj.contains(l)))
                        continue;

                    double dxuij_wl = 0;

                    if (xi.contains(l))
                        dxuij_wl = xi.get(l);
                    if (xj.contains(l))
                        dxuij_wl -= xj.get(l);

                    double oldWl = W.get(l);
                    double gradWl = dloss_xuij * dxuij_wl  + regW * oldWl;

                    W.add(l, -learnRate * gradWl);
                    loss += regW * oldWl * oldWl;


                    // 2-way interactions
                    /**
                     * Parameter optimized by calculating cache Q[1], using Q can compute gradient more efficient.
                     * Similar method also used in [2].
                     * reference:
                     * [1]. Rendle, Steffen, et al. "Fast context-aware recommendations with factorization machines." Proceedings of the 34th international ACM SIGIR conference on Research and development in Information Retrieval. ACM, 2011.
                     * [2]. https://github.com/ibayer/fastFM-core/blob/master/src/ffm_sgd.c
                     */
                    for (int f = 0; f < k; f ++) {

                        // 1. compute cache Qif for xui and xuj
                        double qxi = 0;
                        for (VectorEntry ve: xi){
                            int ind = ve.index();
                            double x_val = ve.get();
                            qxi += V.get(ind, f) * x_val;
                        }

                        double qxj = 0;
                        for (VectorEntry ve: xj){
                            int ind = ve.index();
                            double x_val = ve.get();
                            qxj += V.get(ind, f) * x_val;
                        }

                        // 2. update Vlf by gradient base on cache
                        double oldVlf = V.get(l, f);
                        double dxuij_vlf = 0;

                        if (xi.contains(l)) {
                            double x_val = xi.get(l);
                            dxuij_vlf = x_val * qxi - (x_val * x_val) * oldVlf;
                        }

                        if (xj.contains(l)) {
                            double x_val = xj.get(l);
                            dxuij_vlf -= x_val * qxj - (x_val * x_val) * oldVlf;
                        }

                        double gradVlf = dloss_xuij * dxuij_vlf + regF * oldVlf;
                        V.add(l, f, -learnRate * gradVlf);
                        loss += regF * oldVlf * oldVlf;
                    }
                }
            }

//            if (isConverged(iter))
//                break;
        }
    }

    /**
     * random sample a key-value from the posCache, and return the feature vector
     * @return random sampled positive feature vector
     * @throws Exception
     */
    private SparseVector positiveSampling() throws LibrecException {
        List<Integer> keys = new ArrayList<Integer>(posCache.keySet());
        int randomUser = keys.get(Randoms.uniform(keys.size()));
        ArrayList<Integer> values = posCache.get(randomUser);
        int randomRow = values.get(Randoms.uniform(values.size()));
        SparseVector userPositive = null;
        try {
            userPositive = userCache.get(randomRow);
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return userPositive;
    }

    /**
     *  random sample a key-value from the negCache based for a specific user, and return the feature vector
     * @param u given user
     * @return random sampled negative feature vector
     * @throws Exception
     */
    private SparseVector negativeSampling(int u) throws LibrecException{
        if (!negCache.containsKey(u)) {
            return null;
        }
        else {
            ArrayList<Integer> values = negCache.get(u);
            int randomRow = values.get(Randoms.uniform(values.size()));
            SparseVector userNegative = null;
            try {
                userNegative = userCache.get(randomRow);
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            return userNegative;
        }
    }

    protected double predict(int userIdx, int itemIdx) throws LibrecException {
        SparseVector x = rateFMData.row(userIdx);
        return predict(x);
    }
}
