librec
======

A java library implementing a suit of state-of-the-art recommender algorithms. 


The implemented recommenders include: 

* lib.rec.core
  * SlopeOne: weighted slope one
  * RegSVD: regularized SVD
  * BiasedMF: user- and item-biased matrix factorization
  * PMF: probabilistic matrix factorization
  * SVDPlusPlus: SVD++
  * CLiMF: collaborative less-is-more filtering
  * SocialMF


* lib.rec.baseline
  * GlobalAverage
  * UserAverage
  * ItemAverage
  * RandomGuess
  * ConstantGuess
  * MostPopular
  * UserKNN
  * ItemKNN


**NOTE:** more algorithms are working in progress. 
