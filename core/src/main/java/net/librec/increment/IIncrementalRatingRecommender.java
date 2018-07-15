package net.librec.increment;


import net.librec.common.LibrecException;

public interface  IIncrementalRatingRecommender{

    /***
     *  upodate rating
     */
    void updateRatings(TableMatrix newRatings) throws LibrecException;

    /**
     * plus rating
     */
    void addRatings(TableMatrix newRatings) throws LibrecException;

    /**
     * remove rating
     */
    void removeRatings(TableMatrix removeRatings) throws LibrecException;

}
