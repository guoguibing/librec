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

import com.google.common.cache.LoadingCache;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import net.librec.annotation.ModelData;
import net.librec.common.LibrecException;
import net.librec.math.algorithm.Randoms;
import net.librec.math.structure.DenseMatrix;
import net.librec.math.structure.DenseVector;
import net.librec.math.structure.MatrixEntry;
import net.librec.math.structure.SparseMatrix;
import net.librec.recommender.cf.rating.BiasedMFRecommender;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * TimeSVD++ Recommender
 *
 * @author Guo Guibing and Ma Chen
 */
@ModelData({"isRating", "timesvd", "userFactors", "itemFactors", "userBiases", "itemBiases", "trainMatrix", "timeMatrix"})
public class TimeSVDRecommender extends BiasedMFRecommender {
    /**
     * the span of days of rating timestamps
     */
    private static int numDays;

    /**
     * {user, mean date}
     */
    private DenseVector userMeanDate;

    /**
     * time decay factor
     */
    private float beta;

    /**
     * number of bins over all the items
     */
    private int numBins;

    /**
     * item's implicit influence
     */
    private DenseMatrix Y;

    /**
     * {item, bin(t)} bias matrix
     */
    private DenseMatrix Bit;

    /**
     * {user, day, bias} table
     */
    private Table<Integer, Integer, Double> But;

    /**
     * user bias weight parameters
     */
    private DenseVector Alpha;

    /**
     * {user, appender} alpha matrix
     */
    private DenseMatrix Auk;

    /**
     * {user, {appender, day, value} } map
     */
    private Map<Integer, Table<Integer, Integer, Double>> Pukt;

    /**
     * {user, user scaling stable part}
     */
    private DenseVector Cu;

    /**
     * {user, day, day-specific scaling part}
     */
    private DenseMatrix Cut;

    /**
     * minimum, maximum timestamp
     */
    private static long minTimestamp, maxTimestamp;

    /**
     * Guava cache configuration
     */
    protected static String cacheSpec;

    /**
     * user-items cache
     */
    private LoadingCache<Integer, List<Integer>> userItemsCache;

    /**
     * matrix of time stamp
     */
    private static SparseMatrix timeMatrix;

    /**
     * factorized item-factor matrix
     */
    protected DenseMatrix Q;

    /**
     * factorized user-factor matrix
     */
    protected DenseMatrix P;

