package net.librec.recommender.rec;

import net.librec.common.LibrecException;
import net.librec.math.structure.SparseMatrix;

import java.util.List;

import static net.librec.math.algorithm.Maths.g;


/**
 *@author Keqiang Wang
 */
public abstract class SocialRecommender extends MatrixFactorizationRecommender {
    // socialMatrix: social rate matrix, indicating a user is connecting to a number of other users
    // trSocialMatrix: inverse social matrix, indicating a user is connected by a number of other users
    protected SparseMatrix socialMatrix;

    // social regularization
    protected float regSocial;


    // shared social cache for all social recommenders
    protected List<List<Integer>> userFriendsList;

    @Override
    public void setup() throws LibrecException {
        super.setup();
        regSocial = conf.getFloat("rec.social.regularization", 0.01f);
        // social path for the socialMatrix
        String socialPath = conf.get("dataset.social");
        socialMatrix = getDataModel().getDataFeature().getUserFeature();
    }

    @Override
    protected double predict(int userIdx, int itemIdx, boolean bounded) throws LibrecException {
        double predictRating = predict(userIdx, itemIdx);

        if (bounded)
            return denormalize(g(predictRating));

        return predictRating;
    }

    /**
     * denormalize a prediction to the region (minRate, maxRate)
     */
    protected double denormalize(double predictRating) {
        return minRate + predictRating * (maxRate - minRate);
    }

}

