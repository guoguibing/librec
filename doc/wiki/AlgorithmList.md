# Algorithm list
----

### Recommender Algorithm List
| superClass                                           | directory path  | short name           | algorithm                       |
|------------------------------------------------------|-----------------|----------------------|---------------------------------|
|MatrixRecommender |   baseline |   constantguess   |   ConstantGuessRecommender|
|MatrixRecommender |   baseline |   globalaverage   |   GlobalAverageRecommender|
|MatrixRecommender |   baseline |   itemaverage   |   ItemAverageRecommender|
|MatrixProbabilisticGraphicalRecommender |   baseline |   itemcluster   |   ItemClusterRecommender|
|MatrixRecommender |   baseline |   mostpopular   |   MostPopularRecommender|
|MatrixRecommender |   baseline |   randomguess   |   RandomGuessRecommender|
|MatrixRecommender |   baseline |   useraverage   |   UserAverageRecommender|
|MatrixProbabilisticGraphicalRecommender |   baseline |   usercluster   |   UserClusterRecommender|
|MatrixProbabilisticGraphicalRecommender |   cf |   bhfree   |   BHFreeRecommender|
|MatrixProbabilisticGraphicalRecommender |   cf |   bucm   |   BUCMRecommender|
|MatrixRecommender |   cf |   itemknn   |   ItemKNNRecommender|
|MatrixRecommender |   cf |   itemknn   |   ItemKNNRecommender|
|MatrixRecommender |   cf |   userknn   |   UserKNNRecommender|
|MatrixRecommender |   cf |   userknn   |   UserKNNRecommender|
|MatrixFactorizationRecommender |   cf.ranking |   aobpr   |   AoBPRRecommender|
|MatrixProbabilisticGraphicalRecommender |   cf.ranking |   aspectmodelranking   |   AspectModelRecommender|
|MatrixFactorizationRecommender |   cf.ranking |   bnppf   |   BNPPFRecommeder|
|MatrixFactorizationRecommender |   cf.ranking |   bpoissmf   |   BPoissMFRecommender|
|MatrixFactorizationRecommender |   cf.ranking |   bpr   |   BPRRecommender|
|MatrixFactorizationRecommender |   cf.ranking |   climf   |   CLIMFRecommender|
|MatrixFactorizationRecommender |   cf.ranking |   cofiset   |   CoFiSetRecommender|
|MatrixFactorizationRecommender |   cf.ranking |   eals   |   EALSRecommender|
|MatrixFactorizationRecommender |   cf.ranking |   fismauc   |   FISMaucRecommender|
|MatrixFactorizationRecommender |   cf.ranking |   fismrmse   |   FISMrmseRecommender|
|MatrixFactorizationRecommender |   cf.ranking |   gbpr   |   GBPRRecommender|
|MatrixProbabilisticGraphicalRecommender |   cf.ranking |   itembigram   |   ItemBigramRecommender|
|MatrixProbabilisticGraphicalRecommender |   cf.ranking |   lda   |   LDARecommender|
|MatrixFactorizationRecommender |   cf.ranking |   listrankmf   |   ListRankMFRecommender|
|MatrixFactorizationRecommender |   cf.ranking |   nmfitemitem   |   NMFItemItemRecommender|
|MatrixProbabilisticGraphicalRecommender |   cf.ranking |   plsa   |   PLSARecommender|
|MatrixFactorizationRecommender |   cf.ranking |   pnmf   |   PNMFRecommender|
|MatrixFactorizationRecommender |   cf.ranking |   rankals   |   RankALSRecommender|
|MatrixFactorizationRecommender |   cf.ranking |   rankpmf   |   RankPMFRecommender|
|MatrixFactorizationRecommender |   cf.ranking |   ranksgd   |   RankSGDRecommender|
|MatrixFactorizationRecommender |   cf.ranking |   slim   |   SLIMRecommender|
|MatrixFactorizationRecommender |   cf.ranking |   wbpr   |   WBPRRecommender|
|MatrixFactorizationRecommender |   cf.ranking |   wrmf   |   WRMFRecommender|
|MatrixProbabilisticGraphicalRecommender |   cf.rating |   aspectmodelrating   |   AspectModelRecommender|
|cf.rating.BiasedMFRecommender |   cf.rating |   asvdpp   |   ASVDPlusPlusRecommender|
|MatrixFactorizationRecommender |   cf.rating |   biasedmf   |   BiasedMFRecommender|
|MatrixFactorizationRecommender |   cf.rating |   bpmf   |   BPMFRecommender|
|FactorizationMachineRecommender |   cf.rating |   ffm   |   FFMRecommender|
|FactorizationMachineRecommender |   cf.rating |   fmals   |   FMALSRecommender|
|FactorizationMachineRecommender |   cf.rating |   fmftrl   |   FMFTRLRecommender|
|FactorizationMachineRecommender |   cf.rating |   fmsgd   |   FMSGDRecommender|
|MatrixProbabilisticGraphicalRecommender |   cf.rating |   gplsa   |   GPLSARecommender|
|MatrixFactorizationRecommender |   cf.rating |   irrg   |   IRRGRecommender|
|MatrixProbabilisticGraphicalRecommender |   cf.rating |   ldcc   |   LDCCRecommender|
|MatrixFactorizationRecommender |   cf.rating |   llorma   |   LLORMARecommender|
|MatrixFactorizationRecommender |   cf.rating |   mfals   |   MFALSRecommender|
|MatrixFactorizationRecommender |   cf.rating |   nmf   |   NMFRecommender|
|MatrixFactorizationRecommender |   cf.rating |   pmf   |   PMFRecommender|
|MatrixRecommender |   cf.rating |   rbm   |   RBMRecommender|
|MatrixFactorizationRecommender |   cf.rating |   remf   |   ReMFRecommender|
|MatrixFactorizationRecommender |   cf.rating |   rfrec   |   RFRecRecommender|
|cf.rating.BiasedMFRecommender |   cf.rating |   svdpp   |   SVDPlusPlusRecommender|
|MatrixProbabilisticGraphicalRecommender |   cf.rating |   urp   |   URPRecommender|
|TensorRecommender |   content |   convmf   |   ConvMFRecommender|
|TensorRecommender |   content |   efm   |   EFMRecommender|
|TensorRecommender |   content |   hft   |   HFTRecommender|
|TensorRecommender |   content |   tfidf   |   TFIDFRecommender|
|TensorRecommender |   content |   topicmfat   |   TopicMFATRecommender|
|TensorRecommender |   content |   topicmfmt   |   TopicMFMTRecommender|
|FactorizationMachineRecommender |   context.ranking |   dlambdafm   |   DLambdaFMRecommender|
|SocialRecommender |   context.ranking |   sbpr   |   SBPRRecommender|
|TensorRecommender |   context.rating |   cptf   |   CPTFRecommender|
|SocialRecommender |   context.rating |   rste   |   RSTERecommender|
|SocialRecommender |   context.rating |   socialmf   |   SocialMFRecommender|
|SocialRecommender |   context.rating |   sorec   |   SoRecRecommender|
|SocialRecommender |   context.rating |   soreg   |   SoRegRecommender|
|cf.rating.BiasedMFRecommender |   context.rating |   timesvd   |   TimeSVDRecommender|
|SocialRecommender |   context.rating |   trustmf   |   TrustMFRecommender|
|SocialRecommender |   context.rating |   trustsvd   |   TrustSVDRecommender|
|MatrixFactorizationRecommender |   cf.ranking |   fismauc   |   FISMaucRecommender|
|MatrixFactorizationRecommender |   cf.ranking |   fismrmse   |   FISMrmseRecommender|
|MatrixFactorizationRecommender |   cf.rating |   biasedmf   |   BiasedMFRecommender|
|MatrixFactorizationRecommender |   cf.rating |   biasedmf   |   BiasedMFRecommender|
|MatrixFactorizationRecommender |   cf.rating |   nmf   |   NMFRecommender|
|MatrixFactorizationRecommender |   cf.rating |   pmf   |   PMFRecommender|
|MatrixRecommender |   ext |   associationrule   |   AssociationRuleRecommender|
|MatrixRecommender |   ext |   bipolarslopeone   |   BipolarSlopeOneRecommender|
|MatrixRecommender |   ext |   external   |   ExternalRecommender|
|MatrixRecommender |   ext |   personalitydiagnosis   |   PersonalityDiagnosisRecommender|
|cf.ranking.RankSGDRecommender |   ext |   prankd   |   PRankDRecommender|
|MatrixRecommender |   ext |   slopeone   |   SlopeOneRecommender|
|MatrixRecommender |   hybrid |   hybrid   |   HybridRecommender|
|MatrixRecommender |   cf |   itemknn   |   ItemKNNRecommender|
|MatrixFactorizationRecommender |   cf.rating |   pmf   |   PMFRecommender|
|MatrixRecommender |   cf |   userknn   |   UserKNNRecommender|
|MatrixRecommender |   nn.ranking |   cdae   |   CDAERecommender|
|MatrixRecommender |   nn.rating |   autorec   |   AutoRecRecommender|
|MatrixFactorizationRecommender |   poi |   rankgeofm   |   RankGeoFMRecommender|
|AbstractRecommender |   poi |   usg   |   USGRecommender|


