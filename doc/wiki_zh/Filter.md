# Filter
Filter可以在评估时根据一定规则来过滤掉部分数据。
Filter的过滤对象是由recommender产生的recommendedList，recommendedList由一组recommendedItem构成，每个recommendedItem表示为一个三元组:(userId itemId value)。
目前支持的过滤器为GenericRecommendedFilter，其功能是返回recommendedList中包含指定userId或itemId的recommendedItem，指定的userId和itemId在GenericRecommendedFilter中以列表的形式提前设置。
目前Filter仅支持在Java代码中使用.

GenericRecommendedFilter过滤效果:

```
userIdList = {"1", "2"}
recommendedList = {
    {userId:1 itemId:1 value:1.0},
    {userId:1 itemId:2 value:2.0},
    {userId:1 itemId:3 value:3.0},
    {userId:2 itemId:1 value:4.0},
    {userId:2 itemId:2 value:5.0},
    {userId:2 itemId:3 value:6.0},
    {userId:3 itemId:1 value:7.0},
    {userId:3 itemId:2 value:8.0},
    {userId:3 itemId:3 value:9.0}
}
filtered recommendedList = {
    {userId:1 itemId:2 value:2.0},
    {userId:2 itemId:3 value:6.0},
    {userId:1 itemId:1 value:1.0},
    {userId:2 itemId:1 value:4.0},
    {userId:2 itemId:2 value:5.0},
    {userId:1 itemId:3 value:3.0}
}
```

GenericRecommendedFilter使用示例:

```java

// specify the userIds and itemIds for filter
userIdList = new ArrayList<>();
itemIdList = new ArrayList<>();
for (int i=1; i<=2; i++) {
    userIdList.add(Integer.toString(i));
    itemIdList.add(Integer.toString(4-i));
}

// generate recommendedList by recommender
Configuration conf = new Configuration();
Resource resource = new Resource("rec/cf/userknn-test.properties");
conf.addResource(resource);
DataModel dataModel = new TextDataModel(conf);
dataModel.buildDataModel();
RecommenderContext context = new RecommenderContext(conf, dataModel);
RecommenderSimilarity similarity = new PCCSimilarity();
similarity.buildSimilarityMatrix(dataModel);
context.setSimilarity(similarity);
Recommender recommender = new UserKNNRecommender();
recommender.setContext(context);
recommender.recommend(context);
List<RecommendedItem> recommendedItemList = recommender.getRecommendedList();

// filter the recommendedList with GenericRecommendedFilter
GenericRecommendedFilter filter = new GenericRecommendedFilter();
filter.setUserIdList(userIdList);
filter.setItemIdList(itemIdList);
recommendedItemList = filter.filter(recommendedItemList);
```
