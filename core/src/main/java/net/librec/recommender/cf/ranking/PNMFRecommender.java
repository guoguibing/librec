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


import com.google.common.collect.BiMap;
import net.librec.common.LibrecException;
import net.librec.math.structure.SparseVector;
import net.librec.recommender.AbstractRecommender;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;


/**
 * Projective Nonnegative Matrix Factorization
 *
 * (only implicit or binary feedback supported)
 *
 * <ul>
 * <li>Yuan, Zhijian, and Erkki Oja. "Projective nonnegative matrix factorization for image compression and feature extraction." Image analysis (2005): 333-342.</li>
 * <li>Yang, Zhirong, Zhijian Yuan, and Jorma Laaksonen. "Projective non-negative matrix factorization with applications to facial image processing." International Journal of Pattern Recognition and Artificial Intelligence 21.08 (2007): 1353-1362.</li>
 * <li>Yang, Zhirong, and Erkki Oja. "Unified development of multiplicative algorithms for linear and quadratic nonnegative matrix factorization." IEEE transactions on neural networks 22.12 (2011): 1878-1891.</li>
 * <li>Zhang, He, Zhirong Yang, and Erkki Oja. "Adaptive multiplicative updates for projective nonnegative matrix factorization." International Conference on Neural Information Processing. Springer, Berlin, Heidelberg, 2012.</li>
 * </ul>
 *
 * PNMF tries to model the probability with P(V) ~ W * W^T * V
 *
 * Where V is the observed purchase user item matrix.
 * And W is the trained matrix from a latent-factor space to the items.
 *
 * In contrast to this the model of NMF is P(V) ~ W * H
 *
 * You can say:
 *
 * PNMF is training a Item-Item model
 * NMF is training a User-Item model
 *
 * Item-Item models are much better usable for fast online recommendation with a lot of new or fast changing users.
 *
 * Simply store history of user anywhere else and do request the recommender with the users item histories instead with the user itself.
 *
 * In this Recommender the Divergence D(V || W*W^T*V) is minimized.
 * See Formula 16 in "Projective non-negative matrix factorization with applications to facial image processing"
 *
 * Since the Divergence is only calculated on non zero elements this results in an algorithm with acceptable training time on big data.
 *
 * Some performance optimization is done via parallel computing.
 *
 * But until now no SGD or adaptive multiplicative update is done.
 *
 * Multiplicative update is done with square root for sure but slow convergence.
 *
 * There is also no special treatment of over fitting.
 * So be careful with to much latent factors on small training data.
 *
 *
 * You can test the recommender with following properties:
 * ( I have used movielens csv data for testing )
 *
 * rec.recommender.class=pnmf
 * rec.iterator.maximum=50
 * rec.factor.number=20
 * rec.recommender.isranking=true
 * rec.recommender.ranking.topn=10
 *
 * data.model.splitter=loocv
 * data.splitter.loocv=user
 * data.convert.binarize.threshold=0
 * rec.eval.classes=auc,ap,arhr,hitrate,idcg,ndcg,precision,recall,rr
 *
 *
 * @author Daniel Velten, Karlsruhe, Germany
 */
public class PNMFRecommender extends AbstractRecommender {

    private static final int PARALLELIZE_USER_SPLIT_SIZE = 5000;

    private double[][] w;

    private int numFactors;
    private int numIterations;


    @Override
    protected void setup() throws LibrecException {
        super.setup();

        numFactors = conf.getInt("rec.factor.number", 15);
        numIterations = conf.getInt("rec.iterator.maximum",100);

        w = new double[numFactors][numItems];

        initMatrix(w);

    }


    private void initMatrix(double[][] m) {
        double initValue = 1d / (numItems * 2d);

        Random random = new Random(123456789L);

        for (int i = 0; i < m.length; i++){
            for (int j = 0; j < m[i].length; j++){
                m[i][j] = (random.nextDouble() + 1) * initValue;
            }
        }
    }


    @Override
    public void trainModel() {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        LOG.info("availableProcessors=" + availableProcessors);
        ExecutorService executorService = Executors.newFixedThreadPool(availableProcessors);
        for (int iter = 0; iter <= numIterations; ++iter) {
            LOG.info("Starting iteration=" + iter);
            train(executorService, iter);
        }
        executorService.shutdown();

    }



    /**
     *
     * Only for storing results of the parallel executed tasks
     */
    private static class AggResult {

        private final double[][] resultNumerator;
        private final double[] summedLatentFactors;
        private final int[] countUsersBoughtItem;
        private final double sumLog;

        public AggResult(double[][] resultNumerator, double[] summedLatentFactors, int[] countUsersBoughtItem, double sumLog) {
            this.resultNumerator = resultNumerator;
            this.summedLatentFactors = summedLatentFactors;
            this.countUsersBoughtItem = countUsersBoughtItem;
            this.sumLog = sumLog;
        }

    }