### Algorithm Configuration List
#### Baseline
#####ConstantGuessRecommender
```
rec.recommender.class=constantguess
```
#####GlobalAverageRecommender
```
rec.recommender.class=globalaverage
```
#####ItemAverageRecommender
```
rec.recommender.class=itemaverage
```
#####ItemClusterRecommender
```
rec.recommender.class=itemcluster
rec.pgm.number=10
rec.iterator.maximum=20
```
#####MostPopularRecommender
```
rec.recommender.class=mostpopular
rec.recommender.isranking=true
```
#####RandomGuessRecommender
```
rec.recommender.class=randomguess
# setting dataset format(UIR, UIRT)
data.cache = false
```
#####UserAverageRecommender
```
rec.recommender.class=useraverage
```
#####UserClusterRecommender
```
rec.recommender.class=usercluster
rec.factory.number=10
rec.iterator.maximum=20
```
#### Collaborative Filtering (item ranking)
#####AoBPRRecommender
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
#####AspectModelRecommender
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
#####BNPPFRecommeder
```
rec.recommender.class=bnppf
rec.iterator.maximum=1
rec.factor.number=10
rec.recommender.isranking=true
rec.recommender.ranking.topn=10

rec.alpha=0.3
rec.c=0.3
rec.a=0.3
rec.b=0.3
```
#####BPoissMFRecommender
```
rec.recommender.class=bpoissmf
rec.iterator.maximum=100
rec.factor.number=10
rec.recommender.isranking=true
rec.recommender.ranking.topn=10

data.convert.binarize.threshold=0.0

rec.a=0.3
rec.a.prime=0.3
rec.b.prime=1.0
rec.c=0.3
rec.c.prime=0.3
rec.d.prime=1.0
```
#####BPRRecommender
```
rec.recommender.class=bpr
rec.iterator.learnrate=0.01
rec.iterator.learnrate.maximum=0.01
rec.iterator.maximum=50
rec.user.regularization=0.01
rec.item.regularization=0.01
rec.factor.number=10
rec.learnRate.bolddriver=false
rec.learnRate.decay=1.0
data.convert.binarize.threshold=0.0
rec.recommender.isranking=true
rec.recommender.ranking.topn=10

```
#####CLIMFRecommender
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
#####CoFiSetRecommender
```
rec.recommender.class=cofiset
rec.iterator.learnrate=0.01
rec.iterator.learnrate.maximum=0.01
rec.iterator.maximum=20
rec.user.regularization=0.01
rec.item.regularization=0.01
rec.bias.regularization=0.01
rec.factor.number=10
rec.presence.size=2
rec.absence.size=1
rec.learnrate.bolddriver=false
rec.learnrate.decay=1.0
rec.recommender.isranking=true
rec.recommender.ranking.topn=10
```
#####EALSRecommender
```
rec.recommender.class=eals
rec.iterator.maximum=20
rec.user.regularization=0.01
rec.item.regularization=0.01
rec.factor.number=200
rec.recommender.isranking=true
rec.recommender.ranking.topn=10

#0：eALS MF; 1：WRMF; 2: both
rec.eals.wrmf.judge=1

#the overall weight of missing data c0
rec.eals.overall=128

#the significance level of popular items over un-popular ones
rec.eals.ratio=0.4

#confidence weight coefficient, alpha in original paper
rec.wrmf.weight.coefficient=1.0

```
#####FISMaucRecommender
```
rec.recommender.class=fismauc
rec.iteration.learnrate=0.00001
rec.iterator.maximum=5
rec.recommender.isranking=true

rec.recommender.rho=0.5
rec.recommender.beta=0.6
rec.recommender.alpha=0.9
rec.recommender.gamma=0.1
rec.factor.number=10

guava.cache.spec=maximumSize=200,expireAfterAccess=2m
```
#####FISMrmseRecommender
```
rec.recommender.class=fismrmse
rec.iteration.learnrate=0.0001
rec.iterator.maximum=14
rec.recommender.isranking=true

rec.recommender.rho=1
rec.recommender.beta=0.6
rec.recommender.alpha=0.8
rec.factor.number=10
rec.recommender.userBiasReg=0.1
rec.recommender.itemBiasReg=0.1

guava.cache.spec=maximumSize=200,expireAfterAccess=2m

```
#####GBPRRecommender
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
rec.gpbr.rho=1.5
rec.gpbr.gsize=2

