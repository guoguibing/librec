/**
 * Copyright (C) 2016 LibRec
 *
 * This file is part of LibRec.
 * LibRec is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LibRec is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LibRec. If not, see <http://www.gnu.org/licenses/>.
 */
package net.librec.data.convertor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;

import net.librec.math.structure.SparseMatrix;
import net.librec.util.Strings;

/**
 * Generic Data Convertor
 *
 * @author WangYuFeng and liuxz
 */
public class TextDataConvertor extends AbstractDataConvertor {
	private static final Log LOG = LogFactory.getLog(TextDataConvertor.class);
	private static final int BSIZE = 1024 * 1024;
	private static final String DATA_COLUMN_DEFAULT_FORMAT = "UIR";
	// store time data as {user, item, timestamp} matrix

	private String dataColumnFormat;
	private String inputDataPath;

	// user/item {raw id, inner id} map
	private BiMap<String, Integer> userIds, itemIds;

	// time unit may depend on data sets, e.g. in MovieLens, it is unix seconds
	private TimeUnit timeUnit;

	// already loaded files/total files in dataDirectory
	private float loadFilePathRate;
	// loaded data size /total data size in one data file
	private float loadDataFileRate;

	// loaded data size /total data size in all data file
	private float loadAllFileRate;

	public TextDataConvertor(String inputDataPath) {
		this(DATA_COLUMN_DEFAULT_FORMAT, inputDataPath);
	}

	public TextDataConvertor(String dataColumnFormat, String inputDataPath) {
		this.dataColumnFormat = dataColumnFormat;
		this.inputDataPath = inputDataPath;
		this.timeUnit = TimeUnit.SECONDS;
	}

	public void processData() throws IOException {
		readData(dataColumnFormat, inputDataPath);
	}

	/**
	 *
	 * @return
	 * @throws IOException
	 * @throws Exception
	 */
	public void readData(String dataColumnFormat, String inputDataPath) throws IOException {
		readData(dataColumnFormat, inputDataPath, -1);
	}

