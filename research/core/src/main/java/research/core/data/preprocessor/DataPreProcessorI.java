package research.core.data.preprocessor;

import java.io.Serializable;

import research.core.data.Dataset;

/**
 * 数据预处理器接口
 * 
 * @author liweigu714@163.com
 *
 */
public interface DataPreProcessorI extends Serializable {
	void preProcess(Dataset toPreProcess);
}
