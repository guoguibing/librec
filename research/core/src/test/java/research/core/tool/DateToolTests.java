package research.core.tool;

import java.text.ParseException;
import java.util.Date;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * 日期工具测试
 * 
 * @author liweigu
 *
 */
public class DateToolTests {
	/**
	 * 日期转换测试
	 * 
	 * @throws ParseException 文本解析异常
	 */
	@Test
	public void dateFormat() throws ParseException {
		String s = "2019-01-15 14:33:00";
		// 将字符串转为日期对象
		Date date = DateTool.getTimeFormat().parse(s);
		// 将日期对象转为字符串
		String s2 = DateTool.getDayFormat().format(date);
		// 检验结果是否符合预期
		assertEquals(s2, "20190115");
	}
}
