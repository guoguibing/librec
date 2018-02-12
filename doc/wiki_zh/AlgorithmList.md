# Algorithm list
----

### Recommender Algorithm List
| superClass                                           | directory path  | short name           | algorithm                       |
|------------------------------------------------------|-----------------|----------------------|---------------------------------|
| AbstractRecommender                                  | baseline        | constantguess        | ConstantGuessRecommender        |
| AbstractRecommender                                  | baseline        | globalaverage        | GlobalAverageRecommender        |
| AbstractRecommender                                  | baseline        | itemaverage          | ItemAverageRecommender          |
| ProbabilisticGraphicalRecommender                    | baseline        | itemcluster          | ItemClusterRecommender          |
| AbstractRecommender                                  | baseline        | mostpopular          | MostPopularRecommender          |
| AbstractRecommender                                  | baseline        | randomguess          | RandomGuessRecommender          |
| AbstractRecommender                                  | baseline        | useraverage          | UserAverageRecommender          |
| ProbabilisticGraphicalRecommender                    | baseline        | usercluster          | UserClusterRecommender          |
| MatrixFactorizationRecommender                       | cf.ranking      | aobpr                | AoBPRRecommender                |
| ProbabilisticGraphicalRecommender                    | cf.ranking      | aspectmodelranking   | AspectModelRecommender          |
| MatrixFactorizationRecommender                       | cf.ranking      | bpr                  | BPRRecommender                  |
| MatrixFactorizationRecommender                       | cf.ranking      | climf                | CLIMFRecommender                |
| MatrixFactorizationRecommender                       | cf.ranking      | eals                 | EALSRecommender                 |
| MatrixFactorizationRecommender                       | cf.ranking      | fismauc              | FISMaucRecommender              |
| MatrixFactorizationRecommender                       | cf.ranking      | fismrmse             | FISMrmseRecommender             |
| MatrixFactorizationRecommender                       | cf.ranking      | gbpr                 | GBPRRecommender                 |
| ProbabilisticGraphicalRecommender                    | cf.ranking      | itembigram           | ItemBigramRecommender           |
| ProbabilisticGraphicalRecommender                    | cf.ranking      | lda                  | LDARecommender                  |
| MatrixFactorizationRecommender                       | cf.ranking      | Listwisemf           | ListwiseMFRecommender           |
| ProbabilisticGraphicalRecommender                    | cf.ranking      | plsa                 | PLSARecommender                 |
| MatrixFactorizationRecommender                       | cf.ranking      | rankals              | RankALSRecommender              |
| MatrixFactorizationRecommender                       | cf.ranking      | ranksgd              | RankSGDRecommender              |
| AbstractRecommender                                  | cf.ranking      | slim                 | SLIMRecommender                 |
| MatrixFactorizationRecommender                       | cf.ranking      | wbpr                 | WBPRRecommender                 |
| MatrixFactorizationRecommender                       | cf.ranking      | wrmf                 | WRMFRecommender                 |
| ProbabilisticGraphicalRecommender                    | cf.rating       | aspectmodelrating    | AspectModelRecommender          |
| BiasedMFRecommender → MatrixFactorizationRecommender | cf.rating      | asvdpp               | ASVDPlusPlusRecommender         |
| MatrixFactorizationRecommender                       | cf.rating       | biasedmf             | BiasedMFRecommender             |
| MatrixFactorizationRecommender                       | cf.rating       | bnpoissmf            | BNPoissMFRecommender            |
| MatrixFactorizationRecommender                       | cf.rating       | bpmf                 | BPMFRecommender                 |
| MatrixFactorizationRecommender                       | cf.rating       | bpoissmf             | BPoissMFRecommender             |
| FactorizationMachineRecommender                      | cf.rating       | fmals                | FMALSRecommender                |
| FactorizationMachineRecommender                      | cf.rating       | fmsgd                | FMSGDRecommender                |
| ProbabilisticGraphicalRecommender                    | cf.rating       | gplsa                | GPLSARecommender                |
| ProbabilisticGraphicalRecommender                    | cf.rating       | ldcc                 | LDCCRecommender                 |
| MatrixFactorizationRecommender                       | cf.rating       | llorma               | LLORMARecommender               |
| MatrixFactorizationRecommender                       | cf.rating       | mfals                | MFALSRecommender                |
| MatrixFactorizationRecommender                       | cf.rating       | nmf                  | NMFRecommender                  |
| MatrixFactorizationRecommender                       | cf.rating       | pmf                  | PMFRecommender                  |
| AbstractRecommender                                  | cf.rating       | rbm                  | RBMRecommender                  |
| MatrixFactorizationRecommender                       | cf.rating       | rfrec                | RFRecRecommender                |
| BiasedMFRecommender → MatrixFactorizationRecommender | cf.rating       | svdpp          | SVDPlusPlusRecommender          |
| ProbabilisticGraphicalRecommender                    | cf.rating       | urp                  | URPRecommender                  |
| ProbabilisticGraphicalRecommender                    | cf              | bhfree               | BHFreeRecommender               |
| ProbabilisticGraphicalRecommender                    | cf              | bucm                 | BUCMRecommender                 |
| AbstractRecommender                                  | cf              | itemknn              | ItemKNNRecommender              |
| AbstractRecommender                                  | cf              | userknn              | UserKNNRecommender              |
| BiasedMFRecommender → MatrixFactorizationRecommender | content         | efm                  | EFMRecommender                  |
| BiasedMFRecommender → MatrixFactorizationRecommender | content         | hft                  | HFTRecommender                  |
| SocialRecommender                                    | context.ranking | sbpr                 | SBPRRecommender                 |
| TensorRecommender                                    | context.rating  | bptf                 | BPTFRecommender                 |
| TensorRecommender                                    | context.rating  | pitf                 | PITFRecommender                 |
| SocialRecommender                                    | context.rating  | rste                 | RSTERecommender                 |
| SocialRecommender                                    | context.rating  | socialmf             | SocialMFRecommender             |
| SocialRecommender                                    | context.rating  | sorec                | SoRecRecommender                |
| SocialRecommender                                    | context.rating  | soreg                | SoRegRecommender                |
| BiasedMFRecommender → MatrixFactorizationRecommender | context.rating  | timesvd              | TimeSVDRecommender              |
| SocialMFRecommender                                  | context.rating  | trustmf              | TrustMFRecommender              |
| SocialRecommender                                    | context.rating  | trustsvd             | TrustSVDRecommender             |
| AbstractRecommender                                  | ext             | associationrule      | AssociationRuleRecommender      |
| AbstractRecommender                                  | ext             | external             | ExternalRecommender             |
| AbstractRecommender                                  | ext             | personalitydiagnosis | PersonalityDiagnosisRecommender |
| RankSGDRecommender → MatrixFactorizationRecommender  | ext             | prankd               | PRankDRecommender               |
| AbstractRecommender                                  | ext             | slopeone             | SlopeOneRecommender             |
| AbstractRecommender                                  | hybrid          | hybrid               | HybridRecommender               |

