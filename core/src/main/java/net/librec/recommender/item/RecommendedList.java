/**
 * Copyright (C) 2016 LibRec
 * <p>
 * This file is part of LibRec.
 * LibRec is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * LibRec is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with LibRec. If not, see <http://www.gnu.org/licenses/>.
 */
package net.librec.recommender.item;

import net.librec.annotation.LibrecWaring;

import java.util.Iterator;
import java.util.List;

/**
 * Recommended List
 *
 * @author WangYuFeng and Keqiang Wang
 */
public interface RecommendedList {

    /**
     * Returns the number of elements in this list.  If this list contains
     * more than <tt>Integer.MAX_VALUE</tt> elements, returns
     * <tt>Integer.MAX_VALUE</tt>.
     *
     * @return the number of elements in this list
     */
    int size();

    /**
     * add UserItemIdx
     *
     * @param userIdx user index
     * @param itemIdx item index
     * @param rating  rating value
     * @return  true if the operation succeed
     */
    public boolean addUserItemIdx(int userIdx, int itemIdx, double rating);

    /**
     * get ItemIdxList By UserIdx
     *
     * @param userIdx  user index
     * @return item index list
     */
    public List<ItemEntry<Integer, Double>> getItemIdxListByUserIdx(int userIdx);

    /**
     * remove UserIdx
     *
     * @param userIdx  user index
     * @return  item index list
     */
    public List<ItemEntry<Integer, Double>> removeUserIdx(int userIdx);

    /**
     * Returns <tt>true</tt> if this list contains the specified userIdx.
     * More formally, returns <tt>true</tt> if and only if this list contains userIdx  <tt>e</tt> such that
     * <tt>(userIdx==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;userIdx.equals(e))</tt>.
     *
     * @param userIdx element whose presence in this list is to be tested
     * @return <tt>true</tt> if this list contains the specified userIdx
     */
    public boolean contains(int userIdx);

    /**
     * get Entry Value
     *
     * @param userIdx  user index
     * @param itemIdx  item index
     * @return  entry value
     */
    @Deprecated
    @LibrecWaring(value = "It is best not to use this method! Too slow and the complexity is O(itemIdxList.size()).")
    public double getEntryValue(int userIdx, int itemIdx);

    /**
     * get the iterator of user index
     *
     * @return user index iterator
     */
    public Iterator<Integer> userIterator();

    /**
     * get the iterator of user-item-rating entry
     *
     * @return user item-rating-entry iterator
     */
    public Iterator<UserItemRatingEntry> entryIterator();

    /**
     * top n ranked Items at user userIdx
     *
     * @param userIdx user userIdx
     * @param topN    top n ranked Items
     */
    public void topNRankItemsByUser(int userIdx, int topN);

    /**
     * top n ranked Items for all userIdx
     *
     * @param itemTopN top n ranked Items
     */
    public void topNRank(int itemTopN);
}
