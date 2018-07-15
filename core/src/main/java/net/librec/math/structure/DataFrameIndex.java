package net.librec.math.structure;

import it.unimi.dsi.fastutil.ints.*;

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by Liuxz on 2018/6/24.
 */
public class DataFrameIndex {
    Int2ObjectOpenHashMap<IntArrayList> dataTable = new Int2ObjectOpenHashMap<>();
    DataFrame df;
    int index;

    public DataFrameIndex(DataFrame df, int index){
        this.df = df;
        this.index = index;
    }

    public void generateDataFrameIndex() {
        for (int i = 0; i < df.getData().get(index).size(); i++) {
            if (!dataTable.containsKey((int) df.get(i, index))) {
                dataTable.put((int) df.get(i, index), new IntArrayList());
            }
            dataTable.get((int) df.get(i, index)).add(index);
        }
    }

    public IntArrayList getIndices(int row){
        return dataTable.get(row);
    }

    private class DataFrameIndexIterator implements Iterator<AbstractInt2ObjectMap.BasicEntry<IntArrayList>>{
        IntSet  set = dataTable.keySet();
        IntIterator setIerator = set.iterator();
        @Override
        public boolean hasNext() {
            return setIerator.hasNext();
        }

        @Override
        public AbstractInt2ObjectMap.BasicEntry<IntArrayList> next() {
            int key = setIerator.nextInt();
            IntArrayList value = dataTable.get(key);
            return new AbstractInt2ObjectMap.BasicEntry<>(key, value);
        }
    }
}