### Algorithm Configuration List
#### Baseline
##### ConstantGuessRecommender
```
rec.recommender.class=constantguess
```
##### GlobalAverageRecommender
```
rec.recommender.class=globalaverage
```
##### ItemAverageRecommender
```
rec.recommender.class=itemaverage
```
##### ItemClusterRecommender
```
rec.recommender.class=itemcluster
rec.pgm.number=10
rec.iterator.maximum=20
```
##### MostPopularRecommender
```
rec.recommender.class=mostpopular
rec.recommender.isranking=true
```
##### RandomGuessRecommender
```
rec.recommender.class=randomguess
```
##### UserAverageRecommender
```
rec.recommender.class=useraverage
```
##### UserClusterRecommender
```
rec.recommender.class=usercluster
rec.factory.number=10
rec.iterator.maximum=20
```
#### Collaborative Filtering (item ranking)
##### AOBPRRecommender
```
rec.recommender.class=aobpr
rec.item.distribution.parameter = 500
rec.iterator.learnrate=0.01
rec.iterator.learnrate.maximum=0.01
rec.iterator.maximum=100
rec.user.regularization=0.01
rec.item.regularization=0.01
rec.factor.number=10
rec.learnrate.bolddriver=false
rec.learnrate.decay=1.0
rec.recommender.isranking=true
rec.recommender.ranking.topn=10
```
##### AspectModelRecommender
```
rec.recommender.class=aspectmodelranking
rec.iterator.maximum=20
rec.recommender.isranking=true
rec.recommender.ranking.topn=10
data.splitter.cv.number=5
rec.pgm.burnin=10
rec.pgm.samplelag=10
rec.topic.number=10
```
##### BPRRecommender
```
rec.recommender.class=bpr
rec.iterator.learnrate=0.01
rec.iterator.learnrate.maximum=0.01
rec.iterator.maximum=100
rec.user.regularization=0.01
rec.item.regularization=0.01
rec.factor.number=10
rec.learnRate.bolddriver=false
rec.learnRate.decay=1.0
rec.recommender.isranking=true
rec.recommender.ranking.topn=10
```
##### CLIMFRecommender
```
rec.recommender.class=climf
rec.iterator.learnrate=0.001
rec.iterator.learnrate.maximum=0.01
rec.iterator.maximum=100
rec.user.regularization=0.01
rec.item.regularization=0.01
rec.factor.number=10
rec.learnrate.bolddriver=false
rec.learnrate.decay=1.0
rec.recommender.isranking=true
rec.recommender.ranking.topn=10
```
##### EALSRecommender
```
rec.recommender.class=eals
rec.iterator.maximum=10
rec.user.regularization=0.01
rec.item.regularization=0.01
rec.factor.number=10
rec.recommender.isranking=true
rec.recommender.ranking.topn=10

# 0：eALS MF; 1：WRMF; 2: both
rec.eals.wrmf.judge=1

# the overall weight of missing data c0
rec.eals.overall=128

# the significance level of popular items over un-popular ones
rec.eals.ratio=0.4

# confidence weight coefficient, alpha in original paper
rec.wrmf.weight.coefficient=4.0
```
##### FISMaucRecommender
```
rec.recommender.class=fismauc
rec.iterator.learnrate=0.01
rec.iterator.learnrate.maximum=0.01
rec.iterator.maximum=10
rec.user.regularization=0.01
rec.item.regularization=0.01
rec.factor.number=10
rec.learnrate.bolddriver=false
rec.learnrate.decay=1.0
rec.recommender.isranking=true
rec.recommender.ranking.topn=10

rec.fismauc.rho=2
rec.fismauc.alpha=1.5
```
##### FISMrmseRecommender
```
rec.recommender.class=fismrmse
rec.iteration.learnrate=0.01
rec.iterator.maximum=100
rec.recommender.isranking=true

rec.fismrmse.rho=1
rec.fismrmse.alpha=1.5
```
##### GBPRRecommender
```
rec.recommender.class=gbpr
rec.iterator.learnrate=0.01
rec.iterator.learnrate.maximum=0.01
rec.iterator.maximum=100
rec.user.regularization=0.01
rec.item.regularization=0.01
rec.factor.number=10
rec.learnrate.bolddriver=false
rec.learnrate.decay=1.0
rec.recommender.isranking=true
rec.recommender.ranking.topn=10
```
##### ItemBigramRecommender
```
rec.recommender.class=itembigram
data.column.format=UIRT
data.input.path=test/ratings-date.txt
rec.iterator.maximum=100
rec.topic.number=10
rec.recommender.isranking=true
rec.recommender.ranking.topn=10
rec.user.dirichlet.prior=0.01
rec.topic.dirichlet.prior=0.01
rec.pgm.burnin=10
rec.pgm.samplelag=10
```
##### LDARecommender
```
rec.recommender.class=lda
rec.iterator.maximum=100
rec.topic.number = 10
rec.recommender.isranking=true
rec.recommender.ranking.topn=10
rec.user.dirichlet.prior=0.01
rec.topic.dirichlet.prior=0.01
rec.pgm.burnin=10
rec.pgm.samplelag=10
data.splitter.cv.number=5
# (0.0 may be a better choose than -1.0)
data.convert.binarize.threshold=0.0
```
##### ListwiseMFRecommender
```
rec.recommender.class=listwisemf
rec.iterator.learnrate=0.01
rec.iterator.learnrate.maximum=0.01
rec.iterator.maximum=100
rec.user.regularization=0.01
rec.item.regularization=0.01
rec.factor.number=10
rec.learnrate.bolddriver=false
rec.learnrate.decay=1.0
rec.recommender.isranking=true
rec.recommender.ranking.topn=10
```
##### PLSARecommender
```
rec.recommender.class=plsa
rec.iteration.learnrate=0.01
rec.iterator.maximum=100
rec.recommender.isranking=true
rec.topic.number = 10
rec.recommender.ranking.topn=10
# (0.0 may be a better choose than -1.0)
data.convert.binarize.threshold=0.0
```
##### RankALSRecommender
```
rec.recommender.class=rankals
rec.iterator.learnrate=0.01
rec.iterator.learnrate.maximum=0.01
rec.iterator.maximum=100
rec.user.regularization=0.01
rec.item.regularization=0.01
rec.factor.number=10
rec.learnrate.bolddriver=false
rec.learnrate.decay=1.0
rec.recommender.isranking=true
rec.recommender.ranking.topn=10

rec.rankals.support.weight=true
```
##### RankSGDRecommender
```
rec.recommender.class=ranksgd
rec.iterator.learnrate=0.01
rec.iterator.learnrate.maximum=0.01
rec.iterator.maximum=30
rec.user.regularization=0.01
rec.item.regularization=0.01
rec.factor.number=10
rec.learnrate.bolddriver=false
rec.learnrate.decay=1.0
rec.recommender.isranking=true
rec.recommender.ranking.topn=10
```
##### SLIMRecommender
```
rec.recommender.class=slim
rec.similarity.class=cos
# can only use item similarity
rec.recommender.similarities=item
rec.iterator.maximum=40
rec.similarity.shrinkage=10
rec.recommender.isranking=true
rec.recommender.ranking.topn=10
rec.neighbors.knn.number=50
rec.recommender.earlystop=true

rec.slim.regularization.l1=1
rec.slim.regularization.l2=5
```
##### WBPRRecommender
```
rec.recommender.class=wbpr
rec.iterator.learnrate=0.01
rec.iterator.learnrate.maximum=0.01
rec.iterator.maximum=10
rec.user.regularization=0.01
rec.item.regularization=0.01
rec.bias.regularization=0.01
rec.factor.number=10
rec.learnrate.bolddriver=false
rec.learnrate.decay=1.0
rec.recommender.isranking=true
rec.recommender.ranking.topn=10
```
##### WRMFRecommender
```
rec.recommender.class=wrmf
rec.iterator.maximum=20
rec.user.regularization=0.01
rec.item.regularization=0.01
rec.factor.number=10
rec.recommender.isranking=true
rec.recommender.ranking.topn=10

# confidence weight coefficient, alpha in original paper
rec.wrmf.weight.coefficient=4.0
```
#### Collaborative Filtering (rating prediction)
##### AspectModelRecommender
```
rec.recommender.class=aspectmodelrating
rec.iteration.learnrate=0.01
rec.iterator.maximum=100
```
##### ASVDPlusPlusRecommender
```
rec.recommender.class=asvdpp
rec.iteration.learnrate=0.01
rec.iterator.maximum=20
```
##### BiasedMFRecommender
```
rec.recommender.class=biasedmf
rec.iterator.learnrate=0.01
rec.iterator.learnrate.maximum=0.01
rec.iterator.maximum=1
rec.user.regularization=0.01
rec.item.regularization=0.01
rec.bias.regularization=0.01
rec.factor.number=10
rec.learnrate.bolddriver=false
rec.learnrate.decay=1.0
```
##### BNPoissMFRecommender
```
rec.recommender.class=bnpoissmf
rec.iterator.learnrate=0.01
rec.iterator.learnrate.maximum=0.01
rec.iterator.maximum=100
rec.user.regularization=0.01
rec.item.regularization=0.01
rec.factor.number=10
rec.learnrate.bolddriver=false
rec.learnrate.decay=1.0
```
##### BPMFRecommender
```
rec.recommender.class=bpmf
rec.iterator.maximum=100
rec.user.regularization=0.01
rec.item.regularization=0.01
rec.factor.number=10
```
##### BPoissMFRecommender
```
rec.recommender.class=bpoissmf
rec.iterator.learnrate=0.01
rec.iterator.learnrate.maximum=0.01
rec.iterator.maximum=100
rec.user.regularization=0.01
rec.item.regularization=0.01
rec.factor.number=10
rec.learnrate.bolddriver=false
rec.learnrate.decay=1.0
```
##### FMALSRecommender
```
data.input.path=arfftest/data.arff
data.column.format=UIR
data.model.splitter=ratio
data.convertor.format=arff
data.model.format=arff

rec.recommender.class=fmals
rec.iterator.learnRate=0.01
rec.iterator.maximum=100
rec.factor.number=10
```
##### FMSGDRecommender
```
data.input.path=arfftest/data.arff
data.column.format=UIR
data.model.splitter=ratio
data.convertor.format=arff
data.model.format=arff

rec.recommender.class=fmsgd
rec.iterator.learnRate=0.001
rec.iterator.maximum=100
rec.factor.number=10
```
##### GPLSARecommender
```
rec.recommender.class=gplsa
rec.iteration.learnrate=0.01
rec.iterator.maximum=100
rec.recommender.smoothWeight=2
rec.recommender.isranking=false
rec.topic.number = 10
```
##### LDCCRecommender
```
rec.recommender.class=ldcc
rec.iteration.learnrate=0.01
rec.iterator.maximum=100
```
##### LLORMARecommender
```
rec.recommender.class=llorma
rec.llorma.global.factors.num = 10
rec.iterator.learnrate=0.01
rec.iterator.learnrate.maximum=0.01
rec.iterator.maximum=100
rec.user.regularization=0.01
rec.item.regularization=0.01
rec.factor.number=10
rec.learnrate.bolddriver=false
rec.learnrate.decay=1.0
```
##### MFALSRecommender
```
rec.recommender.class=mfals
rec.iterator.maximum=100
rec.user.regularization=0.01
rec.item.regularization=0.01
rec.factor.number=10
```
##### NMFRecommender
```
rec.recommender.class=nmf
rec.iterator.learnrate=0.01
rec.iterator.learnrate.maximum=0.01
rec.iterator.maximum=100
rec.user.regularization=0.01
rec.item.regularization=0.01
rec.factor.number=10
rec.learnrate.bolddriver=false
rec.learnrate.decay=1.0
```
##### PMFRecommender
```
rec.recommender.class=pmf
rec.iterator.learnrate=0.01
rec.iterator.learnrate.maximum=0.01
rec.iterator.maximum=50
rec.user.regularization=0.01
rec.item.regularization=0.01
rec.factor.number=10
rec.learnrate.bolddriver=false
rec.learnrate.decay=1.0
```
##### RBMRecommender
```
rec.recommender.class=rbm
rec.iterator.maximum=20
data.input.path=movielens/ml-100k/ratings.txt
rec.factor.number=500
rec.epsilonw=0.01
rec.epsilonvb=0.01
rec.epsilonhb=0.01
rec.tstep=1
rec.momentum=0.1
rec.lamtaw=0.01
rec.lamtab=0.0
rec.predictiontype=mean
```
##### RFRecRecommender
```
rec.recommender.class=rfrec
rec.iterator.learnrate=0.01
rec.iterator.learnrate.maximum=0.01
rec.iterator.maximum=100
rec.user.regularization=0.01
rec.item.regularization=0.01
rec.factor.number=10
rec.learnrate.bolddriver=false
rec.learnrate.decay=1.0
```
##### SVDPlusPlusRecommender
```
rec.recommender.class=svdpp
rec.iterator.learnrate=0.01
rec.iterator.learnrate.maximum=0.01
rec.iterator.maximum=13
rec.user.regularization=0.01
rec.item.regularization=0.01
rec.impItem.regularization=0.001
rec.factor.number=10
rec.learnrate.bolddriver=false
rec.learnrate.decay=1.0
```
##### URPRecommender
```
rec.recommender.class=urp
rec.iteration.learnrate=0.01
rec.iterator.maximum=100
```
#### Collaborative Filtering (rating prediction and item ranking)
##### BHFreeRecommender
```
rec.recommender.class=bhfree
rec.pgm.burnin=10
rec.pgm.samplelag=10
rec.iterator.maximum=100
# true for item ranking, false for rating prediction
rec.recommender.isranking=true
rec.recommender.ranking.topn=10
```
##### BUCMRecommender
```
rec.recommender.class=bucm
rec.pgm.burnin=10
rec.pgm.samplelag=10

rec.iterator.maximum=100
rec.pgm.topic.number=10
rec.bucm.alpha=0.01
rec.bucm.beta=0.01
rec.bucm.gamma=0.01
# true for item ranking, false for rating prediction
rec.recommender.isranking=true
rec.recommender.ranking.topn=10
```
##### ItemKNNRecommender
```
rec.recommender.class=itemknn
# true for item ranking, false for rating prediction
rec.recommender.isranking=false
rec.recommender.ranking.topn=10
rec.recommender.similarities=item
rec.similarity.class=pcc
rec.neighbors.knn.number=50
rec.similarity.shrinkage=10
```
##### UserKNNRecommender
```
rec.similarity.class=pcc
rec.neighbors.knn.number=50
rec.recommender.class=userknn
rec.recommender.similarities=user
# true for item ranking, false for rating prediction
rec.recommender.isranking=false
rec.recommender.ranking.topn=10
rec.filter.class=generic
rec.similarity.shrinkage=10
```
#### Content
##### EFMRecommender
```
data.input.path=efmtest/efm.txt
rec.recommender.class=efm
rec.iterator.learnrate=0.01
rec.iterator.learnrate.maximum=0.01
rec.iterator.maximum=100
rec.user.regularization=0.01
rec.item.regularization=0.01
rec.factor.number=10
rec.learnrate.bolddriver=false
rec.learnrate.decay=1.0
rec.recommender.lambda.user=0.05
rec.recommender.lambda.item=0.05
rec.bias.regularization = 0.01
```
##### HFTRecommender
```
data.input.path=hfttest/hft.txt/
rec.recommender.class=hft
rec.iterator.learnrate=0.01
rec.iterator.learnrate.maximum=0.01
rec.iterator.maximum=2
rec.user.regularization=0.01
rec.item.regularization=0.01
rec.factor.number=10
rec.learnrate.bolddriver=false
rec.learnrate.decay=1.0
rec.eval.enable = 1
rec.recommender.lambda.user=0.05
rec.recommender.lambda.item=0.05
rec.bias.regularization = 0.01
```
#### Context(item ranking)
##### SBPRRecommender
```
data.appender.class=social
data.appender.path=test/test-append-dir

rec.recommender.class=sbpr
rec.iterator.learnrate=0.01
rec.iterator.learnrate.maximum=0.01
rec.iterator.maximum=200
rec.user.regularization=0.01
rec.item.regularization=0.01
rec.bias.regularization=0.01
rec.factor.number=128
rec.learnrate.bolddriver=false
rec.learnrate.decay=1.0
rec.recommender.earlystop=false
rec.recommender.verbose=true
rec.recommender.isranking=true
rec.recommender.ranking.topn=10
```
#### Context(rating prediction)
##### BPTFRecommender
```
rec.recommender.class=bptf
rec.iteration.learnrate=0.01
rec.iterator.maximum=100
```
##### PITFRecommender
```
rec.recommender.class=pitf
rec.iteration.learnrate=0.01
rec.iterator.maximum=100
```
##### RSTERecommender
```
data.appender.class=social
data.appender.path=test/test-append-dir

rec.recommender.class=rste
rec.iterator.learnrate=0.05
rec.iterator.learnrate.maximum=0.05
rec.iterator.maximum=200
rec.user.regularization=0.01
rec.item.regularization=0.01
rec.social.regularization=0.01
rec.factor.number=10
rec.learnrate.bolddriver=false
rec.learnrate.decay=1.0
rec.recommender.earlystop=false
rec.recommender.verbose=true
rec.user.social.ratio=0.8
```
##### SocialMFRecommender
```
data.appender.class=social
data.appender.path=test/test-append-dir

rec.recommender.class=socialmf
rec.iterator.learnrate=0.05
rec.iterator.learnrate.maximum=0.05
rec.iterator.maximum=100
rec.user.regularization=0.01
rec.item.regularization=0.01
rec.social.regularization=0.01
rec.factor.number=10
rec.learnrate.bolddriver=false
rec.learnrate.decay=1.0
rec.recommender.earlystop=false
rec.recommender.verbose=true
```
##### SoRecRecommender
```
data.appender.class=social
data.appender.path=test/test-append-dir

rec.recommender.class=sorec
rec.iterator.learnrate=0.05
rec.iterator.learnrate.maximum=0.05
rec.iterator.maximum=1000
rec.user.regularization=0.01
rec.item.regularization=0.01
rec.rate.social.regularization=0.01
rec.user.social.regularization=0.01
rec.social.regularization=0.01
rec.factor.number=10
rec.learnrate.bolddriver=false
rec.learnrate.decay=1.0
rec.recommender.earlystop=false
rec.recommender.verbose=true
```
##### SoRegRecommender
```
data.appender.class=social
data.appender.path=test/test-append-dir

rec.recommender.class=soreg
rec.recommender.similarities=social
rec.similarity.class=pcc
rec.iterator.learnrate=0.001
rec.iterator.learnrate.maximum=0.01
rec.iterator.maximum=10
rec.user.regularization=0.01
rec.item.regularization=0.01
rec.social.regularization=0.01
rec.factor.number=10
rec.learnrate.bolddriver=false
rec.learnrate.decay=1.0
rec.recommender.earlystop=false
rec.recommender.verbose=true
rec.similarity.shrinkage=10
```
##### TimeSVDRecommender
```
rec.recommender.class=timesvd
data.column.format=UIRT
data.input.path=test/ratings-date.txt
rec.iterator.learnrate=0.01
rec.iterator.learnrate.maximum=0.01
rec.iterator.maximum=100
rec.user.regularization=0.01
rec.item.regularization=0.01
rec.learnrate.decay=1.0
```
##### TrustMFRecommender
```
data.appender.class=social
data.appender.path=test/test-append-dir

rec.recommender.class=trustmf
rec.iterator.learnrate=0.01
rec.iterator.learnrate.maximum=0.01
rec.iterator.maximum=30
rec.user.regularization=0.01
rec.item.regularization=0.01
rec.social.regularization=0.01
rec.factor.number=10
rec.learnrate.bolddriver=false
rec.learnrate.decay=1.0
rec.recommender.earlystop=false
rec.recommender.verbose=true
rec.social.model=T
```
##### TrustSVDRecommender
```
data.appender.class=social
data.appender.path=test/test-append-dir

rec.recommender.class=trustsvd
rec.iterator.learnrate=0.01
rec.iterator.learnrate.maximum=0.01
rec.iterator.maximum=50
rec.user.regularization=0.01
rec.item.regularization=0.01
rec.social.regularization=0.01
rec.bias.regularization=0.01
rec.factor.number=10
rec.learnrate.bolddriver=false
rec.learnrate.decay=1.0
rec.recommender.earlystop=false
rec.recommender.verbose=true
```
#### Extra
##### AssociationRuleRecommender
```
rec.recommender.class=associationrule
```
##### ExternalRecommender
```
rec.recommender.class=external
```
##### PersonalityDiagnosisRecommender
```
rec.recommender.class=personalitydiagnosis
rec.PersonalityDiagnosis.sigma=0.1
```
##### PRankDRecommender
```
rec.recommender.class=prankd
rec.similarity.class=cos
rec.recommender.similarities=item
rec.similarity.shrinkage=10
rec.iterator.learnrate=0.01
rec.iterator.learnrate.maximum=0.01
rec.iterator.maximum=50
rec.user.regularization=0.01
rec.item.regularization=0.01
rec.factor.number=10
rec.learnrate.bolddriver=false
rec.learnrate.decay=1.0
rec.sim.filter=4.0
```
##### SlopeOneRecommender
```
rec.recommender.class=slopeone
rec.eval.enable=true
rec.iterator.maximum=50
rec.factory.number=30
rec.iterator.learn.rate=0.001
rec.recommender.lambda.user=0.05
rec.recommender.lambda.item=0.05
```
#### Hybrid
##### HybridRecommender
```
rec.recommender.class=hybrid
rec.hybrid.lambda=0.1
rec.iterator.maximum=50
rec.factory.number=30
rec.iterator.learn.rate=0.001
rec.recommender.lambda.user=0.05
rec.recommender.lambda.item=0.05
```
