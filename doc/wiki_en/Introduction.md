# Introduction
## 1. Overview
LibRec is an open source recommender system based on Java 1.7 with GPL-3.0 protocol.
LibRec contains a large number of recommendation algorithms and can quickly solve the problem of rating and ranking through these algorithms. Furthermore, LibRec has been referred in the RecSys wiki.

Compared with version 1.4, the version 2 reconstructs the whole structure of the program, and the interface is more reasonable and more scalable. Recommendation algorithm for LibRec has been achieved, users can use the command line parameters or configuration file to execute, or import the jar package for other projects.

LibRec requires the JDK version to be larger than 1.7.

## 2. Features

* **Rich Algorithms** LibRec has integrated more than 70 kinds of recommendation algorithms. Specifically, these algorithms can be divided into benchmark algorithms, collaborative filtering algorithms, content-based algorithms, context-aware algorithms, hybrid algorithms and other extended algorithms. In the new version, more than 40 new algorithms have been added, including probabilistic graph models, tensor decomposition models, factorization machines, review based models, and deep learning modules (RBM). Each core developer in the team is responsible for the development and testing on one kind of algorithm.

* **Excellent Modularities** Compared with LibRec 1.x, the underlying structure of the new version gains an in-depth optimization, especially in terms of modularity. The new version of the recommendation library can be divided into the following three modules: data preprocessing, algorithm training, and postprocessing. In the data preprocessing module, data conversion and segmentation support two formats of data input and conversion. One is the common User-Item-Rating format, and the other is the ARFF format that is more general, users can also extend new types of data to enhance the existing ARFF format. In the data segmentation, LibRec can split the data by ratio, given-N, k-fold cross validation, leave-one-out, etc.. In the recommendation model module, LibRec includes context-awareness and algorithm-integration. The context-awareness refers to the scene information, such as the user similarity, and the algorithm-integration is the logic implementation of the algorithm. After training recommendation models, LibRec supports two kinds of operations. One is to evaluate the test set, by the metrics such as MAE, RMSE, AUC, MAP, NDCG. The other is, given a user (or scene), predicting the score, or conducting item recommendation query operations. Users can also implement the filter interface to achieve more custom filters.

* **Flexible Configurations** The new version of LibRec inherited the characteristics of configurations, but has been updated and developed. The implementation of the new configuration refers to the characteristics of other well-known data mining tool libraries, and has been effectively improved in flexibility. Specifically, we extract a number of public configurations, but also retain specific configuration parameters for the independent algorithms. In order to use the configuration easily, we have reserved the available reference configurations for most algorithms.

* **Efficient Execution Performance** LibRec has always paid great attention to the efficiency of the algorithm implementation, and optimized the framework and algorithm implementation as much as possible. Compared with other recommendation algorithms, LibRec can accomplish the jobs in a shorter time than the other libraries.

* **Simple Usage** The early version of LibRec can only run independently, it is difficult to integrate the use of other projects. Due to the good structure of the module, the new version can run separately, and can also be used as a dependency library for other projects

* **Great Scalabilities** LibRec provides a good public interface for users to personalize,  including the data type, recommendation algorithm, output type, evaluation factor, filter, etc.. When using LibRec to develop a new algorithm, users typically only need to pay attention to the logic of the new algorithm, without worrying about other parts of the implementation.

## 3. Getting started
### 3.1 Clone source code from github
The latest code is hosted in GitHub, users can get the source code by the following ways.
by git

```
git clone https://github.com/guoguibing/librec.git --recursive && cd librec
```

by wget

```
wget -c https://codeload.github.com/guoguibing/librec/zip/2.0.0-alpha && unzip 2.0.0-alpha && rm 2.0.0-alpha && cd librec-2.0.0-alpha
```

### 3.2 Run a recommender in console

```
bin/librec rec -exec -D dfs.data.dir=./data -D dfs.result.dir=./result -D rec.recommender.class=globalaverage
```

If you had access to the Hadoop system before, you should be familiar with this calling method.
LibRec uses the command line to enter the appropriate parameters and submit the calculation work, the calculation process is printed in the terminal.
The final results are saved in the result folder of the current directory.

### 3.3 Run a recommender in IDE
There are two ways to conduct an algorithm in LibRec: specify the configurations in the program, or read the configuration file.

