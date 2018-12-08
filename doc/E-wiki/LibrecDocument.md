# Librec Document


---

# introduce to Librec
## Overview
LibRec is an open source recommender system based on the java 1.7 and the GPL-3.0 protocol.
LibRec contains a number of recommendation algorithms that can be easily applied to solve rating and ranking problems. Due to the impact on recommendation algorithms, LibRec is referred by RecSys wiki.

Compared with the version 1.4, the version 2.0 reconstructs the whole structure of the program, which makes it more extensible and the interface is more reasonable. For the accomplished program, users can use the command line and arguments, or configuration files to execute the program. To develop a new recommendation algorithm, users can inherit the corresponding interfaces.

Since the structure of LibRec changes, for algorithms in the version 1.4, users need to modify following configurations to adapt to the version 2.0.



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
LibRec defines the configuration file via the property format.
The methods of data input and data split are defined in driver.classes.props.
After setting configurations of different algorithms, the program can loaded the configuration via -conf.


### include librec on other project


# learning more about LibRec
## LibRec Basics
### brief summary of LibRec structure
## Data propercess pipeline
### prepare data
#### data structure
The sparse matrix is stored in both the CSR and CSC format.
#### loading data
The class to read data is the Convertor, which is calling the readData method to employ data input. The corresponding configuratoin example follows.

#### splitting data 
LibRec has several data split methods, including splitting by ratio, K-fold corss-validation, leave-one-out, and GivenN. The configuration example is shown as follows.

### running an algorithms
#### setting similarity
#### choose one recommender algorithm
Currently, there are around 60 algorithms in LibRec.
#### evaluate the result
LibRec implements MAE, MPE, MSE, and RMSE for the rating task.
For ranking algorithms, LibRec implements about ten metrics, such as Precision, Recall, and AUC.

# recommand algorithms
## baseline
## ranking
## rating
## content


## Other important package
### io
### JobStatus
### Reflection
LibRec reads and generates the corresponding class via the property configuration.


# Complete other algorithms
In LibRec, the recommendation algorithm is classified into several types, i.e., matrix-factorization-based, factor-factorization-based, probabilistic-graphical-model-based, tensor-based. To implement a new recommendation algorithm, users can inherit the corresponding abstract class according to the type of the algorithm, or inherit the the abstract class of AbstractRecommender directly.

# Librec API Documents

# Frequently Asked Questions
