package research.core.vo;

import java.util.ArrayList;
import java.util.List;

/**
 * 行为结果
 * 
 * @author liweigu
 *
 */
public class ActionResult extends BaseVo {
	private static final long serialVersionUID = 5073742148197563281L;
	private User user;
	private Action action;
	private Item item;
	private Items items;
	private Label label;

	public ActionResult(User user, Action action, Item item, Label label) {
		this.user = user;
		this.action = action;
		this.item = item;
		this.label = label;
	}

	public ActionResult(User user, Action action, Items items, Label label) {
		this.user = user;
		this.action = action;
		this.items = items;
		this.label = label;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public Action getAction() {
		return action;
	}

	public void setAction(Action action) {
		this.action = action;
	}

	public Item getItem() {
		return item;
	}

	public void setItem(Item item) {
		this.item = item;
	}

	public Label getLabel() {
		return label;
	}

	public void setLabel(Label label) {
		this.label = label;
	}

	@Override
	public List<Double> getDoubleValue() {
		List<Double> values = new ArrayList<Double>();

		List<Double> userValues;
		if (this.user != null) {
			userValues = this.user.getDoubleValue();
		} else {
			// 用默认值。 TODO
			userValues = new ArrayList<Double>();
			for (int i = 0; i < 66; i++) {
				userValues.add(0.);
			}
		}
		values.addAll(userValues);

		values.addAll(this.action.getDoubleValue());

		List<Double> itemValues;
		if (this.item != null) {
			itemValues = this.item.getDoubleValue();
		} else if (this.items != null) {
			itemValues = new ArrayList<Double>();
			for (int i = 0; i < this.items.getItems().size(); i++) {
				Item item = this.items.getItems().get(i);
				itemValues.addAll(item.getDoubleValue());
			}
			while (itemValues.size() < (2 + 4 * 7)) {
				itemValues.add(itemValues.size(), 0.);
			}
		} else {
			// 用默认值。 TODO
			itemValues = new ArrayList<Double>();
			while (itemValues.size() < (2 + 4 * 7)) {
				itemValues.add(itemValues.size(), 0.);
			}
		}
		values.addAll(itemValues);

		if (this.label != null) {
			values.addAll(this.label.getDoubleValue());
		} else {
			// 不添加label值
		}

		return values;
	}

}
