package research.core.tool;

import java.text.SimpleDateFormat;

/**
 * 日期工具
 * 
 * @author liweigu714@163.com
 *
 */
public class DateTool {
	/**
	 * 获取"yyyy-MM-dd HH:mm:ss"日期格式
	 * 
	 * @return 日期格式
	 */
	public static SimpleDateFormat getTimeFormat() {
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	}

	/**
	 * 获取"yyyyMMddHHmmss"日期格式
	 * 
	 * @return 日期格式
	 */
	public static SimpleDateFormat getCompactTimeFormat() {
		return new SimpleDateFormat("yyyyMMddHHmmss");
	}

	/**
	 * 获取"yyyyMMdd"日期格式
	 * 
	 * @return 日期格式
	 */
	public static SimpleDateFormat getDayFormat() {
		return new SimpleDateFormat("yyyyMMdd");
	}
}
