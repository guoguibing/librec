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
package net.librec.recommender.context.rating;

import net.librec.common.LibrecException;
import net.librec.math.algorithm.Randoms;
import net.librec.math.structure.*;
import net.librec.math.structure.Vector.VectorEntry;
import net.librec.recommender.cf.rating.BiasedMFRecommender;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * TimeSVD++ Recommender
 *
 * @author Guo Guibing and Ma Chen
 */
public class TimeSVDRecommender extends BiasedMFRecommender {
    /**
     * the span of days of rating timestamps
     */
    private static int numDays;
    /**
     * minimum, maximum timestamp
     */
    private static long minTimestamp, maxTimestamp;
    /**
     * matrix of time stamp
     */
    private static SequentialAccessSparseMatrix instantMatrix;
    /**
     * {user, mean date}
     */
    private DenseVector userMeanDays;
    /**
     * number of bins over all the items
     */
    private int numSections;
    /**
     * {user, appender} alpha matrix
     */
    private DenseMatrix userImplicitFactors;
    /**
     * item's implicit influence
     */
    private DenseMatrix itemImplicitFactors;
    private DenseMatrix userExplicitFactors;
    private DenseMatrix itemExplicitFactors;
    /**
     * {item, bin(t)} bias matrix
     */
    private DenseMatrix itemSectionBiases;
    /**
     * {user, day, bias} table
     */
    private  DenseMatrix userDayBiases;
//    private Table<Integer, Integer, Double> userDayBiases;
    /**
     * user bias weight parameters
     */
    private DenseVector userBiasWeights;
    /**
     * {user, {appender, day, value} } map
     */
//    private Table<Integer, Integer, double[]> userDayFactors;
    private Map<Integer, double[]>[] userDayFactors;

    /**
     * {user, user scaling stable part}
     */
    private DenseVector userScales;
    /**
     * {user, day, day-specific scaling part}
     */
    private DenseMatrix userDayScales;

    private double beta = 0.4;

    private RowSequentialAccessSparseMatrix trainTimeMatrix, testTimeMatrix;

    /**
     * get the number of days for a given time difference
     *
     * @param duration the difference between two time stamps
     * @return number of days for a given time difference
     */
    private static int days(long duration) {
        return (int) TimeUnit.SECONDS.toDays(duration);
    }

    /**
     * get the number of days between two timestamps
     *
     * @param t1 time stamp 1
     * @param t2 time stamp 2
     * @return number of days between two timestamps
     */
    private static int days(long t1, long t2) {
        return days(Math.abs(t1 - t2));
    }

