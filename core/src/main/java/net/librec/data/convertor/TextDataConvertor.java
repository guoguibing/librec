/**
 * Copyright (C) 2016 LibRec
 * <p>
 * This file is part of LibRec.
 * LibRec is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * LibRec is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with LibRec. If not, see <http://www.gnu.org/licenses/>.
 */
package net.librec.data.convertor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;

import net.librec.math.structure.SparseMatrix;
import net.librec.util.StringUtil;

/**
 * A <tt>TextDataConvertor</tt> is a class to convert a data file from CSV
 * format to a target format.
 *
 * @author WangYuFeng and liuxz
 */
public class TextDataConvertor extends AbstractDataConvertor {

    /** Log */
    private static final Log LOG = LogFactory.getLog(TextDataConvertor.class);

    /** The default format of input data file */
    private static final String DATA_COLUMN_DEFAULT_FORMAT = "UIR";

    /** The format of input data file */
    private String dataColumnFormat;

    /** the path of the input data file */
    private String inputDataPath;

    /** the threshold to binarize a rating. If a rating is greater than the threshold, the value will be 1;
     * otherwise 0. To disable this appender, i.e., keep the original rating value, set the threshold a negative value
     */
    private double binThold = -1.0;

    /** user/item {raw id, inner id} map */
    private BiMap<String, Integer> userIds, itemIds;

    /**
     * time unit may depend on data sets, e.g. in MovieLens, it is unix seconds
     */
    private TimeUnit timeUnit = TimeUnit.SECONDS;

    /** already loaded files/total files in dataDirectory */
    private float loadFilePathRate;

    /** loaded data size /total data size in one data file */
    private float loadDataFileRate;

    /** loaded data size /total data size in all data file */
    private float loadAllFileRate;

    /**
     * Initializes a newly created {@code TextDataConvertor} object with the
     * path of the input data file.
     *
     * @param inputDataPath
     *            the path of the input data file
     */
    public TextDataConvertor(String inputDataPath) {
        this(DATA_COLUMN_DEFAULT_FORMAT, inputDataPath, -1.0);
    }

    /**
     * Initializes a newly created {@code TextDataConvertor} object with the
     * path and format of the input data file.
     *
     * @param dataColumnFormat
     *            the path of the input data file
     * @param inputDataPath
     *            the format of the input data file
     */
    public TextDataConvertor(String dataColumnFormat, String inputDataPath) {
        this(dataColumnFormat, inputDataPath, -1.0);
    }

    /**
     * Initializes a newly created {@code TextDataConvertor} object with the
     * path and format of the input data file.
     *
     * @param dataColumnFormat
     *            the format of the input data file
     * @param inputDataPath
     *            the path of the input data file
     * @param binThold the threshold to binarize a rating. If a rating is greater than the threshold, the value will be 1;
     *            otherwise 0. To disable this appender, i.e., keep the original rating value, set the threshold a
     *            negative value
     */
    public TextDataConvertor(String dataColumnFormat, String inputDataPath, double binThold) {
        this.dataColumnFormat = dataColumnFormat;
        this.inputDataPath = inputDataPath;
        this.binThold = binThold;
    }

    /**
     * Initializes a newly created {@code TextDataConvertor} object with the
     * path and format of the input data file.
     *
     * @param dataColumnFormat
     *            the path of the input data file
     * @param inputDataPath
     *            the format of the input data file
     * @param binThold
     *            the threshold to binarize a rating. If a rating is greater than the threshold, the value will be 1;
     *            otherwise 0. To disable this appender, i.e., keep the original rating value, set the threshold a
     *            negative value
     * @param userIds
     *            userId to userIndex map
     * @param itemIds
     *            itemId to itemIndex map
     */
    public TextDataConvertor(String dataColumnFormat, String inputDataPath, double binThold,
                             BiMap<String, Integer> userIds, BiMap<String, Integer> itemIds) {
        this(dataColumnFormat, inputDataPath,binThold);
        this.userIds=userIds;
        this.itemIds=itemIds;
    }

    /**
     * Process the input data.
     *
     * @throws IOException
     *         if the <code>inputDataPath</code> is not valid.
     */
    @Override
	public void processData() throws IOException {
        readData(dataColumnFormat, inputDataPath, binThold);
    }

