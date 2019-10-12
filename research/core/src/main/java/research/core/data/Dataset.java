package research.core.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据集
 * 
 * @author dayang
 *
 */
public class Dataset implements Serializable {
	
	private static final long serialVersionUID = -8850364536156316502L;

	/**
	 * 数据
	 */
	private List<Row> data;
	
	/**
	 * dataset名字
	 */
	private String name;
	
	/**
	 * 数据表头
	 */
	private List<String> header;
	
	/**
	 * 数据表列的属性
	 */
	private List<String> attrType;
	
	private Map<String,Integer> innerMap;
	
	public Dataset(List<Row> data, String name, List<String> header, List<String> attrType) {
		this.data = data;
		this.name = name;
		this.header = header;
		this.attrType = attrType;
		initInnerMap(header);
	}
	
	public Dataset(Dataset dataset) {
		this.data = dataset.getData();
		this.name = dataset.getName();
		this.header = dataset.getHeader();
		this.attrType = dataset.getAttrType();
		initInnerMap(dataset.getHeader());
	}
	
	private void initInnerMap(List<String> header) {
		this.innerMap = new HashMap<String, Integer>();
		for (int i = 0; i < header.size(); i++) {
			innerMap.put(header.get(i), i);
		}
	}
	
	/**
	 * 以表格形式将前20条记录打印到控制台
	 */
	public void show() {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * 
	 * @param objects
	 */
	public void add(Object[] objects) {
		Row row = new Row(objects);
		this.add(row);
	}
	
	/**
	 * Add a new row on the end of the dataset. 
	 * @param row
	 */
	public void add(Row row) {
		if(data == null) {
			data = new ArrayList<Row>();
		}
		
		data.add(row);
	}
	
	/**
	 * @return the data
	 */
	public List<Row> getData() {
		return data;
	}

	/**
	 * @param data the data to set
	 */
	public void setData(List<Row> data) {
		this.data = data;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the header
	 */
	public List<String> getHeader() {
		return header;
	}

	/**
	 * @param header the header to set
	 */
	public void setHeader(List<String> header) {
		this.header = header;
	}

	/**
	 * @return the attrType
	 */
	public List<String> getAttrType() {
		return attrType;
	}

	/**
	 * @param attrType the attrType to set
	 */
	public void setAttrType(List<String> attrType) {
		this.attrType = attrType;
	}

	/**
	 * 数值列的基本统计信息，包括计数、平均值、最小值和最大值。 {{{ ds.describe("age", "height").show()
	 *
	 * // output: 
	 * // summary age height
	 * // count 10.0 10.0 
	 * // sum  
	 * // mean 53.3 178.05 
	 * // stddev 11.6 15.7 
	 * // min 18.0 163.0 
	 * // max 92.0 192.0 }}}
	 * 
	 * @return
	 */
	public Dataset describe(String... clos) {
		//统计数据由五行组成，第一行为表头，第二行为计数，第三行为平均值，第四行为最小值，第五行为最大值
		List<Row> rowList = new ArrayList<Row>(5);
		List<String> header = new ArrayList<String>(clos.length+1);
		header.add("summary");
		header.addAll(Arrays.asList(clos));
		List<String> attrTypeList = new ArrayList<String>(clos.length+1);
		Row countRow = new Row(clos.length + 1);
		countRow.add("count");
		
		Row sumRow = new Row(clos.length +1);
		sumRow.add("sum");
		
		Row meanRow = new Row(clos.length + 1);
		meanRow.add("mean");
		
		Row minRow = new Row(clos.length+1);
		minRow.add("min");
		
		Row maxRow = new Row(clos.length +1 );
		maxRow.add("max");
		
		for(int i=0;i<clos.length;i++) {
			double min = data.get(0).getDouble(innerMap.get(clos[i]));
			double sum = 0;
			double max = min;
			for(int j=0;j<data.size();j++) {
				sum = sum + data.get(j).getDouble(innerMap.get(clos[i]));
				if(data.get(j).getDouble(innerMap.get(clos[i]))>max) {
					max = data.get(j).getDouble(innerMap.get(clos[i]));
				}
				if(data.get(j).getDouble(innerMap.get(clos[i]))<min) {
					min = data.get(j).getDouble(innerMap.get(clos[i]));
				}
			}
			countRow.add(data.size());
			sumRow.add(sum);
			minRow.add(min);
			maxRow.add(max);
			meanRow.add(sum/data.size());
		}
		rowList.add(countRow);
		rowList.add(sumRow);
		rowList.add(meanRow);
		rowList.add(minRow);
		rowList.add(maxRow);
		
		Dataset result = new Dataset(rowList, "describe", header, attrTypeList);  
		return result;
	}
	
	//TODO
	public int summary() {
		return 0;
	}
	
	public boolean equals() {
		return false;
	}
	
	/**
	 * Check wether the dataset is empty.
	 * @return
	 */
	public boolean isEmpty() {
		if (data == null) {
			return true;
		} else {
			return data.isEmpty();
		}
	}
	
	/**
	 * 返回第一行的数据
	 * @return
	 */
	public Row head() {
		if(isEmpty()) {
			return null;
		}else {
			return data.get(0);
		}
	}
	
	/**
	 * 返回第一行的数据
	 * @return
	 */
	public Row first() {
		return head();
	}
	
	/**
	 * 返回数据集的行数
	 * @return
	 */
	public int count() {
		if(isEmpty()) {
			return 0;
		}else {
			return data.size();
		}
	}
	
	/**
	 * 将所有列名作为数组返回
	 * 
	 * @return
	 */
	public String[] columns() {
		if (header == null) {
			return null;
		}
		String[] columns = new String[header.size()];
		header.toArray(columns);
		return columns;
	}
	
}