```
#####ItemBigramRecommender
```
rec.recommender.class=itembigram
data.column.format=UIRT
data.input.path=test/datamodeltest/ratings-date.txt
rec.iterator.maximum=100
rec.topic.number=10
rec.recommender.isranking=true
rec.recommender.ranking.topn=10
rec.user.dirichlet.prior=0.01
rec.topic.dirichlet.prior=0.01
rec.pgm.burnin=10
rec.pgm.samplelag=10

```
#####LDARecommender
```
rec.recommender.class=lda
rec.iterator.maximum=1000
rec.topic.number = 10
rec.recommender.isranking=true
rec.recommender.ranking.topn=10
rec.user.dirichlet.prior=0.01
rec.topic.dirichlet.prior=0.01
rec.pgm.burnin=100
rec.pgm.samplelag=10
data.splitter.cv.number=5
# (0.0 maybe a better choose than -1.0)
data.convert.binarize.threshold=0.0

```
#####ListRankMFRecommender
```
rec.recommender.class=listrankmf
data.splitter.ratio=user
rec.iterator.learnrate=1.0
rec.iterator.learnrate.maximum=100
rec.iterator.maximum=30
rec.user.regularization=0.06
rec.item.regularization=0.06
rec.factor.number=5
rec.learnrate.bolddriver=false
rec.learnrate.decay=1.0
rec.recommender.isranking=true
rec.recommender.ranking.topn=10
```
#####NMFItemItemRecommender
```
rec.recommender.class=nmfitemitem
rec.iterator.maximum=50
rec.factor.number=20
rec.recommender.isranking=true
rec.recommender.ranking.topn=10
rec.nmfitemitem.do_not_estimate_yourself=true
rec.nmfitemitem.adaptive_update_rules=true
rec.nmfitemitem.parallelize_split_user_size=5000
```
#####PLSARecommender
```
rec.recommender.class=plsa
rec.iteration.learnrate=0.01
rec.iterator.maximum=100
rec.recommender.isranking=true
rec.topic.number = 10
rec.recommender.ranking.topn=10
# (0.0 maybe a better choose than -1.0)
data.convert.binarize.threshold=0.0
```
#####PNMFRecommender
```
rec.recommender.class=pnmf
rec.iterator.maximum=25
rec.factor.number=10
rec.recommender.isranking=true
rec.recommender.ranking.topn=10
data.convert.binarize.threshold=0
```
#####RankALSRecommender
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
#####RankPMFRecommender
```
rec.recommender.class=rankpmf
rec.iterator.maximum=20
rec.confidence.a = 1
rec.confidence.b = 0.01
rec.user.regularization=0.1
rec.item.regularization=10
rec.factor.number=10
rec.recommender.isranking=true
rec.recommender.ranking.topn=10
```
#####RankSGDRecommender
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
#####SLIMRecommender
```
rec.recommender.class=slim

