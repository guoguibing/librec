LibRec
======

**LibRec** is a Java library for recommender systems (Java version 1.7 or higher required). It implements a suit of state-of-the-art and up-to-date recommendation algorithms. It consists of three major components: **interfaces**, **data structures** and **recommendation algorithms**. To learn more, check out the [home page](http://trust.sce.ntu.edu.sg/~gguo1/librec/), or look into a simple [tutorial](http://trust.sce.ntu.edu.sg/~gguo1/librec/tutorial.html). 


### Features

* **Cross-platform:** as a Java software, LibRec can be easily deployed and executed in any platforms, including MS Windows, Linux and Mac OS.
* **Fast execution:** LibRec runs much **faster** than other libraries, and a detailed comparison over different algorithms on various datasets is available via [here](http://trust.sce.ntu.edu.sg/~gguo1/librec/).
* **Easy configuration:** LibRec configs recommenders using a configuration file: *librec.conf*. Click [here](http://trust.sce.ntu.edu.sg/~gguo1/librec/tutorial.html#config) to check out the details.
* **Easy expansion:** LibRec provides a set of well-designed recommendation interfaces by which new algorithms can be easily implemented.

### Algorithms

* **Baseline**: GlobalAvg, UserAvg, ItemAvg, Random, Constant, MostPop
* **Core**: UserKNN, ItemKNN, RegSVD, PMF, SVD++, BiasedMF, CLiMF, BPMF, SocialMF, TrustMF, WRMF
* **Extension**: NMF, SlopeOne, Hybrid

### Download

The first version of the LibRec library is available to [download](http://trust.sce.ntu.edu.sg/~gguo1/librec/release/librec-v1.0.zip) now. 

### GPL License

LibRec was developped by [Guibing Guo](http://trust.sce.ntu.edu.sg/~gguo1/) at [Nanyang Technological University](http://www.ntu.edu.sg/). It is only allowed for non-commercial usage. 

LibRec is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. LibRec is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. 

You should have received a copy of the GNU General Public License along with LibRec. If not, see http://www.gnu.org/licenses/.
