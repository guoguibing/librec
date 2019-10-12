/**
 * 
 */
package research.data.spark;

import java.util.Properties;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import research.core.tool.SparkTool;

/**
 * 
 * spark数据源读取器
 * 
 * @author dayang
 *
 */
public class SparkReader {

	/**
	 * 日志
	 */
	private static final Logger LOG = LoggerFactory.getLogger(SparkReader.class);

	/**
	 * CSV格式
	 */
	private static final String FORMAT_CSV = "CSV";

	/**
	 * JSON格式
	 */
	private static final String FORMAT_JSON = "JSON";

	/**
	 * TEXT格式
	 */
	private static final String FORMAT_TEXT = "TEXT";

	/**
	 * JDBC格式
	 */
	private static final String FORMAT_JDBC = "JDBC";

	/**
	 * 数据文件格式，包括以下几种：csv、json、text、jdbc
	 */
	private String dataFormat;

	/**
	 * 文件全路径
	 */
	private String inputDataPath;

	/**
	 * 分隔符,默认为逗号
	 */
	private String seq = ",";

	/**
	 * 属性文件，JDBC读取器使用，除了包含数据库的基本配置外，还需要包含url和table
	 */
	private Properties properties;

	/**
	 * 默认构造函数
	 */
	public SparkReader() {

	}

	/**
	 * 构造函数
	 * 
	 * @param dataFormat
	 */
	public SparkReader(String dataFormat) {
		this.dataFormat = dataFormat;
	}

	/**
	 * 构造函数
	 * 
	 * @param properties
	 */
	public SparkReader(Properties properties) {
		this.properties = properties;
	}

	/**
	 * 构造函数
	 * 
	 * @param dataFormat
	 * @param inputDataPath
	 * @param seq
	 */
	public SparkReader(String dataFormat, String inputDataPath, String seq) {
		this.dataFormat = dataFormat;
		this.inputDataPath = inputDataPath;
		this.seq = seq;
	}

	/**
	 * @return the dataFormat
	 */
	public String getDataFormat() {
		return dataFormat;
	}

	/**
	 * @param dataFormat the dataFormat to set
	 */
	public void setDataFormat(String dataFormat) {
		this.dataFormat = dataFormat;
	}

	/**
	 * @return the inputDataPath
	 */
	public String getInputDataPath() {
		return inputDataPath;
	}

	/**
	 * @param inputDataPath the inputDataPath to set
	 */
	public void setInputDataPath(String inputDataPath) {
		this.inputDataPath = inputDataPath;
	}

	/**
	 * @return the seq
	 */
	public String getSeq() {
		return seq;
	}

	/**
	 * @param seq the seq to set
	 */
	public void setSeq(String seq) {
		this.seq = seq;
	}

	/**
	 * @return the properties
	 */
	public Properties getProperties() {
		return properties;
	}

	/**
	 * @param properties the properties to set
	 */
	public void setProperties(Properties properties) {
		this.properties = properties;
	}

	/**
	 * 读数据
	 * 
	 * @return
	 */
	public Dataset<Row> readData() {
		if (FORMAT_CSV.equalsIgnoreCase(dataFormat)) {
			return readCSVData();
		} else if (FORMAT_JSON.equalsIgnoreCase(dataFormat)) {
			return readJSONData();
		} else if (FORMAT_TEXT.equalsIgnoreCase(dataFormat)) {
			return readTextData();
		} else if (FORMAT_JDBC.equalsIgnoreCase(dataFormat)) {
			return readJDBCData();
		} else {
			throw new UnsupportedOperationException("Unknow dataFormat:" + dataFormat);
		}

	}

	/**
	 * 读jdbc数据
	 * 
	 * @return
	 */
	private Dataset<Row> readJDBCData() {
		if (properties == null) {
			LOG.error("Can't read jdbc data,the properties is null.");
			return null;
		} else if (!properties.containsKey("url") || properties.containsKey("table")) {
			LOG.error("Can't read jdbc data,the url or table is null.");
		}
		Dataset<Row> dataset = SparkTool.getSparkSession().read().jdbc(properties.getProperty("url"), properties.getProperty("table"), properties);
		return dataset;
	}

	/**
	 * 读文本文件
	 * 
	 * @return
	 */
	private Dataset<Row> readTextData() {
		Dataset<Row> dataset = SparkTool.getSparkSession().read().text(inputDataPath);
		return dataset;
	}

	/**
	 * 读CSV文件
	 * 
	 * @return
	 */
	private Dataset<Row> readCSVData() {
		Dataset<Row> dataset = SparkTool.getSparkSession().read().option("header", "true").option("sep", seq).csv(inputDataPath);
		return dataset;
	}

	/**
	 * 读JSON文件
	 * 
	 * @return
	 */
	private Dataset<Row> readJSONData() {
		Dataset<Row> dataset = SparkTool.getSparkSession().read().json(inputDataPath);
		return dataset;
	}

}
