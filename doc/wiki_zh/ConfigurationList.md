# Configuration list

```python
# set data directory
dfs.data.dir=../data
# set result directory
# recommender result will output in this folder
dfs.result.dir=../result
# set log directory
dfs.log.dir=../log

# convertor
# load data and splitting data
# into two (or three) set
# setting dataset name
data.input.path=filmtrust
# setting dataset format(UIR, UIRT)
data.column.format=UIR
# setting method of split data
# value can be ratio, loocv, given, KCV
data.model.splitter=ratio
#data.splitter.cv.number=5
# using rating to split dataset
data.splitter.ratio=rating
# filmtrust dataset is saved by text
# text, arff is accepted
data.model.format=text
# the ratio of trainset
# this value should in (0,1)
data.splitter.trainset.ratio=0.8

# Detailed configuration of loocv, given, KCV
# is written in User Guide

# set the random seed for reproducing the results (split data, init parameters and other methods using random)
# default is set 1l
# if do not set ,just use System.currentTimeMillis() as the seed and could not reproduce the results.
rec.random.seed=1

# binarize threshold mainly used in ranking
# -1.0 - maxRate, binarize rate into -1.0 and 1.0
# binThold = -1.0， do nothing
# binThold = value, rating > value is changed to 1.0 other is 0.0, mainly used in ranking
# for PGM 0.0 maybe a better choose
data.convert.binarize.threshold=-1.0

# evaluation the result or not
rec.eval.enable=true

# specifies evaluators
# rec.eval.classes=auc,precision,recall...
# if rec.eval.class is blank
# every evaluator will be calculated
# rec.eval.classes=auc,precision,recall

# evaluator value set is written in User Guide
# if this algorithm is ranking only true or false
rec.recommender.isranking=false

#can use user,item,social similarity, default value is user, maximum values:user,item,social
#rec.recommender.similarities=user
```
## others

### random
为了保证每次生成的结果是可以复现的, 随机数字的初始值通过配置项`rec.ramdom.seed`来设置
示例配置如下:

```
rec.random.seed=1
```

java示例代码如下

```java
conf.set("rec.random.seed","1");
```

### verbose
对于部分推荐算法, 可以选择将每次迭代的学习情况打印输出. 涉及到的配置项为`rec.recommender.verbose`. 示例配置如下:

```java
rec.recommender.verbose=true
```

java示例代码如下:

```java
conf.set("rec.recommender.verbose","true")
```