package net.librec.increment;

// put this class to the top

import net.librec.recommender.MatrixFactorizationRecommender;

public abstract class  IncrementalRecommender extends MatrixFactorizationRecommender implements IIncrementalRecommender{

    /**
     *  the user max id after incremental updating
     */
    protected  int maxUserId;

    /**
     *  the item max id after incremental updating
     */
    protected  int maxItemId;

    public IncrementalRecommender(){

        this.maxUserId = this.numUsers;
        this.maxItemId = this.numItems;
    }

    /***
     *
     * @param userId
     */
    protected void addUser(int userId){

    }

    /**
     *
     * @param itemId
     */
    protected void addItem(int itemId){
    }
}
