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

import com.google.common.collect.BiMap;
import net.librec.math.structure.DataFrame;
import net.librec.util.StringUtil;
import okio.BufferedSource;
import okio.Okio;
import okio.Source;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * A <tt>TextDataConvertor</tt> is a class to convert a data file from CSV
 * format to a target format.
 *
 * @author WangYuFeng and liuxz
 */
public class TextDataConvertor extends AbstractDataConvertor {

    /**
     * Log
     */
    private static final Log LOG = LogFactory.getLog(TextDataConvertor.class);

    /**
     * The size of the buffer
     */
    private static final int BSIZE = 1024 * 1024;

    /**
     * The default format of input data file
     */
    private static final String DATA_COLUMN_DEFAULT_FORMAT = "UIR";
    private String dataColumnFormat;
    private String[] header;
    private String[] attr;
    private String sep;
    private float fileRate;
    /**
     * the path of the input data file
     */
    private String[] inputDataPath;

    /**
     * the threshold to binarize a rating. If a rating is greater than the threshold, the value will be 1;
     * otherwise 0. To disable this appender, i.e., keep the original rating value, set the threshold a negative value
     */
    private double binThold = -1.0;

    /**
     * user/item {raw id, inner id} map
     */
    private BiMap<String, Integer> userIds, itemIds;

    /**
     * time unit may depend on data sets, e.g. in MovieLens, it is unix seconds
     */
    private TimeUnit timeUnit = TimeUnit.SECONDS;

    /**
     * Initializes a newly created {@code TextDataConvertor} object with the
     * path of the input data file.
     *
     * @param inputDataPath the path of the input data file
     */
    public TextDataConvertor(String inputDataPath) {
        this(DATA_COLUMN_DEFAULT_FORMAT, inputDataPath, ",");
    }

    public TextDataConvertor(String dataColumnFormat, String inputDataPath) {
        this(dataColumnFormat, inputDataPath, ",");
    }

    public TextDataConvertor(String[] header, String[] attr, String inputDataPath, String sep) {
        this.header = header;
        this.attr = attr;
        this.inputDataPath = inputDataPath.split(",");
        this.sep = sep;
    }

    public TextDataConvertor(String dataColumnFormat, String inputDataPath, String sep) {
        this.dataColumnFormat = dataColumnFormat;
        this.inputDataPath = inputDataPath.split(",");
        this.sep = sep;
    }

    public TextDataConvertor(String[] inputDataPath) {
        this(DATA_COLUMN_DEFAULT_FORMAT, inputDataPath, ",");
    }

    public TextDataConvertor(String[] inputDataPath, String sep) {
        this(DATA_COLUMN_DEFAULT_FORMAT, inputDataPath, sep);
    }

    public TextDataConvertor(String dataColumnFormat, String[] inputDataPath, String sep) {
        this.dataColumnFormat = dataColumnFormat;
        this.inputDataPath = inputDataPath;
        this.sep = sep;
    }


    /**
     * Process the input data.
     *
     * @throws IOException if the <code>inputDataPath</code> is not valid.
     */
    @Override
    public void processData() throws IOException {
        readData(inputDataPath);
    }

    private void readData(String... inputDataPath) throws IOException {
        LOG.info(String.format("Dataset: %s", Arrays.toString(inputDataPath)));
        matrix = new DataFrame();
        if (Objects.isNull(header)) {
            if (dataColumnFormat.toLowerCase().equals("uirt")) {
                header = new String[]{"user", "item", "rating", "datetime"};
                attr = new String[]{"STRING", "STRING", "NUMERIC", "DATE"};
            } else {
                header = new String[]{"user", "item", "rating"};
                attr = new String[]{"STRING", "STRING", "NUMERIC"};
            }
        }

        matrix.setAttrType(attr);
        matrix.setHeader(header);
        List<File> files = new ArrayList<>();
        SimpleFileVisitor<Path> finder = new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                files.add(file.toFile());
                return super.visitFile(file, attrs);
            }
        };
        for (String path : inputDataPath) {
            Files.walkFileTree(Paths.get(path.trim()), finder);
        }
        int numFiles = files.size();
        int cur = 0;
        Pattern pattern = Pattern.compile(sep);
        for (File file : files) {
            try (Source fileSource = Okio.source(file);
                 BufferedSource bufferedSource = Okio.buffer(fileSource)) {
                String temp;
                while ((temp = bufferedSource.readUtf8Line()) != null) {
                    if ("".equals(temp.trim())) {
                        break;
                    }
                    String[] eachRow = pattern.split(temp);
                    for (int i = 0; i < header.length; i++) {
                        if (Objects.equals(attr[i], "STRING")) {
                            DataFrame.setId(eachRow[i], matrix.getHeader(i));
                        }
                    }
                    matrix.add(eachRow);
                }
                LOG.info(String.format("DataSet: %s is finished", StringUtil.last(file.toString(), 38)));
                cur++;
                fileRate = cur / numFiles;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        List<Double> ratingScale = matrix.getRatingScale();
        if (ratingScale != null) {
            LOG.info(String.format("rating Scale: %s", ratingScale.toString()));
        }
        LOG.info(String.format("user number: %d,\t item number is: %d", matrix.numUsers(), matrix.numItems()));
    }

    @Override
    public void progress() {
        getJobStatus().setProgress(fileRate);
    }
}
