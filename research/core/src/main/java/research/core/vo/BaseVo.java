package research.core.vo;

import java.util.ArrayList;
import java.util.List;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

import research.core.tool.SparkTool;

/**
 * 数据对象基类
 * 
 * @author liweigu714@163.com
 *
 */
public abstract class BaseVo implements VoI {
	private static final long serialVersionUID = -3659395323547619813L;
	protected List<Double> values;

	@Override
	public void setDoubleValue(List<Double> values) {
		this.values = values;
	}

	@Override
	public List<Double> getDoubleValue() {
		return values;
	}

	/**
	 * 返回数据集
	 * 
	 * @return 数据集
	 */
	@Override
	public Dataset<Row> dataset() {
		List<Row> rows = new ArrayList<Row>();
		Row row = RowFactory.create(this.getDoubleValue());
		rows.add(row);
		JavaRDD<Row> rowsRdd = SparkTool.getSparkContext().parallelize(rows);
		List<StructField> fields = new ArrayList<>();
		for (int i = 0; i < this.getDoubleValue().size(); i++) {
			fields.add(DataTypes.createStructField("col" + i, DataTypes.DoubleType, true));
		}
		StructType schema = DataTypes.createStructType(fields);
		SparkTool.getSparkSession().createDataFrame(rowsRdd, schema);
		throw new UnsupportedOperationException();
	}
}
