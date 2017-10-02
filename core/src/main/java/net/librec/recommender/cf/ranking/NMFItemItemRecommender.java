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
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;


/**
 * Nonnegative Matrix Factorization of the item to item purchase matrix
 *
 * (currently only implicit or binary input supported)
 *
 * NMFItemItem uses as model of the probability distribution P(V) ~ W * H * V
 *
 * Where V is the observed purchase user item matrix.
 * And W and H are trained matrices.
 *
 * H is the matrix for 'analyzing' the current purchase item history and calculates the assumed latent feature vectors.
 * W is the matrix for 'estimating' the next item purchased from the latent feature vectors.
 *
 * In contrast to this the original Nonnegative Matrix Factorization is a factorization of the item - user matrix.
 *
 * <ul>
 * <li>Lee, Daniel D., and H. Sebastian Seung. "Learning the parts of objects by non-negative matrix factorization." Nature 401.6755 (1999): 788.</li>
 * <li>Yuan, Zhijian, and Erkki Oja. "Projective nonnegative matrix factorization for image compression and feature extraction." Image analysis (2005): 333-342.</li>
 * <li>Yang, Zhirong, Zhijian Yuan, and Jorma Laaksonen. "Projective non-negative matrix factorization with applications to facial image processing." International Journal of Pattern Recognition and Artificial Intelligence 21.08 (2007): 1353-1362.</li>
 * <li>Yang, Zhirong, and Erkki Oja. "Unified development of multiplicative algorithms for linear and quadratic nonnegative matrix factorization." IEEE transactions on neural networks 22.12 (2011): 1878-1891.</li>
 * <li>Zhang, He, Zhirong Yang, and Erkki Oja. "Adaptive multiplicative updates for projective nonnegative matrix factorization." International Conference on Neural Information Processing. Springer, Berlin, Heidelberg, 2012.</li>
 * <li>Kabbur, Santosh, Xia Ning, and George Karypis. "FISM: Factored Item Similarity Models for top-n recommender systems." Proceedings of the 19th ACM SIGKDD international conference on Knowledge discovery and data mining. ACM, 2013.</li>
 * </ul>
 *
 *
 * Item-Item models are much better usable for fast online recommendation with a lot of new or fast changing users.
 * (Solves the cold start problem of new users)
 *
 * Item-Item models could perhaps suffer from self estimation of the item while training.
 * There is a experimental optimization analog to follow research article:
 *
 * Kabbur, Santosh, Xia Ning, and George Karypis. "FISM: Factored Item Similarity Models for top-n recommender systems." Proceedings of the 19th ACM SIGKDD international conference on Knowledge discovery and data mining. ACM, 2013.
 *
 * You can switch this optimization on/off with property
 *
 * rec.nmfitemitem.do_not_estimate_yourself=true
 *
 * Simply store history of user anywhere else
 * and request the recommender with the users item histories instead with the user (or 'user-ID') itself.
 *
 * In this Recommender the Divergence D(V || W*H*V) is minimized.
 *
 * Since the Divergence is only calculated on non zero elements this results in an algorithm with acceptable training time on big data.
 *
 * Some performance optimization is done via parallel computing.
 *
 * But until now no SGD is done.
 *
 * There is also experimental implementation of adaptive multiplicative update rules.
 * Zhang, He, Zhirong Yang, and Erkki Oja. "Adaptive multiplicative updates for projective nonnegative matrix factorization." International Conference on Neural Information Processing. Springer, Berlin, Heidelberg, 2012.
 *
 * This can be switched on/off with property:
 *
 * rec.nmfitemitem.adaptive_update_rules=true
 *
 * Both matrices are updated in one iteration step at once.
 *
 * There is also no special treatment of over fitting.
 * So be careful with to much latent factors on very small training data.
 *
 * You can test the recommender with following properties:
 * ( I have used movielens csv data for testing )
 *

 *
 * data.model.splitter=loocv
 * data.splitter.loocv=user
 * data.convert.binarize.threshold=0
 * rec.eval.classes=auc,ap,arhr,hitrate,idcg,ndcg,precision,recall,rr
 *
 *
 * @author Daniel Velten, Karlsruhe, Germany
 */
public class NMFItemItemRecommender extends AbstractRecommender {


    private double[][] w_reconstruct;
    private double[][] h_analyze;

    private int numFactors;
    private int numIterations;

    private double divergenceFromLastStep;
    private double exponent = 0.5;

    private int parallelizeSplitUserSize = 5000;
    private boolean doNotEstimateYourself = true;
    private boolean adaptiveUpdateRules = true;


