package research.data.writer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.List;

public class FileDataWriter {
	/**
	 * 写本地文件
	 * 
	 * @param output 本地文件路径
	 * @param lines 文件内容，每行内容对应列表里一个字符串。
	 */
	public static void writeFile(String output, List<String> lines) {
		boolean append = true;
		writeFile(output, lines, append);
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
