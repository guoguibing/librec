package net.librec.recommender.cf.ranking;

import net.librec.common.LibrecException;
import net.librec.math.algorithm.Maths;
import net.librec.math.structure.DenseMatrix;
import net.librec.math.structure.DenseVector;
import net.librec.math.structure.MatrixEntry;
import net.librec.math.structure.VectorEntry;
import net.librec.recommender.MatrixFactorizationRecommender;

import java.util.Iterator;
import java.util.List;

/**
 * Shi et al., <strong>List-wise learning to rank with matrix factorization for
 * collaborative filtering</strong>, RecSys 2010.
 *
 * Alpha version
 * @author Yuanyuan Jin and Keqiang Wang
 */
public class ListRankMFRecommender extends MatrixFactorizationRecommender {
    public DenseVector userExp;

    protected void setup() throws LibrecException {
        super.setup();
        userFactors.init(1.0);
        userFactors.scale(0.1);
        itemFactors.init(1.0);
        itemFactors.scale(0.1);

        userExp = new DenseVector(numUsers);
        for (MatrixEntry matrixentry : trainMatrix) {
            int userIdx = matrixentry.row();
            double realRating = matrixentry.get()/maxRate;
            userExp.add(userIdx, Math.exp(realRating));
        }
    }

    @Override
    protected void trainModel() throws LibrecException {
        DenseMatrix lastUserFactors;
        DenseMatrix lastItemFactors;
        double lastLoss = getLoss(userFactors,itemFactors);

        for (int iter = 1; iter <= numIterations; iter++) {
            lastUserFactors = userFactors;
            lastItemFactors = itemFactors;

            learnRate *= 2;

            DenseMatrix tempUserFactors = new DenseMatrix(numUsers, numFactors);
            DenseMatrix tempItemFactors = new DenseMatrix(numItems, numFactors);

            for (int userIdx = 0; userIdx < numUsers; userIdx++) {
                double uexp=0.0d;
                List<Integer> items = trainMatrix.getColumns(userIdx);
                for (int itemIdx : items) {
                    uexp += Math.exp(Maths.logistic(DenseMatrix.rowMult(userFactors, userIdx, itemFactors, itemIdx)));
                }

                for (VectorEntry vectorEntry : trainMatrix.row(userIdx)) {
                    int itemIdx = vectorEntry.index();
                    double realRating = vectorEntry.get()/maxRate;
                    double predictRating = DenseMatrix.rowMult(userFactors, userIdx, itemFactors, itemIdx);
                    double normalizedRealRating = Math.exp(realRating)/userExp.get(userIdx);
                    double normalizedPredictRating = Math.exp(Maths.logistic(predictRating))/uexp;
                    double error = (normalizedPredictRating - normalizedRealRating)*Maths.logisticGradientValue(predictRating);

                    for(int factorIdx = 0; factorIdx < numFactors; factorIdx++){
                        double userFactorValue = userFactors.get(userIdx, factorIdx);
                        double itemFactorValue = itemFactors.get(itemIdx, factorIdx);

                        double userGradientValue = error * itemFactorValue ;
                        tempUserFactors.add(userIdx, factorIdx, userGradientValue);

                        double itemGradientValue = error * userFactorValue ;
                        tempItemFactors.add(itemIdx, factorIdx, itemGradientValue);
                    }
                }
            }

            userFactors = userFactors.add(userFactors.scale(-learnRate*regUser));
            userFactors = userFactors.add(tempUserFactors.scale(-learnRate));
            itemFactors = itemFactors.add(itemFactors.scale(-learnRate*regItem));
            itemFactors = itemFactors.add(tempItemFactors.scale(-learnRate));

            loss = getLoss(userFactors, itemFactors);

            while(loss > lastLoss){
                userFactors = lastUserFactors;
                itemFactors = lastItemFactors;
                learnRate /= 2;
                userFactors = userFactors.add(userFactors.scale(-learnRate*regUser));
                userFactors = userFactors.add(tempUserFactors.scale(-learnRate));
                itemFactors = itemFactors.add(itemFactors.scale(-learnRate*regItem));
                itemFactors = itemFactors.add(tempItemFactors.scale(-learnRate));

                loss = getLoss(userFactors, itemFactors);
            }

            String info =  " iter " + iter + ": loss = " + loss + ", delta_loss = " + (lastLoss-loss);
            LOG.info(info);

            lastLoss=loss;
        } // end of training
    }

    public  double getLoss(DenseMatrix userFactors, DenseMatrix itemFactors){
        double uexp;
        double loss = 0.0d;
        for (int userIdx = 0; userIdx < numUsers; userIdx++) {
            uexp = 0.0d;
            List<Integer> items = trainMatrix.getColumns(userIdx);
            for (int itemIdx : items) {
                uexp += Math.exp(Maths.logistic(DenseMatrix.rowMult(userFactors, userIdx, itemFactors, itemIdx)));
            }
            Iterator<VectorEntry> itemVectorIterator = trainMatrix.colIterator(userIdx);
            while (itemVectorIterator.hasNext()) {
                VectorEntry itemEntry = itemVectorIterator.next();
                int itemIdx = itemEntry.index();
                double realRating = itemEntry.get()/maxRate;
                double predictRating = DenseMatrix.rowMult(userFactors, userIdx, itemFactors, itemIdx);
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

        return  loss;
    }
}
