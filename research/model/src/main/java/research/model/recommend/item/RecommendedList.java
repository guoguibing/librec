package research.model.recommend.item;

import com.google.common.collect.TreeMultimap;
import research.model.utils.Lists;

import java.util.*;

/**
 * data format:
 * <EntryIdx_1, <ItemId_1_1, Value_1_1>, <ItemId_1_2, Value_1_2>, <ItemId_1_3, Value_1_3>,...>,
 * <EntryIdx_2, <ItemId_2_1, Value_2_1>, <ItemId_2_2, Value_2_2>, <ItemId_2_3, Value_2_3>,...>,
 * ...,
 * <EntryIdx_n, <ItemId_n_1, Value_n_>, <ItemId_n_2, Value_n_2>, <ItemId_n_3, Value_n_3>,...>
 * <p>
 * Created by wkq on 12/05/2017.
 */
public class RecommendedList {

    /**
     * is ranking not mapping idx of user and item
     */
    private boolean independentRanking = false;

    /**
     * predict value List of contexts
     */
    private transient List<List<KeyValue<Integer, Double>>> elementData;

    private transient TreeMultimap<Integer, KeyValue<Integer, Double>> contextMultimap = null;

    /**
     * Constructs an empty list with the specified initial capacity.
     *
     * @param initialCapacity the initial capacity of the list
     * @throws IllegalArgumentException if the specified initial capacity
     *                                  is negative
     */
    public RecommendedList(int initialCapacity) {
        this.elementData = new ArrayList<>(initialCapacity);
    }


    /**
     * Constructs an empty list with the specified initial capacity.
     *
     * @param initialCapacity the initial capacity of the list
     * @throws IllegalArgumentException if the specified initial capacity
     *                                  is negative
     */
    public RecommendedList(int initialCapacity, boolean independentRanking) {
        this.elementData = new ArrayList<>(initialCapacity);
        this.independentRanking = independentRanking;
    }


    /**
     * Returns the number of elements in this list.
     *
     * @return the number of elements in this list
     */
    public int size() {
        return elementData.size();
    }


    /**
     * top n ranked Keys for all context
     *
     * @param topN top n ranked Keys
     */
    public void topNRank(int topN) {
        for (int contextIdx = 0; contextIdx < size(); ++contextIdx) {
            setList(contextIdx,
                    Lists.sortKeyValueListTopK(elementData.get(contextIdx), true, topN));
        }
    }


    /**
     * top n ranked List at context contextIdx
     *
     * @param contextIdx user userIdx
     * @param topN       top n ranked Items
     */
    public void topNRankByIndex(int contextIdx, int topN) {
        setList(contextIdx,
                Lists.sortKeyValueListTopK(elementData.get(contextIdx), true, topN));
    }

    /**
     * Checks if the given context index is in range. If not, throws an appropriate
     * runtime exception. This method does *not* check if the index is negative:
     * It is always used immediately prior to an array access, which throws an
     * ArrayIndexOutOfBoundsException if index is negative.
     *
     * @param contextIdx context index
     */
    private void rangeCheck(int contextIdx) {
        int size = size();
        if (contextIdx > size || contextIdx < 0)
            throw new IndexOutOfBoundsException(outOfBoundsMsg(contextIdx, size));
    }

    /**
     * Constructs an IndexOutOfBoundsException detail message. Of the many
     * possible refactorings of the error handling code, this "outlining"
     * performs best with both server and client VMs.
     *
     * @param index index
     * @param size  cardinality
     * @return return out of bounds message
     */
    private String outOfBoundsMsg(int index, int size) {
        return " Context" + " Index: " + index + ", Size: " + size;
    }


    /**
     * set the specified element at the context index.
     *
     * @param contextIdx  context index
     * @param elementList element to be appended at the context index
     */
    public void setList(int contextIdx, List<KeyValue<Integer, Double>> elementList) {
        rangeCheck(contextIdx);
        elementData.set(contextIdx, elementList);
    }

    /**
     * append the specified element to the end of the  list.
     *
     * @param elementList element to be appended to this list
     */
    public void addList(ArrayList<KeyValue<Integer, Double>> elementList) {
        elementData.add(elementList);
    }


    /**
     * Appends the specified element to the end of this list.
     *
     * @param contextIdx context index
     * @param key        key index
     * @param score      predicted score value
     * @return <tt>true</tt> (as specified by {@link Collection#add})
     */
    public boolean add(int contextIdx, int key, double score) {
        rangeCheck(contextIdx);
        elementData.get(contextIdx).add(new KeyValue<>(key, score));
        return true;
    }

    /**
     * Appends the specified element to the end of this list.
     *
     * @param userId userId
     * @param itemId itemId
     * @param score      predicted score value
     */
    public void addIndependently(int userId, int itemId, double score) {
        if (contextMultimap == null) {
            synchronized (this) {
                if (independentRanking) {
                    contextMultimap = TreeMultimap.create(Comparator.comparingInt(k -> k),
                            Comparator.comparingDouble(v -> -v.value));
                } else {
                    contextMultimap = TreeMultimap.create(Comparator.comparingInt(k -> k),
                            Comparator.comparingInt(v -> v.key));
                }
            }
        }
        contextMultimap.put(userId, new KeyValue<>(itemId, score));
    }