rec.similarity.class=cos
#can only use item similarity
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
#####WBPRRecommender
```
rec.recommender.class=wbpr
rec.iterator.learnrate=0.01
rec.iterator.learnrate.maximum=0.01
rec.iterator.maximum=20
rec.user.regularization=0.01
rec.item.regularization=0.01
rec.bias.regularization=0.01
rec.factor.number=128
rec.learnrate.bolddriver=false
rec.learnrate.decay=1.0
rec.recommender.isranking=true
rec.recommender.ranking.topn=10
```
#####WRMFRecommender
```
rec.recommender.class=wrmf
rec.iterator.maximum=20
rec.user.regularization=0.01
rec.item.regularization=0.01
rec.factor.number=20
rec.recommender.isranking=true
rec.recommender.ranking.topn=10

#confidence weight coefficient, alpha in original paper
rec.wrmf.weight.coefficient=4.0
```
#### Collaborative Filtering (rating prediction)
#####AspectModelRecommender
```
rec.recommender.class=aspectmodelrating
rec.iteration.learnrate=0.01
rec.iterator.maximum=100
```
#####ASVDPlusPlusRecommender
```
rec.recommender.class=asvdpp
rec.iteration.learnrate=0.01
rec.iterator.maximum=20
```
#####BiasedMFRecommender
```
rec.recommender.class=biasedmf
rec.iterator.learnrate=0.002
rec.iterator.learnrate.maximum=0.01
rec.iterator.maximum=100
rec.user.regularization=0.01
rec.item.regularization=0.01
rec.bias.regularization=0.01
rec.factor.number=20
rec.learnrate.bolddriver=false
rec.learnrate.decay=1.0
```
#####BPMFRecommender
```
rec.recommender.class=bpmf
rec.iterator.maximum=150
rec.factor.number=20

rec.recommender.user.mu=0.0
rec.recommender.item.mu=0.0

rec.recommender.user.beta=1.0
rec.recommender.item.beta=1.0

rec.recommender.user.wishart.scale=1.0
rec.recommender.item.wishart.scale=1.0

rec.recommender.rating.sigma=2.0

# rec.learnrate.bolddriver=false
# rec.learnrate.decay=1.0
rec.recommender.gibbs.iterations = 1
```
#####FFMRecommender
```
data.input.path=test/datamodeltest/ratings.arff
data.column.format=UIR
data.model.splitter=ratio
data.convertor.format=arff
data.model.format=arff

rec.recommender.class=ffm
rec.iterator.learnRate=0.001
rec.iterator.maximum=100
rec.factor.number=10
```
#####FMALSRecommender
```
data.input.path=test/datamodeltest/ratings.arff
data.column.format=UIR
data.model.splitter=ratio
data.convertor.format=arff
data.model.format=arff

rec.recommender.class=fmals
rec.iterator.learnRate=0.01
rec.iterator.maximum=100
rec.factor.number=10
```
#####FMFTRLRecommender
```
data.input.path=test/datamodeltest/ratings.arff
data.column.format=UIR
data.model.splitter=ratio
data.convertor.format=arff
data.model.format=arff

rec.recommender.class=fmftrl

rec.iterator.maximum=30
rec.factor.number=10

rec.regularization.lambda1=0.05
rec.regularization.lambda2=1.0
rec.learningRate.alpha=0.015
rec.learningRate.beta=1
```
#####FMSGDRecommender
```
data.input.path=test/datamodeltest/ratings.arff
data.column.format=UIR
data.model.splitter=ratio
data.convertor.format=arff
data.model.format=arff

rec.recommender.class=fmsgd
rec.iterator.learnRate=0.001
rec.iterator.maximum=100
rec.factor.number=10
```
#####GPLSARecommender
```
rec.recommender.class=gplsa
rec.iteration.learnrate=0.01
rec.iterator.maximum=100
rec.recommender.smoothWeight=2
rec.recommender.isranking=false
rec.topic.number = 10
```
#####IRRGRecommender
```
rec.recommender.class=irrg
rec.iterator.learnrate=0.001
rec.iterator.learnrate.maximum=10

rec.iterator.maximum=200
rec.user.regularization=0.001
rec.item.regularization=0.001
rec.alpha=0.1
rec.factor.number=10
rec.learnrate.bolddriver=true
rec.learnrate.decay=1.0

```
#####LDCCRecommender
```
rec.recommender.class=ldcc
rec.iteration.learnrate=0.01
rec.iterator.maximum=1000
rec.pgm.burnin=100
rec.pgm.samplelag=10

rec.pgm.number.users=10
rec.pgm.number.items=10
rec.pgm.user.alpha=0.1
rec.pgm.item.alpha=0.1
rec.pgm.rating.beta=0.2
```
#####LLORMARecommender
```
rec.recommender.class=llorma
rec.global.factors.num=10
rec.global.iteration.learnrate=0.0005
rec.global.user.regularization=0.1
rec.global.item.regularization=0.1
rec.global.iteration.maximum=200
rec.iterator.learnrate=0.01
rec.iterator.learnrate.maximum=0.01
rec.iterator.maximum=200
rec.user.regularization=0.01
rec.item.regularization=0.01
rec.factor.number=6
rec.learnrate.bolddriver=false
rec.learnrate.decay=1.0
rec.model.num=55
rec.thread.count=8
```
#####MFALSRecommender
```
rec.recommender.class=mfals
rec.iterator.maximum=100
rec.user.regularization=0.01
rec.item.regularization=0.01
rec.factor.number=10
```
#####NMFRecommender
```
rec.recommender.class=nmf
rec.iterator.maximum=10
rec.factor.number=100
rec.learnrate.bolddriver=false
rec.learnrate.decay=1.0
```
#####PMFRecommender
```
rec.recommender.class=pmf
rec.iterator.learnrate=0.01
rec.iterator.learnrate.maximum=0.01
rec.iterator.maximum=70
rec.user.regularization=0.08
rec.item.regularization=0.08
rec.factor.number=6
rec.learnrate.bolddriver=false
rec.learnrate.decay=1.0
```
#####RBMRecommender
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
#####ReMFRecommender
```
data.appender.class=auxiliary
data.appender.path=twitter/user_hierarchy.txt
data.input.path=twitter/london.txt

rec.recommender.class=remf
rec.iterator.learnrate=0.0001
rec.iterator.learnrate.maximum=10

rec.iterator.maximum=130
rec.user.regularization=0.05
rec.item.regularization=0.05
rec.alpha=0.01
rec.side=user
rec.factor.number=10
rec.learnrate.bolddriver=true
rec.learnrate.decay=1.0
```
#####RFRecRecommender
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
#####SVDPlusPlusRecommender
```
rec.recommender.class=svdpp
rec.iterator.learnrate=0.002
rec.iterator.learnrate.maximum=0.05
rec.iterator.maximum=100
rec.user.regularization=0.01
rec.item.regularization=0.01
rec.impItem.regularization=0.01
rec.bias.regularization=0.01
rec.factor.number=20
rec.learnrate.bolddriver=false
rec.learnrate.decay=1.0
```
#####URPRecommender
```
rec.recommender.class=urp
rec.iteration.learnrate=0.01
rec.iterator.maximum=100
```
#####UserKNNRecommender
```
#data.input.path=filmtrust/rating
data.column.format=UIRT

rec.similarity.class=pcc
rec.neighbors.knn.number=20
rec.recommender.class=userknn
rec.recommender.similarities=user
rec.recommender.isranking=false
rec.recommender.ranking.topn=10
rec.filter.class=generic
rec.similarity.shrinkage=10
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
#####ConvMFRecommender
```
# the dataset needs decompressing first
data.input.path=test/hfttest/digital_music.arff
#data.input.path=test/hfttest/musical_instruments.arff
data.convertor.format=arff
data.model.format=arff
rec.recommender.class=convmf
rec.iterator.learnrate=0.001
rec.iterator.learnrate.maximum=0.01
rec.iterator.maximum=20
rec.user.regularization=0.1
rec.item.regularization=0.1
rec.factor.number=32
rec.learnrate.bolddriver=false
rec.learnrate.decay=1.0
rec.word2vec.path=test/hfttest/word2vec_org
rec.word2vec.dimension=200
rec.document.length=100
rec.featuremap.num=32
```
#####EFMRecommender
```
# the dataset here is sampled from th Labeled DC reviews dataset at http://yongfeng.me/dataset/
# arrange your own dataset if you need.
data.input.path=test/efmtest/dc_dense.arff
data.splitter.trainset.ratio=0.8
data.convertor.format=arff
data.model.format=arff
rec.recommender.class=efm
rec.iterator.maximum=50
rec.factor.number=10
rec.factor.explicit=5
rec.regularization.lambdax=1
rec.regularization.lambday=1
rec.regularization.lambdau=0.01
rec.regularization.lambdah=0.01
rec.regularization.lambdav=0.01

