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
package net.librec.recommender.cf.rating;

import net.librec.common.LibrecException;
import net.librec.math.algorithm.Randoms;
import net.librec.recommender.MatrixFactorizationRecommender;
import net.librec.util.Lists;
import net.librec.util.ZeroSetter;

import java.util.Random;

/**
 * This class implementing user-oriented Restricted Boltzmann Machines for
 * Collaborative Filtering
 * <p>
 * The origin paper:
 * <p>
 * Salakhutdinov, R., Mnih, A. Hinton, G, Restricted BoltzmanMachines for
 * Collaborative Filtering, To appear inProceedings of the 24thInternational
 * Conference onMachine Learning 2007.
 * http://www.cs.toronto.edu/~rsalakhu/papers/rbmcf.pdf
 *
 * @author bin wu(Email:wubin@gs.zzu.edu.cn)
 */
public class RBMReccommender extends MatrixFactorizationRecommender {
    int featureNumber;
    int softmax;
    int maxIter;
    int tSteps;
    double epsilonw;
    double epsilonvb;
    double epsilonhb;
    double momentum;
    double lamtaw;
    double lamtab;
    double[][][] weights;
    double[][] visbiases;
    double[] hidbiases;

    double[][][] CDpos;
    double[][][] CDneg;
    double[][][] CDinc;

    double[] poshidact;
    double[] neghidact;
    char[] poshidstates;
    char[] neghidstates;
    double[] hidbiasinc;

    char[] curposhidstates;

    double[][] posvisact;
    double[][] negvisact;
    double[][] visbiasinc;
    double[][] negvisprobs;

    char[] negvissoftmax;
    int[] moviecount;
    String PredictionType;

    public RBMReccommender() {

    }

    protected void setup() throws LibrecException {
        super.setup();

        softmax = ratingScale.size();
        this.maxIter = numIterations;
        featureNumber = conf.getInt("-fn", 100);
        epsilonw = conf.getDouble("-ew", 0.001);
        epsilonvb = conf.getDouble("-evb", 0.001);
        epsilonhb = conf.getDouble("-ehb", 0.001);
        tSteps = conf.getInt("-t", 1);
        momentum = conf.getDouble("-m", 0.0d);
        lamtaw = conf.getDouble("-lw", 0.001);
        lamtab = conf.getDouble("-lb", 0.0d);
        PredictionType = conf.get("-p", "mean");
        weights = new double[numItems][softmax][featureNumber];
        visbiases = new double[numItems][softmax];
        hidbiases = new double[featureNumber];

        CDpos = new double[numItems][softmax][featureNumber];
        CDneg = new double[numItems][softmax][featureNumber];
        CDinc = new double[numItems][softmax][featureNumber];

        poshidact = new double[featureNumber];
        neghidact = new double[featureNumber];
        poshidstates = new char[featureNumber];
        neghidstates = new char[featureNumber];
        hidbiasinc = new double[featureNumber];

        curposhidstates = new char[featureNumber];

        posvisact = new double[numItems][softmax];
        negvisact = new double[numItems][softmax];
        visbiasinc = new double[numItems][softmax];
        negvisprobs = new double[numItems][softmax];

        negvissoftmax = new char[numItems];
        moviecount = new int[numItems];

        int[][] moviecount = new int[numItems][softmax];
        for (int u = 0; u < numUsers; u++) {
            int num = trainMatrix.rowSize(u);
            for (int j = 0; j < num; j++) {
                int m = trainMatrix.row(u).getIndex()[j];
                int r = (int) trainMatrix.get(u, m);
                moviecount[m][r]++;
            }
        }
        for (int i = 0; i < numItems; i++) {
            for (int j = 0; j < featureNumber; j++) {
                for (int k = 0; k < softmax; k++) {
                    weights[i][k][j] = Randoms.gaussian(0, 0.01);
                }
            }
        }
        ZeroSetter.zero(hidbiases, featureNumber);

        for (int i = 0; i < numItems; i++) {
            int mtot = 0;
            for (int k = 0; k < softmax; k++) {
                mtot += moviecount[i][k];
            }
            for (int k = 0; k < softmax; k++) {
                if (mtot == 0) {
                    visbiases[i][k] = new Random().nextDouble() * 0.001;
                } else {
                    visbiases[i][k] = Math.log(((double) moviecount[i][k]) / ((double) mtot));
                    // visbiases[i][k] = Math.log(((moviecount[i][k]) + 1) /
                    // (trainMatrix.columnSize(i)+ softmax));
                }
            }
        }
    }

