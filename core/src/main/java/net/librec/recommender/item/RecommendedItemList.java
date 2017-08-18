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
import net.librec.util.Lists;

import java.io.Serializable;
import java.util.*;

/**
 * Recommended Item List
 *
 * @author WangYuFeng and Keqiang Wang
 */
public class RecommendedItemList implements RecommendedList, Serializable {
    private static final long serialVersionUID = -7323990117815388002L;

    /**
     * recommended items List of every users
     */
    private transient List<List<ItemEntry<Integer, Double>>> elementData;

    /**
     * index of user index data
     */
    private transient int[] indexOfUserIdx;

    /**
     * the number of users (the number of ArrayList)
     */
    private int size;

    /**
     * store the idle index list
     */
    private Queue<Integer> idleIndexList;

    /**
     * the max user index
     */
    private int maxUserIdx;

    /**
     * Constructs an empty list with the specified initial capacity(number of users).
     *
     * @param maxUserIdxParam the max user index
     * @throws IllegalArgumentException if the specified initial capacity is negative
     */
    public RecommendedItemList(int maxUserIdxParam) {
        this(maxUserIdxParam, 0);
    }

    /**
     * Constructs an empty list with the specified initial capacity(number of users).
     *
     * @param maxUserIdxParam the max user index
     * @param initCapacityParam  initial capacity
     * @throws IllegalArgumentException if the specified initial capacity is negative
     */
    public RecommendedItemList(int maxUserIdxParam, int initCapacityParam) {
        this.maxUserIdx = maxUserIdxParam;
        if (maxUserIdx < 0)
            throw new IllegalArgumentException("Illegal max user index: " + maxUserIdx);

        initCapacityParam = initCapacityParam > maxUserIdx ? (maxUserIdx + 1) : initCapacityParam;

        this.elementData = new ArrayList<List<ItemEntry<Integer, Double>>>(initCapacityParam);
        this.idleIndexList = new LinkedList<Integer>();
        indexOfUserIdx = new int[maxUserIdx + 1];
        Arrays.fill(indexOfUserIdx, -1);
        this.size = 0;
    }

    /**
     * set the specified element to the end of this list.
     *
     * @param userIdx  user index
     * @param itemList element to be appended to this list
     * @return <tt>true</tt> (as specified by {@link Collection#add})
     */
    public boolean setItemIdxList(int userIdx, List<ItemEntry<Integer, Double>> itemList) {
        if(itemList.size()>0) {
            userRangeCheck(userIdx);
            checkIndex(userIdx);
            elementData.set(indexOfUserIdx[userIdx], itemList);
            return true;
        } else {
            return false;
        }
    }

    /**
     * append the specified element to the end of this list.
     *
     * @param userIdx  user index
     * @param itemList element to be appended to this list
     * @return <tt>true</tt> (as specified by {@link Collection#add})
     */
    public boolean addItemIdxList(int userIdx, ArrayList<ItemEntry<Integer, Double>> itemList) {
        userRangeCheck(userIdx);
        checkIndex(userIdx);
        elementData.get(indexOfUserIdx[userIdx]).addAll(itemList);
        return true;
    }

    /**
     * Appends the specified element to the end of this list.
     *
     * @param userIdx  user index
     * @param itemIdx  item index
     * @param rating   rating value
     * @return <tt>true</tt> (as specified by {@link Collection#add})
     */
    public boolean addUserItemIdx(int userIdx, int itemIdx, double rating) {
        userRangeCheck(userIdx);
        checkIndex(userIdx);
        elementData.get(indexOfUserIdx[userIdx]).add(new ItemEntry<Integer, Double>(itemIdx, rating));
        return true;
    }