    @Override
    protected void setup() throws LibrecException {
        super.setup();

        numFactors = conf.getInt("rec.factor.number", 15);
        numIterations = conf.getInt("rec.iterator.maximum",100);

        doNotEstimateYourself = conf.getBoolean("rec.nmfitemitem.do_not_estimate_yourself", true);
        adaptiveUpdateRules = conf.getBoolean("rec.nmfitemitem.adaptive_update_rules", true);
        parallelizeSplitUserSize = conf.getInt("rec.nmfitemitem.parallelize_split_user_size", 5000);

        w_reconstruct = new double[numFactors][numItems];
        h_analyze = new double[numFactors][numItems];

        initMatrix(w_reconstruct);
        initMatrix(h_analyze);

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


        private final double[][] resultNumeratorAnalyze;
        private final double[][] resultNumeratorReconstruct;
        private final double[][] resultDenominatorReconstructDiff;
        private final int boughtItems;
        private final double sumLog;
        private final int[] countUsersBoughtItem;
        private final double[] resultDenominatorReconstruct2;

        public AggResult(double[][] resultNumeratorAnalyze, double[][] resultNumeratorReconstruct, double[][] resultDenominatorReconstructDiff, int boughtItems, double sumLog, int[] countUsersBoughtItem, double[] resultDenominatorReconstruct2) {
            this.resultNumeratorAnalyze = resultNumeratorAnalyze;
            this.resultNumeratorReconstruct = resultNumeratorReconstruct;
            this.resultDenominatorReconstructDiff = resultDenominatorReconstructDiff;
            this.boughtItems = boughtItems;
            this.sumLog = sumLog;
            this.countUsersBoughtItem = countUsersBoughtItem;
            this.resultDenominatorReconstruct2 = resultDenominatorReconstruct2;
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
            double[][] resultNumeratorAnalyze = new double[numFactors][numItems];
            double[][] resultNumeratorReconstruct = new double[numFactors][numItems];
            double[][] resultDenominatorReconstructDiff = new double[numFactors][numItems]; // Used in denominator
            double[] resultDenominatorReconstruct = new double[numFactors]; // Used in denominator

            int boughtItems = 0;
            double sumLog = 0; // only for calculating divergence for logging/debug
            int[] countUsersBoughtItem = new int[numItems]; // Used in denominator

            for (int userIdx = fromUser; userIdx < toUser; userIdx++) {
                SparseVector itemRatingsVector = trainMatrix.row(userIdx);
                int minCount = doNotEstimateYourself ? 2 : 1;
                if (itemRatingsVector.getCount() >= minCount) {

                    int[] itemIndices = itemRatingsVector.getIndex();
                    double[] allUserLatentFactors = predictFactors(itemIndices);

                    for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                        resultDenominatorReconstruct[factorIdx] += allUserLatentFactors[factorIdx];
                    }

                    double[] analyze_numerator = new double[numFactors];
                    for (int itemIdx : itemIndices) {
                        double[] thisUserLatentFactors = new double[numFactors];
                        for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                            if (doNotEstimateYourself){
                                resultDenominatorReconstructDiff[factorIdx][itemIdx] +=  h_analyze[factorIdx][itemIdx];
                                thisUserLatentFactors[factorIdx] = allUserLatentFactors[factorIdx] - h_analyze[factorIdx][itemIdx];
                            } else {
                                thisUserLatentFactors[factorIdx] = allUserLatentFactors[factorIdx];
                            }
                        }

                        double estimate = 0;
                        for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                            estimate += thisUserLatentFactors[factorIdx] * w_reconstruct[factorIdx][itemIdx];
                        }
                        double estimateFactor = 1d/estimate;
                        sumLog += Math.log(estimateFactor);
                        boughtItems++;
                        countUsersBoughtItem[itemIdx]++;


                        for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                            double latent = thisUserLatentFactors[factorIdx];
                            resultNumeratorReconstruct[factorIdx][itemIdx] += estimateFactor * latent;

                            double numerator = estimateFactor * w_reconstruct[factorIdx][itemIdx];
                            analyze_numerator[factorIdx] += numerator;
                            if (doNotEstimateYourself){
                                resultNumeratorAnalyze[factorIdx][itemIdx] -= numerator;

                            }
                        }
                    }
                    for (int lItemIdx : itemIndices) {

                        for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                            resultNumeratorAnalyze[factorIdx][lItemIdx] += analyze_numerator[factorIdx];
                        }
                    }
                }

            }
            return new AggResult(resultNumeratorAnalyze, resultNumeratorReconstruct, resultDenominatorReconstructDiff, boughtItems, sumLog, countUsersBoughtItem, resultDenominatorReconstruct);
        }
    }



    private void train(ExecutorService executorService, int iteration) {

        // Creating the parallel execution tasks
        List<ParallelExecTask> tasks = new ArrayList<>((numUsers / parallelizeSplitUserSize) + 1);
        for (int fromUser = 0; fromUser < numUsers; fromUser += parallelizeSplitUserSize) {
            int toUserExclusive = Math.min(numUsers, fromUser + parallelizeSplitUserSize);
            ParallelExecTask task = new ParallelExecTask(fromUser, toUserExclusive);
            tasks.add(task);
        }
        try {
            // Executing the tasks in parallel
            List<Future<AggResult>> results = executorService.invokeAll(tasks);

            double[][] resultNumeratorAnalyze = new double[numFactors][numItems];
            double[][] resultNumeratorReconstruct = new double[numFactors][numItems];
            double[][] resultDenominatorReconstructDiff = new double[numFactors][numItems]; // Used in denominator
            double[] resultDenominatorReconstruct2 = new double[numFactors]; // Used in denominator

            int boughtItems = 0;
            double sumLog = 0; // only for calculating divergence for logging/debug
            int[] countUsersBoughtItem = new int[numItems];

            // Adding all the AggResults together..
            for (Future<AggResult> future: results) {
                AggResult result = future.get();
                for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                    for (int itemIdx = 0; itemIdx < numItems; itemIdx++) {
                        resultNumeratorAnalyze[factorIdx][itemIdx] += result.resultNumeratorAnalyze[factorIdx][itemIdx];
                        resultNumeratorReconstruct[factorIdx][itemIdx] += result.resultNumeratorReconstruct[factorIdx][itemIdx];
                        resultDenominatorReconstructDiff[factorIdx][itemIdx] += result.resultDenominatorReconstructDiff[factorIdx][itemIdx];
                    }
                    resultDenominatorReconstruct2[factorIdx] += result.resultDenominatorReconstruct2[factorIdx];
                }
                for (int itemIdx = 0; itemIdx < numItems; itemIdx++) {
                    countUsersBoughtItem[itemIdx] += result.countUsersBoughtItem[itemIdx];
                }
                boughtItems += result.boughtItems;
                sumLog += result.sumLog;
            }
            // Norms of w are not calculated in parallel (not dependent on user)
            double[] wNorm = new double[numFactors];
            for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                double sum = 0;
                for (int itemIdx = 0; itemIdx < numItems; itemIdx++) {
                    sum += w_reconstruct[factorIdx][itemIdx];

                }
                wNorm[factorIdx] = sum;
            }

            // Calculation of Divergence is not needed. Only for debugging/logging purpose
            double divergence = calculateDivergence(boughtItems, sumLog, iteration, resultDenominatorReconstruct2, resultDenominatorReconstructDiff);

            if (adaptiveUpdateRules){
                if (iteration == 0 || divergence > divergenceFromLastStep){
                    LOG.info("divergence > divergenceFromLastStep. Setting exponent to 0.5.");
                    exponent = 0.5;
                } else {
                    if (exponent < 1.45){
                        exponent += 0.1;
                    }
                    LOG.info("divergence <= divergenceFromLastStep. Exponent is now: " + exponent);
                }
                divergenceFromLastStep = divergence;
            }

			/*
			 * Multiplicative updates are done here
			 *
			 * Look here for explanation:
			 * "Adaptive multiplicative updates for projective nonnegative matrix factorization."
			 *
			 */

            // We do update of both matrices at once
            // Could be changed here:

