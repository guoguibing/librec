package research.core.tool;

import java.io.UnsupportedEncodingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 字符串工具
 * 
 * @author liweigu714@163.com
 *
 */
public class StringTool {
	private static Logger LOG = LoggerFactory.getLogger(StringTool.class);

	/**
	 * 转换编码
	 * 
	 * @param str 字符串
	 * @param fromEncoding 从编码
	 * @param toEncoding 到编码。如果toEncoding为空则返回原值。
	 * @return 字符串
	 */
	public static String encode(String str, String fromEncoding, String toEncoding) {
		String result = str;

		if (toEncoding != null && toEncoding.length() > 0) {
			try {
				if (fromEncoding == null || fromEncoding.length() == 0) {
					result = new String(str.getBytes(), toEncoding);
				} else {
					result = new String(str.getBytes(fromEncoding), toEncoding);
				}
			} catch (UnsupportedEncodingException e) {
				LOG.warn("转换编码失败", e);
			}
		}

		return result;
	}

}
