package research.data.reader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import research.core.tool.StringTool;

/**
 * Excel文件读取器
 * 
 * @author liweigu714@163.com
 *
 */
public class ExcelReader {
	private static Logger LOG = LoggerFactory.getLogger(ExcelReader.class);

	/**
	 * 读本地文件
	 * 
	 * @param path 文件路径
	 * @param fromEncoding 编码
	 * @param toEncoding 编码
	 * @return 文件内容
	 */
	public static List<List<List<String>>> readFile(String path, String fromEncoding, String toEncoding) {
		List<List<List<String>>> sheetDatas = new ArrayList<List<List<String>>>();

		File xlsFile = new File(path);
		// 获得工作簿
		Workbook workbook = null;
		try {
			workbook = WorkbookFactory.create(xlsFile);
		} catch (Exception e) {
			LOG.warn("打开excel失败。path=" + path, e);
			return null;
		}
		// 获得工作表个数
		int sheetCount = workbook.getNumberOfSheets();
		// 遍历工作表
		for (int i = 0; i < sheetCount; i++) {
			List<List<String>> sheetData = new ArrayList<List<String>>();
			Sheet sheet = workbook.getSheetAt(i);
			// 获得行数
			int rows = sheet.getLastRowNum() + 1;
			// 获得列数，先获得一行，在得到改行列数
			Row tmp = sheet.getRow(0);
			if (tmp == null) {
				continue;
			}
			int cols = tmp.getPhysicalNumberOfCells();
			// 读取数据
			for (int row = 0; row < rows; row++) {
				List<String> rowData = new ArrayList<String>();
				Row r = sheet.getRow(row);
				for (int col = 0; col < cols; col++) {
					Cell cell = r.getCell(col);
					String value = cell == null ? "" : cell.getStringCellValue();
					value = StringTool.encode(value, fromEncoding, toEncoding);
					rowData.add(value);
				}
				sheetData.add(rowData);
			}
			sheetDatas.add(sheetData);
		}
		try {
			workbook.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return sheetDatas;
	}
}