    @Override
    protected void trainModel() throws LibrecException {
        int loopcount = 0;
        Random randn = new Random();
        while (loopcount < maxIter) {
            loopcount++;
            Zero();
            int[] visitingSeq = new int[numUsers];
            for (int i = 0; i < visitingSeq.length; i++) {
                visitingSeq[i] = i;
            }
            Lists.shaffle(visitingSeq);
            for (int p = 0; p < visitingSeq.length; p++) {
                int u = visitingSeq[p];
                int num = trainMatrix.rowSize(u);
                double[] sumW = new double[featureNumber];
                negvisprobs = new double[numItems][softmax];
                for (int i = 0; i < num; i++) {
                    int m = trainMatrix.row(u).getIndex()[i];
                    int r = (int) trainMatrix.get(u, m);
                    moviecount[m]++;
                    posvisact[m][r] += 1.0;

                    for (int h = 0; h < featureNumber; h++) {
                        sumW[h] += weights[m][r][h];
                    }
                }
                for (int h = 0; h < featureNumber; h++) {
                    double probs = 1.0 / (1.0 + Math.exp(-sumW[h] - hidbiases[h]));
                    if (probs > randn.nextDouble()) {
                        poshidstates[h] = 1;
                        poshidact[h] += 1.0;
                    } else {
                        poshidstates[h] = 0;
                    }
                }
                for (int h = 0; h < featureNumber; h++) {
                    curposhidstates[h] = poshidstates[h];
                }
                int stepT = 0;
                do {
                    boolean finalTStep = (stepT + 1 >= tSteps);

                    for (int i = 0; i < num; i++) {
                        int m = trainMatrix.row(u).getIndex()[i];

                        for (int h = 0; h < featureNumber; h++) {
                            if (curposhidstates[h] == 1) {
                                for (int r = 0; r < softmax; r++)
                                    negvisprobs[m][r] += weights[m][r][h];
                            }
                        }

                        for (int r = 0; r < softmax; r++)
                            negvisprobs[m][r] = 1. / (1 + Math.exp(-negvisprobs[m][r] - visbiases[m][r]));

                        double tsum = 0;
                        for (int r = 0; r < softmax; r++) {
                            tsum += negvisprobs[m][r];
                        }

                        if (tsum != 0) {
                            for (int r = 0; r < softmax; r++) {
                                negvisprobs[m][r] /= tsum;
                            }
                        }

                        double randval = randn.nextDouble();

                        if ((randval -= negvisprobs[m][0]) <= 0.0)
                            negvissoftmax[m] = 0;
                        else if ((randval -= negvisprobs[m][1]) <= 0.0)
                            negvissoftmax[m] = 1;
                        else if ((randval -= negvisprobs[m][2]) <= 0.0)
                            negvissoftmax[m] = 2;
                        else if ((randval -= negvisprobs[m][3]) <= 0.0)
                            negvissoftmax[m] = 3;
                        else
                            negvissoftmax[m] = 4;

                        if (finalTStep)
                            negvisact[m][negvissoftmax[m]] += 1.0;
                    }

                    ZeroSetter.zero(sumW, featureNumber);
                    for (int i = 0; i < num; i++) {
                        int m = trainMatrix.row(u).getIndex()[i];

                        for (int h = 0; h < featureNumber; h++) {
                            sumW[h] += weights[m][negvissoftmax[m]][h];
                        }
                    }

                    for (int h = 0; h < featureNumber; h++) {
                        double probs = 1.0 / (1.0 + Math.exp(-sumW[h] - hidbiases[h]));

                        if (probs > randn.nextDouble()) {
                            neghidstates[h] = 1;
                            if (finalTStep)
                                neghidact[h] += 1.0;
                        } else {
                            neghidstates[h] = 0;
                        }
                    }

                    if (!finalTStep) {
                        for (int h = 0; h < featureNumber; h++)
                            curposhidstates[h] = neghidstates[h];
                        ZeroSetter.zero(negvisprobs, numItems, softmax);
                    }

                } while (++stepT < tSteps);

                for (int i = 0; i < num; i++) {
                    int m = trainMatrix.row(u).getIndex()[i];
                    int r = (int) trainMatrix.get(u, m);

                    for (int h = 0; h < featureNumber; h++) {
                        if (poshidstates[h] == 1) {
                            CDpos[m][r][h] += 1.0;
                        }
                        CDneg[m][negvissoftmax[m]][h] += (double) neghidstates[h];
                    }
                }
                update(u, num);
            }
        }

    }

