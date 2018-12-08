# CommandLine walkthrough
In this section, we demonstrate how to use command line to read and save data, and models. Also, we demonstrate how to make recommendations by using the arguments typed from the command line.

## Usage

```
Usage: librec <command> [options]...
commands:
  rec                       run recommender
  data                      load data

global options:
  --help                    display this help text
  --exec                    run Recommender
  --version                 show Librec version info

job options:
  -conf <file>              path to config file
  -D, -jobconf <prop>       set configuration items (key=value)
  -libjars                  add entend jar files to classpath
```

## Training

```
./librec rec -exec -D rec.recommender.class=globalaverage -conf ../core/src/main/resources/rec/baseline/globalaverage-test.properties -libjars ../lib/log4j-1.2.17.jar
```
rec/data: specify the recommendation algorithm and data input

-exec: execute recommendation algorithms, reserved in the verison 2.0


-D | -jobconf [options] corresponding configurations. For detailed configurations, please refer to [ConfigurationList](./ConfigurationLlist) and [AlgorithmList](./AlgorithmList)

-conf: [path/to/properties] load configuration files

-libjars: load the jar libraries from other paths to the classpath, specifically, the jar files in the directory of lib are loaded automatically. The examples are shown bellow.

## Configuration files
In LibRec, the configurations of a program are stored in core/librec.properties and corresponding algorithm configuration files, respectively. Especially, librec.properties stores the configurations of data input, spliting and evaluation.
Configuration examples are shown as follows.

```
# set data directoy
dfs.data.dir=../data

# set result directory
# recommender result will output in this folder
dfs.result.dir=../result

# not implement in this version
# instead of printing logs in console
dfs.log.dir=../log

data.input.path=filmtrust
data.column.format=UIR
data.model.splitter=ratio
data.model.format=text
data.splitter.ratio=rating

# splitter, reference to basics.md
data.splitter.ratio=0.8
data.splitter.cv.number=5
rec.parallel.support=true

# setting evaluation, reference to basics.md
rec.eval.enable=true
rec.random.seed=1
```

Since different algorithms have various configurations, the configuration of an algorithm is stored in the corresponding directory of the algorithm. Users can type arguments from the command line, or use the modified configuration file when execute the algorithm. 
For the detailed configurations of each algorithm, please refer to [AlgorithmList](./AlgorithmList)