    /**
     * Returns the itemEntry of user index in this list.
     *
     * @param userIdx user index
     * @return the itemEntry of user index in this list
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public List<ItemEntry<Integer, Double>> getItemIdxListByUserIdx(int userIdx) throws IndexOutOfBoundsException {
        userRangeCheck(userIdx);
        int index = indexOfUserIdx[userIdx];
        if (index < 0) {
            return new ArrayList<ItemEntry<Integer, Double>>();
        } else {
            return elementData.get(index);
        }
    }

    /**
     * check if the element Data contains the userIdx. if contain , do noting.
     * else, add a new new ArrayList<ItemEntry<Integer, Double>>() to  elementData
     *
     * @param userIdx user idx
     */
    private void checkIndex(int userIdx) {
        int index = indexOfUserIdx[userIdx];
        if (index < 0) {
            if (!idleIndexList.isEmpty()) {
                elementData.set(indexOfUserIdx[userIdx], new ArrayList<ItemEntry<Integer, Double>>());
                indexOfUserIdx[userIdx] = idleIndexList.poll();
            } else {
                indexOfUserIdx[userIdx] = elementData.size();
                elementData.add(new ArrayList<ItemEntry<Integer, Double>>());
            }
            size++;
        }
    }

    /**
     * Returns the element at the specified position in this list.
     *
     * @param userIdx index of the element to return
     * @param itemIdx index of the element to return
     * @return the element at the specified position in this list
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    @Deprecated
    @LibrecWaring("It is best not to use this method! Too slow and the complexity is O(itemIdxList.size()).")
    public double getEntryValue(int userIdx, int itemIdx) throws IndexOutOfBoundsException {
        userRangeCheck(userIdx);
        List<ItemEntry<Integer, Double>> itemEntryList = this.getItemIdxListByUserIdx(userIdx);
        for (ItemEntry<Integer, Double> itemEntry : itemEntryList) {
            if (itemEntry != null && itemEntry.getKey() == itemIdx) {
                return itemEntry.getValue();
            }
        }
        return -1.0;
    }

    /**
     * Removes the element at the specified position in this list. Shifts any
     * subsequent elements to the left (subtracts one from their indices).
     *
     * @param userIdx user index
     * @return the element that was removed from the list
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public List<ItemEntry<Integer, Double>> removeUserIdx(int userIdx) throws IndexOutOfBoundsException {
        userRangeCheck(userIdx);
        int index = indexOfUserIdx[userIdx];
        List<ItemEntry<Integer, Double>> oldValue = new ArrayList<ItemEntry<Integer, Double>>();
        if (index > 0) {
            oldValue = elementData.get(index);
            elementData.set(index, null);
            indexOfUserIdx[userIdx] = -1;
            size--;
            idleIndexList.offer(index);
        }
        return oldValue;
    }

    /**
     * top n ranked Items for all userIdx
     *
     * @param itemTopN top n ranked Items
     */
    public void topNRank(int itemTopN) {
        Iterator<Integer> userItr = userIterator();
        while (userItr.hasNext()) {
            int userIdx = userItr.next();
            setItemIdxList(userIdx,
                    Lists.sortItemEntryListTopK(getItemIdxListByUserIdx(userIdx), true, itemTopN));
        }
    }


    /**
     * top n ranked Items at user userIdx
     *
     * @param userIdx user userIdx
     * @param topN    top n ranked Items
     */
    public void topNRankItemsByUser(int userIdx, int topN) {
        setItemIdxList(userIdx,
                Lists.sortItemEntryListTopK(getItemIdxListByUserIdx(userIdx), true, topN));
    }

    /**
     * Returns <tt>true</tt> if this list contains the specified userIdx.
     * More formally, returns <tt>true</tt> if and only if this list contains userIdx  <tt>e</tt> such that
     * <tt>(userIdx==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;userIdx.equals(e))</tt>.
     *
     * @param userIdx element whose presence in this list is to be tested
     * @return <tt>true</tt> if this list contains the specified userIdx
     */
    @Override
    public boolean contains(int userIdx) {
        return userIdx <= maxUserIdx && indexOfUserIdx[userIdx] >= 0;
    }

