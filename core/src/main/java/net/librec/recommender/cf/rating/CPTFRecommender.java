package net.librec.recommender.cf.rating;

import net.librec.common.LibrecException;
import net.librec.math.structure.DenseMatrix;
import net.librec.math.structure.TensorEntry;
import net.librec.recommender.TensorRecommender;

/**
 * CANDECOMP/PARAFAC (CP) Tensor Factorization <br>
 * <p>
 * Shao W., <strong>Tensor Completion</strong> (Section 3.2), Saarland University.
 *
 * @author Guo Guibing and Keqiang Wang
 */
public class CPTFRecommender extends TensorRecommender {
    /**
     * dimension-feature matrices
     */
    private DenseMatrix[] featureMatrix;

    /*
    * (non-Javadoc)
    *
    * @see net.librec.recommender.AbstractRecommender#setup()
    */
    @Override
    protected void setup() throws LibrecException {
        super.setup();

        featureMatrix = new DenseMatrix[numDimensions];

        for (int dimIdx = 0; dimIdx < numDimensions; dimIdx++) {
            featureMatrix[dimIdx] = new DenseMatrix(dimensions[dimIdx], numFactors);
            featureMatrix[dimIdx].init(0.01d); // randomly initialization
            normalize(dimIdx);
        }
    }

    protected void normalize(int d) {
        // column-wise normalization
        for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
            double norm = 0;
            for (int featureIdx = 0; featureIdx < featureMatrix[d].numRows(); featureIdx++) {
                norm += Math.pow(featureMatrix[d].get(featureIdx, factorIdx), 2);
            }
            norm = Math.sqrt(norm);

            for (int featureIdx = 0; featureIdx < featureMatrix[d].numRows(); featureIdx++) {
                featureMatrix[d].set(featureIdx, factorIdx, featureMatrix[d].get(featureIdx, factorIdx) / norm);
            }
        }
    }

    @Override
    protected void trainModel() throws LibrecException {
        for (int iter = 1; iter < numIterations; iter++) {

            DenseMatrix[] tempFeatureMatrix = new DenseMatrix[numDimensions];
            for (int dimIdx = 0; dimIdx < numDimensions; dimIdx++) {
                tempFeatureMatrix[dimIdx] = new DenseMatrix(dimensions[dimIdx], numFactors);
            }

            // SGD Optimization
            loss = 0.0d;
            // Step 1: compute gradients
            for (TensorEntry trainTensorEntry : trainTensor) {
                int[] keys = trainTensorEntry.keys();
                double realRating = trainTensorEntry.get();
                if (realRating <= 0)
                    continue;

                double predictRating = predict(keys);
                double error = realRating - predictRating;

                loss += error * error;

                // compute gradients
                for (int dimIdx = 0; dimIdx < numDimensions; dimIdx++) {

                    for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {

                        // multiplication of other dimensions
                        double sgd = 1;
                        for (int otherDimIdx = 0; otherDimIdx < numDimensions; otherDimIdx++) {
                            if (otherDimIdx == dimIdx) {
                                continue;
                            }

                            sgd *= featureMatrix[otherDimIdx].get(keys[otherDimIdx], factorIdx);
                        }
                        tempFeatureMatrix[dimIdx].add(keys[dimIdx], factorIdx, sgd * error);
                    }
                }
            }

            // Step 2: update variables
            for (int dimIdx = 0; dimIdx < numDimensions; dimIdx++) {
                // update each featureMatrix[dimIdx](rowIdx, colIdx)
                for (int rowIdx = 0; rowIdx < featureMatrix[dimIdx].numRows(); rowIdx++) {
                    for (int colIdx = 0; colIdx < featureMatrix[dimIdx].numColumns(); colIdx++) {
                        double featureMatrixDimRowColValue = featureMatrix[dimIdx].get(rowIdx, colIdx);
                        featureMatrix[dimIdx].add(rowIdx, colIdx, learnRate * (tempFeatureMatrix[dimIdx].get(rowIdx,
                                colIdx) - reg * featureMatrixDimRowColValue));

                        loss += reg * featureMatrixDimRowColValue * featureMatrixDimRowColValue;
                    }
                }
            }

            loss *= 0.5;
            if (isConverged(iter)) {
                break;
            }
            updateLRate(iter);
        }
    }

    /**
     * predict a specific rating for user userIdx on item itemIdx with some other contexts indices, note that the
     * prediction is not bounded. It is useful for building models with no need
     * to bound predictions.
     *
     * @param keys user index, item index and context indices
     * @return predictive rating for user userIdx on item itemIdx with some other contexts indices without bound
     * @throws LibrecException if error occurs
     */
    @Override
    protected double predict(int[] keys) throws LibrecException {
        double predictRating = 0.0d;

        for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {

            double prod = 1.0d;
            for (int dimIdx = 0; dimIdx < numDimensions; dimIdx++) {
                prod *= featureMatrix[dimIdx].get(keys[dimIdx], factorIdx);
            }

            predictRating += prod;
        }
        return predictRating;
    }
}
