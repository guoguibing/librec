LibRec
======

**LibRec** (http://www.librec.net) is a Java library for recommender systems (Java version 1.7 or higher required). It implements a suit of state-of-the-art recommendation algorithms. It consists of three major components: **Generic Interfaces**, **Data Structures** and **Recommendation Algorithms**. 

**Links:** [Home](http://www.librec.net) | [Getting Started](http://www.librec.net/tutorial.html) | [Algorithms](http://www.librec.net/tutorial.html#algos) | [Examples](http://librec.net/example.html) | [Demo](https://www.youtube.com/watch?v=B0kfYNfCwwo) | [Datasets](http://www.librec.net/datasets.html)  

![LibRec Structure](http://www.librec.net/images/librec.png)

### Features

* **Cross-platform:** as a Java software, LibRec can be easily deployed and executed in any platforms, including MS Windows, Linux and Mac OS.
* **Fast execution:** LibRec runs much **faster** than other libraries, and a detailed comparison over different algorithms on various datasets is available via [here](http://www.librec.net/example.html).
* **Easy configuration:** LibRec configs recommenders using a configuration file: [librec.conf](http://www.librec.net/tutorial.html#config). 
* **Easy expansion:** LibRec provides a set of well-designed recommendation interfaces by which new algorithms can be easily implemented.

### Download
* **librec-v1.4** (under development, see [what's new](CHANGES.md))
  * LibRec开发团队正在招募中，[马上查阅](团队招募.md)招募详情。
* **[librec-v1.3](http://www.librec.net/release/librec-v1.3.zip)**
* **[librec-v1.2](http://www.librec.net/release/librec-v1.2.zip)**
* **[librec-v1.1](http://www.librec.net/release/librec-v1.1.zip)**
* **[librec-v1.0](http://www.librec.net/release/librec-v1.0.zip)**


### Code Snippet

You can use **LibRec** as a part of your projects, and use the following codes to run a recommender. 

<pre>
public void main(String[] args) throws Exception {

	// config logger
	Logs.config("log4j.xml", true);

	// config recommender
	String configFile = "librec.conf"; 

	// run algorithm
	LibRec librec = new LibRec();
	librec.setConfigFiles(configFile);
	librec.execute(args);
}
</pre>

### Reference
Please cite the following papers if LibRec is helpful to your research. 

1. Guibing Guo, Jie Zhang, Zhu Sun and Neil Yorke-Smith, [LibRec: A Java Library for Recommender Systems](http://ceur-ws.org/Vol-1388/demo_paper1.pdf), in Posters, Demos, Late-breaking Results and Workshop Proceedings of the 23rd Conference on User Modelling, Adaptation and Personalization (UMAP), 2015.

### Acknowledgement

I would like to expression my appreciation to the following people for contributing source codes to LibRec, including [Bin Wu](https://github.com/wubin7019088), [Ge Zhou](https://github.com/466152112), [Ran Locar](https://github.com/ranlocar), [Tao Lian](https://github.com/taolian). 

I also appreciate many others for reporting bugs and issues, and for providing valuable suggestions and support. 

### Publications
LibRec has been used in the following publications (let me know if your paper is not listed):

1. G. Guo, J. Zhang and N. Yorke-Smith, TrustSVD: Collaborative Filtering with Both the Explicit and Implicit Influence of User Trust and of Item Ratings, in Proceedings of the 29th AAAI Conference on Artificial Intelligence (AAAI), 2015, 123-129.
2. Z. Sun, G. Guo and J. Zhang, Exploiting Implicit Item Relationships for Recommender Systems, in Proceedings of the 23rd International Conference on User Modeling, Adaptation and Personalization (UMAP), 2015.


### GPL License

LibRec is [free software](http://www.gnu.org/philosophy/free-sw.html): you can redistribute it and/or modify it under the terms of the [GNU General Public License (GPL)](http://www.gnu.org/licenses/gpl.html) as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. LibRec is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. 

You should have received a copy of the GNU General Public License along with LibRec. If not, see http://www.gnu.org/licenses/.
