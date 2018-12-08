package net.librec.increment;

// put this class to the top

public abstract class IncrementalRankingRecommender extends IncrementalRecommender implements IIncrementalRankingRecommender{

    /**
     *     *
     * */
    public boolean updateUsers;
    public boolean updateItems;

    public IncrementalRankingRecommender()
    {
        updateUsers = true;
        updateItems = true;
    }

    /***
     *
     * @param userId
     */
    public void addUser(int userId){

    }
    public void updateUser(int userId){

    }
    public void removeUser(int userId){

    }

    /**
     *
     * @param itemId
     */
    public void addItem(int itemId){

    }
    public void updateItem(int itemId){

    }
    public void removeItem(int itemId){

    }

}
