package net.librec.data.structure;

public abstract class AbstractContextDataList<T extends AbstractBaseDataEntry, E extends ContextDataEntry> extends BaseDataList<T> {
    abstract E getContextDataEntry(int contextIdx);
}

