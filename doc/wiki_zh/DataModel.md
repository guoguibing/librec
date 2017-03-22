# DataModel

DataModel类用于对推荐算法所使用的数据进行准备. 主要完成对不同类型数据的读取, 组织结构 ,分割数据集, 根据配置项对数据进行二值化处理等.
相应的, 定义为DataModel类型的变量在指向子类的方法时, 需要传出Configuration类型的对象. 其中读取文件路径等配置项可以使用setConf来设置.
相应的, 在Java代码中, 可以根据读取数据类型直接生成对应的DataModel. 生成构造器的参数为Configuration的对象. 之后调用buildDataModel进行读取和分割.具体的数据读取过程与分割过程需要在调用buildDataModel之前通过setConf的方式传入对象中. 具体java代码如下

```java
DataModel dataModel = new TextDataModel(conf);
dataModel.buildDataModel();
```

## 1. Convertor
Convertor 读取的数据集文件夹路径通过dfs.data.dir配置项读取,数据集名称通过 data.input.path指定. 之后根据数据类型配置项读取文件夹下所有数据, 并以SparseTensor形式保存. data.column.path 用于指定数据集每一列的含义.data.convertor.binarize.threshold为而值化时所采用的阈值, 即当数值大于阈值时为1, 小于阈值时为0. 当阈值小于0时, 默认不进行二值化处理.
相关配置项示例:

```
dfs.data.dir=../data
data.input.path=filmtrust # dataset name
data.column.format=UIR # user-item-rating
data.convert.binarize.threshold=-1.0
data.model.format=text
```

LibRec还可以读取UIRT格式的数据, 示例配置项如下:

```
dfs.data.dir=../data
data.input.path=filmtrust
data.column.format=UIRT
data.convertor.binarize.threshold=-1.0
data.model.format=text
```

当数据列大于4时,需要使用arff格式的数据来进行存储. 相应的推荐配置项为

```
dfs.data.dir=../data
data.input.path=arffsetname
data.model.format=arff
```

在Java中的需要使用Configuration 类的set方法来依次写入对象.

```java
Configuration conf = new Configuration();

conf.set("dfs.data.dir","../data");
conf.set("data.input.path","filmtrust");
conf.set("data.column.format","UIRT");
conf.set("data.conver.binariza.threshold","-1.0");
```

实现时可以使用DataModel完成全部操作操作, 也可以单独实例出Convertor对象用于读取数据. 进行读取以及获取相应数据的示例代码如下

```java
Configuration conf = new Configuration();

conf.set(Configured.CONF_DATA_COLUMN_FORMAT, "UIR");
conf.set("inputDataPath", conf.get("dfs.data.dir") + "/test/matrix4by4.txt");
textDataConvertor = new TextDataConvertor(conf.get("inputDataPath"));
textDataConvertor.processData();

SparseMatrix preference = textDataConvertor.getPreferenceMatrix();
SparseMatrix datetimeMatrix = textDataConvertor.getDatetimeMatrix();
```

相对应的, 对Arff格式的数据进行读取需要实例ArffDataConvertor对象. 进行读取以及获取相应数据的示例代码如下:

```java
Configuration conf = new Configuration();
conf.set("dfs.data.dir","../data");
conf.set("inputDataPath", conf.get("dfs.data.dir") + "/path/to.arff");
ArffDataConvertor arffLoder = new ArffDataConvertor(conf.get("inputDataPath"));
arffLoder.readData();
SparseTensor sparseTensor = arffLoder.getSparseTensor();
ArrayList<ArffInstance> instances = arffLoder.getInstances();
String s1 = arffLoder.getRelationName();
String s2 = arffLoder.getAttributes().get(0).getName();
```

## 2. Splitter
LibRec中含有数据的划分方式共五类, 将数据集根据一定比例划分为训练集与测试集(及验证集), 留出其中一个作为验证, 给定N个作为验证, K折交叉验证, 以及测试数据集与训练数据集等五种方式. 其中部分分割方式又含有基于user或item等其他分割方式.

### 2.1 ratio
根据比例来进行划分数据集.

```
data.model.splitter=ratio
data.splitter.ratio=rating # by rating
data.splitter.trainset.ratio=0.8 # resting data used as test set
```

在Java中示例代码如下:

```java
conf.set("data.splitter.ratio", "rating");
conf.set("data.splitter.trainset.ratio", "0.8");

// TextDataConvertor convertor = new TextDataConvertor(args)
convertor.processData();
RatioDataSplitter splitter = new RatioDataSplitter(convertor, conf);
splitter.splitData();
```

其中data.splitter.ratio的配置可以使用rating, user, userfixed, item, valid, ratingdate, userdate, itemdate. userfixed在数据量较小时可以获得更精确的划分结果. 使用valid时还需要指定data.splitter.validset.ratio. 附带有date的方法为根据数据量在date序列上进行划分. 在时间数值上较大的划分为testset, 时间数值较小的划分为trainset.

### 2.2 loocv
随机留出一位或者留出最后一位user或者item来作为测试数据, 剩下的数据作为训练数据

```
data.model.splitter=loocv
data.splitter.loocv=user
```

在Java中示例代码如下:

```java
conf.set("data.splitter.loocv", "user");
convertor.processData();

LOOCVDataSplitter splitter = new LOOCVDataSplitter(convertor, conf);
splitter.splitData();
```

其中data.splitter.loocv的配置可以使用user, item, userdate, itemdate. 附带date的配置会先按时间顺序进行排序，选取每个user或item最新的评分记录组成测试集。

### 2.3 givenn
保留n个user或item作为测试数据, 剩下的数据作为训练数据.