	/**
	 * Read data from the data file. Note that we didn't take care of the
	 * duplicated lines.
	 *
	 * @param cols
	 *            the indexes of the relevant columns in the data file: {user,
	 *            item, [rating, timestamp] (optional)}
	 * @param binThold
	 *            the threshold to binarize a rating. If a rating is greater
	 *            than the threshold, the value will be 1; otherwise 0. To
	 *            disable this feature, i.e., keep the original rating value,
	 *            set the threshold a negative value
	 * @return a sparse matrix storing all the relevant data
	 * @throws IOException
	 */
	public void readData(String dataColumnFormat, String inputDataPath, double binThold) throws IOException {
		LOG.info(String.format("Dataset: %s", Strings.last(inputDataPath, 38)));
		// Table {row-id, col-id, rate}
		Table<Integer, Integer, Double> dataTable = HashBasedTable.create();
		// Table {row-id, col-id, timestamp}
		Table<Integer, Integer, Long> timeTable = null;
		// Map {col-id, multiple row-id}: used to fast build a rating matrix
		Multimap<Integer, Integer> colMap = HashMultimap.create();
		// BiMap {raw id, inner id} userIds, itemIds
		this.userIds = HashBiMap.create();
		this.itemIds = HashBiMap.create();
		final List<File> files = new ArrayList<File>();
		final ArrayList<Long> fileSizeList = new ArrayList<Long>();
		SimpleFileVisitor<Path> finder = new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				fileSizeList.add(file.toFile().length());
				files.add(file.toFile());
				return super.visitFile(file, attrs);
			}
		};
		Files.walkFileTree(Paths.get(inputDataPath), finder);
		LOG.info("All dataset files " + files.toString());
		long allFileSize = 0;
		for (Long everyFileSize : fileSizeList) {
			allFileSize = allFileSize + everyFileSize.longValue();
		}
		LOG.info("All dataset files size " + Long.toString(allFileSize));
		int readingFileCount = 0;
		long loadAllFileByte = 0;
		// loop every dataFile collecting from walkFileTree
		for (File dataFile : files) {
			LOG.info("Now loading dataset file " + dataFile.toString().substring(
					dataFile.toString().lastIndexOf(File.separator) + 1, dataFile.toString().lastIndexOf(".")));
			readingFileCount += 1;
			loadFilePathRate = readingFileCount / (float) files.size();
			long readingOneFileByte = 0;
			FileInputStream fileInputStream = new FileInputStream(dataFile);
			FileChannel fileRead = fileInputStream.getChannel();
			ByteBuffer buffer = ByteBuffer.allocate(BSIZE);
			int len;
			String bufferLine = new String();
			byte[] bytes = new byte[BSIZE];
			while ((len = fileRead.read(buffer)) != -1) {
				readingOneFileByte += len;
				loadDataFileRate = readingOneFileByte / (float) fileRead.size();
				loadAllFileByte += len;
				loadAllFileRate = loadAllFileByte / (float) allFileSize;
				buffer.flip();
				buffer.get(bytes, 0, len);
				bufferLine = bufferLine.concat(new String(bytes, 0, len));
				// String spl = System.getProperty("line.separator");
				String[] bufferData = bufferLine.split(System.getProperty("line.separator") + "+");
				boolean isComplete = bufferLine.endsWith(System.getProperty("line.separator"));
				int loopLength = isComplete ? bufferData.length : bufferData.length - 1;
				for (int i = 0; i < loopLength; i++) {
					String line = new String(bufferData[i]);
					String[] data = line.trim().split("[ \t,]+");
					String user = data[0];
					String item = data[1];
					Double rate = ((dataColumnFormat.equals("UIR") || dataColumnFormat.equals("UIRT"))
							&& data.length >= 3) ? Double.valueOf(data[2]) : 1.0;

					// binarize the rating for item recommendation task
					if (binThold >= 0) {
						rate = rate > binThold ? 1.0 : -1.0;
					}

					// inner id starting from 0
					int row = userIds.containsKey(user) ? userIds.get(user) : userIds.size();
					userIds.put(user, row);

					int col = itemIds.containsKey(item) ? itemIds.get(item) : itemIds.size();
					itemIds.put(item, col);

					dataTable.put(row, col, rate);
					colMap.put(col, row);
					// record rating's issuing time
					if (StringUtils.equals(dataColumnFormat, "UIRT") && data.length >= 4) {
						if (timeTable == null) {
							timeTable = HashBasedTable.create();
						}
						// convert to million-seconds
						long mms = 0L;
						try {
							mms = Long.parseLong(data[3]); // cannot format
															// 9.7323480e+008
						} catch (NumberFormatException e) {
							mms = (long) Double.parseDouble(data[3]);
						}
						long timestamp = timeUnit.toMillis(mms);
						timeTable.put(row, col, timestamp);
					}
				}
				if (!isComplete) {
					bufferLine = bufferData[bufferData.length - 1];
				}
				buffer.clear();
			}
			fileRead.close();
			fileInputStream.close();
		}
		int numRows = numUsers(), numCols = numItems();
		// build rating matrix
		preferenceMatrix = new SparseMatrix(numRows, numCols, dataTable, colMap);
		if (timeTable != null)
			datetimeMatrix = new SparseMatrix(numRows, numCols, timeTable, colMap);
		// release memory of data table
		dataTable = null;
		timeTable = null;
	}

	/**
	 * progress
	 */
	@Override
	public void progress() {
		getJobStatus().setProgress(loadAllFileRate);
	}

	/**
	 * @return rate of loading files in data directory
	 */
	public double getFilePathRate() {
		return loadFilePathRate;
	}

	/**
	 * @return rate of alreadyLoaded/allData in one file
	 */
	public double getDataFileRate() {
		return loadDataFileRate;
	}

	public double getLoadAllFileRate() {
		return loadAllFileRate;
	}

	/**
	 * @return number of users
	 */
	public int numUsers() {
		return userIds.size();
	}

	/**
	 * @return number of items
	 */
	public int numItems() {
		return itemIds.size();
	}

	/**
	 * @param rawId
	 *            raw user id as String
	 * @return inner user id as int
	 */
	public int getUserId(String rawId) {
		return userIds.get(rawId);
	}

	/**
	 * @param rawId
	 *            raw item id as String
	 * @return inner item id as int
	 */
	public int getItemId(String rawId) {
		return itemIds.get(rawId);
	}

	/**
	 * @return user {rawid, inner id} mappings
	 */
	public BiMap<String, Integer> getUserIds() {
		return userIds;
	}

	/**
	 * @return item {rawid, inner id} mappings
	 */
	public BiMap<String, Integer> getItemIds() {
		return itemIds;
	}

	/**
	 * set the time unit of the data file
	 */
	public void setTimeUnit(TimeUnit timeUnit) {
		this.timeUnit = timeUnit;
	}

}