    /*
     * (non-Javadoc)
     *
     * @see net.librec.recommender.cf.rating.BiasedMFRecommender#setup()
     */
    @Override
    protected void setup() throws LibrecException {
        super.setup();

        beta = conf.getFloat("rec.learnrate.decay", 0.015f);
        numBins = conf.getInt("rec.numBins", 6);

        timeMatrix = (SparseMatrix) getDataModel().getDatetimeDataSet();
        getMaxAndMinTimeStamp();

        numDays = days(maxTimestamp, minTimestamp) + 1;

        userBiases = new DenseVector(numUsers);
        userBiases.init();

        itemBiases = new DenseVector(numItems);
        itemBiases.init();

        Alpha = new DenseVector(numUsers);
        Alpha.init();

        Bit = new DenseMatrix(numItems, numBins);
        Bit.init();

        Y = new DenseMatrix(numItems, numFactors);
        Y.init();

        Auk = new DenseMatrix(numUsers, numFactors);
        Auk.init();

        But = HashBasedTable.create();
        Pukt = new HashMap<>();

        Cu = new DenseVector(numUsers);
        Cu.init();

        Cut = new DenseMatrix(numUsers, numDays);
        Cut.init();

        cacheSpec = conf.get("guava.cache.spec", "maximumSize=200,expireAfterAccess=2m");
        userItemsCache = trainMatrix.rowColumnsCache(cacheSpec);

        P = new DenseMatrix(numUsers, numFactors);
        Q = new DenseMatrix(numItems, numFactors);
        P.init();
        Q.init();

        // global average date
        double sum = 0;
        int cnt = 0;
        for (MatrixEntry me : trainMatrix) {
            int u = me.row();
            int i = me.column();
            double rui = me.get();

            if (rui <= 0)
                continue;

            sum += days((long) timeMatrix.get(u, i), minTimestamp);
            cnt++;
        }
        double globalMeanDate = sum / cnt;

        // compute user's mean of rating timestamps
        userMeanDate = new DenseVector(numUsers);
        List<Integer> Ru = null;
        for (int u = 0; u < numUsers; u++) {

            sum = 0;
            try {
                Ru = userItemsCache.get(u);
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            for (int i : Ru) {
                sum += days((long) timeMatrix.get(u, i), minTimestamp);
            }

            double mean = (Ru.size() > 0) ? (sum + 0.0) / Ru.size() : globalMeanDate;
            userMeanDate.set(u, mean);
        }
    }

    @Override
    protected void trainModel() throws LibrecException {
        for (int iter = 1; iter <= numIterations; iter++) {
            loss = 0;

            for (MatrixEntry me : trainMatrix) {
                int u = me.row();
                int i = me.column();
                double rui = me.get();

                long timestamp = (long) timeMatrix.get(u, i);
                // day t
                int t = days(timestamp, minTimestamp);
                int bin = bin(t);
                double dev_ut = dev(u, t);

                double bi = itemBiases.get(i);
                double bit = Bit.get(i, bin);
                double bu = userBiases.get(u);

                double cu = Cu.get(u);
                double cut = Cut.get(u, t);

                // lazy initialization
                if (!But.contains(u, t))
                    But.put(u, t, Randoms.random());
                double but = But.get(u, t);

                double au = Alpha.get(u); // alpha_u

                double pui = globalMean + (bi + bit) * (cu + cut); // mu + bi(t)
                pui += bu + au * dev_ut + but; // bu(t)

                // qi * yj
                List<Integer> Ru = null;
                try {
                    Ru = userItemsCache.get(u);
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }

                double sum_y = 0;
                for (int j : Ru) {
                    sum_y += DenseMatrix.rowMult(Y, j, Q, i);
                }
                double wi = Ru.size() > 0 ? Math.pow(Ru.size(), -0.5) : 0;
                pui += sum_y * wi;

                // qi * pu(t)
                if (!Pukt.containsKey(u)) {
                    Table<Integer, Integer, Double> data = HashBasedTable.create();
                    Pukt.put(u, data);
                }

                Table<Integer, Integer, Double> Pkt = Pukt.get(u);
                for (int k = 0; k < numFactors; k++) {
                    double qik = Q.get(i, k);

                    // lazy initialization
                    if (!Pkt.contains(k, t))
                        Pkt.put(k, t, Randoms.random());

                    double puk = P.get(u, k) + Auk.get(u, k) * dev_ut + Pkt.get(k, t);

                    pui += puk * qik;
                }

                double eui = pui - rui;
                loss += eui * eui;

                // update bi
                double sgd = eui * (cu + cut) + regBias * bi;
                itemBiases.add(i, -learnRate * sgd);
                loss += regBias * bi * bi;

                // update bi,bin(t)
                sgd = eui * (cu + cut) + regBias * bit;
                Bit.add(i, bin, -learnRate * sgd);
                loss += regBias * bit * bit;

                // update cu
                sgd = eui * (bi + bit) + regBias * cu;
                Cu.add(u, -learnRate * sgd);
                loss += regBias * cu * cu;

                // update cut
                sgd = eui * (bi + bit) + regBias * cut;
                Cut.add(u, t, -learnRate * sgd);
                loss += regBias * cut * cut;

                // update bu
                sgd = eui + regBias * bu;
                userBiases.add(u, -learnRate * sgd);
                loss += regBias * bu * bu;

                // update au
                sgd = eui * dev_ut + regBias * au;
                Alpha.add(u, -learnRate * sgd);
                loss += regBias * au * au;

                // update but
                sgd = eui + regBias * but;
                double delta = but - learnRate * sgd;
                But.put(u, t, delta);
                loss += regBias * but * but;

                for (int k = 0; k < numFactors; k++) {
                    double qik = Q.get(i, k);
                    double puk = P.get(u, k);
                    double auk = Auk.get(u, k);
                    double pkt = Pkt.get(k, t);

                    // update qik
                    double pukt = puk + auk * dev_ut + pkt;

                    double sum_yk = 0;
                    for (int j : Ru)
                        sum_yk += Y.get(j, k);

                    sgd = eui * (pukt + wi * sum_yk) + regItem * qik;
                    Q.add(i, k, -learnRate * sgd);
                    loss += regItem * qik * qik;

                    // update puk
                    sgd = eui * qik + regUser * puk;
                    P.add(u, k, -learnRate * sgd);
                    loss += regUser * puk * puk;

                    // update auk
                    sgd = eui * qik * dev_ut + regUser * auk;
                    Auk.add(u, k, -learnRate * sgd);
                    loss += regUser * auk * auk;

                    // update pkt
                    sgd = eui * qik + regUser * pkt;
                    delta = pkt - learnRate * sgd;
                    Pkt.put(k, t, delta);
                    loss += regUser * pkt * pkt;

                    // update yjk
                    for (int j : Ru) {
                        double yjk = Y.get(j, k);
                        sgd = eui * wi * qik + regItem * yjk;
                        Y.add(j, k, -learnRate * sgd);
                        loss += regItem * yjk * yjk;
                    }
                }
            }

            loss *= 0.5;

            if (isConverged(iter))
                break;
        }
    }

    /**
     * predict a specific rating for user userIdx on item itemIdx.
     *
     * @param userIdx user index
     * @param itemIdx item index
     * @return predictive rating for user userIdx on item itemIdx
     * @throws LibrecException if error occurs
     */
    protected double predict(int userIdx, int itemIdx) throws LibrecException {
        // retrieve the test rating timestamp
        long timestamp = (long) timeMatrix.get(userIdx, itemIdx);
        int t = days(timestamp, minTimestamp);
        int bin = bin(t);
        double dev_ut = dev(userIdx, t);

        double pred = globalMean;

        // bi(t): eq. (12)
        pred += (itemBiases.get(itemIdx) + Bit.get(itemIdx, bin)) * (Cu.get(userIdx) + Cut.get(userIdx, t));

        // bu(t): eq. (9)
        double but = But.contains(userIdx, t) ? But.get(userIdx, t) : 0;
        pred += userBiases.get(userIdx) + Alpha.get(userIdx) * dev_ut + but;

        // qi * yj
        List<Integer> Ru = null;
        try {
            Ru = userItemsCache.get(userIdx);
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        double sum_y = 0;
        for (int j : Ru)
            sum_y += DenseMatrix.rowMult(Y, j, Q, itemIdx);

        double wi = Ru.size() > 0 ? Math.pow(Ru.size(), -0.5) : 0;
        pred += sum_y * wi;

        // qi * pu(t)
        for (int k = 0; k < numFactors; k++) {
            double qik = Q.get(itemIdx, k);
            // eq. (13)
            double puk = P.get(userIdx, k) + Auk.get(userIdx, k) * dev_ut;

            if (Pukt.containsKey(userIdx)) {
                Table<Integer, Integer, Double> pkt = Pukt.get(userIdx);
                if (pkt != null) {
                    // eq. (13)
                    puk += (pkt.contains(k, t) ? pkt.get(k, t) : 0);
                }
            }

            pred += puk * qik;
        }

        return pred;
    }

    /**
     * get the time deviation for a specific timestamp
     *
     * @param userId the inner id of a user
     * @param t the time stamp
     * @return the time deviation for a specific timestamp t w.r.t the mean date tu
     */
    private double dev(int userId, int t) {
        double tu = userMeanDate.get(userId);

        // date difference in days
        double diff = t - tu;

        return Math.signum(diff) * Math.pow(Math.abs(diff), beta);
    }

    /**
     * get the bin number for a specific time stamp
     *
     * @param day time stamp of a day
     * @return the bin number (starting from 0..numBins-1) for a specific timestamp t;
     */
    private int bin(int day) {
        return (int) (day / (numDays + 0.0) * numBins);
    }

    /**
     * get the number of days for a given time difference
     *
     * @param diff the difference between two time stamps
     * @return number of days for a given time difference
     */
    private static int days(long diff) {
        return (int) TimeUnit.MILLISECONDS.toDays(diff);
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

    /**
     * get the maximum and minimum time stamps in the time matrix
     *
     */
    private void getMaxAndMinTimeStamp() {
        minTimestamp = Long.MAX_VALUE;
        maxTimestamp = Long.MIN_VALUE;

        for (MatrixEntry entry : timeMatrix) {
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