    public RecommendedList[] joinTransform(RecommendedList thatList, int topN){
        int contextIdx = 0;
        this.elementData.parallelStream().forEach(keyValue -> keyValue = null);
        thatList.elementData.parallelStream().forEach(keyValue -> keyValue = null);
        for (Integer key : contextMultimap.keySet()) {
            int valueIdx = 0;
            NavigableSet<KeyValue<Integer, Double>> thatValue = thatList.getContextMultimap().get(key);
            if (thatValue != null) {
                Iterator<KeyValue<Integer, Double>> contextIte = contextMultimap.get(key).iterator();
                Iterator<KeyValue<Integer, Double>> thatContextIte = thatValue.iterator();
                while (contextIte.hasNext() && thatContextIte.hasNext()) {
                    KeyValue<Integer, Double> contextValue = contextIte.next();
                    KeyValue<Integer, Double> thatContextValue = thatContextIte.next();
                    this.addList(new ArrayList<>());
                    thatList.addList(new ArrayList<>());
                    if (topN < 0) {
                        add(contextIdx, valueIdx, contextValue.getValue());
                        thatList.add(contextIdx, valueIdx, thatContextValue.getValue());
                        valueIdx++;
                    } else {
                        if (topN > valueIdx) {
                            add(contextIdx, valueIdx, contextValue.getValue());
                            thatList.add(contextIdx, valueIdx, thatContextValue.getValue());
                            valueIdx++;
                        } else {
                            break;
                        }
                    }
                }
                contextIdx++;
            }
        }
        return new RecommendedList[]{this, thatList};
    }

    /**
     *
     * @param topN
     * @return
     */
    public RecommendedList transform(int topN) {
        int contextIdx = 0;
        for(Integer key : contextMultimap.keySet()){
            int valueIdx = 0;
            for(KeyValue<Integer, Double> value : contextMultimap.get(key)){
                addList(new ArrayList<>());
                if (topN < 0) {
                    add(contextIdx, valueIdx, value.getValue());
                } else {
                    if (topN > valueIdx){
                        add(contextIdx, valueIdx, value.getValue());
                    } else {
                        break;
                    }
                }
                valueIdx++;
            }
            contextIdx++;
        }
        return this;
    }

    public TreeMultimap<Integer, KeyValue<Integer, Double>> getContextMultimap(){
        return contextMultimap;
    }
    /**
     * Returns the key score pairs list of context index in this list.
     *
     * @param contextIdx context index
     * @return the key score pairs list of context index in this list.
     */
    public List<KeyValue<Integer, Double>> getKeyValueListByContext(int contextIdx) {
        rangeCheck(contextIdx);
        return elementData.get(contextIdx);
    }

    /**
     * Returns the key score pairs list of context index in this list.
     *
     * @param contextIdx context index
     * @return the key score pairs list of context index in this list.
     */
    public Set<Integer> getKeySetByContext(int contextIdx) {
        rangeCheck(contextIdx);
        Set<Integer> keySet = new HashSet<>();
        for (KeyValue<Integer, Double> keyValue : elementData.get(contextIdx)) {
            keySet.add(keyValue.getKey());
        }
        return keySet;
    }

    /**
     * get the iterator of user index
     *
     * @return user index iterator
     */
    public Iterator<KeyValue<Integer, Double>> iterator(int contextIdx) {
        return elementData.get(contextIdx).iterator();
    }

    /**
     * get the iterator of user-item-rating entry
     *
     * @return user item-rating-entry iterator
     */
    public Iterator<ContextKeyValueEntry> iterator() {
        return new RecommenderIterator();
    }


    /**
     * iterator of context-key-score entry
     */
    private class RecommenderIterator implements Iterator<ContextKeyValueEntry> {
        private final ContextKeyValueEntry entry = new ContextKeyValueEntry();
        private int contextIdx;
        private int keyIdx;
        private Iterator<KeyValue<Integer, Double>> KeyValueEntryItr;

        RecommenderIterator() {
            contextIdx = 0;
            keyIdx = 0;
            KeyValueEntryItr = iterator(contextIdx);

            while (!KeyValueEntryItr.hasNext() && (contextIdx + 1) < size()) {
                KeyValueEntryItr = iterator(++contextIdx);
            }
        }

        public boolean hasNext() {
            return KeyValueEntryItr.hasNext() || (contextIdx + 1) < size();
        }

        public ContextKeyValueEntry next() {

            KeyValue<Integer, Double> KeyValueEntry = KeyValueEntryItr.next();
            entry.setContextIdx(contextIdx);
            entry.setKey(KeyValueEntry.getKey());
            entry.setValue(KeyValueEntry.getValue());
            entry.setKeyIdx(keyIdx);
            ++keyIdx;

            while (!KeyValueEntryItr.hasNext() && (contextIdx + 1) < size()) {
                KeyValueEntryItr = iterator(++contextIdx);
                keyIdx = 0;
            }

            return entry;
        }

        @Override
        public void remove() {
            elementData.get(contextIdx).remove(keyIdx);
        }

        public void setValue(double value) {
            elementData.get(contextIdx).get(keyIdx).setValue(value);
            entry.setValue(value);
        }
    }
}
