package net.librec.util;

public class RatingContext implements Comparable<RatingContext> {

    /** rating time stamp */
    private long timestamp;

    /** user index */
    private int user;

    /** item index */
    private int item;

    /**
     * Create a new object with the given rating time stamp, user index
     * and item index.
     *
     * @param user       user index
     * @param item       item index
     * @param timestamp  rating time stamp
     */
    public RatingContext(int user, int item, long timestamp) {
        this.user = user;
        this.item = item;
        this.timestamp = timestamp;
    }

    @Override
    public int compareTo(RatingContext o) {
        // TODO Auto-generated method stub

        double res = this.timestamp - o.timestamp;
        if (res > 0)
            return 1;
        else if (res < 0)
            return -1;
        return 0;
    }

    /**
     * Get the user index of the context.
     *
     * @return the user index of the context
     */
    public int getUser() {
        return user;
    }

    /**
     * Get the item index of the context.
     *
     * @return the item index of the context
     */
    public int getItem() {
        return item;
    }

}
