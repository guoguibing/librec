/**
 * 
 */
package research.data.reader.db;

import java.util.Date;

/**
 * 用户行为数据
 * @author ZhengYangPing
 */
public class UserItem {
	/**
	 * id
	 */
	private int id;

	/**
	 * 用户标识
	 */
	private int userId;

	/**
	 * 商品标识
	 */
	private int itemId;

	/**
	 * 用户对商品的行为类型，包括浏览、收藏、加购物车、购买，对应取值分别为1、2、3、4
	 */
	private int behaviorType;

	/**
	 * 用户位置的空间标识
	 */
	private String userGeohash;

	/**
	 * 商品分类标识
	 */
	private String itemCategory;

	/**
	 * 行为发生时间
	 */
	private Date createTime;

	public UserItem() {
		super();
	}

	public UserItem(int id, int userId, int itemId, int behaviorType, String userGeohash, String itemCategory, Date createTime) {
		super();
		this.id = id;
		this.userId = userId;
		this.itemId = itemId;
		this.behaviorType = behaviorType;
		this.userGeohash = userGeohash;
		this.itemCategory = itemCategory;
		this.createTime = createTime;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getUserId() {
		return userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
	}

	public int getItemId() {
		return itemId;
	}

	public void setItemId(int itemId) {
		this.itemId = itemId;
	}

	public int getBehaviorType() {
		return behaviorType;
	}

	public void setBehaviorType(int behaviorType) {
		this.behaviorType = behaviorType;
	}

	public String getUserGeohash() {
		return userGeohash;
	}

	public void setUserGeohash(String userGeohash) {
		this.userGeohash = userGeohash;
	}

	public String getItemCategory() {
		return itemCategory;
	}

	public void setItemCategory(String itemCategory) {
		this.itemCategory = itemCategory;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

}
