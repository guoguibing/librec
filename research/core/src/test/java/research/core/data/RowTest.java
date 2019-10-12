/**
 * 
 */
package research.core.data;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

/**
 * Row 测试类
 * @author dayang
 *
 */
public class RowTest {

	private Row row;

	@Before
	public void setUp() {
		row = new Row();
		row.add(1);
		row.add(3.23);
		row.add("dayang");
		row.add(true);
		row.add(null);
	}

	@Test
	public void testLength() {
		assertEquals(5, row.length());
	}

	@Test
	public void testSize() {
		assertEquals(5, row.size());
	}

	@Test
	public void testGetDouble() {
		assertEquals(1.0, row.getDouble(0), 1);
		assertEquals(3.23, row.getDouble(1), 2);
	}

	@Test
	public void testGetInt() {
		assertEquals(1, row.getInt(0));
	}

	@Test
	public void testGetBoolean() {
		assertEquals(true, row.getBoolean(3));
	}

	@Test
	public void testGetString() {
		assertEquals("1", row.getString(0));
		assertEquals("3.23", row.getString(1));
		assertEquals("dayang", row.getString(2));
		assertEquals("true", row.getString(3));
	}

	@Test
	public void testIsNull() {
		assertEquals(null, row.get(4));
	}

	@Test
	public void testApply() {
		assertEquals(1, row.apply(0));
		assertEquals(3.23, row.apply(1));
	}

}
