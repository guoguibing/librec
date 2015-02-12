LibRec
======

**LibRec** (http://www.librec.net) is a Java library for recommender systems (Java version 1.7 or higher required). It implements a suit of state-of-the-art and up-to-date recommendation algorithms. It consists of three major components: **Generic Interfaces**, **Data Structures** and **Recommendation Algorithms**. 

**Links:** [Getting Started](http://www.librec.net/tutorial.html) | [Examples](http://librec.net/example.html) |  [Datasets](http://www.librec.net/datasets.html) | [Source Code](https://github.com/guoguibing/librec). 


### Features

* **Cross-platform:** as a Java software, LibRec can be easily deployed and executed in any platforms, including MS Windows, Linux and Mac OS.
* **Fast execution:** LibRec runs much **faster** than other libraries, and a detailed comparison over different algorithms on various datasets is available via [here](http://www.librec.net/example.html).
* **Easy configuration:** LibRec configs recommenders using a configuration file: [librec.conf](http://www.librec.net/tutorial.html#config). 
* **Easy expansion:** LibRec provides a set of well-designed recommendation interfaces by which new algorithms can be easily implemented.

### Algorithms

* **Baseline**: GlobalAvg, UserAvg, ItemAvg, Random, Constant, MostPop
* **Rating Prediction**: UserKNN, ItemKNN, RegSVD, PMF, SVD++, BiasedMF, BPMF, SocialMF, TrustMF, SoRec, SoReg, RSTE, TrustSVD;
* **Item Ranking**: BPR, CLiMF, RankALS, RankSGD, WRMF, SLIM, GBPR, SBPR, FISM; 
* **Extension**: NMF, SlopeOne, Hybrid, PD, AR, PRankD

The references for all the algorithms are summarized [here](http://www.librec.net/tutorial.html#algos)

### Download
* librec-v1.2 (milestone version) is under development, see [what's new](CHANGES.md)
  * **[librec-v1.2-rc1](http://www.librec.net/release/librec-v1.2-rc1.zip)**
* **[librec-v1.1](http://www.librec.net/release/librec-v1.1.zip)**
* **[librec-v1.0](http://www.librec.net/release/librec-v1.0.zip)**

### GPL License

LibRec (c) 2015 is developped by [Guibing Guo](http://www.luckymoon.me/). 

LibRec (c) 2014 was developped by [Guibing Guo](http://www.luckymoon.me/), under the supervision of Dr. [Jie Zhang](http://www.ntu.edu.sg/home/zhangj/) at Nanyang Technological University. 

LibRec is only allowed for non-commercial usage. 

LibRec is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. LibRec is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. 

You should have received a copy of the GNU General Public License along with LibRec. If not, see http://www.gnu.org/licenses/.