    /**
     *
     * Task for parallel execution.
     *
     * Executes calculations for users between 'fromUser' and 'toUser'.
     *
     */
    private class ParallelExecTask implements Callable<AggResult> {

        private final int fromUser;
        private final int toUser;

        public ParallelExecTask(int fromUser, int toUser) {
            this.fromUser = fromUser;
            this.toUser = toUser;
        }

        @Override
        public AggResult call() throws Exception {
            //LOG.info("ParallelExecTask: Starting fromUser=" + fromUser + " toUser=" + toUser);
            double[][] resultNumerator = new double[numFactors][numItems];
            double[] summedLatentFactors = new double[numFactors]; // Used in denominator
            int[] countUsersBoughtItem = new int[numItems]; // Used in denominator
            double sumLog = 0; // sumLog not really needed, only for calculating divergence for logging/debug

            //  See Formula 16 in "Projective non-negative matrix factorization with applications to facial image processing"
            for (int userIdx = fromUser; userIdx < toUser; userIdx++) {
                SparseVector itemRatingsVector = trainMatrix.row(userIdx);
                if (itemRatingsVector.getCount() > 0) {

                    double[] thisUserLatentFactors = predictFactors(itemRatingsVector);
                    for (int factorIdx = 0; factorIdx < summedLatentFactors.length; factorIdx++) {
                        summedLatentFactors[factorIdx] += thisUserLatentFactors[factorIdx];
                    }


                    double[] second_term_numerator = new double[numFactors];
                    for (int itemIdx : itemRatingsVector.getIndex()) {
                        double estimate = 0;
                        for (int factorIdx = 0; factorIdx < thisUserLatentFactors.length; factorIdx++) {
                            estimate += thisUserLatentFactors[factorIdx] * w[factorIdx][itemIdx];
                        }
                        double estimateFactor = 1d/estimate;
                        sumLog += Math.log(estimateFactor);
                        countUsersBoughtItem[itemIdx]++;

                        // Adding the terms of first sum numerator immediately
                        for (int factorIdx = 0; factorIdx < thisUserLatentFactors.length; factorIdx++) {
                            // This is not a sum loop, we are just setting all values
                            double first_term_numerator = estimateFactor * thisUserLatentFactors[factorIdx];
                            resultNumerator[factorIdx][itemIdx] += first_term_numerator;
                        }
                        // This for loop is for the second sum numerator, the inner sum, but added later..
                        for (int factorIdx = 0; factorIdx < thisUserLatentFactors.length; factorIdx++) {
                            second_term_numerator[factorIdx] += estimateFactor * w[factorIdx][itemIdx];
                        }
                    }
                    // Now we are able to add the second term numerator to each w element.
                    for (int itemIdx : itemRatingsVector.getIndex()) {

                        for (int factorIdx = 0; factorIdx < second_term_numerator.length; factorIdx++) {
                            resultNumerator[factorIdx][itemIdx] += second_term_numerator[factorIdx];
                        }
                    }
                }

            }
            return new AggResult(resultNumerator, summedLatentFactors, countUsersBoughtItem, sumLog);
        }
    }

