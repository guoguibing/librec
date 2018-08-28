# DataModel

The DataModel class is used for data preparation. It is applied to read, split, and binarize the data.
Making an instance of the subclass of the DataModel class needs to pass in an object of the Configuration class. Users can use the setConf method to set the data reading path and other configurations.
In the Java code, users can generate the DataModel instance according to the type of data. The argument of the generated constructor is an object of the Configuration class. Examples are shown below.

```java
DataModel dataModel = new TextDataModel(conf);
dataModel.buildDataModel();
```

## 1. Convertor
Convertor reads the data in a directory where the data path is stored in dfs.data.dir. The name of the data set is specified by setting the data.input.path configuration. After setting configurations, Convertor reads all the data in a certain directory and saves them as a SparseTensor instance. data.column.path is used to designate the meaning of each column. data.convertor.binarize.threshold is used as the threshold for binarizing, which means when the value is larger than the threshold, this value would be set to one. When the value is less than the threshold, this value would be set to zero. When the threshold is less than zero, the default setting would not apply binarizing.
Examples are shown below.

```
dfs.data.dir=../data
data.input.path=filmtrust # dataset name
data.column.format=UIR # user-item-rating
data.convert.binarize.threshold=-1.0
data.model.format=text
```

LibRec can also read UIRT data. The example is shown as follows.

```
dfs.data.dir=../data
data.input.path=filmtrust
data.column.format=UIRT
data.convertor.binarize.threshold=-1.0
data.model.format=text
```

When the number of columns is larger than four, it is needed to store the data via the ARFF (Attribute-Relation File Format) format. The example is demonstrated below.

```
dfs.data.dir=../data
data.input.path=arffsetname
data.model.format=arff
```

In the Java code, users need to apply the set method of the Configuration class to set the generated object.

```java
Configuration conf = new Configuration();

conf.set("dfs.data.dir","../data");
conf.set("data.input.path","filmtrust");
conf.set("data.column.format","UIRT");
conf.set("data.conver.binariza.threshold","-1.0");
```

Users can use the DataModel class to accomplish all the operations related to data reading. Also, users can make an instance of the Convertor class then read the data. Examples are shown below.

```java
Configuration conf = new Configuration();

conf.set(Configured.CONF_DATA_COLUMN_FORMAT, "UIR");
conf.set("inputDataPath", conf.get("dfs.data.dir") + "/test/matrix4by4.txt");
textDataConvertor = new TextDataConvertor(conf.get("inputDataPath"));
textDataConvertor.processData();

SparseMatrix preference = textDataConvertor.getPreferenceMatrix();
SparseMatrix datetimeMatrix = textDataConvertor.getDatetimeMatrix();
```

Users need to make an instance of the ArffDataConvertor class when read the ARFF data. The example is shown as follows.

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
LibRec has several ways to split the data. First, data can be split to the train set, test set (and validation set) following a certain ratio. Second, leaving one sample as the validation set. Third, leaving several (N) samples as the validation set. Fourth, K-fold cross-validation. Specifically, users can apply the mentioned methods to split the data on users or items.

### 2.1 ratio
Split the data according to a ratio.

```
data.model.splitter=ratio
data.splitter.ratio=rating # by rating
data.splitter.trainset.ratio=0.8 # resting data used as test set
```

The example of the Java code.

```java
conf.set("data.splitter.ratio", "rating");
conf.set("data.splitter.trainset.ratio", "0.8");

// TextDataConvertor convertor = new TextDataConvertor(args)
convertor.processData();
RatioDataSplitter splitter = new RatioDataSplitter(convertor, conf);
splitter.splitData();
```

The options of data.splitter.ratio are rating, user, userfixed, item, valid, ratingdate, userdate, itemdate. The userfixed configuration can achieve more accurate splitting results when the number of fixed users is small. When using the valid configuration, users need to specify data.splitter.validset.ratio. The configuration with the date is to split the data based on the time sequence. The data with a smaller time stamp is divided as the train set, and the rest is the test set.

### 2.2 loocv
Randomly pick up one user or item, or select the last user or item as the test data, and the rest as the train data.

```
data.model.splitter=loocv
data.splitter.loocv=user
```

The example is shown below.

```java
conf.set("data.splitter.loocv", "user");
convertor.processData();

LOOCVDataSplitter splitter = new LOOCVDataSplitter(convertor, conf);
splitter.splitData();
```

The options of data.splitter.loocv are user, item, userdate, itemdate. The configuration with a date is to split data according to the time stamp. The latest rating record of the user or item would be selected as the test set.

### 2.3 givenn
Keep N users or items as the test data, and the rest as the train data.

```
data.model.splitter=givenn
data.splitter.givenn=user
data.splitter.givenn.n=10
```

In the Java code, the example is shown below.

```java
conf.set("data.splitter.givenn", "user");
conf.set("data.splitter.givenn.n", "1");
convertor.processData();

GivenNDataSplitter splitter = new GivenNDataSplitter(convertor, conf);
splitter.splitData();
```

The options of data.splitter.givenn are user, item, userdate, itemdate.

### 2.4 kcv
K-fold cross-validation, splits the data into K folds. Every time, it selects one fold as the test set and the rest as the train set. Evaluation would be applied on each fold. After K times, the final evaluation result would the average of all the folds.

```
data.model.splitter=kcv
data.splitter.cv.number=5 # K-fold
```

In the Java code, the example is shown below.

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
When using preserved data as the test set, users need to set the data.testset.path configuration to specify the path of preserved test data. The path of preserved data should be under the directory of the train set, which means when reading all the data, preserved data can also be read.

```
data.model.splitter=testset
data.testset.path=nameoftestfile/dir
```

In the Java code, the example is shown below.

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

A part of the algorithms needs to introduce the relationship matrix between different users and items. Thus, LibRec implements the input class of the user-user-relation or item-item-relation. When applying the relationship matrix, users need to set data.appender.class and data.appender.path.

The example of setting configurations through the configuration file is shown below.

```java
data.appender.class=social
data.appender.path=directory/to/relationData
```

In the Java code, the example is shown below.

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
Setting the instance of the Configuration class and generating the corresponding DataModel instance is a good way to execute algorithms in LibRec. As for the data format of the Text and ARFF, users can make the instances of corresponding DataModel to read the data. The example is shown below.

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
The example of the ARFF format follows.

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

If users split the data by KCV or Loocv, before splitting, the index of the test set needs to be determined. The example is shown below.

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

The range of data.splitter.cv.index is from 1 to data.splitter.cv.number. When executing algorithms, it is computed only for the current train set and test set. Thus, if all the results are needed, users have to set data.splitter.cv.index iteratively and apply the splitting algorithm correspondingly.

The program would check whether it needs to read the data in the process of data reading. If the program has read the data, reading and splitting would not be applied.
Thus, executing the buildDataModel method several times would re-index the train set and test set but not re-read and re-split the data.

Currently, there is no constructor and set method, which take instances of the Convertor class and the Splitter class as arguments in the DataModel class. Thus, the individual generated instance of the Convertor class, Splitter class, or Appender class cannot be passed to the DataModel class.
