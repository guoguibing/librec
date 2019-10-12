package research.data.reader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件读取器
 * 
 * @author liweigu714@163.com
 *
 */
public class FileTool {
	/**
	 * 编码值为 "utf-8"
	 */
	private static String Encoding = "utf-8";

	/**
	 * 读本地文件
	 * 
	 * @param filePath 文件路径
	 * @return 文件内容，每行内容对应列表里一个字符串。
	 */
	public static List<String> readFile(String filePath) {
		return readFile(new File(filePath));
	}

	/**
	 * 读本地文件
	 * 
	 * @param inputFile 文件
	 * @return 文件内容，每行内容对应列表里一个字符串。
	 */
	public static List<String> readFile(File inputFile) {
		return readFile(inputFile, 0, -1);
	}

	/**
	 * 读本地文件
	 * 
	 * @param filePath 文件路径
	 * @param start 起始索引
	 * @param count 记录数
	 * @return 文件内容，每行内容对应列表里一个字符串。
	 */
	public static List<String> readFile(String filePath, int start, int count) {
		return readFile(new File(filePath), start, count);
	}

	/**
	 * 读本地文件
	 * 
	 * @param inputFile 文件
	 * @param start 起始索引
	 * @param count 记录数。-1表示读取全部记录。
	 * @return 文件内容，每行内容对应列表里一个字符串。
	 */
	public static List<String> readFile(File inputFile, int start, int count) {
		ArrayList<String> lines = new ArrayList<String>();

		boolean readAll = count == -1;
		// System.out.println("readAll = " + readAll);

		try {
			InputStreamReader isr = new InputStreamReader(new FileInputStream(inputFile), Encoding);
			BufferedReader read = new BufferedReader(isr);
			String line = null;
			int index = 0;
			while ((line = read.readLine()) != null) {
				if (index >= start && (readAll || count > 0)) {
					lines.add(line);
					--count;
				}
				++index;
				if (!readAll && count < 0) {
					break;
				}
			}
			read.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return lines;
	}

	/**
	 * 写本地文件
	 * 
	 * @param output 本地文件路径
	 * @param lines 文件内容，每行内容对应列表里一个字符串。
	 * @param append 是否追加文件，如果为false则重写文件。
	 */
	public static void writeFile(String output, List<String> lines, boolean append) {
		try {
			BufferedWriter write = new BufferedWriter(new FileWriter(output, append));

			for (String line : lines) {
				write.write(line);
				char lineBreak = '\n';
				char[] chars = { lineBreak };
				write.write(chars);
			}

			write.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
