/**
 * 
 */
package research.core.data;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

/**
 * Dataset测试类
 * 
 * @author dayang
 *
 */
public class DatasetTest {

	private Dataset dataset;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		List<Row> row = new ArrayList<Row>();
		Row row1 = new Row(5);
		row1.add(1);
		row1.add("zhengnen");
		row1.add(1985);
		row1.add(86);
		row.add(row1);
		Row row2 = new Row(5);
		row2.add(2);
		row2.add("zhengyang");
		row2.add(1987);
		row2.add(79);
		row.add(row2);
		Row row3 = new Row(5);
		row3.add(3);
		row3.add("zhengjing");
		row3.add(1990);
		row3.add(95);
		row.add(row3);
		String name = "test";
		List<String> header = new ArrayList<String>();
		header.add("Id");
		header.add("Name");
		header.add("Year");
		header.add("Score");
		List<String> attrType = new ArrayList<String>();
		attrType.add("NUMERIC");
		attrType.add("STRING");
		attrType.add("NUMERIC");
		attrType.add("NUMERIC");

		dataset = new Dataset(row, name, header, attrType);
	}

	@Test
	public void testCount() {
		assertEquals(3,dataset.count());
	}
	
	@Test
	public void testIsEmpty() {
		assertEquals(false, dataset.isEmpty());
	}
	
	@Test
	public void testHead() {
		Row row = new Row();
		row.add(1);
		row.add("zhengnen");
		row.add(1985);
		row.add(86);
		assertEquals(row, dataset.first());
	}
	
	@Test
	public void testColumns() {
		String[] header = new String[] {"Id", "Name", "Year", "Score"};
		assertArrayEquals(header,dataset.columns());
	}
	
	@Test
	public void testDescribe() {
		Dataset result = dataset.describe("Id", "Year", "Score");
		String[] columns = new String[] {"summary","Id","Year","Score"};
		assertArrayEquals(columns, result.columns());
		assertEquals(5, result.count());
		
		//count,sum,mean,min,max
		Row row1= new Row();
		row1.add("count");
		row1.add(3);
		row1.add(3);
		row1.add(3);
		assertEquals(row1,result.getData().get(0));
		
		Row row2 = new Row();
		row2.add("sum");
		row2.add(1.0+2+3);
		row2.add(1985.0+1987+1990);
		row2.add(86.0+79+95);
		assertEquals(row2,result.getData().get(1));
		
		Row row3 = new Row();
		row3.add("mean");
		row3.add((1.0+2+3)/3);
		row3.add((1985.0+1987+1990)/3);
		row3.add((86.0+79+95)/3);
		assertEquals(row3, result.getData().get(2));
		
		Row row4 = new Row();
		row4.add("min");
		row4.add(1.0);
		row4.add(1985.0);
		row4.add(79.0);
		assertEquals(row4, result.getData().get(3));
		
		Row row5 = new Row();
		row5.add("max");
		row5.add(3.0);
		row5.add(1990.0);
		row5.add(95.0);
		assertEquals(row5, result.getData().get(4));
	}

}
