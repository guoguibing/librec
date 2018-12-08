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

package net.librec.recommender.context.ranking;

import net.librec.annotation.ModelData;
import net.librec.common.LibrecException;
import net.librec.math.algorithm.Maths;
import net.librec.math.algorithm.Randoms;
import net.librec.math.structure.*;
import net.librec.math.structure.Vector;
import net.librec.recommender.FactorizationMachineRecommender;

import java.util.*;
import java.util.stream.Collectors;

/**
 * DLambdaFM Recommender
 * YUAN et al., <strong>LambdaFM: Learning Optimal Ranking with Factorization Machines Using Lambda Surrogates</strong>
 * CIKM 2016.
 *
 * @author Fajie Yuan, Songlin Zhai and Yatong Sun
 */
@ModelData({"isRanking", "lambdafm", "userFactors", "itemFactors"})
public class DLambdaFMRecommender extends FactorizationMachineRecommender {

    public static double max=Integer.MAX_VALUE;
    private double rho;
    private int lossf;
    private double lRate;
    private HashMap<Integer, Integer> itemFeatureMapping;

    @Override
    protected void setup() throws LibrecException {
        super.setup();
        rho   = conf.getDouble("rec.recommender.rho", 0.1);
        lossf = conf.getInt("rec.recommender.lossf", 1);
        lRate = conf.getDouble("rec.iterator.learnRate", 0.1);

        itemFeatureMapping = new HashMap<>();
        for (TensorEntry te : trainTensor) {
            int[] entryKeys = te.keys();
            int itemIdx = entryKeys[1];
            int featureIdx = entryKeys[2];
            if (!itemFeatureMapping.containsKey(itemIdx)) {
                itemFeatureMapping.put(itemIdx, featureIdx);
            }
        }

        for (TensorEntry te : testTensor) {
            int[] entryKeys = te.keys();
            int itemIdx = entryKeys[1];
            int featureIdx = entryKeys[2];
            if (!itemFeatureMapping.containsKey(itemIdx)) {
                itemFeatureMapping.put(itemIdx, featureIdx);
            }
        }
    }

