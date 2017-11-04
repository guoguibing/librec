package net.librec.recommender.ext;

import net.librec.common.LibrecException;
import net.librec.math.structure.DenseMatrix;
import net.librec.math.structure.SparseVector;
import net.librec.recommender.AbstractRecommender;

/**
 * Biploar Slope One: Lemire and Maclachlan,
 * <strong>
 * Slope One Predictors for Online Rating-Based Collaborative Filtering
 * </strong>, SDM 2005.
 *
 * @author Qian Shaofeng
 *
 */
public class BipolarSlopeOneRecommender extends AbstractRecommender{
    /**
     * matrices for item-item differences with number of occurrences/cardinality
     */
    private DenseMatrix likeDevMatrix, dislikeDevMatrix, likeCardMatrix, dislikeCardMatrix;

    /**
     * the user rating average, use int value can get high accuracy
     */
    private int[] averageRating;
    /**
     * initialization
     *
     * @throws LibrecException if error occurs
     */
    @Override
    protected void setup() throws LibrecException {
        super.setup();

        likeDevMatrix = new DenseMatrix(numItems, numItems);
        likeCardMatrix = new DenseMatrix(numItems, numItems);

        dislikeDevMatrix = new DenseMatrix(numItems, numItems);
        dislikeCardMatrix = new DenseMatrix(numItems, numItems);

        averageRating = new int[numUsers];
    }

    /**
     * train model
     *
     * @throws LibrecException if error occurs
     */
    @Override
    protected void trainModel() throws LibrecException {

        // compute items' differences
        for (int userIdx = 0; userIdx < numUsers; userIdx++) {
            SparseVector itemRatingsVector = trainMatrix.row(userIdx);
            averageRating[userIdx] = (int) itemRatingsVector.mean();
            int[] items = itemRatingsVector.getIndex();

            for (int itemIdx : items) {
                double userItemRating = itemRatingsVector.get(itemIdx);
                for (int comparedItemIdx : items) {
                    if (itemIdx != comparedItemIdx) {

                        double comparedRating = itemRatingsVector.get(comparedItemIdx);
                        if (userItemRating >= averageRating[userIdx] && comparedRating >= averageRating[userIdx]) {
                            likeDevMatrix.add(itemIdx, comparedItemIdx, userItemRating - comparedRating);
                            likeCardMatrix.add(itemIdx, comparedItemIdx, 1);
                        }
                        else if (userItemRating < averageRating[userIdx] && comparedRating < averageRating[userIdx]){
                            dislikeDevMatrix.add(itemIdx, comparedItemIdx, userItemRating - comparedRating);
                            dislikeCardMatrix.add(itemIdx, comparedItemIdx, 1);
                        }
                    }
                }
            }

        }

        // normalize differences
        for (int itemIdx = 0; itemIdx < numItems; itemIdx++) {
            for (int comparedItemIdx = 0; comparedItemIdx < numItems; comparedItemIdx++) {
                double card = likeCardMatrix.get(itemIdx, comparedItemIdx);
                if (card > 0) {
                    double sum = likeDevMatrix.get(itemIdx, comparedItemIdx);
                    likeDevMatrix.set(itemIdx, comparedItemIdx, sum / card);
                }

                card = dislikeCardMatrix.get(itemIdx, comparedItemIdx);
                if (card > 0) {
                    double sum = dislikeDevMatrix.get(itemIdx, comparedItemIdx);
                    dislikeCardMatrix.set(itemIdx, comparedItemIdx, sum / card);
                }
            }
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
    @Override
    protected double predict(int userIdx, int itemIdx) throws LibrecException {
        SparseVector itemRatingsVector = trainMatrix.row(userIdx, itemIdx);
        double predictRatings = 0, cardinaryValues = 0;
        for (int comparedItemIdx : itemRatingsVector.getIndex()) {
            double cardinaryValue = likeCardMatrix.get(itemIdx, comparedItemIdx);
            if (cardinaryValue > 0) {
                predictRatings += (likeDevMatrix.get(itemIdx, comparedItemIdx) + itemRatingsVector.get(comparedItemIdx)) * cardinaryValue;
                cardinaryValues += cardinaryValue;
            }

            cardinaryValue = dislikeCardMatrix.get(itemIdx, comparedItemIdx);
            if (cardinaryValue > 0) {
                predictRatings += (dislikeDevMatrix.get(itemIdx, comparedItemIdx) + itemRatingsVector.get(comparedItemIdx)) * cardinaryValue;
                cardinaryValues += cardinaryValue;
            }

        }
        return cardinaryValues > 0 ? predictRatings / cardinaryValues : globalMean;
    }
}
