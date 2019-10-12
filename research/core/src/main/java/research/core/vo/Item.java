package research.core.vo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 物品
 * 
 * @author liweigu714@163.com
 *
 */
public class Item extends BaseVo {
	private static final long serialVersionUID = 7488113199147911961L;

	private int id;
	// 扩展属性
	private Map<String, List<Double>> ext = new HashMap<String, List<Double>>();

	public Item() {
		this.values = new ArrayList<Double>();
	}

	public Item(List<Double> values) {
		this.values = values;
	}

	public int getId() {
		return this.id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Map<String, List<Double>> getExt() {
		return ext;
	}

	public void setExt(Map<String, List<Double>> ext) {
		this.ext = ext;
	}

}
