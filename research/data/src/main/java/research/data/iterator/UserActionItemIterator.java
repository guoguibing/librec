package research.data.iterator;

import java.util.List;
import java.util.Map;

import research.core.data.iterator.BaseDataIterator;
import research.core.vo.ActionResult;
import research.core.vo.BaseVo;
import research.data.reader.UserActionItemReader;

/**
 * 用户-行为-物品数据迭代器
 * 
 * @author liweigu714@163.com
 *
 */
public class UserActionItemIterator extends BaseDataIterator {
	// 索引，代表读取到数据的位置。
	private int index;
	private int batchSize;
	private String userDataPath;
	private String actionDataPath;
	private String itemDataPath;
	private String actionResultDataPath;
	private UserActionItemReader userActionItemReaderInstance;

	/**
	 * 构造函数
	 * 
	 * @param batchSize 批量大小
	 * @param initProps 初始化属性
	 */
	public UserActionItemIterator(int batchSize, Map<String, Object> initProps) {
		this.batchSize = batchSize;
		this.userDataPath = (String) initProps.get("userDataPath");
		this.actionDataPath = (String) initProps.get("actionDataPath");
		this.itemDataPath = (String) initProps.get("itemDataPath");
		this.actionResultDataPath = (String) initProps.get("actionResultDataPath");

		String strategy = null;
		if (initProps.containsKey("strategy")) {
			strategy = (String) initProps.get("strategy");
		}
		if (initProps.containsKey("useSpark")) {
			boolean useSpark = "true".equalsIgnoreCase((String) initProps.get("useSpark"));
			this.userActionItemReaderInstance = new UserActionItemReader(strategy, useSpark);
		} else {
			this.userActionItemReaderInstance = new UserActionItemReader(strategy);
		}
		
		if (initProps.containsKey("outputSize")) {
			int outputSize = (int) initProps.get("outputSize");
			this.userActionItemReaderInstance.setOutputSize(outputSize);
		}
	}

	/**
	 * 返回批量大小
	 * 
	 * @return 批量大小
	 */
	public int batch() {
		return this.batchSize;
	}

	/**
	 * 返回多个数据对象
	 *
	 * @param num 需要返回的数量。如果剩下的数量不够，则只返回剩下的部分。
	 * @return 数据对象列表
	 */
	@Override
	public List<? extends BaseVo> next(int num) {
		// 分批读取训练数据
		List<ActionResult> actionResults = this.userActionItemReaderInstance.readActionResults(this.userDataPath, this.actionDataPath, this.itemDataPath,
				this.actionResultDataPath, this.index, num);
		// 更新索引
		this.index += num;
		return actionResults;
	}

	/**
	 * 返回是否支持异步预读取数据
	 *
	 * @return 是否支持
	 */
	@Override
	public boolean asyncSupported() {
		return false;
	}

	/**
	 * 是否支持重置
	 *
	 * @return 是否支持
	 */
	@Override
	public boolean resetSupported() {
		return true;
	}

	/**
	 * 重置迭代器
	 */
	@Override
	public void reset() {
		this.index = 0;
	}

	/**
	 * 是否存在下一个数据对象
	 * 
	 * @return true表示存在，false表示不存在。
	 */
	@Override
	public boolean hasNext() {
		throw new UnsupportedOperationException("此方法未实现");
	}

	/**
	 * 返回下一个数据对象
	 * 
	 * @return 数据对象
	 */
	@Override
	public BaseVo next() {
		BaseVo vo = null;

		List<? extends BaseVo> vos = this.next(1);
		if (vos.size() > 0) {
			vo = vos.get(0);
		}

		return vo;
	}

}
