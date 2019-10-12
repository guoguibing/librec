
package research.data.reader.db;
import java.util.Date;
import org.hibernate.cfg.Configuration;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.junit.Before;
import org.junit.Test;
import research.data.reader.db.UserItem;
import research.data.reader.db.UserItemDao;


/**
 * 利用h2实现数据库操作测试类
 * 
 * @author ZhengYangPing
 *
 */
public class UserItemDaoTest {

	@Before
	public void setUp() {
		Configuration cfg = new Configuration().configure();
		SchemaExport export = new SchemaExport(cfg);
		export.create(true, true);
	}

	@Test
	public void testSave() {
		UserItem userItem = new UserItem();
		userItem.setId(1);
		userItem.setItemId(1);
		userItem.setUserId(1);
		userItem.setBehaviorType(1);
		userItem.setItemCategory("1");
		userItem.setUserGeohash("1378");
		userItem.setCreateTime(new Date());
		UserItemDao userItemDao = new UserItemDao();
		userItemDao.save(userItem);
	}

	@Test
	public void testDelete() {
		UserItemDao userItemDao = new UserItemDao();
		userItemDao.delete(1);
	}

	@Test
	public void testLoad() {
		UserItemDao userItemDao = new UserItemDao();
		userItemDao.load(1);
	}

	@Test
	public void testLoadAll() {
		UserItemDao userItemDao = new UserItemDao();
		userItemDao.loadAll();
	}

	@Test
	public void testUpdate() {
		UserItemDao userItemDao = new UserItemDao();
		UserItem userItem = new UserItem();
		userItem.setItemId(2);
		userItem.setUserId(2);
		userItem.setBehaviorType(2);
		userItem.setItemCategory("2");
		userItem.setUserGeohash("2222");
		userItem.setCreateTime(new Date());
		userItemDao.update(1, userItem);
	}

}
