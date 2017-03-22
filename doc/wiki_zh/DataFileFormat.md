# DataFileFormat

---

## Text
librec可以直接读取以文本形式保存的数据. 数据以n*3或者n*4的形式进行保存. 每一行为一个user-item-rating 或者 user-item-rating-date实体对. 不同栏之间使用空格或逗号进行分割.示例数据如下:
User-Item-Rating

```
1050 215 3
1050 250 2
1050 251 2.5
```

User-Item-Rating-Date

```
1 1 2	97
1 1 3	75
1 1 4	76
1 4 3	87
1 5 4	96
```
其中User-Item-Rating简写为UIR, User-Item-Rating-Date简写为UIRT.
当使用Text格式的数据来作为输入时, 对以下配置项进行配置

```
data.model.format=text
data.column.format=UIR #or UIRT
```

## Arff
当数据的关系列等于或者超过4维时,推荐使用Arff格式来进行保存.在ARff的头部声明定义数据集的名称. 之后分行声明每一列的名称以及数据类型. 示例数据如下:

```
@RELATION user-movie

@ATTRIBUTE user NUMERIC
@ATTRIBUTE item NUMERIC
@ATTRIBUTE time NUMERIC
@ATTRIBUTE rating NUMERIC

@DATA
1,1,97,2
1,1,75,3
1,1,76,4
1,4,87,3
1,5,96,4
1,6,78,3.5
1,7,1,3.5
```

Arff格式中以%开头为注释行, 声明的标示(@RELATION, @ATTRIBUTE, @DATA)为大小写不敏感.具体Arff数据格式的细节请参阅[Attribute-Relation File Format](http://www.cs.waikato.ac.nz/ml/weka/arff.html)

在LibRec中使用Arff格式的数据需要进行的配置如下

```
data.model.format=arff
```
