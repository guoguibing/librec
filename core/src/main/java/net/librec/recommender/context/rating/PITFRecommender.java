package net.librec.recommender.context.rating;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.librec.math.structure.DenseMatrix;
import net.librec.math.structure.SparseMatrix;
import net.librec.math.structure.SparseVector;

import net.librec.math.structure.TensorEntry;
import net.librec.recommender.TensorRecommender;
import net.librec.common.LibrecException;
import net.librec.math.algorithm.Randoms;
import net.librec.math.algorithm.Maths;

/**
* Pairwise Interaction Tensor Factorization(PITF) <br>
*
* S Rendle, <strong>Pairwise interaction tensor factorization for personalized tag recommendation</strong> (Section 5.3).
*
* @author Guo Guibing
*
*/
public class PITFRecommender extends TensorRecommender {

	// dimension-feature matrices
	private DenseMatrix[] M;
    private TensorEntry[][] batchedTrainset;
    private float smallValue,lRate,reg;
    
	boolean isRankingPred;
	protected static int numDimensions, userDimension, itemDimension, numFactors, numIters;
	protected static int[] dimensions;

    public PITFRecommender() {
        super();
    }

    public PITFRecommender(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
        super();
    }

	@Override
	protected void setup() throws LibrecException{
		M = new DenseMatrix[numDimensions];
        //init the representation of every dimension
		for (int d = 0; d < numDimensions; d++) {
			M[d] = new DenseMatrix(dimensions[d], numFactors);
			M[d].init(smallValue); // randomly initialization
			normalize(d);
		}
	}

	protected void normalize(int d) {
		// column-wise normalization
		for (int f = 0; f < numFactors; f++) {

			double norm = 0;
			for (int r = 0; r < M[d].numRows(); r++) {
				norm += Math.pow(M[d].get(r, f), 2);
			}
			norm = Math.sqrt(norm);

			for (int r = 0; r < M[d].numRows(); r++) {
				M[d].set(r, f, M[d].get(r, f) / norm);
			}
		}
	}
	
    protected void buildWithRatingOptimization()throws Exception{
 	//batch creation
		int row = 0;
		int col = 0;
		int batchSize = 6;
		int numBatch = trainTensor.size()%batchSize>0?(trainTensor.size()/batchSize)+1:(trainTensor.size()/batchSize);
		batchedTrainset = new TensorEntry[numBatch][batchSize];
		if (batchSize<=0||batchSize>trainTensor.size()){
			return;
		}
		else{
			for (TensorEntry te : trainTensor){
				batchedTrainset[row][col] = te;
				col++;
			if (col == batchSize){
					col = 0;
					row ++;
				}
			}
		}
		//learning phrase(SGD Optimization)
		for (int iter = 1; iter < numIters; iter++) {
			double loss = 0;
			for (int i=0; i<numBatch; i++){
				DenseMatrix[] Ms = new DenseMatrix[numDimensions];
				for (int d = 0; d < numDimensions; d++) {
					Ms[d] = new DenseMatrix(dimensions[d], numFactors);
				}
				// Step 1: compute gradients
				for (TensorEntry te : batchedTrainset[i]) {
					if(te != null){
						int[] keys = te.keys();
						double rate = te.get();
						if (rate <= 0)
							continue;
						double pred = predict(keys);
						double e = rate - pred;
						loss += e * e;
						for (int d = 0; d < numDimensions; d++) {
							for (int f = 0; f < numFactors; f++) {
								double sgd = 0;
								for (int dd = 0; dd < numDimensions; dd++) {
									if (dd == d)
										continue;
									sgd += M[dd].get(keys[dd], f);
								}
								Ms[d].add(keys[d], f, sgd * e);
							}
						}
					}
				}
			    // Step 2: update variables
				for (int d = 0; d < numDimensions; d++) {
					// update each M[d](r, c)
					for (int r = 0; r < M[d].numRows(); r++) {
						for (int c = 0; c < M[d].numColumns(); c++) {
							double Mrc = M[d].get(r, c);
							M[d].add(r, c, lRate * (Ms[d].get(r, c) - reg * Mrc));
							loss += reg * Mrc * Mrc;
						}
					}
				}
			}
			loss *= 0.5;
			
		}
    }
    protected void buildWithRankingOptimization()throws Exception{
 	  for(int iter = 1; iter <= numIters; iter++) {
			double loss = 0;
			for (int s = 0, smax = 50; s < smax; s++) {
				for (TensorEntry te: trainTensor){
					// randomly draw samples
					int[] posSample = te.keys();
					int[] negSample = te.keys();


					int featureSize = posSample.length;
					int[] fiberKey = new int[featureSize-1];
					for(int i=0; i<featureSize-1; i++){
						fiberKey[i] = posSample[i];
					}

					int[] positiveValues = trainTensor.fiber(featureSize-1, fiberKey).getIndex();
					int negtiveValue = 0;
					do {
						negtiveValue = Randoms.uniform(dimensions[featureSize-1]);
					} while (Arrays.asList(positiveValues).contains(negtiveValue));
					negSample[featureSize-1] = negtiveValue;

					double xpos = predict(posSample);
					double xneg = predict(negSample);
					double xdiff = xpos - xneg;

					double vals = -Math.log(Maths.logistic(xdiff));
					loss += vals;

					double cmg = Maths.logistic(-xdiff);
					double gd = 0;

					for(int f=0; f<numFactors; f++){

						for (int d = 0; d < numDimensions-1; d++){
							gd = M[numDimensions-1].get(posSample[numDimensions-1], f)- M[numDimensions-1].get(negSample[numDimensions-1], f);
							M[d].add(posSample[d], f, lRate * (cmg*gd - reg * M[d].get(posSample[d], f)));
						}

						gd = 0;
						for(int d = 0; d < numDimensions-1; d++){
							gd += M[d].get(posSample[d], f);
						}

						M[numDimensions-1].add(posSample[numDimensions-1], f, lRate * (cmg*gd - reg * M[numDimensions-1].get(posSample[numDimensions-1], f)));
						M[numDimensions-1].add(negSample[numDimensions-1], f, lRate * (-cmg*gd - reg * M[numDimensions-1].get(negSample[numDimensions-1], f)));
					}
				}
			}
			
		}
 }

	@Override
	protected void trainModel(){
		if(isRankingPred){
			try {
				this.buildWithRankingOptimization();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else{
			try {
				this.buildWithRatingOptimization();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	protected double predict(int[] keys)  {
		double pred = 0;
		for (int f = 0; f < numFactors; f++) {
			double prod = 0;
			for (int d = 0; d < numDimensions; d++) {
				for (int d1=d+1; d1<numDimensions; d1++){
					prod += M[d].get(keys[d], f)*M[d1].get(keys[d1], f);
				}
			}
			pred += prod;
		}
		return pred;
	}
}