    private void update(int user, int num) {

        int bSize = 100;
        if (((user + 1) % bSize) == 0 || (user + 1) == numUsers) {
            int numcases = user % bSize;
            numcases++;
            for (int m = 0; m < numItems; m++) {

                if (moviecount[m] == 0)
                    continue;
                for (int h = 0; h < featureNumber; h++) {

                    for (int r = 0; r < softmax; r++) {
                        double CDp = CDpos[m][r][h];
                        double CDn = CDneg[m][r][h];
                        if (CDp != 0.0 || CDn != 0.0) {
                            CDp /= ((double) moviecount[m]);
                            CDn /= ((double) moviecount[m]);
                            CDinc[m][r][h] = momentum * CDinc[m][r][h]
                                    + epsilonw * ((CDp - CDn) - lamtaw * weights[m][r][h]);
                            weights[m][r][h] += CDinc[m][r][h];
                        }
                    }
                }
                for (int r = 0; r < softmax; r++) {
                    if (posvisact[m][r] != 0.0 || negvisact[m][r] != 0.0) {
                        posvisact[m][r] /= ((double) moviecount[m]);
                        negvisact[m][r] /= ((double) moviecount[m]);
                        visbiasinc[m][r] = momentum * visbiasinc[m][r]
                                + epsilonvb * (posvisact[m][r] - negvisact[m][r] - lamtab * visbiases[m][r]);
                        visbiases[m][r] += visbiasinc[m][r];
                    }
                }
            }
            for (int h = 0; h < featureNumber; h++) {
                if (poshidact[h] != 0.0 || neghidact[h] != 0.0) {
                    poshidact[h] /= ((double) (numcases));
                    neghidact[h] /= ((double) (numcases));
                    hidbiasinc[h] = momentum * hidbiasinc[h]
                            + epsilonhb * (poshidact[h] - neghidact[h] - lamtab * hidbiases[h]);
                    hidbiases[h] += hidbiasinc[h];
                }
            }
            Zero();
        }
    }

    private void Zero() {
        CDpos = new double[numItems][softmax][featureNumber];
        CDneg = new double[numItems][softmax][featureNumber];
        poshidact = new double[featureNumber];
        neghidact = new double[featureNumber];
        posvisact = new double[numItems][softmax];
        negvisact = new double[numItems][softmax];
        moviecount = new int[numItems];
    }

    protected double predict(int u, int m) throws LibrecException {
        double[][] negvisprobs = new double[numItems][softmax];
        double[] poshidprobs = new double[featureNumber];
        int trainNumber = trainMatrix.rowSize(u);
        double[] sumW = new double[featureNumber];
        for (int i = 0; i < trainNumber; i++) {
            int item = trainMatrix.row(u).getIndex()[i];
            int rate = (int) trainMatrix.get(u, item);

            for (int h = 0; h < featureNumber; h++) {
                sumW[h] += weights[item][rate][h];
            }
        }

        for (int h = 0; h < featureNumber; h++) {
            poshidprobs[h] = 1.0 / (1.0 + Math.exp(0 - sumW[h] - hidbiases[h]));
        }

        for (int i = 0; i < trainNumber; i++) {
            int item = trainMatrix.row(u).getIndex()[i];
            for (int h = 0; h < featureNumber; h++) {
                for (int r = 0; r < softmax; r++) {
                    negvisprobs[item][r] += poshidprobs[h] * weights[item][r][h];
                }
            }

            for (int r = 0; r < softmax; r++) {
                negvisprobs[item][r] = 1.0 / (1.0 + Math.exp(0 - negvisprobs[item][r] - visbiases[item][r]));
            }

            double tsum = 0;
            for (int r = 0; r < softmax; r++) {
                tsum += negvisprobs[item][r];
            }

            if (tsum != 0) {
                for (int r = 0; r < softmax; r++) {
                    negvisprobs[item][r] /= tsum;
                }
            }
        }
        double predict = 0;
        if (PredictionType.equals("max")) {

            int max_index = 0;
            double max_value = negvisprobs[m][0];
            for (int r = 0; r < softmax; r++) {
                if (negvisprobs[m][r] > max_value) {
                    max_value = negvisprobs[m][r];
                    max_index = r;
                }
            }
            predict = max_index + 1;
        } else if (PredictionType.equals("mean")) {
            double mean = 0.0;
            for (int r = 0; r < softmax; r++) {
                mean += negvisprobs[m][r] * (r + 1);
            }
            predict = mean;
        }
        return predict;
    }

}

enum PredictionType {
    MAX, MEAN
}
