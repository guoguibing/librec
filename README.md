LibRec
======

**LibRec** (http://www.librec.net) is a Java library for recommender systems (Java version 1.7 or higher required). It implements a suit of state-of-the-art recommendation algorithms. It consists of three major components: **Generic Interfaces**, **Data Structures** and **Recommendation Algorithms**. 

**Links:** [Home](http://www.librec.net) | [Getting Started](http://www.librec.net/tutorial.html) | [Examples](http://librec.net/example.html) |  [Datasets](http://www.librec.net/datasets.html) | [Source Code](https://github.com/guoguibing/librec) 


### Features

* **Cross-platform:** as a Java software, LibRec can be easily deployed and executed in any platforms, including MS Windows, Linux and Mac OS.
* **Fast execution:** LibRec runs much **faster** than other libraries, and a detailed comparison over different algorithms on various datasets is available via [here](http://www.librec.net/example.html).
* **Easy configuration:** LibRec configs recommenders using a configuration file: [librec.conf](http://www.librec.net/tutorial.html#config). 
* **Easy expansion:** LibRec provides a set of well-designed recommendation interfaces by which new algorithms can be easily implemented.

### Algorithms

* **Baseline**: GlobalAvg, UserAvg, ItemAvg, Random, Constant, MostPop
* **Rating Prediction**: UserKNN, ItemKNN, RegSVD, PMF, SVD++, BiasedMF, BPMF, SocialMF, TrustMF, SoRec, SoReg, RSTE, TrustSVD;
* **Item Ranking**: BPR, CLiMF, RankALS, RankSGD, WRMF, SLIM, GBPR, SBPR, WBPR, FISM, LDA; 
* **Extension**: NMF, SlopeOne, Hybrid, PD, AR, PRankD, External;

The references for all the algorithms are summarized [here](http://www.librec.net/tutorial.html#algos)

### Download
* **[librec-v1.2](http://www.librec.net/release/librec-v1.2.zip)** (milestone version, [what's new](CHANGES.md))
* **[librec-v1.1](http://www.librec.net/release/librec-v1.1.zip)**
* **[librec-v1.0](http://www.librec.net/release/librec-v1.0.zip)**

### Authors

* [Guibing Guo](http://www.luckymoon.me/)

### GPL License

LibRec is [free software](http://www.gnu.org/philosophy/free-sw.html): you can redistribute it and/or modify it under the terms of the [GNU General Public License (GPL)](http://www.gnu.org/licenses/gpl.html) as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. LibRec is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. 

You should have received a copy of the GNU General Public License along with LibRec. If not, see http://www.gnu.org/licenses/.
