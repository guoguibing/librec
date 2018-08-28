# DataFileFormat

---

## Text
LibRec can read the Text data directly, where the data is stored with three or four columns. Every row is a user-item-rating triple or a user-item-rating-date quadruple. The different column is split by spaces or a comma. Examples are demonstrated as follows.
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
Specifically, User-Item-Rating is abbreviated as UIR, and User-Item-Rating-Date is abbreviated as UIRT. Users can set the following configurations, when adopt the Text format as data input.

```
data.model.format=text
data.column.format=UIR #or UIRT
```

## Arff
When data columns are larger than four, the Arff data format is recommended to store the data. The very top line of the Arff data defines the name of a data set. Each following line is the column name and data type. Examples are shown bellow.

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

In the Arff data format, comments are initiated with %, and declarations are not case-sensitive. For the detailed Arff data format, please refer to [Attribute-Relation File Format](http://www.cs.waikato.ac.nz/ml/weka/arff.html).

Users need to set the following configuration when apply the Arff data format in LibRec.

```
data.model.format=arff
```