```
data.model.splitter=givenn
data.splitter.givenn=user
data.splitter.givenn.n=10
```

在Java中, 示例代码如下

```java
conf.set("data.splitter.givenn", "user");
conf.set("data.splitter.givenn.n", "1");
convertor.processData();

GivenNDataSplitter splitter = new GivenNDataSplitter(convertor, conf);
splitter.splitData();
```

其中data.splitter.givenn的配置可以使用user, item, userdate, itemdate.

### 2.4 kcv
K折交叉验证. 即将数据集划分为K份, 每次选择其中一份作为测试数据集, 余下数据作为验证数据集, 共进行K次.每次会进行一次评估, 在K折结束之后会再次对K折计算结果进行一次评估.

```
data.model.splitter=kcv
data.splitter.cv.number=5 # K-fold
```

在Java中, 示例代码如下

```java
convertor.processData();
KCVDataSplitter splitter = new KCVDataSplitter(convertor, conf);

for (int i = 1; i <= 6; i++) {
  // split into 5 parts by default
  splitter.splitFolds();
  splitter.splitData(i);
}
```

### 2.5 testset
预留出部分数据集作为测试数据集. 这里需要设置配置项`data.testset.path`也就是指定预留出测试数据的路径. 这个路径要在训练数据的路径之下. 也就是说, 在读取所有数据时, 预留出的测试数据也应该被读取到.

```
data.model.splitter=testset
data.testset.path=nameoftestfile/dir
```

其中Java示例代码如下

```java
conf.set("inputDataPath", conf.get("dfs.data.dir") + "/given-testset");
conf.set(Configured.CONF_DATA_COLUMN_FORMAT, "UIR");
conf.set("data.testset.path", "/given-testset/test/ratings_0.txt");
convertor = new TextDataConvertor(conf.get(Configured.CONF_DATA_COLUMN_FORMAT), conf.get("inputDataPath"));
convertor.processData();
GivenTestSetDataSplitter splitter = new GivenTestSetDataSplitter(convertor,conf);
splitter.splitData();
```

## 3. Appender

部分算法(如rste算法)需要引入不同user或者不同item之间的关系矩阵, 因此在LibRec中实现了针对于user-user-relation 或者item-item-relation 的数据读取类.在配置文件中需要增加对配置项`data.appender.class`与`data.appender.path`的配置.

通过文件的示例配置如下

```java
data.appender.class=social
data.appender.path=directory/to/relationData
```

在Java中, 示例代码如下:

```java
String inputPath = conf.get("dfs.data.dir") + "/" + conf.get("data.input.path");
TextDataConvertor textDataConvertor = new TextDataConvertor(inputPath);
textDataConvertor.processData();
conf.set("data.appender.path", "test/datamodeltest/trust.txt");
SocialDataAppender dataFeatrue = new SocialDataAppender(conf);
dataAppender.setUserMappingData(textDataConvertor.getUserIds());
dataAppender.processData();
```

## 4. Java code Snippet
如果用于LibRec中的算法, 直接通过conf来设定参数并用于生成相应的DataModel是比较合理的做法. 对于Text与Arff格式的数据可以分别实例相应的DataModel来进行读取.具体实现方式如下:

```java
Configuration conf = new Configuration();

conf.set("dfs.data.dir","../data");
conf.set("data.input.path","filmtrust");
conf.set("data.column.format","UIRT");
conf.set("data.convertor.binariza.threshold","-1.0");

conf.set("data.model.splitter","ratio");
conf.set("data.splitter.ratio","rating");
conf.set("data.splitter.trainset.ratio","0.8");

DataModel dataModel = new TextDataModel(conf);
dataModel.buildDataModel();
```

arff格式的实例如下:

```java
Configuration conf = new Configuration();

conf.set("dfs.data.dir","../data");
conf.set("data.input.path","filmtrust");
conf.set("data.column.format","UIR");
conf.set("data.convertor.binariza.threshold","-1.0");

conf.set("data.model.splitter","ratio");
conf.set("data.splitter.ratio","rating");
conf.set("data.splitter.trainset.ratio","0.8");

DataModel dataModel = new ArffDataModel(conf);
dataModel.buildDataModel();
```

如果分割方式采用KCV或者Loocv等方式, 在分割数据集之前, 还需要设置测试集的索引. 示例如下

```java
Configuration conf = new Configuration();

conf.set("dfs.data.dir","../data");
conf.set("data.input.path","filmtrust");
conf.set("data.column.format","UIR");
conf.set("data.convertor.binariza.threshold","-1.0");

conf.set("data.model.splitter","kcv");
conf.set("data.splitter.cv.number","5")
conf.set("data.splitter.cv.index","1")
DataModel dataModel = new TextDataModel(conf);
dataModel.buildDataModel();
```

其中配置项data.splitter.cv.index的取值范围为1到data.splitter.cv.number. 之后执行算法时, 只是针对于当前这次训练集测试集合进行计算. 因此如果要统计所有次的结果, 还需要实现循环来分别设置配置项`"data.splitter.cv.index"`并进行分割执行算法.

相应的引入Appender矩阵的, 只用加上相应的配置项即可. 不再单独列出.

在数据读取过程中, 会检测是否已经进行的数据读取, 如果已经读取则不再进行读取和分割. 因此在同一次运行多次执行buildDataModel会重新索引训练集和测试集但是不会重新读取和分割.

目前针对于DataModel没有设置以Convertor和Splitter为参数的构造器以及set方法, 因此单独生成的Convertor, Splitter, 与Appender实例不能传入到DataModel中.
