package research.core.vo;

import java.util.ArrayList;
import java.util.List;

/**
 * 标签
 * 
 * @author liweigu714@163.com
 *
 */
public class Label extends BaseVo {
	private static final long serialVersionUID = -683249698941076369L;

	public Label() {
		this.values = new ArrayList<Double>();
	}

	public Label(List<Double> values) {
		this.values = values;
	}

}
