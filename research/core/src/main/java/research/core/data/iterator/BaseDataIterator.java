package research.core.data.iterator;

import java.util.List;

import research.core.data.preprocessor.DataPreProcessorI;

/**
 * 数据迭代器基类
 * 
 * @author liweigu714@163.com
 *
 */
public abstract class BaseDataIterator implements DataIteratorI {
	protected List<DataPreProcessorI> preProcessors;

	/**
	 * 设置数据预处理器
	 *
	 * @param preProcessors 数据预处理器列表
	 */
	@Override
	public void setPreProcessor(List<DataPreProcessorI> preProcessors) {
		this.preProcessors = preProcessors;
	}

	/**
	 * 返回数据预处理器
	 *
	 * @return 数据预处理器
	 */
	@Override
	public List<DataPreProcessorI> getPreProcessor() {
		return this.preProcessors;
	}
}