rec.explain.flag=true
rec.explain.userids=480 8517 550
rec.explain.numfeature=5


```
#####HFTRecommender
```
# The training approach is SGD instead of L-BFGS, so it can be slow if the dataset
# is big. if you want a quick test, try the path : test/hfttest/musical_instruments.arff
# path of the full dataset is : test/hfttest/musical_instruments_full.arff
data.input.path=test/hfttest/musical_instruments.arff
data.convertor.format=arff
data.model.format=arff
rec.recommender.class=hft
rec.iterator.learnrate=0.01
rec.iterator.learnrate.maximum=0.01
rec.iterator.maximum=2
rec.user.regularization=0.01
rec.item.regularization=0.01
rec.factor.number=10
rec.learnrate.bolddriver=false
rec.learnrate.decay=1.0
rec.recommender.lambda.user=0.05
rec.recommender.lambda.item=0.05
rec.bias.regularization = 0.01
```
#####TFIDFRecommender
```
# The training approach is SGD instead of L-BFGS, so it can be slow if the dataset
# is big. if you want a quick test, try the path : test/hfttest/musical_instruments.arff
# path of the full dataset is : test/hfttest/musical_instruments_full.arff
data.convert.sep = ,
data.input.path=test/hfttest/musical_instruments.arff
data.convertor.format=arff
data.model.format=arff
rec.recommender.class=tfidf
rec.recommender.isranking=true
#rec.iterator.learnrate=0.01
#rec.iterator.learnrate.maximum=0.01
#rec.iterator.maximum=2
#rec.user.regularization=0.01
#rec.item.regularization=0.01
#rec.factor.number=10
#rec.learnrate.bolddriver=false
#rec.learnrate.decay=1.0
#rec.recommender.lambda.user=0.05
#rec.recommender.lambda.item=0.05
#rec.bias.regularization = 0.01
```
#####TopicMFATRecommender
```
# The training approach is SGD instead of L-BFGS, so it can be slow if the dataset
# is big. if you want a quick test, try the path : test/hfttest/musical_instruments.arff
# path of the full dataset is : test/hfttest/musical_instruments_full.arff
data.input.path=test/hfttest/digital_music.arff
data.convertor.format=arff
data.model.format=arff
rec.recommender.class=topicmfat
rec.regularization.lambda=0.001
rec.regularization.lambdaU=0.001
rec.regularization.lambdaV=0.001
rec.regularization.lambdaB=0.001
rec.topic.number=10
rec.iterator.learnrate=0.01
rec.iterator.maximum=10
rec.init.mean=0.0
rec.init.std=0.01
```
#####TopicMFMTRecommender
```
# The training approach is SGD instead of L-BFGS, so it can be slow if the dataset
# is big. if you want a quick test, try the path : test/hfttest/musical_instruments.arff
# path of the full dataset is : test/hfttest/musical_instruments_full.arff
data.input.path=test/hfttest/digital_music.arff
data.convertor.format=arff
data.model.format=arff
rec.recommender.class=topicmfmt
rec.regularization.lambda=0.001
rec.regularization.lambdaU=0.001
rec.regularization.lambdaV=0.001
rec.regularization.lambdaB=0.001
rec.topic.number=10
rec.iterator.learnrate=0.01
rec.iterator.maximum=10
rec.init.mean=0.0
rec.init.std=0.01
```
#### Context(item ranking)
#####DLambdaFMRecommender
```
rec.recommender.class=dlambdafm

