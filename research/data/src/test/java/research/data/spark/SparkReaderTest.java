/**
 * 
 */
package research.data.spark;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.junit.jupiter.api.Test;

/**
 * @author dayang
 *
 */
class SparkReaderTest {

	@Test
	public void testReadCSVData() {
		SparkReader sparkReader = new SparkReader();
		sparkReader.setDataFormat("CSV");
		sparkReader.setSeq(",");
		sparkReader.setInputDataPath(System.getProperty("user.dir") + "/src/test/java/research/data/spark/user.csv");
		Dataset<Row> dataset = sparkReader.readData();
		String[] actualColumns = dataset.columns();
		String[] expectedColumns = new String[] { "id", "name", "sex", "age", "height", "weight" };
		assertArrayEquals(expectedColumns, actualColumns);
		Row r = dataset.first();
		assertEquals(r.size(), 6);
		dataset.show();
	}
	
	@Test
	public void testReadTextData() {
		SparkReader sparkReader = new SparkReader();
		sparkReader.setDataFormat("Text");
		sparkReader.setInputDataPath(System.getProperty("user.dir") + "/src/test/java/research/data/spark/user.txt");
		Dataset<Row> dataset = sparkReader.readData();
		String[] actualColumns = dataset.columns();
		String[] expectedColumns = new String[] { "value" };
		assertArrayEquals(expectedColumns, actualColumns);
		Row r = dataset.first();
		assertEquals(r.getString(0), "10001,dayang,1,35,170,65");
		dataset.show();
	}
	
	
}
