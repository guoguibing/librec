package net.librec.recommender.item;

/**
 * Created by wkq on 19/05/2017.
 */
public class AbstractContext {
    private int userIdx;

    public AbstractContext(int userIdx){
        this.userIdx = userIdx;
    }

    public int getUserIdx() {
        return userIdx;
    }

    public void setUserIdx(int userIdx) {
        this.userIdx = userIdx;
    }
}