    @Override
    protected void trainModel() throws LibrecException {
        DenseVector grad = new VectorBasedDenseVector(p);
        DenseVector grad_visited = new VectorBasedDenseVector(p);
        SequentialSparseVector x_i;
        SequentialSparseVector x_j;
        for (int iter = 1; iter <= numIterations; iter++) {
            loss = 0;
            for (int s = 0, smax = numUsers * 300; s < smax; s++) {
                // sample user (u), positive item (i), negative  (j)
                int u = 0, i = 0, j = 0;
                while (true) {
                    u = Randoms.uniform(numUsers);
                    SequentialSparseVector pu = trainMatrix.row(u);
                    Set<Integer> u_items = Arrays.stream(pu.getIndices()).boxed().collect(Collectors.toSet());

                    if (pu.getNumEntries() == 0)
                        continue;
                    int[] is = pu.getIndices();
                    i = is[Randoms.uniform(is.length)];
                    do {
                        try {
                            j = ChooseNeg(10, u, pu);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    } while (u_items.contains(j));
                    break;
                }
                // build input vector of i, j

                int feature_i = itemFeatureMapping.get(i);
                int feature_j = itemFeatureMapping.get(j);

                x_i = tenserKeysToFeatureVector(new int[]{u, i, feature_i});
                x_j = tenserKeysToFeatureVector(new int[]{u, j, feature_j});

                int[] i_index_List = x_i.getIndices();
                int[] j_index_List = x_j.getIndices();

                double si = 1.0;
                double sj = 0.0;
                double sij = si-sj;

                double xui = 0.0;
                double xuj = 0.0;
                try {
                    xui = predict(x_i);
                    xuj = predict(x_j);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                DenseVector sum_pos = sum(x_i); // should be calculated by predict(x_i) to save time
                DenseVector sum_neg = sum(x_j); // should be calculated by predict(x_j)  to save time
                double xuij = xui - xuj;
                double Sij = sij > 0 ? 1: (sij == 0? 0: -1);
                double pij_real = 0.5 * (1 + Sij); //from ground truth
                double pij = Maths.logistic(xuij);

                double cmg = getGradMag(lossf, xuij);
                loss += -pij_real * Math.log(pij) - (1 - pij_real) * Math.log(1 - pij);


                for (int idx: i_index_List) {
                    grad.set(idx, 0);
                    grad_visited.set(idx, 0);
                }
                for (int idx : j_index_List) {
                    grad.set(idx, 0);
                    grad_visited.set(idx, 0);
                }
                for (Vector.VectorEntry ve : x_i) {
                    grad.plus(ve.index(), ve.get());
                }
                for (Vector.VectorEntry ve : x_j) {
                    grad.plus(ve.index(), -ve.get());
                }

                // Update the weight matrix for every positive and negative item
                for (int idx : i_index_List) {
                    if(grad_visited.get(idx) == 0) { //Update like bu, bi
                        W.plus(idx, lRate * (cmg * (grad.get(idx) ) - regW * W.get(idx)));
                        grad_visited.set(idx, 1);
                    }
                }
                for (int idx : j_index_List) {
                    if(grad_visited.get(idx) == 0) {//Update like bu, bj
                        W.plus(idx, lRate * (cmg * (grad.get(idx)) - regW * W.get(idx)));
                        grad_visited.set(idx, 1);
                    }
                }

                //Update v_ij
                for (int f = 0; f < numFactors; f++) {
                    for (int idx : i_index_List) {
                        grad.set(idx, 0);
                        grad_visited.set(idx, 0);
                    }
                    for (int idx : j_index_List) {
                        grad.set(idx, 0);
                        grad_visited.set(idx, 0);
                    }
                    for (Vector.VectorEntry ve : x_i) {
                        int idx = ve.index();
                        double value = ve.get();
                        grad.plus(idx,sum_pos.get(f) * value - V.get(idx, f) * value * value);
                    }
                    for (Vector.VectorEntry ve : x_j) {
                        int idx = ve.index();
                        double value = ve.get();
                        grad.plus(idx, -(sum_neg.get(f) * value - V.get(idx, f) * value * value));
                    }

                    for (int idx : i_index_List) {
                        if(grad_visited.get(idx) == 0) {
                            V.plus(idx, f, lRate * (cmg * grad.get(idx) -  regF * V.get(idx, f)));
                            grad_visited.set(idx, 1);
                        }
                    }
                    for (int idx : j_index_List) {
                        if(grad_visited.get(idx) == 0) {
                            V.plus(idx, f, lRate * (cmg * grad.get(idx) - regF * V.get(idx, f)));
                            grad_visited.set(idx, 1);
                        }
                    }
                }

            }
            if (isConverged(iter)) {
                break;
            }
            lastLoss = loss;
        }
    }

    private DenseVector sum(SequentialSparseVector x) {
        DenseVector sum = new VectorBasedDenseVector(numFactors);
        for (int f = 0; f < numFactors; f++) {
            double sum_f = 0;
            sum.set(f, 0);
            for (Vector.VectorEntry ve : x) {
                int idx = ve.index();
                double d = V.get(idx, f) * ve.get();
                sum_f += d;
                sum.set(f, sum_f);
            }
        }
        return sum;
    }
    /**
     * Randomly select 100 sample then score then rank then exp distribution
     * @param size
     * @param u
     * @return
     * @throws Exception
     */
    private int ChooseNeg(int size, int u, SequentialSparseVector pu) throws Exception {
        Set<Integer> u_items = Arrays.stream(pu.getIndices()).boxed().collect(Collectors.toSet());

        if (size > numItems) {
            throw new IllegalArgumentException();
        }
        final double[] RankingPro;
        RankingPro = new double[numItems];
        Arrays.fill(RankingPro, -100.0); //For comparision below, otherwise element =0 , some scores are negative
        RandomAccessSparseVector x_j;
        for (int i = 0; i < size; i++) {
            int j=0;
            do {
                j = Randoms.uniform(numItems);
            } while (u_items.contains(j));
            int feature_j = itemFeatureMapping.get(j);
            int[] featureKeys = new int[]{u, j, feature_j};

            RankingPro[j] = predict(featureKeys);
        }

        Integer[] iidRank = new Integer[numItems];
        for (int i = 0; i < numItems; i++)
            iidRank[i] = i;
        Arrays.sort(iidRank, new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return (RankingPro[o1] > RankingPro[o2] ? -1
                        : (RankingPro[o1] < RankingPro[o2] ? 1 : 0));
            }
        });
        double sum = 0.0;
        double[] iidRelativeRank = new double[numItems];
        for (int i = 0; i < size; ++i) {
            int iid = iidRank[i];// iidRank [2360, 1248, 626, 2385, 2543] means
            // item 2360 rank first
            iidRelativeRank[iid] = Math.exp(-(i + 1) / (size*rho));
            sum += iidRelativeRank[iid];
        }
        Map<Integer,Integer> map=new HashMap<Integer, Integer>();
        double[] iidRelativeRank_small=new double[size];
        int k=0;
        for (int i = 0; i < iidRelativeRank.length; i++) {
            if (iidRelativeRank[i] != 0) {// non-zero elements=cardinality
                iidRelativeRank[i] = iidRelativeRank[i] / sum;
                iidRelativeRank_small[k] = iidRelativeRank[i];
                map.put(k,  i);
                k++;
            }
        }
        int index= Randoms.discrete(iidRelativeRank_small);//It is quicker
        return map.get(index);
    }

    protected double getGradMag(int losstype, double xuij){
        double z=1.0;
        double cmg=0;
        switch (losstype) {
            case 0:// Hinge loss
                if (z * xuij <= 1)
                    cmg = z;
                break;
            case 1:// Rennie loss
                if (z * xuij <= 0)
                    cmg = -z;
                else if (z * xuij <= 1)
                    cmg = (1 - z * xuij) * (-z);
                else
                    cmg = 0;
                cmg = -cmg;
                break;
            case 2:// logistic loss, BPR
                cmg = Maths.logistic(-xuij);
                break;
            case 3:// Frank loss
                cmg = Math.sqrt(Maths.logistic(xuij)) / (1 + Math.exp(xuij));
                break;
            case 4:// Exponential loss
                cmg = Math.exp(-xuij);
                break;
            case 5://quadratically smoothed
                if ( xuij <= 1)
                    cmg = 0.5*(1-xuij);
                break;
            default:
                break;
        }
        return cmg;
    }

    protected double predict(int userIdx, int itemIdx) throws LibrecException {
        int featureIdx = itemFeatureMapping.get(itemIdx);
        return predict(new int[]{userIdx, itemIdx, featureIdx});
    }
}
