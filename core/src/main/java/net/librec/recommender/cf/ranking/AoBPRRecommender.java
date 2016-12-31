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
import net.librec.math.algorithm.Stats;
import net.librec.math.structure.DenseVector;
import net.librec.math.structure.MatrixEntry;
import net.librec.math.structure.SparseMatrix;
import net.librec.recommender.MatrixFactorizationRecommender;
import net.librec.recommender.item.RecommendedItemList;
import net.librec.util.Lists;

import java.util.*;

import static net.librec.math.algorithm.Maths.logistic;

/**
 * AoBPR: BPR with Adaptive Oversampling<br>
 * <p>
 * Rendle and Freudenthaler, <strong>Improving pairwise learning for item recommendation from implicit
 * feedback</strong>, WSDM 2014.
 *
 * @author guoguibing and Keqiang Wang
 */
@ModelData({"isRanking", "aobpr", "userFactors", "itemFactors"})
public class AoBPRRecommender extends MatrixFactorizationRecommender {
    private int loopNumber;

    /**
     * item geometric distribution parameter
     */
    private int lambdaItem;

    private double[] var;
    private int[][] factorRanking;
    private double[] RankingPro;

    private List<Set<Integer>> userItemsSet;

    @Override
    protected void setup() throws LibrecException {
        super.setup();
        //set for this alg
        lambdaItem = (int) (conf.getFloat("rec.item.distribution.parameter") * numItems);
        //lamda_Item=500;
        loopNumber = (int) (numItems * Math.log(numItems));

        var = new double[numFactors];
        factorRanking = new int[numFactors][numItems];

        RankingPro = new double[numItems];
        double sum = 0;
        for (int i = 0; i < numItems; i++) {
            RankingPro[i] = Math.exp(-(i + 1) / lambdaItem);
            sum += RankingPro[i];
        }
        for (int i = 0; i < numItems; i++) {
            RankingPro[i] /= sum;
        }
        recommendedList = new RecommendedItemList(numUsers);
    }