    /*
     * (non-Javadoc)
     *
     * @see net.librec.recommender.cf.rating.BiasedMFRecommender#setup()
     */
    protected void setup() throws LibrecException {
        super.setup();
        beta = conf.getDouble("rec.timesvd.beta", 0.1D);
        numSections = conf.getInt("rec.numBins", 20);
        instantMatrix = (SequentialAccessSparseMatrix) getDataModel().getDatetimeDataSet();
        getMaxAndMinTimeStamp();
        numDays = days(maxTimestamp, minTimestamp) + 1;

        userBiasWeights = new VectorBasedDenseVector(numUsers);
        userBiasWeights.init(initMean, initStd);

        itemSectionBiases = new DenseMatrix(numItems, numSections);
        itemSectionBiases.init(initMean, initStd);

        itemImplicitFactors = new DenseMatrix(numItems, numFactors);
        itemImplicitFactors.init(initMean, initStd);

        userImplicitFactors = new DenseMatrix(numUsers, numFactors);
        userImplicitFactors.init(initMean, initStd);

        userDayBiases = new DenseMatrix(numUsers, numDays);

        userDayFactors = new Map[numUsers];
        for(int userIndex=0; userIndex< numUsers;userIndex++){
            userDayFactors[userIndex] = new HashMap<>();
        }

        userScales = new VectorBasedDenseVector(numUsers);
        userScales.init(initMean, initStd);

        userDayScales = new DenseMatrix(numUsers, numDays);
        userDayScales.init(initMean, initStd);

        userExplicitFactors = new DenseMatrix(numUsers, numFactors);
        userExplicitFactors.init(initMean, initStd);

        itemExplicitFactors = new DenseMatrix(numItems, numFactors);
        itemExplicitFactors.init(initMean, initStd);

        // global average date
        double mean;
        double sum = 0D;
        int count = 0;
        trainTimeMatrix = new RowSequentialAccessSparseMatrix(trainMatrix, true);

        for (MatrixEntry matrixEntry : trainTimeMatrix) {
            int userIndex = matrixEntry.row();
            int itemIndex = matrixEntry.column();
            int tempDay = days((long) instantMatrix.get(userIndex, itemIndex), minTimestamp);
            matrixEntry.set(tempDay);
            sum += tempDay;
            double[] dayFactors = new double[numFactors];
            for (int factorIndex = 0; factorIndex < numFactors; factorIndex++) {
                dayFactors[factorIndex] = Randoms.uniform(initMean, initStd);
            }
            userDayFactors[userIndex].put(tempDay, dayFactors);
            userDayBiases.set(userIndex, tempDay, Randoms.uniform(initMean, initStd));
            count++;
        }

        double[] dayFactors = new double[numFactors];
        testTimeMatrix = new RowSequentialAccessSparseMatrix(testMatrix, true);

        for (MatrixEntry matrixEntry : testTimeMatrix) {
            int userIndex = matrixEntry.row();
            int itemIndex = matrixEntry.column();
            int tempDay = days((long) instantMatrix.get(userIndex, itemIndex), minTimestamp);
            matrixEntry.set(tempDay);
            if (!userDayFactors[userIndex].containsKey(tempDay)){
                userDayFactors[userIndex].put(tempDay, dayFactors);
            }
        }
        System.gc();

        double globalMeanDays = sum / count;
        // compute user's mean of rating timestamps
        userMeanDays = new VectorBasedDenseVector(numUsers);
        for (int userIndex = 0; userIndex < numUsers; userIndex++) {
            sum = 0D;
            SequentialSparseVector userVector = trainTimeMatrix.row(userIndex);
            for (VectorEntry vectorEntry: userVector) {
                sum += days((long) vectorEntry.get(), minTimestamp);
            }
            mean = (userVector.size() > 0) ? (sum + 0D) / userVector.size() : globalMeanDays;
            userMeanDays.set(userIndex, mean);
        }
    }

