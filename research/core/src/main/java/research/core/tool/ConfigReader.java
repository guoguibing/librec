package research.core.tool;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 配置信息读取器
 * 
 * @author liweigu714@163.com
 *
 */
public class ConfigReader {
	static Logger LOG = LoggerFactory.getLogger(ConfigReader.class);
	// 用于缓存
	private static Map<String, Properties> Props = new HashMap<String, Properties>();

	/**
	 * 读取配置值
	 * 
	 * @param key 配置key
	 * @param file 配置文件，如"config.properties"
	 * @return 配置值
	 */
	public static String getValue(String key, String file) {
		Properties prop;
		String value = null;
		try {
			if (Props.containsKey(file)) {
				prop = Props.get(file);
			} else {
				prop = readPropertiesFile(file);
			}
			value = prop.getProperty(key);
		} catch (Exception e) {
			LOG.info("读取配置信息发生异常", e);
		}
		return value;
	}

	/**
	 * 读取配置文件
	 * 
	 * @param fileName 配置文件
	 * @return 配置信息
	 */
	private static Properties readPropertiesFile(String fileName) {
		// 从user.dir读取文件流
		String filePath = System.getProperty("user.dir") + File.separator + fileName;
		InputStream inStream = null;
		try {
			inStream = new FileInputStream(filePath);
		} catch (FileNotFoundException localFileNotFoundException) {
		}
		// 如果文件流为空，再从Resource读取文件流
		if (inStream == null) {
			inStream = ConfigReader.class.getClassLoader().getResourceAsStream(fileName);
		}
		Properties prop = new Properties();
		try {
			if (inStream != null) {
				prop.load(inStream);
				inStream.close();
			} else {
				prop = null;
			}
		} catch (Exception e) {
			LOG.info("读取配置信息发生异常", e);
			prop = null;
		}
		return prop;
	}
}
