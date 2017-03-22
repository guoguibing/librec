
# Recommender

## Similarity

相似性矩阵用于构建数据集中user-user或者item-item之间的距离.
在使用命令行配置时, 示例配置如下:

```
rec.similarity.class=cos
rec.recommender.similarities=user
```
其中`rec.similarity.class`为指定相应的距离度量函数. `rec.recommender.similarities`为指定user-user之间的距离或者item-item之间的距离. 在Social类的推荐方法中, 还可以指定计算social-social之间的距离.

在Java程序中, 相应的示例程序如下

```java
conf.set("rec.recommender.similarities","user");
RecommenderSimilarity similarity = new CosineSimilarity();
similarity.buildSimilarityMatrix(dataModel);
```

生成RecommenderSimilarity对象之后, 还需要调用buildSimilarityMatrix方法来进行相似度矩阵的计算.

下表为LibRec中已经实现的距离度量算法以及相应的简写.


|         Similarity        | shortname |
|:-------------------------:|:---------:|
|    BinaryCosineSimilarity |    bcos   |
|      CosineSimilarity     |    cos    |
|       CPCSimilarity       |    cpc    |
|       MSESimilarity       |   msesim  |
|       MSDSimilarity       |    msd    |
|       PCCSimilarity       |    pcc    |
|       KRCCSimilarity      |    krcc   |
| DiceCoefficientSimilarity |    dice   |
|     JaccardSimilarity     |  jaccard  |
|    ExJaccardSimilarity    | exjaccard |


## Algorithms

在使用配置项和命令行运行LibRec时, 执行的推荐算法由配置项`rec.recommender.class`指定.配置如下

```
rec.recommender.class=shortname #e.g. aobpr
```

不同算法的简写请参阅[Algorithm list.md](./Algorithm list)

在java实现中, 实例Configuration对象, DataModel对象, Similarity矩阵对象之后, 作为RecommenderContext的构造器参数生成RecommenderContext的对象. 此处可以直接实例相应的推荐算法类, 因此无需设置配置项`rec.recommedner.class`. 示例代码如下:

```java
RecommenderContext context = new RecommenderContext(conf, dataModel, similarity);

conf.set("rec.neighbors.knn.number","50");
conf.set("rec.recommender.isranking=false");

Recommender recommender = new UserKNNRecommender();
recommender.recommend(context);
```

推荐算法根据不同的计算方式具有各自相应的配置项. 目前LibRec中使用的推荐算法分别有基于矩阵分解算法, 基于因子分解机算法, 概率图模型, 基于张量的算法等. 下面依次给出基于不同算法的推荐算法配置项. 一般来说, 以矩阵分解为例, 继承矩阵分解接口的推荐算法在进行推荐计算时除去配置矩阵分解的配置项之外, 还需要配置其他配置项. 如BPMF算法中,还需要配置`rec.recommender.user.mu`等配置项.

实现基于以上算法的推荐系统只需继承相应的抽象类即可. 不同算法的配置项列举在下一栏, 目前已经实现的所有推荐算法配置项列举在[Algorithm  List](./AlgorithmList)中.


### AbstractRecommender

```
# if ranking
rec.recommender.isranking=true
rec.recommender.ranking.topn=10

```

直接实现AbstractRecommender抽象类的算法有

| directory path | short name           | algorithm                       |
|----------------|----------------------|---------------------------------|
| baseline       | constantguess        | ConstantGuessRecommender        |
| baseline       | globalaverage        | GlobalAverageRecommender        |
| baseline       | itemaverage          | ItemAverageRecommender          |
| baseline       | mostpopular          | MostPopularRecommender          |
| baseline       | randomguess          | RandomGuessRecommender          |
| baseline       | useraverage          | UserAverageRecommender          |
| cf.rating      | rbm                  | RBMRecommender                  |
| cf             | itemknn              | ItemKNNRecommender              |
| cf             | userknn              | UserKNNRecommender              |
| cf.ranking     | slim                 | SLIMRecommender                 |
| ext            | associationrule      | AssociationRuleRecommender      |
| ext            | external             | ExternalRecommender             |
| ext            | personalitydiagnosis | PersonalityDiagnosisRecommender |
| ext            | slopeone             | SlopeOneRecommender             |
| hybrid         | hybrid               | HybridRecommender               |


### Probabilistic Graphical Recommender

```
rec.iterator.maximum=1000
rec.pgm.burn-in=100
rec.pgm.samplelag=100
```

直接继承自Probabilistic Graphical Recommender的算法有

| directory path | short name         | algorithm              |
|----------------|--------------------|------------------------|
| baseline       | itemcluster        | ItemClusterRecommender |
| baseline       | usercluster        | UserClusterRecommender |
| cf             | bhfree             | BHFreeRecommender      |
| cf             | bucm               | BUCMRecommender        |
| cf.ranking     | aspectmodelranking | AspectModelRecommender |
| cf.ranking     | itembigram         | ItemBigramRecommender  |
| cf.ranking     | lda                | LDARecommender         |
| cf.ranking     | plsa               | PLSARecommender        |
| cf.rating      | aspectmodelrating  | AspectModelRecommender |
| cf.rating      | gplsa              | GPLSARecommender       |
| cf.rating      | ldcc               | LDCCRecommender        |
| cf.rating      | urp                | URPRecommender         |

