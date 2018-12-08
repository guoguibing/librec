package net.librec.increment;

// put this class on the top

interface IIncrementalRecommender {



    /***
     * Remove all feedback by one user.
     * @param userId
     */
    void removeUser(int userId);


    /**
     *  Update user infomation
     * @param itemId
     */
    void removeItem(int itemId);
}
