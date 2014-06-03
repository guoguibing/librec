## LibRec Changes 

### librec-v1.1

* Changes to librec.conf
    * New recommendation methods implemented: 
        * WRMF, AR, PD, RankALS, SoRec, SoReg, RSTE  
    * Support a number of testing views of the testing set:
        * all: the ratings of all users are used. 
        * cold-start: the ratings of cold-start users who rated less than 5 items (in the training set) are used.
    * Support two new validation methods:
        * Given N: For each user, N ratings will be preserved as training set, while the rest are used as test set. 
        * Given ratio: Similarly as Given N, a ratio of users' ratings will be used for training and others for testing. 
        * val.ratio: Its meaning is changed to the ratio of data for training, rather than the ratio of data for testing.

### librec-v1.0

* A set of recommendations have been implemented. 
