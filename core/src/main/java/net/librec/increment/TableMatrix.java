package net.librec.increment;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import net.librec.math.algorithm.Randoms;

import java.util.*;

public class TableMatrix {

    /**
     *
     */
    protected Table<Integer, Integer, Double> table;

    /**
     *
     */
    protected int size;

    /**
     *
     */
    protected int rowSize;

    /**
     *
     */
    protected int columnSize;

    /**
     *
     */
    public TableMatrix() {
        table = HashBasedTable.create();
    }

    /**
     *
     * @param numColmns
     */
    public TableMatrix(int numColmns){
        this();
        for(int i = 0; i < 1; i++){
            for(int j = 0; j < numColmns; j++){
                table.put(i, j, 0.0);
            }
        }
    }


    /**
     *
     * @param numRows
     * @param numColmns
     */
    public TableMatrix(int numRows, int numColmns){
        this();
        for(int i = 0; i < numRows; i++){
            for(int j = 0; j < numColmns; j++){
                table.put(i, j, 0.0);
            }
        }
    }

    /**
     *
     * @param initVal
     */
    public void init(double initVal){
        this.rowSize = this.rowSize();
        this.columnSize = this.columnSize();
        for (int i = 0; i < this.rowSize; i++)
            for (int j = 0; j < this.columnSize; j++)
                table.put(i, j, initVal);
    }

    /**
     *
     * @param mean
     * @param sigma
     */
    public void init(double mean, double sigma){
        this.rowSize = this.rowSize();
        this.columnSize = this.columnSize();
        for (int i = 0; i < this.rowSize; i++)
            for (int j = 0; j < this.columnSize; j++)
                table.put(i, j, Randoms.gaussian(mean, sigma));
    }

    /**
     * @param id
     * @return
     */
    public double get(int id) {
        if(table.contains(0, id)){
            double value = table.get(0, id);
            return value;
        }
        else {
            return -1;
        }
    }


    /**
     * @param userId
     * @param itemId
     * @return
     */
    public double get(int userId, int itemId) {
        if(table.contains(userId, itemId)){
            double value = table.get(userId, itemId);
            return value;
        }
        else{
            return -1;
        }
    }

    /**
     * @param userId
     * @param itemId
     * @param value
     * @return
     */
//    public double put(int userId, int itemId, double value) {
//        double previouslyValue = table.put(userId, itemId, value);
//        return previouslyValue;
//    }

    /**
     * @param itemId
     * @param value
     * @return
     */
    public double set(int itemId, double value) {
        double previouslyValue = table.put(0, itemId, value);
        return previouslyValue;
    }


    /**
     * @param userId
     * @param itemId
     * @param value
     * @return
     */
    public double set(int userId, int itemId, double value) {
        double previouslyValue = table.put(userId, itemId, value);
        return previouslyValue;
    }

    /***
     *
     * @param value
     * @return
     */
    public double add(double value){
        double previouslyValue = set(0, this.columnSize(), value);
        return previouslyValue;
    }


    /***
     *
     * @param id
     * @param value
     * @return
     */
    public double add(int id, double value){
        double previouslyValue = 0.0;
        if(table.contains(0, id)) {
            previouslyValue = set(0, id, this.get(id) + value);

        }
        else{
            previouslyValue = set(0, id, value);
        }
        return previouslyValue;
    }


    /***
     *
     * @param userId
     * @param itemId
     * @param value
     * @return
     */
    public double add(int userId, int itemId, double value){
        double previouslyValue = 0.0;
        if(table.contains(userId, itemId)) {
            previouslyValue = set(userId, itemId, this.get(userId, itemId) + value);

        }
        else{
            previouslyValue = set(userId, itemId, value);
        }
        return previouslyValue;
    }


    /**
     * @param userId
     * @param itemId
     * @return
     */
    public double reomve(int userId, int itemId) {
        double previouslyValue = table.remove(userId, itemId);
        return previouslyValue;
    }

    /**
     * @return
     */
    public int size() {
        size = table.size();
        return size;
    }

    /**
     *
     * @return
     */
    public int rowSize(){
        int rsize = table.rowKeySet().size();
        return rsize;
    }

    /**
     *
     * @return
     */
    public int columnSize(){
        int csize = table.columnKeySet().size();
        return  csize;
    }

    /**
     *
     * @return
     */
    public Iterator<Table.Cell<Integer, Integer, Double>> iterator(){
        Set<Table.Cell<Integer, Integer, Double>> tableSet = table.cellSet();
        Iterator<Table.Cell<Integer, Integer, Double>> it = tableSet.iterator();
        return it;
    }

    /**
     *
     * @param vec1
     * @param vec2
     * @return
     */
    public static double inner(List<Double> vec1, List<Double> vec2){
        assert vec1.size() == vec2.size();

        double result = 0;
        for (int i = 0; i < vec1.size(); i++)
            result += vec1.get(i) * vec2.get(i);

        return result;
    }

    /**
     *
     * @param m
     * @param mrow
     * @param n
     * @param nrow
     * @return
     */
    public static double rowMult(TableMatrix m, int mrow, TableMatrix n, int nrow){
        assert m.columnSize() == n.columnSize();
        int columnSize = m.columnSize();
        double res = 0;
        for (int j = 0, k = columnSize; j < k; j++){
            res += m.get(mrow, j) * n.get(nrow, j);
        }

        return res;
    }

    /**
     *
     * @param rowId
     * @return
     */
    public List<Double> row (int rowId){
        List<Double> list = new ArrayList<>();
        Map<Integer, Double> rowMap = table.row(rowId);
        for(Map.Entry<Integer, Double> entry: rowMap.entrySet()){
            double ratingData = entry.getValue();
            list.add(ratingData);
        }
        return list;
    }

    /**
     *
     * @param columnId
     * @return
     */
    public List<Double> getColumnVector (int columnId){
        List<Double> list = new ArrayList<>();
        Map<Integer, Double> columnMap = table.column(columnId);
        for(Map.Entry<Integer, Double> entry: columnMap.entrySet()){
            double ratingData = entry.getValue();
            list.add(ratingData);
        }
        return list;
    }

    /**
     *
     * @param mapData
     */
    private void iterRowMapdata (Map<Integer, Double> mapData){
        for(Map.Entry<Integer, Double> entry: mapData.entrySet()){
            int itemId = entry.getKey();
            double ratingData = entry.getValue();
        }
    }

    /**
     *
     * @param rowNum
     */
    public void addRow(int rowNum){
        addRow(rowNum, 0.0d);
    }

    /**
     *
     * @param initValue
     */
    public void addRow(int rowNum, double initValue){
        int rSize = this.rowSize();
        int cSize = this.columnSize();
        if(rowNum > rSize){
            for(int i =0; i < cSize; i++){
                this.set(rSize+1, i, initValue);
            }
        }
    }

    /**
     *
     * @param rowId
     * @param value
     */
    public void setRowToOneValue(int rowId, double value){
        int cSize = this.columnSize();
        for(int i =0; i < cSize; i++){
            this.set(rowId, i, value);
        }
    }


}
