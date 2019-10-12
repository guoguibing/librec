package research.core.data.iterator;

import java.util.Iterator;
import java.util.List;

import research.core.data.preprocessor.DataPreProcessorI;
import research.core.vo.BaseVo;

/**
 * 数据迭代器接口
 * 
 * @author liweigu
 *
 */
public interface DataIteratorI extends Iterator<BaseVo> {
	/**
	 * 返回多个数据对象
	 *
	 * @param num 需要返回的数量。如果剩下的数量不够，则只返回剩下的部分。
	 * @return 数据对象列表
	 */
	List<? extends BaseVo> next(int num);

	/**
	 * 返回是否支持异步预读取数据
	 *
	 * @return 是否支持
	 */
	boolean asyncSupported();

	/**
	 * 是否支持重置
	 *
	 * @return 是否支持
	 */
	boolean resetSupported();

	/**
	 * 重置迭代器
	 */
	void reset();

	/**
	 * 设置数据预处理器
	 *
	 * @param preProcessors 数据预处理器列表
	 */
	void setPreProcessor(List<DataPreProcessorI> preProcessors);

	/**
	 * 返回数据预处理器
	 *
	 * @return 数据预处理器
	 */
	List<DataPreProcessorI> getPreProcessor();

}
