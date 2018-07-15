// Copyright (C) 2014 Guibing Guo
//
// This file is part of LibRec.
//
// LibRec is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// LibRec is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with LibRec. If not, see <http://www.gnu.org/licenses/>.
//

package net.librec.math.structure;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Table;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.librec.conf.Configuration;
import org.apache.commons.logging.LogFactory;
import net.librec.math.structure.DataFrameIndex;

import java.io.Serializable;
import java.util.*;

/**
 * Data Structure: DataFrame
 *
 * @author Liuxz
 */

public class DataFrame implements Serializable, DataSet{
    private static final org.apache.commons.logging.Log LOG = LogFactory.getLog(DataFrame.class);

    class MultiIndex{
        Set<String> labels;
        List<String>  levels;
    }

    private static Map<String, BiMap<String, Integer>> featuresInnerMapping;
    private String name;
    private List<String> header;
    private List<String> attrType;

    private List<List<Object>> data;

    private List<Double> ratingScale;

    public DataFrame(){
        this.data = new ArrayList<>();
    }

    /**
     * Construct a dataFrame from another dataFrame
     *
     * @param df the original dataFrame
     */
    public DataFrame(DataFrame df){
        this.data = df.getData();
        this.name = df.getName();
        this.header = df.getHeader();
        this.attrType = df.getAttrType();
    }

    @Override
    public int size() {
        if (data == null){
            return 0;
        }
        return data.get(0).size();
    }

    /**
     * plus a new row on the end of the  dataFrame
     *
     * @param input
     */
    public void add(String[] input){
        if (data == null){
            data = new ArrayList<>(input.length);
        }
        if (data.size() == 0){
            for (int i =0; i < input.length; i++){
                data.add(new ArrayList<>());
            }
        }
        for(int i =0; i < input.length; i++){
            getData().get(i).add(addData(i, input[i]));
        }
    }

    /**
     * parse data and add to dataFrame
     *
     * @param input
     */
    private Object addData(int columnIndex, String input){
        if (Objects.equals(attrType.get(columnIndex), "STRING")) {
            return getId(input, header.get(columnIndex));
        }else if (Objects.equals(attrType.get(columnIndex), "NOMINAL")){
            return getId(input, header.get(columnIndex));
        }else if (Objects.equals(attrType.get(columnIndex), "NUMERIC")){
            return Double.valueOf(input);
        }else if (Objects.equals(attrType.get(columnIndex), "DATE")){
            return Long.parseLong(input);
        }else{
            return null;
        }
    }

    /**
     * set header
     * @param header
     */
    public void setHeader(String[] header){
        this.header = Arrays.asList(header);
    }

    /**
     *  construct sparse matrix from data frame
     * @param conf data.convert.columns, data.convert.binarize.threshold
     * @return
     */
    public SequentialAccessSparseMatrix toSparseMatrix(Configuration conf){

        if (Objects.equals(conf.get("data.convert.columns"), null)){
            if (Objects.equals(conf.get("data.convert.binarize.threshold"), null)){
                return toSparseMatrix();
            }else{
                return toSparseMatrix(conf.getDouble("data.convert.binarize.threshold"));
            }
        }else{
            if (Objects.equals(conf.get("data.convert.binarize.threshold"), null)){
                return toSparseMatrix(conf.get("data.convert.columns").split(","));
            }else{
                return toSparseMatrix(conf.get("data.convert.columns").split(","),
                        conf.getDouble("data.convert.binarize.threshold"));
            }
        }
    }

    public SequentialAccessSparseMatrix toSparseMatrix(){
        return toSparseMatrix(new String[]{"user", "item", "rating"}, -1.0);
    }

    public SequentialAccessSparseMatrix toSparseMatrix(double binThold){
        return toSparseMatrix(new String[]{"user", "item","rating"}, binThold);
    }

    public SequentialAccessSparseMatrix toSparseMatrix(String[] headerIndices){
        return  toSparseMatrix(headerIndices, -1.0);
    }


