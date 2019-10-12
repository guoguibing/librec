package research.core.vo;

import java.io.Serializable;
import java.util.List;

/**
 * 物品列表
 * 
 * @author liweigu714@163.com
 *
 */
public class Items implements Serializable {
	private static final long serialVersionUID = 2513754803768719441L;
	private List<Item> items;

	public Items() {
	}

	public Items(List<Item> items) {
		this.items = items;
	}

	public List<Item> getItems() {
		return items;
	}

	public void setItems(List<Item> items) {
		this.items = items;
	}
}
