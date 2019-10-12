/**
 * 
 */
package research.data.reader.db;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;

/**
 * 用户行为数据数据库读写，数据格式为阿里天池大赛《阿里移动推荐算法》题目的数据集格式
 * 
 * @author ZhengYangPing
 *
 */
public class UserItemDao{
	
	/**
	 * 向数据库存入记录
	 * @param userItem
	 */
	public void save(UserItem userItem) {
		Session session = null;
		try {
			session = DataBaseUtil.getSession();
			session.beginTransaction();
			
			session.save(userItem);
			session.getTransaction().commit();
		}catch(Exception e) {
			e.printStackTrace();
			session.getTransaction().rollback();
		}finally {
			DataBaseUtil.closeSession(session);
		}
	}
	
	/**
	 * 删除记录
	 * @param id
	 */
	public void delete(int id) {
		Session session = null;
		try {
			session = DataBaseUtil.getSession();
			session.beginTransaction();

			UserItem userItem = (UserItem) session.load(UserItem.class, id);

			session.delete(userItem);
			session.getTransaction().commit();
		} catch (Exception e) {
			e.printStackTrace();
			session.getTransaction().rollback();
		} finally {
			DataBaseUtil.closeSession(session);
		}
	}
	
	/**
	 * 查询记录
	 * @param id
	 * @return
	 */
	public UserItem load(int id) {
		Session session = null;
		try {
			session = DataBaseUtil.getSession();
			session.beginTransaction();
			
			UserItem userItem = (UserItem)session.load(UserItem.class, id);
			session.getTransaction().commit();
			return userItem;
		}catch(Exception e) {
			e.printStackTrace();
			session.getTransaction().rollback();
		}finally {
			DataBaseUtil.closeSession(session);
		}
		return null;
	}
	
	/**
	 * 更新数据库记录
	 * @param id
	 * @param newUserItem
	 * @return
	 */
	public UserItem update(int id, UserItem newUserItem) {
		Session session = null;
		try {
			session = DataBaseUtil.getSession();
			session.beginTransaction();
			
			UserItem userItem = (UserItem)session.load(UserItem.class, id);
			userItem.setBehaviorType(newUserItem.getBehaviorType());
			userItem.setCreateTime(newUserItem.getCreateTime());
			userItem.setItemCategory(newUserItem.getItemCategory());
			userItem.setItemId(newUserItem.getItemId());
			userItem.setUserGeohash(newUserItem.getUserGeohash());
			userItem.setUserId(newUserItem.getUserId());
			
			session.update(userItem);
			session.getTransaction().commit();
			return userItem;
		}catch(Exception e) {
			e.printStackTrace();
			session.getTransaction().rollback();
		}finally {
			DataBaseUtil.closeSession(session);
		}
		return null;
	}
	
	/**
	 * 查询全部记录
	 * @return
	 */
	public List<UserItem> loadAll() {
		Session session = null;
		try {
			session = DataBaseUtil.getSession();
			session.beginTransaction();
			Criteria criteria = session.createCriteria(UserItem.class);
			List<UserItem> list = criteria.list();
			session.getTransaction().commit();
			return list;
		} catch (Exception e) {
			e.printStackTrace();
			session.getTransaction().rollback();
		} finally {
			DataBaseUtil.closeSession(session);
		}
		return null;
	}
}
