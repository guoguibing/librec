/**
 * 
 */
package research.data.convertor;

import java.io.IOException;

import research.core.data.Dataset;

/**
 * 数据转换器接口
 * @author ZhengYangPing
 *
 */
public interface DataConvertor {
	/**
	 * 处理数据
	 * @throws IOException
	 */
	void processData() throws IOException;
	
	/**
	 * 获得转换后的目标数据
	 * @return 
	 */
	Dataset getMatrix();
}