    /**
     * Read data from the data file. Note that we didn't take care of the
     * duplicated lines.
     *
     * @param dataColumnFormat
     *            the format of input data file
     * @param inputDataPath
     *            the path of input data file
     * @param binThold
     *            the threshold to binarize a rating. If a rating is greater
     *            than the threshold, the value will be 1; otherwise 0. To
     *            disable this appender, i.e., keep the original rating value,
     *            set the threshold a negative value
     * @throws IOException
     *            if the <code>inputDataPath</code> is not valid.
     */
    private void readData(String dataColumnFormat, String inputDataPath, double binThold) throws IOException {
        LOG.info(String.format("Dataset: %s", StringUtil.last(inputDataPath, 38)));
        // Table {row-id, col-id, rate}
        Table<Integer, Integer, Double> dataTable = HashBasedTable.create();
        // Table {row-id, col-id, timestamp}
        Table<Integer, Integer, Long> timeTable = null;
        // Map {col-id, multiple row-id}: used to fast build a rating matrix
        Multimap<Integer, Integer> colMap = HashMultimap.create();

        if (this.userIds == null){
            this.userIds = HashBiMap.create();
        }
        if (this.itemIds == null){
            this.itemIds = HashBiMap.create();
        }
        final List<File> files = new ArrayList<>();
        final ArrayList<Long> fileSizeList = new ArrayList<>();
        SimpleFileVisitor<Path> finder = new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                fileSizeList.add(file.toFile().length());
                files.add(file.toFile());
                return super.visitFile(file, attrs);
            }
        };
        for(String path : inputDataPath.trim().split(" ")){
            Files.walkFileTree(Paths.get(path), finder);
        }

        LOG.info("All dataset files " + files.toString());
        long allFileSize = 0;
        for (Long everyFileSize : fileSizeList) {
            allFileSize = allFileSize + everyFileSize;
        }
        LOG.info("All dataset files size " + Long.toString(allFileSize));
        int readingFileCount = 0;
        // loop every dataFile collecting from walkFileTree

        for (File dataFile : files) {
            LOG.info("Now loading dataset file " + dataFile.toString().substring(dataFile.toString().lastIndexOf(File.separator) + 1, dataFile.toString().lastIndexOf(".")));

            readingFileCount += 1;
            loadFilePathRate = readingFileCount / (float) files.size();
            //long readingOneFileByte = 0;
            
            Pattern pattern = Pattern.compile("[ \t,]+");

			boolean parseDate = "UIRT".equals(dataColumnFormat);
			boolean parseRate = "UIR".equals(dataColumnFormat) || parseDate;
            try(BufferedReader reader = new BufferedReader(new FileReader(dataFile))){
			
                String line;
        		int rows = 0;
	    		while ((line = reader.readLine()) != null) {
	    			try {
		                String[] data = pattern.split(line);
		                String user = data[0];
		                String item = data[1];
						double rate = (parseRate && data.length >= 3) ? Double.parseDouble(data[2]) : 1.0;
		
		                // binarize the rating for item recommendation task
		                if (binThold >= 0) {
		                    rate = rate > binThold ? 1.0 : 0.0;
		                }
		
		                // inner id starting from 0
		                Integer row = userIds.get(user);
		                if (row == null){
		                	row = userIds.size();
			                userIds.put(user, row);
		                }
		
		                Integer col = itemIds.get(item);
		                if (col == null){
		                	col = itemIds.size();
			                itemIds.put(item, col);
		                }
		
		                dataTable.put(row, col, rate);
		                colMap.put(col, row);
		                // record rating's issuing time
		                if (parseDate && data.length >= 4) {
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
	    			} catch (Exception e) {
						LOG.error("Error while parsing row=" + rows + ", values=" + line + ", file=" + dataFile, e);
						throw new IOException("Error while parsing row=" + rows + "  values='" + line + "'  file=" + dataFile, e);
					}
	    			rows++;

	            }
            }
		}
        int numRows = numUsers();
        int numCols = numItems();
        // build rating matrix
        preferenceMatrix = new SparseMatrix(numRows, numCols, dataTable, colMap);
        if (timeTable != null)
            datetimeMatrix = new SparseMatrix(numRows, numCols, timeTable, colMap);
        // release memory of data table
        dataTable = null;
        timeTable = null;
    }

    /**
     * Set the progress for job status.
     */
    @Override
    public void progress() {
        getJobStatus().setProgress(loadAllFileRate);
    }

    /**
     * Return rate of loading files in data directory.
     *
     * @return {@link #loadFilePathRate}
     */
    public double getFilePathRate() {
        return loadFilePathRate;
    }

    /**
     * Return rate of alreadyLoaded/allData in one file.
     *
     * @return {@link #loadDataFileRate}
     */
    public double getDataFileRate() {
        return loadDataFileRate;
    }

    /**
     * Return rate of alreadyLoaded/allData in all files.
     *
     * @return {@link #loadAllFileRate}
     */
    public double getLoadAllFileRate() {
        return loadAllFileRate;
    }

    /**
     * Return the number of users.
     *
     * @return number of users
     */
    public int numUsers() {
        return userIds.size();
    }

    /**
     * Return the number of items.
     *
     * @return number of items
     */
    public int numItems() {
        return itemIds.size();
    }

    /**
     * Return a user's inner id by his raw id.
     *
     * @param rawId
     *            raw user id as String
     *
     * @return inner user id as int
     */
    public int getUserId(String rawId) {
        return userIds.get(rawId);
    }

    /**
     * Return an item's inner id by its raw id.
     *
     * @param rawId
     *            raw item id as String
     *
     * @return inner item id as int
     */
    public int getItemId(String rawId) {
        return itemIds.get(rawId);
    }

    /**
     * Return user {rawid, inner id} mappings
     *
     * @return {@link #userIds}
     */
    public BiMap<String, Integer> getUserIds() {
        return userIds;
    }

    /**
     * Return item {rawid, inner id} mappings
     *
     * @return {@link #itemIds}
     */
    public BiMap<String, Integer> getItemIds() {
        return itemIds;
    }

    /**
     * Set the time unit of the data file.
     *
     * @param timeUnit  the time unit to be set for the data file
     */
    public void setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
    }

}