    protected void trainModel() throws LibrecException {
        DenseVector factorVector = new VectorBasedDenseVector(numFactors);

        for (int iterationStep = 1; iterationStep <= numIterations; iterationStep++) {
            loss = 0D;
            for (int userIndex = 0; userIndex < numUsers; userIndex++) {
                SequentialSparseVector rateVector = trainMatrix.row(userIndex);
                SequentialSparseVector timeVector = trainTimeMatrix.row(userIndex);
                int size = rateVector.size();
                if (size == 0) {
                    continue;
                }

                double[] step = new double[numFactors];

                for (VectorEntry vectorEntry : rateVector) {
                    factorVector.assign((index, value) -> itemImplicitFactors.row(vectorEntry.index()).get(index) + value);
                }
                double scale = Math.pow(size, -0.5);
                factorVector.assign((index, value) -> value * scale);

                for (VectorEntry vectorEntry : rateVector) {
                    int itemExplicitIndex = vectorEntry.index();
                    double rate = vectorEntry.get();
                    int days = (int) timeVector.getAtPosition(vectorEntry.position());
                    // day t
                    int section = section(days);
                    double deviation = deviation(userIndex, days);
                    double userBias = userBiases.get(userIndex);
                    double itemBias = itemBiases.get(itemExplicitIndex);

                    double userScale = userScales.get(userIndex);
                    double dayScale = userDayScales.get(userIndex, days);
                    // lazy initialization
                    double userDayBias = userDayBiases.get(userIndex, days);
                    double itemSectionBias = itemSectionBiases.get(itemExplicitIndex, section);
                    // alpha_u
                    double userWeight = userBiasWeights.get(userIndex);
                    // mu bi(t)
                    double predict = globalMean + (itemBias + itemSectionBias) * (userScale + dayScale);
                    // bu(t)
                    predict += userBias + userWeight * deviation + userDayBias;
                    // qi * yj
                    DenseVector itemExplicitVector = itemExplicitFactors.row(itemExplicitIndex);
                    double sum = factorVector.dot(itemExplicitVector);
                    predict += sum;
                    // qi * pu(t)
                    double[] dayFactors = userDayFactors[userIndex].get(days);
//
                    for (int factorIndex = 0; factorIndex < numFactors; factorIndex++) {
                        double qik = itemExplicitFactors.get(itemExplicitIndex, factorIndex);
                        double puk = userExplicitFactors.get(userIndex, factorIndex) + userImplicitFactors.get(userIndex, factorIndex) * deviation + dayFactors[factorIndex];
                        predict += puk * qik;
                    }
                    double error = predict - rate;
                    loss += error * error;

                    // update bi
                    double sgd = error * (userScale + dayScale) + regBias * itemBias;
                    itemBiases.plus(itemExplicitIndex, -learnRate * sgd);
                    loss += regBias * itemBias * itemBias;

                    // update bi,bin(t)
                    sgd = error * (userScale + dayScale) + regBias * itemSectionBias;
                    itemSectionBiases.plus(itemExplicitIndex, section, -learnRate * sgd);
                    loss += regBias * itemSectionBias * itemSectionBias;

                    // update cu
                    sgd = error * (itemBias + itemSectionBias) + regBias * userScale;
                    userScales.plus(userIndex, -learnRate * sgd);
                    loss += regBias * userScale * userScale;

                    // update cut
                    sgd = error * (itemBias + itemSectionBias) + regBias * dayScale;
                    userDayScales.plus(userIndex, days, -learnRate * sgd);
                    loss += regBias * dayScale * dayScale;

                    // update bu
                    sgd = error + regBias * userBias;
                    userBiases.plus(userIndex, -learnRate * sgd);
                    loss += regBias * userBias * userBias;

                    // update au
                    sgd = error * deviation + regBias * userWeight;
                    userBiasWeights.plus(userIndex, -learnRate * sgd);
                    loss += regBias * userWeight * userWeight;

                    // update but
                    sgd = error + regBias * userDayBias;
                    double delta = userDayBias - learnRate * sgd;
//
                    userDayBiases.set(userIndex, days, delta);
                    loss += regBias * userDayBias * userDayBias;

                    for (int factorIndex = 0; factorIndex < numFactors; factorIndex++) {
                        double userExplicitFactor = userExplicitFactors.get(userIndex, factorIndex);
                        double itemExplicitFactor = itemExplicitFactors.get(itemExplicitIndex, factorIndex);
                        double userImplicitFactor = userImplicitFactors.get(userIndex, factorIndex);
                        delta = dayFactors[factorIndex];

                        sum = 0D;
                        // update userExplicitFactor
                        sgd = error * itemExplicitFactor + regUser * userExplicitFactor;
                        userExplicitFactors.plus(userIndex, factorIndex, -learnRate * sgd);
                        loss += regUser * userExplicitFactor * userExplicitFactor;

                        // update itemExplicitFactors
                        for (VectorEntry explicitVectorEntry : rateVector) {
                            int itemImplicitIndex = explicitVectorEntry.index();
                            sum += itemImplicitFactors.get(itemImplicitIndex, factorIndex);
                        }
                        sgd = error * (userExplicitFactor + userImplicitFactor * deviation + delta + scale * sum) + regItem * itemExplicitFactor;
                        itemExplicitFactors.plus(itemExplicitIndex, factorIndex, -learnRate * sgd);
                        loss += regItem * itemExplicitFactor * itemExplicitFactor;

                        // update userImplicitFactors
                        sgd = error * itemExplicitFactor * deviation + regUser * userImplicitFactor;
                        userImplicitFactors.plus(userIndex, factorIndex, -learnRate * sgd);
                        loss += regUser * userImplicitFactor * userImplicitFactor;

                        // update pkt
                        sgd = error * itemExplicitFactor + regUser * delta;
                        loss += regUser * delta * delta;
                        delta = delta - learnRate * sgd;
                        dayFactors[factorIndex] = delta;

                        step[factorIndex] += error * scale * itemExplicitFactor;

                    }
                }
                double sgd;
                for (VectorEntry vectorEntry : rateVector) {
                    int itemImplicitIndex = vectorEntry.index();
                    for(int factorIndex=0; factorIndex< numFactors; factorIndex++) {
                        double itemImplicitFactor = itemImplicitFactors.get(itemImplicitIndex, factorIndex);
                        sgd = step[factorIndex] + regItem * itemImplicitFactor * size;
                        itemImplicitFactors.plus(itemImplicitIndex, factorIndex, -learnRate * sgd);
                        loss += regItem * itemImplicitFactor * itemImplicitFactor * size;
                    }
                }
            }

            loss *= 0.5D;
            if (isConverged(iterationStep)) {
                break;
            }
            updateLRate(iterationStep);
            lastLoss = loss;
        }
    }

