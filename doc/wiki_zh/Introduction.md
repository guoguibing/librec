# Introduction
## 1. Overview
LibRec 是一个基于java 1.7以GPL-3.0协议发布的开源推荐系统. LibRec内包含大量推荐算法并可以通过这些算法快速解决rating和ranking问题. 目前LibRec已被RecSys wiki收录.

与1.4相比, 2.0版本重新程序的整体结构, 接口更加合理, 可扩展性更强. 对于Librec内已经实现的推荐算法, 可以使用命令行和参数或者配置文件来执行. 也可以通过导入jar包用于其他项目中.

Librec 的运行需要jdk的版本 >= 1.7

## 2. Features

* **丰富的算法集** 截止到目前，LibRec已经集成了70余个各类型推荐算法。具体来说，分为基准算法、协同过滤算法、基于内容的算法、情景感知算法、混合算法和其它扩展算法等。在新的版本里，增加了40余个新算法，包括概率图模型、张量分解模型、因子分解机、基于评论的模型、深度学习模块（RBM）等新颖的算法。团队的每个核心开发人员往往负责某一类型算法的开发和测试工作。
* **良好的模块化**  相对于LibRec 1.x，新版本在底层结构上做了非常深入的优化，尤其是模块化方面。新版本的推荐库可分为以下三部分：数据预处理、推荐算法和训练后处理。在数据预处理模块，主要是数据的转换与分割。支持两种格式数据的输入和转换，一个是常见的 User-Item-Rating 格式，另一个是更通用的ARFF格式，用户还可以扩展新类型的数据以增强现有的ARFF格式。在数据分割方面，支持按Ratio，Given-N，k-fold Cross validation, Leave-one-out等方式。在推荐模型模块，包括情景感知和算法集成。情景感知指的是算法依赖的情景信息，如用户相似度；算法集成则是算法的逻辑实现。在模型训练之后，LibRec支持两种操作：一是对测试集进行评估，得到如MAE、RMSE、AUC、MAP、NDCG等测试结果；二是对给定的用户（或情景）进行评分预测或物品推荐等查询操作，用户可以通过实现filter接口自定义更多的过滤操作。
* **灵活的框架配置** LibRec新版本承袭了基于配置的特点，但是有所更新和发展。新的配置实现参考了其它知名数据挖掘工具库的实现特点，在灵活性上得到了有效的提高。具体来说，我们抽取出很多公共的配置项，也为独立的算法保留了特定的配置参数。为了提高算法的易配置性，我们为大多数算法保留了可用的供参考配置设置。
* **高效的执行性能** LibRec一直非常注重算法执行的高效性，并尽可能地优化框架结构和算法实现。与其它的推荐算法库相比，LibRec能够在得到相当的推荐性能的前提下，在更短的时间内执行完成。
* **简单的框架用法** 简单的框架用法。LibRec早期版本只能独立运行，难以集成在其它工程中使用。由于良好的模块结构，新版本既可以单独运行，也能够作为依赖库应用于其它工程中
* **良好的可扩展性** 良好的易扩展性。LibRec提供了很好的公共接口以便用户进行个性化扩展。包括数据类型、推荐算法、输出类型、评估因子、过滤器等的扩展接口。使用LibRec开发新算法，用户通常只需要关注新算法的逻辑实现，而不需要担心其它部分的实现。

## 3. Getting started
### 3.1 Clone source code from github
当前代码托管在github中, 通过以下方式可以获取源码

by git

```
git clone https://github.com/guoguibing/librec.git --recursive && cd librec
```

by wget

```
wget -c https://codeload.github.com/guoguibing/librec/zip/2.0.0-alpha && unzip 2.0.0-alpha && rm 2.0.0-alpha && cd librec-2.0.0-alpha
```

by maven

```
<dependency>
    <groupId>net.librec</groupId>
    <artifactId>librec-core</artifactId>
    <version>2.0.0</version>
</dependency>
```

### 3.2 Run a recommender in console

```
bin/librec rec -exec -D dfs.data.dir=./data -D dfs.result.dir=./result -D rec.recommender.class=globalaverage
```

如果之前接触过Hadoop等系统, 那么看这个调用方式会很熟悉. LibRec通过调用命令行来输入相应的参数并提交计算作业, 计算过程以日志形式打印在终端, 最终推荐结果保存在当前目录的result文件夹下.

### 3.3 Run a recommender in IDE
在LibRec中执行算法有两种方式:在程序中指定配置项；读取配置文件

指定配置项代码示例:

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
读取配置文件代码示例:
可以如示例中使用相对目录访问jar包中的配置文件，也可以指定自己编辑的配置文件。

```java
public void testRecommender() throws ClassNotFoundException, LibrecException, IOException {
	Resource resource = new Resource("rec/cf/itemknn-test.properties");
	conf.addResource(resource);
	RecommenderJob job = new RecommenderJob(conf);
	job.runJob();
}
```

LibRec的计算过程为构建数据模型, 实例推荐上下文并加入相似度矩阵, 实例相应的推荐算法以及评估器, 最终根据需求可以对结果进行过滤获取相应的结果.

### 3.4 What happend
Librec经过良好的封装, 可以直接通过命令行加载配置项来运行相应的代码,也可以在其他工程中分别实例Java相应的类来进行计算.

在命令行中, rec参数指定程序进行推荐, 其他参数在参数-exec 之后通过-D 或-jobconf来指定. 其中dfs.data.dir 与dfs.result.dir 分别指定了读取数据的路径与存放结果的路径. rec.recommender.class指定运行的算法. 命令行的其他用法请参考[CLI walkthrough](./CLI walkthrough). 其他算法以及配置项请参考[Algorithm list](./Algorithm list). 不同配置项的含义请参考配置文件`librec.properties`

在Java环境中, 所有配置项由实例Configuration类的对象保存. Configuration类可以通过方法`set(str, str)`来设定配置项的key-value, 也可以通过Resources类来读取配置文件. 之后根据需求来实例相应的类并调用合适的方法. 其中DataModel类根据配置文件中的路径来进行数据的读取与分割. 实例化相应的相似度类,使用buildSimilarityMatrix方法来引用数据并构建相似度矩阵. 实例化相应的推荐算法类, 根据推荐结果来进行评估并过滤输出. 具体不同类的方法请参考API Document, 不同抽象类的子类以及相应的方法请参考[DataModel](./DataModel),[Recommender](./Recommender),[Evaluator](./Evaluator),[similarity](./Similarity),[Filter](./Filter)

## 4. Need Help?
- Reporting Bugs:[github issues](https://github.com/guoguibing/librec/issues)
- Hang out with us: [gitter](https://gitter.im/librec/Lobby)
