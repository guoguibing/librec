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


import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.google.common.collect.BiMap;

import net.librec.common.LibrecException;
import net.librec.math.structure.SparseVector;
import net.librec.recommender.AbstractRecommender;
import net.librec.recommender.item.RecommendedItemList;
import net.librec.recommender.item.RecommendedList;


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
public class PNMFRecommender extends AbstractRecommender{

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
		
		LOG.info("numFactors: " + numFactors);
		LOG.info("numIterations: " + numIterations);
		
	}



	/**
	 * 
	 * This is a class only for storing results of the parallel executed tasks
	 */
	private static class AggResult {

		private final double[][] resultFactor;
		private final double[] summedLatentFactors;
		private final int[] countUsersBoughtItem;
		private final double sumLog;

		public AggResult(double[][] resultFactor, double[] summedLatentFactors, int[] countUsersBoughtItem, double sumLog) {
			this.resultFactor = resultFactor;
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
			double[][] resultFactor = new double[numFactors][numItems];
			double[] summedLatentFactors = new double[numFactors]; 
			int[] countUsersBoughtItem = new int[numItems];
			double sumLog = 0;

			for (int userIdx = fromUser; userIdx < toUser; userIdx++) {
                SparseVector itemRatingsVector = trainMatrix.row(userIdx);
                if (itemRatingsVector.getCount() > 0) {

	        		double[] thisUserLatentFactors = predictFactors(itemRatingsVector);
					for (int factorIdx = 0; factorIdx < summedLatentFactors.length; factorIdx++) {
						summedLatentFactors[factorIdx] += thisUserLatentFactors[factorIdx];
					}
					
					
					double[] sum_depends_k = new double[numFactors];
	                for (int itemIdx : itemRatingsVector.getIndex()) {
						double sum = 0;
						for (int factorIdx = 0; factorIdx < thisUserLatentFactors.length; factorIdx++) {
							sum += thisUserLatentFactors[factorIdx] * w[factorIdx][itemIdx];
						}
						double estimateFactor = 1d/sum;
						sumLog += Math.log(estimateFactor);
						countUsersBoughtItem[itemIdx]++;
						
						for (int k = 0; k < thisUserLatentFactors.length; k++) {
							resultFactor[k][itemIdx] += estimateFactor * thisUserLatentFactors[k];
						}
						for (int k = 0; k < thisUserLatentFactors.length; k++) {
							sum_depends_k[k] += estimateFactor * w[k][itemIdx];
						}
					}
	                for (int lItemIdx : itemRatingsVector.getIndex()) {
					
						for (int k = 0; k < sum_depends_k.length; k++) {
							resultFactor[k][lItemIdx] += sum_depends_k[k];
						}
					}
                }
				
			}
			return new AggResult(resultFactor, summedLatentFactors, countUsersBoughtItem, sumLog);
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
			
			double[][] resultFactor = new double[numFactors][numItems];
			double[] summedLatentFactors = new double[numFactors]; 
			int[] countUsersBoughtItem = new int[numItems];
			double sumLog = 0;
			
			// Adding all the AggResults together..
			for (Future<AggResult> future: results) {
				AggResult result = future.get();
				for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
					for (int itemIdx = 0; itemIdx < numItems; itemIdx++) {
						resultFactor[factorIdx][itemIdx] += result.resultFactor[factorIdx][itemIdx];
					}
				}
				for (int lItemIdx = 0; lItemIdx < numItems; lItemIdx++) {
					countUsersBoughtItem[lItemIdx] += result.countUsersBoughtItem[lItemIdx];
				}
				for (int kFactorIdx = 0; kFactorIdx < numFactors; kFactorIdx++) {
					summedLatentFactors[kFactorIdx] += result.summedLatentFactors[kFactorIdx];
				}
				sumLog += result.sumLog;
			}
			// Norms of w are not calculated in parallel
			double[] w_norm = new double[numFactors];
			for (int kFactorIdx = 0; kFactorIdx < numFactors; kFactorIdx++) {
				double sum = 0;
				for (int lItemIdx = 0; lItemIdx < numItems; lItemIdx++) {
					sum += w[kFactorIdx][lItemIdx];
					
				}
				w_norm[kFactorIdx] = sum;
			}
			
			// Calculation of Divergence is not needed. Only for debugging/logging purpose
			printDivergence(summedLatentFactors, countUsersBoughtItem, sumLog, w_norm, iteration);
			
			
			
			/*
			 * Multiplicative updates are done here
			 * 
			 * We use a square root for the factors... 
			 * This results in stable conversion on 'quadratic' update rules.
			 * See papers...
			 */
			for (int kFactorIdx = 0; kFactorIdx < numFactors; kFactorIdx++) {
				for (int lItemIdx = 0; lItemIdx < numItems; lItemIdx++) {
					double old = w[kFactorIdx][lItemIdx];
					double numerator = resultFactor[kFactorIdx][lItemIdx];
					double denominator = countUsersBoughtItem[lItemIdx] * w_norm[kFactorIdx] + summedLatentFactors[kFactorIdx];
					double newVal = old * StrictMath.sqrt(numerator / denominator);
					if (Double.isNaN(newVal)) {
						LOG.warn("Double.isNaN  " + numerator + " " + denominator + " " + old + " " + newVal);
					}
//					if (newVal<1e-24) {
//						LOG.info("1e-24 " + zaehler + " " + nenner + " " + old + " " + newVal);
//						newVal = 1e-24;
//					}
					w[kFactorIdx][lItemIdx] = newVal;
				}

			}
			
		}
		catch (InterruptedException | ExecutionException e) {
			LOG.error("", e);
			throw new IllegalStateException(e);
		}

	}



	private void printDivergence(double[] summedLatentFactors, int[] countUsersBoughtItem, double sumLog, double[] w_norm, int iteration) {
		int countAll = 0;
		for (int i = 0; i < countUsersBoughtItem.length; i++) {
			countAll += countUsersBoughtItem[i];
		}
		
		double sumAllEstimate = 0;
		for (int kFactorIdx = 0; kFactorIdx < numFactors; kFactorIdx++) {
			sumAllEstimate += w_norm[kFactorIdx] * summedLatentFactors[kFactorIdx];
		}
		double divergence = sumLog- countAll + sumAllEstimate;
		LOG.info("Divergence (before iteration " + iteration +")=" + divergence + "  sumLog=" + sumLog + "  countAll=" + countAll + "  sumAllEstimate=" + sumAllEstimate);
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


	@Override
	protected double predict(int userIdx, int itemIdx) throws LibrecException {
        SparseVector itemRatingsVector = trainMatrix.row(userIdx);

		return predict(itemRatingsVector, itemIdx);
	}



	/**
	 * This method is overridden only for performance reasons.
	 * 
	 * Calculate all item ratings at once for one user has much better performance than for each item user combination alone.
	 * 
	 * Effect is significant on big data
	 */
	@Override
	protected RecommendedList recommendRank() throws LibrecException {
        recommendedList = new RecommendedItemList(numUsers - 1, numUsers);

        for (int userIdx = 0; userIdx < numUsers; ++userIdx) {
            SparseVector itemRatingsVector = trainMatrix.row(userIdx);
            double[] thisUserLatentFactors = predictFactors(itemRatingsVector);
			
			
			for (int itemIdx = 0; itemIdx < numItems; itemIdx++) {
                if (itemRatingsVector.contains(itemIdx)) {
                    continue;
                }
				double predictRating = 0;
				for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
					predictRating += thisUserLatentFactors[factorIdx] * w[factorIdx][itemIdx];
				}
                if (Double.isNaN(predictRating)) {
                    continue;
                }
                recommendedList.addUserItemIdx(userIdx, itemIdx, predictRating);
			}
            recommendedList.topNRankItemsByUser(userIdx, topN);
        }

        if(recommendedList.size()==0){
            throw new IndexOutOfBoundsException("No item is recommended, there is something error in the recommendation algorithm! Please check it!");
        }

        return recommendedList;
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