//			if (iteration % 2 ==0){
            double[][] new_w_reconstruct = updateReconstruct(resultNumeratorReconstruct, resultDenominatorReconstructDiff, resultDenominatorReconstruct2);
//				w_reconstruct = new_w_reconstruct;
//			} else {
            updateAnalyze(resultNumeratorAnalyze, wNorm, countUsersBoughtItem);
//			}
            w_reconstruct = new_w_reconstruct;

        }
        catch (InterruptedException | ExecutionException e) {
            LOG.error("", e);
            throw new IllegalStateException(e);
        }

    }

    private void updateAnalyze(double[][] resultNumeratorAnalyze, double[] wNorm, int[] countUsersBoughtItem) {
        for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
            for (int itemIdx = 0; itemIdx < numItems; itemIdx++) {
                double oldValue = h_analyze[factorIdx][itemIdx];
                double numerator = resultNumeratorAnalyze[factorIdx][itemIdx];
                double denominator;
                if (doNotEstimateYourself){
                    denominator = countUsersBoughtItem[itemIdx] * (wNorm[factorIdx] - w_reconstruct[factorIdx][itemIdx]);
                } else {
                    denominator = countUsersBoughtItem[itemIdx] * wNorm[factorIdx];
                }
                double newValue = oldValue * Math.pow(numerator / denominator, exponent);

                //LOG.warn("Analyze Double.isNaN  " + numerator + " " + denominator + " " + oldValue + " " + newValue + "  " + factorIdx + "  " + itemIdx);
                if (Double.isNaN(newValue)) {
                    newValue =0;
                }
//				if (newValue<1e-16) {
//					newValue =1e-16;
//				}
                h_analyze[factorIdx][itemIdx] = newValue;
            }
        }
    }


    private double[][] updateReconstruct(double[][] resultNumeratorReconstruct, double[][] resultDenominatorReconstructDiff,
                                         double[] resultDenominatorReconstruct2) {
        double[][] new_w_reconstruct = new double[numFactors][numItems];
        for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
            for (int itemIdx = 0; itemIdx < numItems; itemIdx++) {

                double oldValue = w_reconstruct[factorIdx][itemIdx];
                double numerator = resultNumeratorReconstruct[factorIdx][itemIdx];
                double denominatorDiff = resultDenominatorReconstructDiff[factorIdx][itemIdx];
                double denominator = resultDenominatorReconstruct2[factorIdx];
                double newValue = oldValue * Math.pow(numerator / (denominator - denominatorDiff), exponent);

//		if (Double.isNaN(newValue)) {
//			LOG.warn("Double.isNaN  " + numerator + " " + denominator +" " + denominator2 + " " + oldValue + " " + newValue);
//		}
//		if (newValue<1e-16) {
//			newValue =1e-16;
//		}
                new_w_reconstruct[factorIdx][itemIdx] = newValue;
            }
        }
        return new_w_reconstruct;
    }


    private double calculateDivergence(int countAll, double sumLog, int iteration, double[] resultDenominatorReconstruct, double[][] resultDenominatorReconstructDiff) {
        double sumAllEstimate = 0;
        for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
            for (int itemIdx = 0; itemIdx < numItems; itemIdx++) {

                double denominatorDiff = resultDenominatorReconstructDiff[factorIdx][itemIdx];
                double denominator = resultDenominatorReconstruct[factorIdx];
                double newValue = denominator - denominatorDiff;

                sumAllEstimate += w_reconstruct[factorIdx][itemIdx] * newValue;
            }
        }

        double divergence = sumLog- countAll + sumAllEstimate;
        LOG.info("Divergence (before iteration " + iteration +")=" + divergence + "  sumLog=" + sumLog + "  countAll=" + countAll + "  sumAllEstimate=" + sumAllEstimate);
        //LOG.info("Divergence (before iteration " + iteration +")=" + divergence);

        return divergence;
    }

    private double predict(SparseVector itemRatingsVector, int itemIdx) {
        double sum = 0;
        for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {

            sum += w_reconstruct[factorIdx][itemIdx] * predictFactor(itemRatingsVector, factorIdx);

        }

        return sum;
    }

    private double predictFactor(SparseVector itemRatingsVector, int factorIdx) {
        double sum = 0;
        for (int itemIdx : itemRatingsVector.getIndex()) {
            sum += w_reconstruct[factorIdx][itemIdx];
        }
        return sum;
    }

    private double[] predictFactors(int[] itemIndices) {
        double[] latentFactors = new double[numFactors];
        for (int itemIdx : itemIndices) {
            for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                latentFactors[factorIdx] += h_analyze[factorIdx][itemIdx];
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
    public void saveModel(String directoryPath) {
        File dir = new File(directoryPath);
        dir.mkdir();

        try{
            File wFile = new File(dir, "w_reconstruct.csv");
            LOG.info("Writing matrix w_reconstruct to file=" + wFile.getAbsolutePath());
            saveMatrix(wFile, w_reconstruct);
            File hFile = new File(dir, "h_analyze.csv");
            LOG.info("Writing matrix h_analyze to file=" + hFile.getAbsolutePath());
            saveMatrix(hFile, h_analyze);
        } catch (Exception e) {
            LOG.error("Could not save model", e);
        }
    }


    private void saveMatrix(File file, double[][] matrix) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        writer.write("\"item_id\"");
        for (int i = 0; i < numFactors; i++) {
            writer.write(',');
            writer.write("\"factor");
            writer.write(Integer.toString(i));
            writer.write("\"");
        }
        writer.write("\r\n");
        BiMap<Integer, String> items = itemMappingData.inverse();
        for (int itemIdx = 0; itemIdx < numItems; itemIdx++) {
            writer.write('\"');
            writer.write(items.get(itemIdx));
            writer.write('\"');
            for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                writer.write(',');
                writer.write(Double.toString(matrix[factorIdx][itemIdx]));
            }
            writer.write("\r\n");
        }
        writer.close();
    }
}