    @Override
    protected void trainModel() throws LibrecException {
        userItemsSet = getUserItemsSet(trainMatrix);
        List<Integer>[] dataLists = getTrainList(trainMatrix);
        List<Integer> userTrainList = dataLists[0];
        List<Integer> itemTrainList = dataLists[1];
        int countIter = 0;

        for (int iter = 1; iter <= numIterations; iter++) {

            loss = 0.0d;
            for (int s = 0, smax = numUsers * 100; s < smax; s++) {
                //update Ranking every |I|log|I|
                if (countIter % loopNumber == 0) {
                    updateRankingInFactor();
                    countIter = 0;
                }
                countIter++;

                // randomly draw (u, i, j)
                int userIdx, posItemIdx, negItemIdx;
                while (true) {
                    int dataIdx = Randoms.uniform(numRates);
                    userIdx = userTrainList.get(dataIdx);
                    Set<Integer> itemSet = userItemsSet.get(userIdx);
                    if (itemSet.size() == 0 || itemSet.size() == numItems)
                        continue;

                    posItemIdx = itemTrainList.get(dataIdx);

                    do {
                        //randoms get a r by exp(-r/lamda)
                        int randomNegItemIndex = 0;
                        do {
                            randomNegItemIndex = Randoms.discrete(RankingPro);
                        } while (randomNegItemIndex > numItems);

                        //randoms get a f by p(f|c)
                        double[] pfc = new double[numFactors];
                        double sumfc = 0;
                        for (int pfcFactprIdx = 0; pfcFactprIdx < numFactors; pfcFactprIdx++) {
                            double tempAbsValue = Math.abs(userFactors.get(userIdx, pfcFactprIdx));
                            sumfc += tempAbsValue * var[pfcFactprIdx];
                            pfc[pfcFactprIdx] = tempAbsValue * var[pfcFactprIdx];
                        }
                        //normalization
                        for (int pfcFactprIdx = 0; pfcFactprIdx < numFactors; pfcFactprIdx++) {
                            pfc[pfcFactprIdx] /= sumfc;
                        }
                        int factorIdx = Randoms.discrete(pfc);

                        //get the r-1 in f item
                        if (userFactors.get(userIdx, factorIdx) > 0) {
                            negItemIdx = factorRanking[factorIdx][randomNegItemIndex];
                        } else {
                            negItemIdx = factorRanking[factorIdx][numItems - randomNegItemIndex - 1];
                        }
                    } while (itemSet.contains(negItemIdx));

                    break;
                }

                // update parameters
                double posPredictRating = predict(userIdx, posItemIdx);
                double negPredictRating = predict(userIdx, negItemIdx);
                double diffValue = posPredictRating - negPredictRating;

                double lossValue = -Math.log(Maths.logistic(diffValue));
                loss += lossValue;

                double deriValue = logistic(-diffValue);

                for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                    double userFactorValue = userFactors.get(userIdx, factorIdx);
                    double posItemFactorValue = itemFactors.get(posItemIdx, factorIdx);
                    double negItemFactorValue = itemFactors.get(negItemIdx, factorIdx);

                    userFactors.add(userIdx, factorIdx, learnRate * (deriValue * (posItemFactorValue - negItemFactorValue) - regUser * userFactorValue));
                    itemFactors.add(posItemIdx, factorIdx, learnRate * (deriValue * userFactorValue - regItem * posItemFactorValue));
                    itemFactors.add(negItemIdx, factorIdx, learnRate * (deriValue * (-userFactorValue) - regItem * negItemFactorValue));

                    loss += regUser * userFactorValue * userFactorValue + regItem * posItemFactorValue * posItemFactorValue + regItem * negItemFactorValue * negItemFactorValue;
                }
            }

            if (isConverged(iter) && earlyStop) {
                break;
            }
            updateLRate(iter);
        }
    }


    public List<Map.Entry<Integer, Double>> sortByDenseVectorValue(DenseVector vector) {
        List<Map.Entry<Integer, Double>> sortList = new ArrayList<>();
        for (int itemIdx = 0, length = vector.getData().length; itemIdx < length; itemIdx++) {
            sortList.add(new AbstractMap.SimpleImmutableEntry(itemIdx, vector.get(itemIdx)));
        }
        Lists.sortList(sortList, true);
        return sortList;
    }

    public void updateRankingInFactor() {
        //echo for each factors
        for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
            DenseVector factorVector = itemFactors.column(factorIdx).clone();
            List<Map.Entry<Integer, Double>> sort = sortByDenseVectorValue(factorVector);
            double[] valueList = new double[numItems];
            for (int itemIdx = 0; itemIdx < numItems; itemIdx++) {
                factorRanking[factorIdx][itemIdx] = sort.get(itemIdx).getKey();
                valueList[itemIdx] = sort.get(itemIdx).getValue();
            }
            //get
            var[factorIdx] = Stats.var(valueList);
        }
    }

    private List<Set<Integer>> getUserItemsSet(SparseMatrix sparseMatrix) {
        List<Set<Integer>> userItemsSet = new ArrayList<>();
        for (int userIdx = 0; userIdx < numUsers; ++userIdx) {
            userItemsSet.add(new HashSet(sparseMatrix.getColumns(userIdx)));
        }
        return userItemsSet;
    }

    private List<Integer>[] getTrainList(SparseMatrix sparseMatrix) {
        List<Integer> userTrainList = new ArrayList<>(), itemTrainList = new ArrayList<>();
        for (MatrixEntry matrixEntry : sparseMatrix) {
            int userIdx = matrixEntry.row();
            int itemIdx = matrixEntry.column();

            userTrainList.add(userIdx);
            itemTrainList.add(itemIdx);
        }
        return new List[]{userTrainList, itemTrainList};
    }
}
