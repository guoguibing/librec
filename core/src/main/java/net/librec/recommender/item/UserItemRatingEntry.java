package net.librec.recommender.item;

/**
 * @author Keqiang Wang
 */
public class UserItemRatingEntry {
    private int userIdx;
    private int itemIdx;
    private double value;

    public UserItemRatingEntry() {

    }

    public UserItemRatingEntry(int userIdx, int itemIdx, double value) {
        this.userIdx = userIdx;
        this.itemIdx = itemIdx;
        this.value = value;
    }

    /**
     * @return the userIdx
     */
    public int getUserIdx() {
        return userIdx;
    }

    /**
     * @param userIdx the userIdx to set
     */
    public void setUserIdx(int userIdx) {
        this.userIdx = userIdx;
    }

    /**
     * @return the itemIdx
     */
    public int getItemIdx() {
        return itemIdx;
    }

    /**
     * @param itemIdx the itemIdx to set
     */
    public void setItemIdx(int itemIdx) {
        this.itemIdx = itemIdx;
    }

    /**
     * @return the value
     */
    public double getValue() {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(double value) {
        this.value = value;
    }
}