    /**
     * predict a specific rating for user userIdx on item itemIdx.
     *
     * @param userIndex user index
     * @param itemIndex item index
     * @return predictive rating for user userIdx on item itemIdx
     * @throws LibrecException if error occurs
     */
    @Override
    protected double predict(int userIndex, int itemIndex) {
        // retrieve the test rating timestamp
        int days = (int) testTimeMatrix.get(userIndex, itemIndex);
//        int days = days(instant, minTimestamp);
        int section = section(days);
        double deviation = deviation(userIndex, days);
        double value = globalMean;

        // bi(t): eq. (12)
        value += (itemBiases.get(itemIndex) + itemSectionBiases.get(itemIndex, section)) * (userScales.get(userIndex) + userDayScales.get(userIndex, days));
        // bu(t): eq. (9)
        value += (userBiases.get(userIndex) + userBiasWeights.get(userIndex) * deviation + userDayBiases.get(userIndex, days));
        // qi * yj
        SequentialSparseVector userVector = trainMatrix.row(userIndex);

        double sum = 0;
        DenseVector itemExplicitVector = itemExplicitFactors.row(itemIndex);
        for (VectorEntry vectorEntry : userVector) {
            DenseVector itemImplicitVector = itemImplicitFactors.row(vectorEntry.index());
            sum += itemImplicitVector.dot(itemExplicitVector);
        }
        double weight = userVector.size() > 0 ? Math.pow(userVector.size(), -0.5D) : 0D;
        value += sum * weight;

        // qi * pu(t)
        double[] dayFactors = userDayFactors[userIndex].get(days);

        for (int factorIndex = 0; factorIndex < numFactors; factorIndex++) {
            double itemExplicitFactor = itemExplicitFactors.get(itemIndex, factorIndex);
            // eq. (13)
            double userExplicitFactor = userExplicitFactors.get(userIndex, factorIndex) + userImplicitFactors.get(userIndex, factorIndex) * deviation;
            userExplicitFactor += dayFactors[factorIndex];
            value += userExplicitFactor * itemExplicitFactor;
        }
        return value;
    }

    /**
     * get the time deviation for a specific timestamp
     *
     * @param userIndex the inner id of a user
     * @param days      the time stamp
     * @return the time deviation for a specific timestamp t w.r.t the mean date
     * tu
     */
    private double deviation(int userIndex, int days) {
        double mean = userMeanDays.get(userIndex);
        // date difference in days
        double deviation = days - mean;
        return Math.signum(deviation) * Math.pow(Math.abs(deviation), beta);
    }

    /**
     * get the bin number for a specific time stamp
     *
     * @param days time stamp of a day
     * @return the bin number (starting from 0..numBins-1) for a specific
     * timestamp t;
     */
    private int section(int days) {
        return (int) (days / (numDays + 0D) * numSections);
    }

    /**
     * get the maximum and minimum time stamps in the time matrix
     */
    private void getMaxAndMinTimeStamp() {
        minTimestamp = Long.MAX_VALUE;
        maxTimestamp = Long.MIN_VALUE;

        for (MatrixEntry entry : instantMatrix) {
            long timeStamp = (long) entry.get();
            if (timeStamp < minTimestamp) {
                minTimestamp = timeStamp;
            }

            if (timeStamp > maxTimestamp) {
                maxTimestamp = timeStamp;
            }
        }
    }
}
