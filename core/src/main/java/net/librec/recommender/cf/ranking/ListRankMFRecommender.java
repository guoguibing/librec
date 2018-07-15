package net.librec.recommender.cf.ranking;

import net.librec.common.LibrecException;
import net.librec.math.algorithm.Maths;
import net.librec.math.structure.DenseMatrix;
import net.librec.math.structure.MatrixEntry;
import net.librec.math.structure.Vector.VectorEntry;
import net.librec.math.structure.VectorBasedDenseVector;
import net.librec.recommender.MatrixFactorizationRecommender;
import org.apache.commons.lang.ArrayUtils;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Shi et al., <strong>List-wise learning to rank with matrix factorization for
 * collaborative filtering</strong>, RecSys 2010.
 * <p>
 * Alpha version
 *
 * @author Yuanyuan Jin and Keqiang Wang
 */
public class ListRankMFRecommender extends MatrixFactorizationRecommender {
    public VectorBasedDenseVector userExp;

    protected void setup() throws LibrecException {
        super.setup();
        userFactors.init(1.0);
        userFactors.times(0.1);
        itemFactors.init(1.0);
        itemFactors.times(0.1);

        userExp = new VectorBasedDenseVector(numUsers);
        for (MatrixEntry matrixentry : trainMatrix) {
            int userIdx = matrixentry.row();
            double realRating = matrixentry.get() / maxRate;
            userExp.plus(userIdx, Math.exp(realRating));
        }
    }

    @Override
    protected void trainModel() throws LibrecException {
        DenseMatrix lastUserFactors;
        DenseMatrix lastItemFactors;
        double lastLoss = getLoss(userFactors, itemFactors);

        for (int iter = 1; iter <= numIterations; iter++) {
            lastUserFactors = userFactors;
            lastItemFactors = itemFactors;

            learnRate *= 2;

            DenseMatrix tempUserFactors = new DenseMatrix(numUsers, numFactors);
            DenseMatrix tempItemFactors = new DenseMatrix(numItems, numFactors);

            for (int userIdx = 0; userIdx < numUsers; userIdx++) {
                double uexp = 0.0d;
                int[] itemIndexes = trainMatrix.row(userIdx).getIndices();
                Integer[] inputBoxed = ArrayUtils.toObject(itemIndexes);
                List<Integer> items = Arrays.asList(inputBoxed);
                for (int itemIdx : items) {
                    uexp += Math.exp(Maths.logistic(userFactors.row(userIdx).dot(itemFactors.row(itemIdx))));
                }

                for (VectorEntry vectorEntry : trainMatrix.row(userIdx)) {
                    int itemIdx = vectorEntry.index();
                    double realRating = vectorEntry.get() / maxRate;
                    double predictRating = userFactors.row(userIdx).dot(itemFactors.row(itemIdx));
                    double normalizedRealRating = Math.exp(realRating) / userExp.get(userIdx);
                    double normalizedPredictRating = Math.exp(Maths.logistic(predictRating)) / uexp;
                    double error = (normalizedPredictRating - normalizedRealRating) * Maths.logisticGradientValue(predictRating);

                    for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                        double userFactorValue = userFactors.get(userIdx, factorIdx);
                        double itemFactorValue = itemFactors.get(itemIdx, factorIdx);

                        double userGradientValue = error * itemFactorValue;
                        tempUserFactors.plus(userIdx, factorIdx, userGradientValue);

                        double itemGradientValue = error * userFactorValue;
                        tempItemFactors.plus(itemIdx, factorIdx, itemGradientValue);
                    }
                }
            }

            userFactors = userFactors.plus(userFactors.times(-learnRate * regUser));
            userFactors = userFactors.plus(tempUserFactors.times(-learnRate));
            itemFactors = itemFactors.plus(itemFactors.times(-learnRate * regItem));
            itemFactors = itemFactors.plus(tempItemFactors.times(-learnRate));

            loss = getLoss(userFactors, itemFactors);

            while (loss > lastLoss) {
                userFactors = lastUserFactors;
                itemFactors = lastItemFactors;
                learnRate /= 2;
                userFactors = userFactors.plus(userFactors.times(-learnRate * regUser));
                userFactors = userFactors.plus(tempUserFactors.times(-learnRate));
                itemFactors = itemFactors.plus(itemFactors.times(-learnRate * regItem));
                itemFactors = itemFactors.plus(tempItemFactors.times(-learnRate));

                loss = getLoss(userFactors, itemFactors);
            }

            String info = " iter " + iter + ": loss = " + loss + ", delta_loss = " + (lastLoss - loss);
            LOG.info(info);

            lastLoss = loss;
        } // end of training
    }

    public double getLoss(DenseMatrix userFactors, DenseMatrix itemFactors) {
        double uexp;
        double loss = 0.0d;
        for (int userIdx = 0; userIdx < numUsers; userIdx++) {
            uexp = 0.0d;
            int[] itemIndexes = trainMatrix.row(userIdx).getIndices();
            Integer[] inputBoxed = ArrayUtils.toObject(itemIndexes);
            List<Integer> items = Arrays.asList(inputBoxed);
            for (int itemIdx : items) {
                uexp += Math.exp(Maths.logistic(userFactors.row(userIdx).dot(itemFactors.row(itemIdx))));
            }
            Iterator<VectorEntry> itemVectorIterator = trainMatrix.row(userIdx).iterator();
            while (itemVectorIterator.hasNext()) {
                VectorEntry itemEntry = itemVectorIterator.next();
                int itemIdx = itemEntry.index();
                double realRating = itemEntry.get() / maxRate;
                double predictRating = userFactors.row(userIdx).dot(itemFactors.row(itemIdx));
                loss -= Math.exp(realRating) / userExp.get(userIdx) * Math.log(Math.exp(Maths.logistic(predictRating)) / uexp);
            }

            for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                double userFactorValue = userFactors.get(userIdx, factorIdx);
                loss += 0.5 * regUser * userFactorValue * userFactorValue;
            }
        }

        for (int itemIdx = 0; itemIdx < numItems; itemIdx++) {
            for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                double itemFactorValue = itemFactors.get(itemIdx, factorIdx);
                loss += 0.5 * regItem * itemFactorValue * itemFactorValue;
            }
        }

        return loss;
    }
}