    /**
     * the number of users (the number of ArrayList)
     *
     * @return  the number of users
     */
    public int getSize() {
        return size;
    }

    /**
     * Checks if the given user index is in range. If not, throws an appropriate
     * runtime exception. This method does *not* check if the index is negative:
     * It is always used immediately prior to an array access, which throws an
     * ArrayIndexOutOfBoundsException if index is negative.
     *
     * @param userIdx user index
     */
    private void userRangeCheck(int userIdx) {
        if (userIdx > maxUserIdx)
            throw new IndexOutOfBoundsException(outOfBoundsMsg(userIdx, " User", maxUserIdx));
    }

    /**
     * Constructs an IndexOutOfBoundsException detail message. Of the many
     * possible refactorings of the error handling code, this "outlining"
     * performs best with both server and client VMs.
     *
     * @param index index
     * @param msg   message
     * @param size  size
     * @return
     */
    private String outOfBoundsMsg(int index, String msg, int size) {
        return msg + " Index: " + index + ", Size: " + size;
    }

    /**
     * get the iterator of user index
     *
     * @return user index iterator
     */
    @Override
    public Iterator<Integer> userIterator() {
        return new UserListIterator();
    }

    /**
     * get the iterator of user-item-rating entry
     *
     * @return user item-rating-entry iterator
     */
    @Override
    public Iterator<UserItemRatingEntry> entryIterator() {
        return new UserItemRatingItr();
    }


    /**
     * user list iterator
     */
    private class UserListIterator implements Iterator<Integer> {
        int cursor; // index of next element to return
        int lastRet = -1; // index of last element returned; -1 if no such

        public UserListIterator() {
            cursor = 0;
            while ((cursor + 1) <= maxUserIdx && indexOfUserIdx[cursor] < 0) {
                cursor++;
            }
        }

        public boolean hasNext() {
            return cursor <= maxUserIdx && indexOfUserIdx[cursor] >= 0;
        }


        public Integer next() {
            lastRet = cursor;
            cursor++;
            while ((cursor + 1) <= maxUserIdx && indexOfUserIdx[cursor] < 0) {
                cursor++;
            }
            return lastRet;
        }

        public void remove() {
            if (lastRet < 0)
                throw new IllegalStateException();
            try {
                RecommendedItemList.this.removeUserIdx(lastRet);
                cursor = lastRet;
                lastRet = -1;
            } catch (IndexOutOfBoundsException ex) {
                throw new ConcurrentModificationException();
            }
        }
    }

    /**
     * iterator of user-item-rating entry
     */
    private class UserItemRatingItr implements Iterator<UserItemRatingEntry> {
        private final UserItemRatingEntry entry = new UserItemRatingEntry();
        private int userIdx;
        private Iterator<Integer> userItr;
        private Iterator<ItemEntry<Integer, Double>> itemEntryItr;

        public UserItemRatingItr() {
            userItr = userIterator();
            if (userItr.hasNext()) {
                userIdx = userItr.next();
                itemEntryItr = RecommendedItemList.this.getItemIdxListByUserIdx(userIdx).iterator();
            }
        }

        public boolean hasNext() {
            return itemEntryItr.hasNext() || userItr.hasNext();
        }

        public UserItemRatingEntry next() {
            if (!itemEntryItr.hasNext()) {
                userIdx = userItr.next();
                itemEntryItr = RecommendedItemList.this.getItemIdxListByUserIdx(userIdx).iterator();
            }
            ItemEntry<Integer, Double> itemEntry = itemEntryItr.next();
            entry.setUserIdx(userIdx);
            entry.setItemIdx(itemEntry.getKey());
            entry.setValue(itemEntry.getValue());

            return entry;
        }

        @Override
        @Deprecated
        public void remove() {
            throw new IllegalStateException();
        }
    }

    @Override
    public int size() {
        return size;
    }
}
