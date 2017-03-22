# Evaluator

The RecommenderEvaluator interface is used to evaluate a specific algorithm. Currently, ranking evaluators include AUC, AveragePrecision, AverageReciprocalHitRank, Diversity, HitRate, IdealDCG, Normalized, Precision, Recall, and REciprocalRank. The evaluators for rating involve MAE, MPE, MSE, RMSE.

If users did not specify the evaluator in the configuration file, evaluation will be applied by using all the corresponding evaluators.

The example to specify an evaluator.

rating:

```
rec.recommender.isranking=false
rec.eval.enable=true
rec.eval.class=mse # if rating
```

ranking:

```
rec.recommender.isranking=true
rec.eval.enable=true
rec.eval.class=auc
rec.recommender.ranking.topn=10
```

The abbreviation of evaluators.

|             Evaluator             | configuration |
|:---------------------------------:|:-------------:|
|            AUCEvaluator           |      auc      |
|     AveragePrecisionEvaluator     |       ap      |
| AverageReciprocalHitRankEvaluator |      arhr     |
|         DiversityEvaluator        |   diversity   |
|          HitRateEvaluator         |    hitrate    |
|         IdealDCGEvaluator         |      idcg     |
|       NormalizedDCGEvaluator      |      ndcg     |
|         PrecisionEvaluator        |   precision   |
|          RecallEvaluator          |     recall    |
|      ReciprocalRankEvaluator      |       rr      |
|            MAEEvaluator           |      mae      |
|            MPEEvaluator           |      mpe      |
|            MSEEvaluator           |      mse      |
|           RMSEEvaluator           |      rmse     |

Specifically, when setting the ranking evaluator for rating algorithms, the results may be generated but meaningless, and cannot be used to compare with other algorithms.
Compared with rating algorithms, applying ranking metrics needs to further set rec.recommender.ranking.topn.

When implementing new evaluators, the instance of the new evaluator is an argument of the evaluating method in the Recommender class, which be passed to the object of the Recommender class. Especially, the Recommender object can be different algorithms. And the instance of RecommenderEvaluator can use different evaluators.
Users can use multiple evaluators to evaluate the recommendation results.
The example code is shown bellow.

```java
RecommenderEvaluator evaluator = new MAEEvaluator();
double evalValue = recommender.evaluate(evaluator);
```
