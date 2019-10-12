package research.data.reader;


import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;
import java.util.*;

/**
* 数据存储结构，暂时，后面使用/base/core/src/main/java/research/core/data/Dataset.java
* Data Structure: Dataset
*
* @author dayang
*/

public class DataSet implements Serializable{
 private static final org.apache.commons.logging.Log LOG = LogFactory.getLog(DataSet.class);

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

 public DataSet(){
     this.data = new ArrayList<>();
 }

 /**
  * Construct a dataFrame from another dataFrame
  *
  * @param df the original dataFrame
  */
 public DataSet(DataSet df){
     this.data = df.getData();
     this.name = df.getName();
     this.header = df.getHeader();
     this.attrType = df.getAttrType();
 }

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
     DataSet.featuresInnerMapping = new HashMap<>();
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

 public static DataSet merge(DataSet a, DataSet b){
     return new DataSet();
 }

 public static DataSet loadArff(String ... path){
     return new DataSet();
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

