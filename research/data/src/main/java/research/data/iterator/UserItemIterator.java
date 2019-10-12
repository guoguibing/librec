package research.data.iterator;

import java.util.List;
import java.util.Map;

import research.core.data.iterator.BaseDataIterator;
import research.core.vo.BaseVo;
import research.core.vo.Rating;
import research.data.reader.UserItemReader;

/**
 * 用户物品数据迭代器
 * 
 * @author liweigu714@163.com
 *
 */
public class UserItemIterator extends BaseDataIterator {
	// 索引，代表读取到数据的位置。
	private int index;
	private int batchSize;
	private String userDataPath;
	private String itemDataPath;
	private String trainDataPath;

	/**
	 * 构造函数
	 * 
	 * @param batchSize 批量大小
	 * @param initProps 初始化属性
	 */
	public UserItemIterator(int batchSize, Map<String, Object> initProps) {
		this.batchSize = batchSize;
		this.userDataPath = (String) initProps.get("userDataPath");
		this.itemDataPath = (String) initProps.get("itemDataPath");
		this.trainDataPath = (String) initProps.get("trainDataPath");
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

	/**
	 * 返回多个数据对象
	 *
	 * @param num 需要返回的数量。如果剩下的数量不够，则只返回剩下的部分。
	 * @return 数据对象列表
	 */
	@Override
	public List<? extends BaseVo> next(int num) {
		// 分批读取训练数据
		List<Rating> ratings = UserItemReader.readRatings(userDataPath, itemDataPath, trainDataPath, this.index, num);
		// 更新索引
		this.index += num;
		return ratings;
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
}