    /**
     *  get SparseMatrix by assigning selected headers.
     * @param headerIndics  String array. The length of array headerIndex should be 3. The strings in headerIndex refer
     *                     to the header name of row, column,and value in sparse matrix, respectively.
     * @param binThold the threshold to binarize a rating.
     * @return new constructed sparseMatrix
     */
    public SequentialAccessSparseMatrix toSparseMatrix(String[] headerIndics, double binThold) {
        if (headerIndics.length != 3) {
            return null;
        }
        int indexColumn1 = header.indexOf(headerIndics[0]);
        int indexColumn2 = header.indexOf(headerIndics[1]);
        int valueColumn = header.indexOf(headerIndics[2]);
        return toSparseMatrix(indexColumn1, indexColumn2, valueColumn, binThold);
    }


    /**
     *  get SparseMatrix by assigning parameter, "datetimeMatrix" and "preferenceMatrix"  are available
     * @param str "datetimeMatrix" or "preferenceMatrix"
     * @return constructed sparseMatrix
     */
    public SequentialAccessSparseMatrix toSparseMatrix(String str){
        if (Objects.equals(str, "datetimeMatrix")){
            return toSparseMatrix(new String[]{"user","item","datetime"});
        }else if (Objects.equals(str, "preferenceMatrix")){
            return toSparseMatrix(new String[]{"user","item","rating"});
        }else{
            return null;
        }
    }

    /**
     *  get SparseMatrix by assigning the index of selected columns.
     * @param indexColumn1 the index of first column in data frame
     * @param indexColumn2 the index of second column in data frame
     * @param valueColumn the index of value column in data frame
     * @return new constructed sparseMatrix
     */
    public SequentialAccessSparseMatrix toSparseMatrix(int indexColumn1, int indexColumn2, int valueColumn){
        return toSparseMatrix(indexColumn1, indexColumn2, valueColumn, -1.0);
    }


//    public SequentialAccessSparseMatrix toSpaseMatrix_(int indexColumn1, int indexColumn2,
//                                                       int valueColumn, double binThold){
//        if ((data.size() == 0) || data.size() <= valueColumn){
//            return null;
//        }
//
//        DataFrameIndex index = new DataFrameIndex(this, indexColumn1);
//        index.generateDataFrameIndex();
//
//        return new SequentialAccessSparseMatrix(
//                featuresInnerMapping.get(header.get(indexColumn1)).size(),
//                featuresInnerMapping.get(header.get(indexColumn2)).size(),
//                index,
//                valueColumn,
//                indexColumn1,
//                this,
//                binThold
//                );
//    }


    /**
     *  get SparseMatrix by the index columns and the value column of dataFrame
     * @param indexColumn1 the index of first column in data frame
     * @param indexColumn2 the index of second column in data frame
     * @param valueColumn  the index of value column in data frame
     * @param binThold the threshold to binarize a rating
     * @return sparseMatrix
     */
    public SequentialAccessSparseMatrix toSparseMatrix(int indexColumn1, int indexColumn2,
                                                       int valueColumn, double binThold ){
        if ((data.size() == 0) || data.size() <= valueColumn){
            return null;
        }

        if (Objects.equals(attrType.get(valueColumn), "NUMERIC")){
            Table<Integer, Integer, Double> dataTable = HashBasedTable.create();
            for (int i = getData().get(0).size()-1; i >=0 ; i--){
                int row = (int)getData().get(indexColumn1).get(i);
                int col = (int)getData().get(indexColumn2).get(i);
                Double rate;
                rate = valueColumn == -1 ? 1.0 : (Double) getData().get(valueColumn).get(i);

                if (binThold >= 0) {
                    rate = rate > binThold ?  1.0: -1.0;
                }
                dataTable.put(row, col, rate);
            }
            flushCache(Arrays.asList(indexColumn1, indexColumn2));
            SequentialAccessSparseMatrix matrix = new SequentialAccessSparseMatrix(
                    featuresInnerMapping.get(header.get(indexColumn1)).size(),
                    featuresInnerMapping.get(header.get(indexColumn2)).size(),
                    dataTable);
            return matrix;
        }else if (Objects.equals(attrType.get(valueColumn), "DATE")){
            Table<Integer, Integer, Long> dataTable = HashBasedTable.create();
            for (int i = getData().get(0).size()-1; i >=0; i--) {
                int row = (int) getData().get(indexColumn1).get(i);
                int col = (int) getData().get(indexColumn2).get(i);
                Long mms = 0L;
                mms = (Long)getData().get(valueColumn).get(i);
                dataTable.put(row, col, mms);
            }
            flushCache(Arrays.asList(indexColumn1, indexColumn2));
            SequentialAccessSparseMatrix matrix = new SequentialAccessSparseMatrix(
                    featuresInnerMapping.get(header.get(indexColumn1)).size(),
                    featuresInnerMapping.get(header.get(indexColumn2)).size(),
                    dataTable);
            return matrix;
        }else{
            LOG.info("fail to create sparseMatrix, please check attributes type");
            return null;
        }
    }