data.convertor.format=arff
data.model.format=arff
data.input.path=test/lambdafm/music.arff
data.convert.binarize.threshold=0.0

rec.iterator.learnrate=0.01
rec.iterator.learnrate.maximum=0.01
rec.iterator.maximum=30
rec.user.regularization=0.01
rec.item.regularization=0.01
rec.factor.number=30
rec.learnRate.bolddriver=false
rec.learnRate.decay=1.0
rec.recommender.isranking=true
rec.recommender.ranking.topn=10
rec.recommender.rho=0.3
rec.recommender.lossf=2
```
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
rec.social.regularization=0.01
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
#####CPTFRecommender
```
data.input.path=test/datamodeltest/ratings.arff
data.column.format=UIR
data.model.splitter=ratio
data.convertor.format=arff
data.model.format=arff
rec.random.seed=1

rec.recommender.class=cptf
rec.iteration.learnrate=0.01
rec.iterator.maximum=100
rec.factor.number=20
rec.tensor.regularization=0.05

rec.learnrate.bolddriver=false
rec.learnrate.decay=1.0
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
### Neural Network
#####CDAERecommender
```
rec.recommender.class=cdae
rec.iterator.learnrate=0.1
rec.iterator.learnrate.maximum=0.01
rec.iterator.maximum=1000
rec.weight.regularization=0.01
rec.hidden.dimension=200
rec.hidden.activation=sigmoid
rec.output.activation=identity
rec.learnrate.bolddriver=false
rec.learnrate.decay=1.0
rec.recommender.isranking=true
rec.recommender.ranking.topn=10
data.convert.binarize.threshold=3
```
#####AutoRecRecommender
```
rec.recommender.class=autorec
rec.iterator.learnrate=0.01
rec.iterator.learnrate.maximum=0.01
rec.iterator.maximum=200
rec.weight.regularization=0.001
rec.hidden.dimension=200
rec.hidden.activation=sigmoid
rec.output.activation=identity
rec.learnrate.bolddriver=false
rec.learnrate.decay=1.0
```

