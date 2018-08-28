# FilmTrust

FileTrust data set was extracted from [FilmTrust](http://trust.mindswap.org/FilmTrust/).

This data set has two files, rating.txt and trust.txt. ratings.txt contains 35,497 data records, which has three attributes--userid, movieid and movieRating.

```
@INPROCEEDINGS{guo2013novel,
   author = {Guo, G. and Zhang, J. and Yorke-Smith, N.},
   title = {A Novel Bayesian Similarity Measure for Recommender Systems},
   booktitle = {Proceedings of the 23rd International Joint Conference on Artificial Intelligence (IJCAI)},
   year = {2013},
   pages = {2619-2625}
}
```
Setting the configuration in the corresponding configuration file is shown as follows.

```
# set data directory
dfs.data.dir=../data

# setting dataset name
data.input.path=filmtrust
```

Setting the configuration in the program is shown as follows.

```java
conf.set("dfs.data.dir", "../data")
conf.set("data.input.path", "filmtrust");
```