### Matrix Factorization Recommender

```
rec.iterator.maximum=100
rec.iterator.learningrate=0.01
rec.iterator.learningrate.maximum=1000
rec.user.regularization=0.01
rec.item.regularization=0.01
rec.factor.number=10
rec.learningrate.bolddriver=false
rec.learningrate.decay=1.0
```

直接或间接继承Matrix Factorization Recommender的推荐算法有

| directory path | short name   | algorithm               |
|----------------|--------------|-------------------------|
| cf.rating      | asvdpp       | ASVDPlusPlusRecommender |
| cf.rating      | svdpp        | SVDPlusPlusRecommender  |
| content        | efm          | EFMRecommender          |
| content        | hft          | HFTRecommender          |
| context.rating | timesvd      | TimeSVDRecommender      |
| cf.ranking     | aobpr        | AoBPRRecommender        |
| cf.ranking     | bpr          | BPRRecommender          |
| cf.ranking     | climf        | CLIMFRecommender        |
| cf.ranking     | eals         | EALSRecommender         |
| cf.ranking     | fismauc      | FISMaucRecommender      |
| cf.ranking     | fismrmse     | FISMrmseRecommender     |
| cf.ranking     | gbpr         | GBPRRecommender         |
| cf.ranking     | listwisemf   | ListwiseMFRecommender   |
| cf.ranking     | rankals      | RankALSRecommender      |
| cf.ranking     | ranksgd      | RankSGDRecommender      |
| cf.ranking     | wbpr         | WBPRRecommender         |
| cf.ranking     | wrmf         | WRMFRecommender         |
| cf.rating      | biasedmf     | BiasedMFRecommender     |
| cf.rating      | bnpoissmf    | BNPoissMFRecommender    |
| cf.rating      | bpmf         | BPMFRecommender         |
| cf.rating      | bpoissmf     | BPoissMFRecommender     |
| cf.rating      | llorma       | LLORMARecommender       |
| cf.rating      | mfals        | MFALSRecommender        |
| cf.rating      | nmf          | NMFRecommender          |
| cf.rating      | pmf          | PMFRecommender          |
| cf.rating      | rfrec        | RFRecRecommender        |

### Factorization Machine Recommender

```

rec.recommender.maxrate=12.0
rec.recommender.minrate=0.0

rec.factor.number=10

rec.fm.regw0=0.01
reg.fm.regW=0.01
reg.fm.regF=10
```

直接继承自Factorization Machine Recommender的算法有

| directory path | short name | algorithm        |
|----------------|------------|------------------|
| cf.rating      | fmals      | FMALSRecommender |
| cf.rating      | fmsgd      | FMSGDRecommender |

### Social recommender

```
rec.iterator.maximum=100
rec.iterator.learningrate=0.01
rec.iterator.learningrate.maximum=1000
rec.user.regularization=0.01
rec.item.regularization=0.01
rec.factor.number=10
rec.learningrate.bolddriver=false
rec.learningrate.decay=1.0

rec.social.regularization=0.01
```

直接继承自Social recommender的算法有

| directory path  	| short name 	| algorithm           	|
|-----------------	|------------	|---------------------	|
| context.rating  	| trustmf    	| TrustMFRecommender  	|
| context.ranking 	| sbpr       	| SBPRRecommender     	|
| context.rating  	| rste       	| RSTERecommender     	|
| context.rating  	| socialmf   	| SocialMFRecommender 	|
| context.rating  	| sorec      	| SoRecRecommender    	|
| context.rating  	| soreg      	| SoRegRecommender    	|
| context.rating  	| trustsvd   	| TrustSVDRecommender 	|


### TensorRecommender

```
rec.recommender.verbose=true
rec.iterator.learningrate=0.01
rec.iterator.learningrate.maximum=1000
rec.factor.number=10
rec.tensor.regularization=0.01
```

直接继承自Tensor Recommender的算法有

| directory path | short name | algorithm       |
|----------------|------------|-----------------|
| context.rating | bptf       | BPTFRecommender |
| context.rating | pitf       | PITFRecommender |


## Implement your own algorithm
在LibRec中实现自己的算法，需要按照自己算法所属的类别，继承对应的抽象类，并按要求实现抽象方法，也可以按自己需求重写抽象类中的方法。
以继承AbstractRecommender为例，实现一个算法的大致流程如下:

```
1.重写setup方法(可选)
    setup方法主要完成的任务是对算法成员变量的初始化，例如从配置文件中读取参数的操作，可以写在这里。如果算法本身不需要额外配置参数，也可以不重写这个方法。
    需要注意的是，在自己重写的setup方法中，需要首先调用原抽象类中的setup方法，即在第一行执行super.setup()，保证算法的基本参数得到初始化。

2.实现trainModel方法
    trainModel方法完成的任务是算法模型的训练，例如对模型的cost function使用gradient descent进行训练的过程，可以写在这里。

3.实现predict方法
    predict方法完成的任务是，使用训练好的模型进行预测。
    例如对于评分预测算法，在predict方法中需要对测试集中的每个评分值进行预测，即对于给定的userIndex和ItemIndex，使用模型预测它们之间的评分。
```
具体的实现代码参考LibRec中recommender目录下的算法即可。
