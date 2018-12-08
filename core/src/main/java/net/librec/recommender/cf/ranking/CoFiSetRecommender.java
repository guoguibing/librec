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

import net.librec.annotation.ModelData;
import net.librec.common.LibrecException;
import net.librec.math.algorithm.Maths;
import net.librec.math.algorithm.Randoms;
import net.librec.math.structure.DenseVector;
import net.librec.math.structure.VectorBasedDenseVector;
import net.librec.recommender.MatrixFactorizationRecommender;
import org.apache.commons.lang.ArrayUtils;

import java.util.*;

/**
 * Weike Pan, Li Chen, <strong>CoFiSet: Collaborative Filtering via Learning Pairwise Preferences over Item-sets</strong>,
 * SIAM 2013.
 *
 * @author SunYatong
 */
@ModelData({"isRanking", "cofiset", "userFactors", "itemFactors", "itemBiases"})
public class CoFiSetRecommender extends MatrixFactorizationRecommender {

    /**
     * bias regularization
     */
    protected double regBias;

    /**
     * item biases
     */
    protected VectorBasedDenseVector itemBiases;

    /**
     * presence set sample cardinality
     */
    protected int pSetSize;

    /**
     * absence set sample cardinality
     */
    protected int aSetSize;

    private List<Set<Integer>> userItemsSet;

    @Override
    protected void setup() throws LibrecException {
        super.setup();
        pSetSize = conf.getInt("rec.presence.size", 1);
        aSetSize = conf.getInt("rec.absence.size", 1);
        regBias = conf.getDouble("rec.bias.regularization", 0.01);
        itemBiases = new VectorBasedDenseVector(numItems);

        itemBiases.init(initMean, initStd);

        userItemsSet = new ArrayList<>();
        for (int userIdx = 0; userIdx < numUsers; ++userIdx) {
            int[] itemIndexes = trainMatrix.row(userIdx).getIndices();
            Integer[] inputBoxed = ArrayUtils.toObject(itemIndexes);
            List<Integer> itemList = Arrays.asList(inputBoxed);
            userItemsSet.add(new HashSet(itemList));
        }
    }

    @Override
    protected void trainModel() throws LibrecException {
        for (int iter = 1; iter <= numIterations; iter++) {
            loss = 0.0d;
            for (int iter_rand = 0; iter_rand < numUsers; iter_rand++) {
                // randomly sample a user
                int userIdx = Randoms.uniform(numUsers);
                // List<Integer> allPresenceItems = trainMatrix.getColumns(userIdx);
                int[] itemIndexes = trainMatrix.row(userIdx).getIndices();
                Integer[] inputBoxed = ArrayUtils.toObject(itemIndexes);
                List<Integer> allPresenceItems = Arrays.asList(inputBoxed);
                if (allPresenceItems.size() < pSetSize || allPresenceItems.size() > numItems-aSetSize)
                    continue;

                // randomly sample absence items
                HashSet<Integer> absenceItemSet = new HashSet<Integer>();
                while(absenceItemSet.size() < aSetSize) {
                    int absenceItemIdx = Randoms.uniform(numItems);
                    if ((!absenceItemSet.contains(absenceItemIdx)) && !allPresenceItems.contains(absenceItemIdx)) {
                        absenceItemSet.add(absenceItemIdx);
                    }
                }

                // randomly sample presence items
                int allPresenceItemNum = allPresenceItems.size();
                HashSet<Integer> presenceItemSet = new HashSet<Integer>();
                while(presenceItemSet.size() < pSetSize) {
                    int presenceItemIdx = allPresenceItems.get(Randoms.uniform(allPresenceItemNum));
                    if ((!presenceItemSet.contains(presenceItemIdx))) {
                        presenceItemSet.add(presenceItemIdx);
                    }
                }

                // calcualte r_uP
                double r_uP = 0.0;
                double[] v_P = new double[numFactors];
                for (int pItemIdx: presenceItemSet) {
                    r_uP += predict(userIdx, pItemIdx);
                    DenseVector itemVec = itemFactors.row(pItemIdx);
                    for (int d=0; d<numFactors; d++) {
                        v_P[d] += itemVec.get(d) / pSetSize;
                    }
                }
                r_uP = r_uP / pSetSize;

                // calculate r_uA
                double[] v_A = new double[numFactors];
                double r_uA = 0.0;
                for (int aItemIdx: absenceItemSet) {
                    r_uA += predict(userIdx, aItemIdx);
                    DenseVector itemVec = itemFactors.row(aItemIdx);
                    for (int d=0; d<numFactors; d++) {
                        v_A[d] += itemVec.get(d) / aSetSize;
                    }
                }
                r_uA = r_uA / aSetSize;

                // calculate gradients
                double loss_uAP = - Maths.logistic(r_uA - r_uP);
                loss += -Math.log(Maths.logistic(r_uP - r_uA));

                // update user factor
                for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                    double oldFactorValue = userFactors.get(userIdx, factorIdx);
                    double grad_U_u_f = loss_uAP * (v_P[factorIdx]-v_A[factorIdx]) + regUser * oldFactorValue;
                    userFactors.plus(userIdx, factorIdx, - learnRate * grad_U_u_f);
                    loss += regUser * oldFactorValue * oldFactorValue;
                }

                // update p_Items' factors and biases
                for (int pItemIdx : presenceItemSet) {
                    for (int factorIdx = 0; factorIdx < numFactors; factorIdx++){
                        double oldFactorValue = itemFactors.get(pItemIdx, factorIdx);
                        double grad_V_i_f = loss_uAP * (userFactors.get(userIdx, factorIdx)/pSetSize) + regItem * oldFactorValue;
                        itemFactors.plus(pItemIdx, factorIdx, - learnRate * grad_V_i_f);
                        loss += regItem * oldFactorValue * oldFactorValue;
                    }

                    double oldBiasValue = itemBiases.get(pItemIdx);
                    double grad_biasV_i = loss_uAP/pSetSize + regBias * oldBiasValue;
                    itemBiases.plus(pItemIdx, - learnRate * grad_biasV_i);
                    loss += regBias * oldBiasValue * oldBiasValue;
                }

                // update a_Items' factors and biases
                for (int aItemIdx : absenceItemSet) {
                    for (int factorIdx = 0; factorIdx < numFactors; factorIdx++){
                        double oldFactorValue = itemFactors.get(aItemIdx, factorIdx);
                        double grad_V_j_f = loss_uAP * (-userFactors.get(userIdx, factorIdx)/absenceItemSet.size()) + regItem * oldFactorValue;
                        itemFactors.set(aItemIdx, factorIdx, - learnRate * grad_V_j_f);
                        loss += regItem * oldFactorValue * oldFactorValue;
                    }

                    double oldBiasValue = itemBiases.get(aItemIdx);
                    double grad_biasV_j = -loss_uAP/aSetSize + regBias * oldBiasValue;
                    itemBiases.plus(aItemIdx, - learnRate * grad_biasV_j);
                    loss += regBias * oldBiasValue * oldBiasValue;
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
        return super.predict(userIdx, itemIdx) + itemBiases.get(itemIdx);
    }
}