    /**
     *  Construct a sparse tensor. All category and columns
     * @return sparseTensor
     */
    public SparseTensor toSparseTensor(){
        List<Integer> _indexColumn = new ArrayList<>();
        int valueColumn = -1;
        for(int i =0; i < header.size(); i ++){
            if (featuresInnerMapping.containsKey(header.get(i))
                    && featuresInnerMapping.get(header.get(i)).size()!=0){
                _indexColumn.add(i);
            }
            if (header.get(i).toLowerCase().equals("rating")){
                valueColumn = i;
            }
        }
        int[] indexColumn = new int[_indexColumn.size()];
        for (int i = 0; i < _indexColumn.size(); i ++){indexColumn[i] = _indexColumn.get(i);}
        return toSparseTensor(indexColumn, valueColumn);
    }

    /**
     *  Construct a sparse tensor by assigning the name of indicesColumn(e.g., 'user') and valueColumn(e.g., 'rating').
     * @param indicesColumn String Arrays
     * @param valueColumn String
     * @return SparseTensor
     */
    public SparseTensor toSparseTensor(String[] indicesColumn, String valueColumn){
        int[] index = new int[indicesColumn.length];
        for (int i = 0; i < indicesColumn.length; i ++){
            index[i] = header.indexOf(indicesColumn[i]);
        }
        return toSparseTensor(index, header.indexOf(valueColumn));
    }

    /**
     *  Construct a sparse tensor.All value of NOMINAL and String attributes
     *  are the dimensions in sparse tensor in default. the "rating" column is used as the value column by default.
     * @param indicesColumn int Arrays
     * @param valueColumn String
     * @return SparseTensor
     */
    public SparseTensor toSparseTensor(int[] indicesColumn, int valueColumn){
        List<Double> rating = new ArrayList<>();
        List<Integer>[] nDKeys = (List<Integer>[]) new List<?>[indicesColumn.length];
        int[] dims = new int[indicesColumn.length];
        int userDimension = -1;
        int itemDimension = -1;
        for (int d = 0; d < indicesColumn.length ; d ++){
            nDKeys[d] = new ArrayList<>(getData().get(indicesColumn[d]).size());
            dims[d] = getInnerMapping(header.get(indicesColumn[d])).size();

            if (Objects.equals(header.get(indicesColumn[d]), "user")){
                userDimension = d;
            }
            if (Objects.equals(header.get(indicesColumn[d]), "item")){
                itemDimension = d;
            }
        }

        for (int d = 0; d<indicesColumn.length; d++){
            for (Object s: getData().get(indicesColumn[d])){
                nDKeys[d].add((Integer)s);
            }
        }

        for (Object s: getData().get(valueColumn)){
            rating.add((Double) s);
        }

        SparseTensor tensor = new SparseTensor(dims, nDKeys, rating);
        if (userDimension != -1){
            tensor.setUserDimension(userDimension);
        }
        if (itemDimension != -1){
            tensor.setItemDimension(itemDimension);
        }
        return tensor;
    }


