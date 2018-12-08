package net.librec.increment.rating;

import net.librec.increment.IncrementalRatingRecommender;

abstract public class KNNRecommender extends IncrementalRatingRecommender {

     // number of neighbors to take into account for predictions
     public int K = 80;

     // underlying baseline predictor
     protected UserItemBaseline baselinesPredictor = new UserItemBaseline();


}