### Point of Interest
#####RankGeoFMRecommender
```
data.appender.class=location
data.appender.path=poi/FourSquare/FoursquareLocation.txt
data.input.path=poi/FourSquare/checkin/trainData.txt
data.model.splitter=testset
data.testset.path=poi/FourSquare/checkin/testData.txt
rec.recommender.class=rankgeofm
rec.factor.number=100
rec.iterator.learnrate=0.001
rec.iterator.learnrate.maximum=0.001
rec.iterator.maximum=200
rec.regularization.C=1.0
rec.regularization.alpha=0.2
rec.ranking.epsilon=0.3
rec.item.knn=300
rec.learnrate.bolddriver=false
rec.learnrate.decay=1.0
rec.recommender.isranking=true
rec.eval.enable=true
rec.recommender.ranking.topn=10
```
#####USGRecommender
```
data.appender.class=location
data.appender.path=poi/Gowalla/Gowalla_poi_coos.txt
data.input.path=poi/Gowalla/checkin/Gowalla_train.txt
data.model.splitter=testset
##all user test set
#data.testset.path=poi/Gowalla/checkin/Gowalla_test.txt
##small user test set
data.testset.path=poi/Gowalla/checkin/testDataFor101users.txt
data.social.path=poi/Gowalla/Gowalla_social_relations.txt
data.convert.binarize.threshold=0.0
rec.recommender.class=usg
rec.alpha=0.1
rec.similarity.class=bcos
rec.recommender.similarities=user
rec.beta=0.1
rec.eta=0.05
rec.limit.userNum=101
rec.recommender.isranking=true
rec.recommender.ranking.topn=10
```
