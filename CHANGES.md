## The Changes of the LibRec Library

### librec-v1.2

* Rating predictions can be outputed now, thanks to [disc5](https://github.com/disc5)'s comment. 
* New recommendation methods implemented:
  * SLIM, FISM, SBPR, GBPR, TrustSVD
* New recommendation methods under testing:
  * timeSVD++
* Interface for context-aware recommender systems added
  * Context, UserContext, ItemContext, RatingContext  -- Data Class
  * ContextRecommender -- Generic Interface
* Others
  * Codes refactored & improved
  * Rename BRPMF to BPR
  * Support conversion from real-valued ratings to binary ones (val.binary.threshold)
  * Utility methods (e.g., data standardization) added to data structure
  * bugs fixed, partially thanks to [albe91](https://github.com/albe91)'s comment. 

### librec-v1.1

* New recommendation methods implemented: 
  * WRMF, AR, PD, RankALS, SoRec, SoReg, RSTE  
* Support a number of testing views of the testing set:
  * all: the ratings of all users are used. 
  * cold-start: the ratings of cold-start users who rated less than 5 items (in the training set) are used.
* Support two new validation methods:
  * Given N: For each user, N ratings will be preserved as training set, while the rest are used as test set. 
  * Given ratio: Similarly as Given N, a ratio of users' ratings will be used for training and others for testing. 
  * val.ratio: Its meaning is changed to the ratio of data for training, rather than the ratio of data for testing.
* Data Structure:
  * DiagMatrix: diagonal matrix added
  * DataConvertor: is added to convert data files from one format to our supporting formats. 
  * A number of enhancement functions are added to matrix, vector class
* Package Refactor:
  * Package *librec.core* is split into two packages: 
    * librec.rating: algorithms for rating predictions; supporting rating-based item ranking. 
    * librec.ranking: algorithms for item ranking. 
* Others
  * Code improved
  * Some bugs fixed

### librec-v1.0

* A set of recommendations have been implemented. 
