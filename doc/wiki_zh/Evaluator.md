# Evaluator

RecommenderEvaluator接口用于实现对特定算法的评估. 目前实现对于ranking的评估器有AUC, AveragePrecision, AverageReciprocalHitRank, Diversity, HitRate, IdealDCG, Normalized, Precision, Recall, REciprocalRank十类评估器. 对于rating实现评估器MAE, MPE, MSE, RMSE四类.

若在配置文件中不进行评估器的指定, 最终评估时使用对应类型的全部评估器.

指定评估器的配置示例:

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

不同评估器相应的简写见下表

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

注意, 在评估器进行配置时, 对于rating的算法如果配置ranking的评估期, 可能最后也会生成结果, 但是此评估结果不具有意义, 亦无法与其他算法进行比较.
与rating相比, 使用ranking算法还需要配置rec.recommender.ranking.topn.

在使用java进行实现评估器时, 评估器的实例作为Recommender类中evaluate方法的参数传入Recommender的对象中. 其中Recommender的对象可以为不同的推荐算法. 而RecommenderEvaluator的实例也可以使用不同的评估器. 在Java中也可以采用多种不同的评估器来对结果进行评估, 只需生成相应的评估器即可.
示例代码如下:

```java
RecommenderEvaluator evaluator = new MAEEvaluator();
double evalValue = recommender.evaluate(evaluator);
```
