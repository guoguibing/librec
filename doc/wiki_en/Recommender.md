
# Recommender

## Similarity

The similarity matrix is used to construct the distance between user and user or item and item in the data set.
The example is shown as follows.

```
rec.similarity.class=cos
rec.recommender.similarities=user
```
`rec.similarity.class` is the distance measure function. `rec.recommender.similarities` is the distance of a specified user-user pair or a item-item pair. For the Social recommendation algorithms, users can also specify the distance of a social-social pair.

In the Java program, the corresponding sample program is shown as follows.

```java
conf.set("rec.recommender.similarities","user");
RecommenderSimilarity similarity = new CosineSimilarity();
similarity.buildSimilarityMatrix(dataModel);
```

After generating the RecommenderSimilarity object, users need to call the buildSimilarityMatrix method to build the similarity matrix.

The table below is the distance measure functions and corresponding abbreviations in LibRec.


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

When users use the configuration and command line to run programs, the recommendation algorithm is specified by `rec.recommender.class`. The configuration is shown as follows.

```
rec.recommender.class=shortname #e.g. aobpr
```

Please refer to [Algorithm list.md](./Algorithm list) for the abbreviations of different algorithms.

In the Java implementation, after making instances of the Configuration object, the DataModel object, and the Similarity matrix object, these three instances are passed in as constructor parameters to generate the RecommenderContext object. Users can make the corresponding instance of the recommendation algorithm, that is to say, no need to set the `rec.recommedner.class` configuration. The example code is shown as follows.

```java
RecommenderContext context = new RecommenderContext(conf, dataModel, similarity);

conf.set("rec.neighbors.knn.number","50");
conf.set("rec.recommender.isranking=false");

Recommender recommender = new UserKNNRecommender();
recommender.recommend(context);
```

Each recommendation algorithm has its own configurations according to different algorithm categories. Current recommendation algorithms implemented in LibRec are matrix-decomposition-based, factor-factorization-based, probability-graph-model-based, and tensor-based. The following part gives different configurations based on different algorithms. In general, taking the matrix factorization for example, making recommendations by inheriting the interface of matrix factorization algorithms not only needs to set the configuration of matrix factorization, but also needs to set other configurations. For example, the BPMF also needs to configure `rec.recommender.user.mu` and other configurations.

The implementation of the recommendation algorithms based on the above algorithms, which only needs to inherit the corresponding abstract classes. Different algorithms are listed in the following table. All the recommendation algorithm configurations are listed in [Algorithm List](./AlgorithmList).


### AbstractRecommender

```
# if ranking
rec.recommender.isranking=true
rec.recommender.ranking.topn=10

```

The algorithms that directly implement the AbstractRecommender abstract class are shown as follows.

| directory path | short name           | algorithm                       |
|----------------|----------------------|---------------------------------|
| baseline       | constantguess        | ConstantGuessRecommender        |
| baseline       | globalaverage        | GlobalAverageRecommender        |
| baseline       | itemaverage          | ItemAverageRecommender          |
| baseline       | mostpopular          | MostPopularRecommender          |
| baseline       | randomguess          | RandomGuessRecommender          |
| baseline       | useraverage          | UserAverageRecommender          |
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

The algorithms that directly inherit the Probabilistic Graphical Recommender class are shown as follows.

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

The algorithms that directly or indirectly inherit the Matrix Factorization Recommender class are shown as follows.

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
| cf.rating      | rbm          | RBMRecommender          |
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

The algorithms that directly inherit the Factorization Machine Recommender class are shown as follows.

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

The algorithms that directly inherit the Social Recommender class are shown as follows.

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

The algorithms that directly inherit the Tensor Recommender class are shown as follows.

| directory path | short name | algorithm       |
|----------------|------------|-----------------|
| context.rating | bptf       | BPTFRecommender |
| context.rating | pitf       | PITFRecommender |


## Implement your own algorithm
Implementing users' own algorithms in LibRec needs to inherit the corresponding abstract class according to the algorithm's category. And users need to implement the abstract methods, or re-write the methods in the abstract class.
In order to inherit the AbstractRecommender class, for example, the implementation of an algorithm is shown as follows.

```
1. Re-write the setup method (optional)
	The main task of the setup method is to initialize the member variables. For example, reading arguments from the configuration file. If the algorithm does not need to set extra configurations, the program does not need to re-write the methods.
	It should be noted that, in the rewrite of the setup method, users need to call the original abstract class in the setup method. That is, to put super.setup() in the first line of the implementation, to ensure that the basic parameters of the algorithm is initialized.

2. Implement the trainModel Method
	The trainModel method accomplish the task of training the model. For example, using gradient descent to optimize the cost function of the model.

3. Implement the Predict Method
	The task of the Predict method is to use the trained model to make predictions.
	For example, for rating prediction algorithms, the predict method is used to predict each rating in the test set. Particularly, for a given userIndex and itemIndex, using the model to predict the rating score of userIndex-itemIndex pair.

```
Please refer to the exact implementation in the recommender directory.
