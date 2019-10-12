/**
 * 
 */
package research.data.reader.db;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

/**
 * Hibernate工具类
 * @author ZhengYangPing
 *
 */
public class DataBaseUtil {
	
	private static SessionFactory factory;

	static {
		try {
			// 读取hibernate.cfg.xml文件
			Configuration cfg = new Configuration().configure();

			// 建立SessionFactory
			factory = cfg.buildSessionFactory();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 打开Session
	 * @return
	 */
	public static Session getSession() {
		return factory.openSession();
	}

	/**
	 * 关闭Session
	 * @param session
	 */
	public static void closeSession(Session session) {
		if (session != null) {
			if (session.isOpen()) {
				session.close();
			}
		}
	}


	public static SessionFactory getSessionFactory() {
		return factory;
	}
}
