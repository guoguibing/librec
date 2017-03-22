# FilmTrust

FilmTrust为2011年从网站[FilmTrust](http://trust.mindswap.org/FilmTrust/) 完整抓取下来的数据集.

本数据集由两部分组成：ratings.txt 和 trust.txt.其中
ratings.txt包含35497条数据, 保存形式为 userid, movieid, movieRating
trust.txt包含1853条数据，保存形式为 trustorId, trusteeId, trustRating

```
@INPROCEEDINGS{guo2013novel,
   author = {Guo, G. and Zhang, J. and Yorke-Smith, N.},
   title = {A Novel Bayesian Similarity Measure for Recommender Systems},
   booktitle = {Proceedings of the 23rd International Joint Conference on Artificial Intelligence (IJCAI)},
   year = {2013},
   pages = {2619-2625}
}
```
在配置文件中配置使用该文件的配置项为

```
# set data directory
dfs.data.dir=../data

# setting dataset name
data.input.path=filmtrust
```

在java程序中配置方法为

```java
conf.set("dfs.data.dir", "../data")
conf.set("data.input.path", "filmtrust");
```