package research.core.vo;

import java.io.Serializable;
import java.util.List;

import org.apache.spark.sql.Dataset;

/**
 * 数据对象接口
 * 
 * @author liweigu714@163.com
 *
 */
public interface VoI extends Serializable {
	/**
	 * 设置浮点值
	 * 
	 * @param values 浮点值
	 */
	void setDoubleValue(List<Double> values);

	/**
	 * 返回浮点值
	 * 
	 * @return 浮点值
	 */
	List<Double> getDoubleValue();

	/**
	 * 返回数据集
	 * 
	 * @return 数据集
	 */
	Dataset dataset();
}