    private void train(ExecutorService executorService, int iteration) {

        // Creating the parallel execution tasks
        List<ParallelExecTask> tasks = new ArrayList<>((numUsers / PARALLELIZE_USER_SPLIT_SIZE) + 1);
        for (int fromUser = 0; fromUser < numUsers; fromUser += PARALLELIZE_USER_SPLIT_SIZE) {
            int toUserExclusive = Math.min(numUsers, fromUser + PARALLELIZE_USER_SPLIT_SIZE);
            ParallelExecTask task = new ParallelExecTask(fromUser, toUserExclusive);
            tasks.add(task);
        }
        try {
            // Executing the tasks in parallel
            List<Future<AggResult>> results = executorService.invokeAll(tasks);

            double[][] resultNumerator = new double[numFactors][numItems];
            double[] summedLatentFactors = new double[numFactors];
            int[] countUsersBoughtItem = new int[numItems];
            double sumLog = 0;

            // Adding all the AggResults together..
            for (Future<AggResult> future: results) {
                AggResult result = future.get();
                for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                    for (int itemIdx = 0; itemIdx < numItems; itemIdx++) {
                        resultNumerator[factorIdx][itemIdx] += result.resultNumerator[factorIdx][itemIdx];
                    }
                }
                for (int itemIdx = 0; itemIdx < numItems; itemIdx++) {
                    countUsersBoughtItem[itemIdx] += result.countUsersBoughtItem[itemIdx];
                }
                for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                    summedLatentFactors[factorIdx] += result.summedLatentFactors[factorIdx];
                }
                sumLog += result.sumLog;
            }
            // Norms of w are not calculated in parallel (not dependent on user)
            double[] wNorm = new double[numFactors];
            for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                double sum = 0;
                for (int itemIdx = 0; itemIdx < numItems; itemIdx++) {
                    sum += w[factorIdx][itemIdx];

                }
                wNorm[factorIdx] = sum;
            }

            // Calculation of Divergence is not needed. Only for debugging/logging purpose
            printDivergence(summedLatentFactors, countUsersBoughtItem, sumLog, wNorm, iteration);



			/*
			 * Multiplicative updates are done here
			 *
			 * See Formula 16 in "Projective non-negative matrix factorization with applications to facial image processing"
			 *
			 * But we apply a square root to the factors...
			 * This results in slow but stable conversion
			 *
			 * Look here for explanation "Adaptive multiplicative updates for projective nonnegative matrix factorization."
			 * ('quadratic' update rules for the divergence)
			 */
            for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                for (int itemIdx = 0; itemIdx < numItems; itemIdx++) {

                    double oldValue = w[factorIdx][itemIdx];
                    double numerator = resultNumerator[factorIdx][itemIdx];
                    double denominator = countUsersBoughtItem[itemIdx] * wNorm[factorIdx] + summedLatentFactors[factorIdx];

                    double newValue = oldValue * StrictMath.sqrt(numerator / denominator);

                    if (Double.isNaN(newValue)) {
                        LOG.warn("Double.isNaN  " + numerator + " " + denominator + " " + oldValue + " " + newValue);
                    }
//					if (newVal<1e-24) {
//						LOG.info("1e-24 " + zaehler + " " + nenner + " " + old + " " + newVal);
//						newVal = 1e-24;
//					}
                    w[factorIdx][itemIdx] = newValue;
                }

            }

        }
        catch (InterruptedException | ExecutionException e) {
            LOG.error("", e);
            throw new IllegalStateException(e);
        }

    }


    // only for logging
    private void printDivergence(double[] summedLatentFactors, int[] countUsersBoughtItem, double sumLog, double[] wNorm, int iteration) {
        int countAll = 0;
        for (int itemIdx = 0; itemIdx < countUsersBoughtItem.length; itemIdx++) {
            countAll += countUsersBoughtItem[itemIdx];
        }

        double sumAllEstimate = 0;
        for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
            sumAllEstimate += wNorm[factorIdx] * summedLatentFactors[factorIdx];
        }
        double divergence = sumLog- countAll + sumAllEstimate;
        //LOG.info("Divergence (before iteration " + iteration +")=" + divergence + "  sumLog=" + sumLog + "  countAll=" + countAll + "  sumAllEstimate=" + sumAllEstimate);
        LOG.info("Divergence (before iteration " + iteration +")=" + divergence);
    }

    private double predict(SparseVector itemRatingsVector, int itemIdx) {
        double sum = 0;
        for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {

            sum += w[factorIdx][itemIdx] * predictFactor(itemRatingsVector, factorIdx);

        }

        return sum;
    }

    private double predictFactor(SparseVector itemRatingsVector, int factorIdx) {
        double sum = 0;
        for (int itemIdx : itemRatingsVector.getIndex()) {
            sum += w[factorIdx][itemIdx];
        }
        return sum;
    }

    private double[] predictFactors(SparseVector itemRatingsVector) {
        double[] latentFactors = new double[numFactors];
        for (int itemIdx : itemRatingsVector.getIndex()) {
            for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                latentFactors[factorIdx] += w[factorIdx][itemIdx];
            }
        }
        return latentFactors;
    }

    /*
     * This is not fast if you call for each item from outside
     * Calculate factors first and then calculate with factors the prediction of each item
     */
    @Override
    protected double predict(int userIdx, int itemIdx) throws LibrecException {
        SparseVector itemRatingsVector = trainMatrix.row(userIdx);

        return predict(itemRatingsVector, itemIdx);
    }


    @Override
    public void saveModel(String filePath) {
        LOG.info("Writing matrix W to file=" + filePath);
        try{
            BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
            writer.write("\"item_id\"");
            for (int i = 0; i < numFactors; i++) {
                writer.write(',');
                writer.write("\"factor\"");
                writer.write(Integer.toString(i));
            }
            writer.newLine();
            BiMap<Integer, String> items = itemMappingData.inverse();
            for (int itemIdx = 0; itemIdx < numItems; itemIdx++) {
                writer.write('\"');
                writer.write(items.get(itemIdx));
                writer.write('\"');
                for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                    writer.write(',');
                    writer.write(Double.toString(w[factorIdx][itemIdx]));
                }
                writer.newLine();
            }


            writer.close();
        } catch (Exception e) {
            LOG.error("Could not save model", e);
        }
    }
}