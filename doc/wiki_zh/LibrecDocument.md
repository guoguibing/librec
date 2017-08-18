# Librec Document


---

# introduce to Librec
## Overview
LibRec 是一个基于java 1.7以GPL-3.0协议发布的开源推荐系统. LibRec内包含大量推荐算法并可以通过这些算法快速解决rating和ranking问题. 目前LibRec已被RecSys wiki收录.

与1.4相比, 2.0版本重新程序的整体结构, 接口更加合理, 可扩展性更强. 对于已经实现的程序, 可以使用命令行和参数或者配置文件来执行. 开发新推荐算法可以通过继承相应的抽象类来实现.

因Librec中系统结构发生变化, 对于1.4中实现的算法, 需要做以下调整来适配LibRec 2.0:



## QuickStart
### getting LibRec
### Run examples in shell
Linux And MAC:
```bash
bash librec rec -exec -conf ../core/src/mian/resources/rec/baseline/usercluster-test.properties
```
Windows:

### Command Line Usage
librec.sh \[CATEGORY\] -exec \[OPTIONS\]
  where CATEGORY is one of:
    data:
    rec:
  OPTIONS:
  -load
  -save
  -D|-jobconf:
  -conf
  -libjars

### using property file to configure settings
Librec通过properties格式来定义配置文件.
数据的读取以及数据集划分算法等可以直接在driver.classes.props进行定义
不同推荐系统的个性化推荐算法可以通过配置项定义之后, 可以使用-conf来进行加载

### include librec on other project


# learning more about LibRec
## LibRec Basics
### brief summary of LibRec structure
librec保存在
## Data propercess pipeline
### prepare data
#### data structure
稀疏矩阵保存时同时以CSR与CSC的格式保存
#### loading data
读取数据的类为Convertor, 通过readData方法来进行数据的读取,相关的配置文件示例如下:

#### splitting data
目前项目中分别实现了通过ratio进行数据分割, K折交叉验证, leave-one-out, 与GivenN四种分割方式. 配置文件实例如下:

### running an algorithms
#### setting similarity
#### choose one recommender algorithm
目前有基于矩阵分解等推荐算法共六十余种,
#### evaluate the result
librec对于rating的算法实现了MAE, MPE, MSE, RMSE四种评估算法
对ranking算法实现了AUE, AveragePrecision等十种评估算法

# recommand algorithms
## baseline
## ranking
## rating
## content


## Other important package
### io
### JobStatus
### Reflection
librec通过property配置项来读取并生成相应的类


# Complete other algorithms
在librec中, 推荐算法的的原理被分为以下几类, 基于矩阵分解, 基于因子分解, 基于概率图模型, 基于张量. 实现的推荐算法可以根据相应类别继承自相应抽象类, 也可以直接继承AbstractRecommender抽象类.

# Librec API Documents

# Frequently Asked Questions