    /**
     * plus a key to the corresponding map and return the value.
     * @param input value
     * @param attrName attribute name
     * @return inner value
     */
    public static int setId(String input, String attrName){
        BiMap<String, Integer> innerMap = getInnerMapping(attrName);
        if (innerMap.containsKey(input)){
            return innerMap.get(input);
        }else{
            int cur = innerMap.size();
            innerMap.put(input, cur);
            return cur;
        }
    }

//    public static void setId(String input, char select){
//        if (select =='U'){
//            setId(input, "user");
//        }else if (select == 'I'){
//            setId(input, "item");
//        }
//    }

    private int getId(String input, String attrName){
        if (featuresInnerMapping.keySet().contains(attrName)){
            return featuresInnerMapping.get(attrName).get(input);
        }else{
            return -1;
        }
    }

    public static BiMap<String, Integer> getInnerMapping(String attrName){
        if (featuresInnerMapping == null){
            featuresInnerMapping = new HashMap<>();
        }
        if (!featuresInnerMapping.keySet().contains(attrName)) {
            featuresInnerMapping.put(attrName, HashBiMap.create());
        }
        return featuresInnerMapping.get(attrName);
    }

    public static void clearInnerMapping(){
        DataFrame.featuresInnerMapping = new HashMap<>();
    }

    public void addHeader(String attrName){
        if (header == null){
            header = new ArrayList<>();
        }
        header.add(attrName);
    }

    public List<String> getHeader(){
        return header;
    }

    public String getHeader(int i){
        if ((i< header.size())&&(0<=i)){
            return header.get(i);
        }else{
            return null;
        }
    }

    public Object get(int index1, int index2){
        return getData().get(index2).get(index1);
    }

    private int getUserId(String user){
        return getInnerMapping("user").get(user);
    }

    private int getItemId(String item){
        return getInnerMapping("item").get(item);
    }

    private void flushCache(List<Integer> index){
        boolean clean = true;
        for (int i = 0; i < getData().size(); i ++){
            if (index.contains(i)){continue;}
            if (getData().get(i).size()!=0){
                clean = false;
            }
        }
        if (clean){
            data = null;
        }
    }

    public static BiMap<String, Integer> getUserIds() {
        return getInnerMapping("user");
    }

    public static BiMap<String, Integer> getItemIds() {
        return getInnerMapping("item");
    }

    public static DataFrame merge(DataFrame a, DataFrame b){
        return new DataFrame();
    }

    public static DataFrame loadArff(String ... path){
        return new DataFrame();
    }

    public void setAttrType(String[] attrType){
        this.attrType = Arrays.asList(attrType);
    }

    public void setHeader(List<String> header){
        this.header = header;
    }

    public List<List<Object>> getData(){
        return data;
    }

    private void setData(List<List<Object>> data) {
        this.data = data;
    }

    public String getName() {
        return name;
    }

    public void setName(String input) {
        this.name = input;
    }

    public List<String> getAttrType() {
        return attrType;
    }

    public void setAttrType(List<String> attrType) {
        this.attrType = new ArrayList<>(attrType);
    }

    public int numUsers(){
        return getUserIds().size();
    }

    public int numItems(){
        return getItemIds().size();
    }

    public List<Double> getRatingScale(){
        int index = header.indexOf("rating");
        if (index == -1){
            return null;
        }
        Set<Object> scale = new HashSet<>(getData().get(index));
        List<Double> ratingScale = new ArrayList<>(scale.size());
        for (Object o: scale){
            ratingScale.add((Double) o);
        }
        ratingScale.sort((o1, o2) -> {
            if (o1 > o2){
                return 1;
            }else{
                return -1;
            }
        });
        return new ArrayList<>(ratingScale);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (String h: header){
            sb.append(' ' + h);
        }
        sb.append("\n");

        for (int i =0; i < getData().get(0).size(); i ++){
            for (int j =0; j< header.size(); j ++ ){
                sb.append(' '+ Double.parseDouble(getData().get(j).get(i).toString()));
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}
