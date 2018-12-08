package net.librec.data.structure;

import java.util.ArrayList;
import java.util.List;

public class BaseDataList<T extends AbstractBaseDataEntry> implements LibrecDataList<T> {
    private List<T> dataEntries;

    public BaseDataList(){
        this.dataEntries = new ArrayList<>();
    }

    public BaseDataList(int initialCapacity){
        this.dataEntries = new ArrayList<>(initialCapacity);
    }

    @Override
    public int size() {
        return dataEntries.size();
    }

    @Override
    public void addDataEntry(T dataEntry) {
        dataEntries.add(dataEntry);
    }

    @Override
    public T getDataEntry(int contextIdx) {
        return dataEntries.get(contextIdx);
    }
}
