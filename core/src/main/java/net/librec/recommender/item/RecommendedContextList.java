package net.librec.recommender.item;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by wkq on 19/05/2017.
 */
public class RecommendedContextList<T extends AbstractContext> {
    private List<T> contextList;

    public RecommendedContextList(){
        contextList = new ArrayList<>();
    }

    public RecommendedContextList(int numContexts){
        contextList = new ArrayList<>(numContexts);
    }

    public T getContext(int contextIdx){
        return contextList.get(contextIdx);
    }

    public Iterator<T> iterator(){
        return contextList.iterator();
    }

    public boolean add(T context){
        return this.contextList.add(context);
    }

    public T set(int contextIdx, T context){
        return this.contextList.set(contextIdx, context);
    }
}
