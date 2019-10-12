package research.core.vo;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户
 * 
 * @author liweigu714@163.com
 *
 */
public class User extends BaseVo {
	private static final long serialVersionUID = -7635233846566806646L;

	private String id;

	public User() {
		this.values = new ArrayList<Double>();
	}

	public User(List<Double> values) {
		this.values = values;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

}
