# CommandLine walkthrough
在这里我们展示如何使用命令行来完成数据与模型的读取及保存, 以及使用命令行来输入参数进行推荐计算.

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
rec/data 为指定程序进行推荐算法/数据读取功能.

-exec 为执行推荐算法.在2.0版本中为保留选项

-D | -jobconf [options] 为加载相关配置, 具体配置项请参考[ConfigurationList](./ConfigurationLlist)及[AlgorithmList](./AlgorithmList)

-conf [path/to/properties] 加载配置文件

-libjars 为加载其他路径下的jar包到classpath中, 其中lib下的jar包自动加载, 当前脚本为示例

## Configuration file
LibRec中一个程序的配置文件分别保存在core/librec.properties和各自算法的独立配置文件内.其中librec.properties 推荐保存的配置为数据的读取分割以及评估.
示例配置如下所示

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

考虑到不同算法的配置项不相同, 因此算法配置项分别保存在相应目录下. 执行不同算法时可以在命令行输入, 也可以使用编辑好的配置文件.
具体每一配置项的内容以及不同算法的配置项请参考[AlgorithmList](./AlgorithmList)
