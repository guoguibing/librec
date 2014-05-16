## Software Changes 

### librec-v1.1

* Changes to librec.conf
    * New recommendation methods implemented: 
        * WRMF: Weighted Regularized Matrix Factorization, for item recommendations;  
    * Support a number of testing views of the testing set:
        * all: the ratings of all users are used. 
        * cold-start: the ratings of cold-start users who rated less than 5 items (in the training set) are used.

### librec-v1.0

* A set of recommendations have been implemented. 