The example of specifying configurations.

```java
public static void main(String[] args) throws Exception {

        // build data model
        Configuration conf = new Configuration();
        conf.set("dfs.data.dir", "G:/LibRec/librec/data");
        TextDataModel dataModel = new TextDataModel(conf);
        dataModel.buildDataModel();

        // build recommender context
        RecommenderContext context = new RecommenderContext(conf, dataModel);

        // build similarity
        conf.set("rec.recommender.similarity.key" ,"item");
        RecommenderSimilarity similarity = new PCCSimilarity();
        similarity.buildSimilarityMatrix(dataModel);
        context.setSimilarity(similarity);

        // build recommender
        conf.set("rec.neighbors.knn.number", "5");
        Recommender recommender = new ItemKNNRecommender();
        recommender.setContext(context);

        // run recommender algorithm
        recommender.recommend(context);

        // evaluate the recommended result
        RecommenderEvaluator evaluator = new RMSEEvaluator();
        System.out.println("RMSE:" + recommender.evaluate(evaluator));

        // set id list of filter
        List<String> userIdList = new ArrayList<>();
        List<String> itemIdList = new ArrayList<>();
        userIdList.add("1");
        itemIdList.add("70");

        // filter the recommended result
        List<RecommendedItem> recommendedItemList = recommender.getRecommendedList();
        GenericRecommendedFilter filter = new GenericRecommendedFilter();
        filter.setUserIdList(userIdList);
        filter.setItemIdList(itemIdList);
        recommendedItemList = filter.filter(recommendedItemList);

        // print filter result
        for (RecommendedItem recommendedItem : recommendedItemList) {
            System.out.println(
                    "user:" + recommendedItem.getUserId() + " " +
                    "item:" + recommendedItem.getItemId() + " " +
                    "value:" + recommendedItem.getValue()
            );
        }
}
```
The example of reading configuration file is shown as follows.
Users can use the relative directory to access the configuration files in the jar package, which is shown in the example, or specify users' own configuration files.

```java
public void testRecommender() throws ClassNotFoundException, LibrecException, IOException {
	Resource resource = new Resource("rec/cf/itemknn-test.properties");
	conf.addResource(resource);
	RecommenderJob job = new RecommenderJob(conf);
	job.runJob();
}
```

The calculation process of LibRec is composed of constructing data model, making instances of the recommendation context and adding the similarity matrix, making instances of corresponding recommendation algorithms and evaluating recommendation results. Finally, according to the requirements, the results can be filtered to obtain the corresponding results

### 3.4 What happened
LibRec has good encapsulation, and can directly load configurations through the command line to run the code. Users also can adopt LibRec in other projects by making instances of corresponding Java classes.

In the command line, the rec parameter specifies the program for recommendation. Other parameters are specified by -D or -jobconf behind the -exec parameter. dfs.data.dir and dfs.result.dir specify the path to read data and store the result, respectively. The Rec.recommender.class specifies the recommendation algorithm.
Please refer to [CLI walkthrough](./CLI walkthrough) for other usage of the command line in LibRec. Please refer to [Algorithm list](./Algorithm list) for other algorithms and configurations. Please refer to the configuration file for the meaning of different `librec.properties` configurations.

In the program, all configurations are saved by instances of the Configuration class. The key-value configuration can be set by the method `set(str, str)` of the Configuration class, also can be set by reading the configuration file through the Resources class. Then the corresponding classes are invoked according to the requirements and appropriate methods. Specifically, the DataModel class reads and separates data according to the path in the configuration file.
The whole executing process of LibRec is, instantiating the similarity class, using the buildSimilarityMatrix method to reference data and constructing the similarity matrix, instantiating the algorithm class, evaluating and filtering outputs according to the recommendation results. The methods of different classes are not the same, please refer to the API Document. For different abstract subclasses and corresponding methods, please refer to the [DataModel](./DataModel), [Recommender](./Recommender), [Evaluator](./Evaluator), [similarity](./Similarity), [Filter](./Filter).

## 4. Need Help?
- Reporting Bugs:[github issues](https://github.com/guoguibing/librec/issues)
- Hang out with us: [gitter](https://gitter.im/librec/Lobby)
