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
package net.librec.data.convertor.appender;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import net.librec.conf.Configuration;
import net.librec.conf.Configured;
import net.librec.data.DataAppender;
import net.librec.data.model.ArffAttribute;
import net.librec.data.model.ArffInstance;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * A <tt>AuxiliaryDataAppender</tt> is a class to process and store user/item auxiliary
 * data.
 *
 * @author SunYatong
 */
public class AuxiliaryDataAppender extends Configured implements DataAppender {

    /**
     * The path of the appender data file
     */
    private String inputDataPath;

    /**
     * The column ids of the input data
     */
    private ArrayList<BiMap<String, Integer>> columnIds;

    /**
     * The attributes of the input data
     */
    private ArrayList<ArffAttribute> attributes;

    /**
     * The attribute types of the input data
     */
    private ArrayList<String> attrTypes;

    /**
     * The relation name of input data
     */
    private String relationName;

    /**
     * The instances of the input data
     */
    private ArrayList<ArffInstance> instances;

    /**
     * Initializes a newly created {@code AuxiliaryDataAppender} object with null.
     */
    public AuxiliaryDataAppender() {
        this(null);
    }

    /**
     * Initializes a newly created {@code AuxiliaryDataAppender} object with a
     * {@code Configuration} object
     *
     * @param conf {@code Configuration} object for construction
     */
    public AuxiliaryDataAppender(Configuration conf) {
        this.conf = conf;
        instances = new ArrayList<>();
        attributes = new ArrayList<>();
        columnIds = new ArrayList<>();
        attrTypes = new ArrayList<>();
    }

    @Override
    public void processData() throws IOException {
        if (conf != null && StringUtils.isNotBlank(conf.get("data.appender.path"))) {
            inputDataPath = conf.get("dfs.data.dir") + "/" + conf.get("data.appender.path");
            readData(inputDataPath);
        }
    }

    /**
     * Read data from the data file.
     *
     * @param inputDataPath the path of the data file
     * @throws IOException if I/O error occurs during reading
     */
    private void readData(String inputDataPath) throws IOException {
        final List<File> files = new ArrayList<File>();
        SimpleFileVisitor<Path> finder = new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                files.add(file.toFile());
                return super.visitFile(file, attrs);
            }
        };
        Files.walkFileTree(Paths.get(inputDataPath), finder);
        for (int i = 0; i < files.size(); i++) {
            if (0 == i) { //read the first file
                BufferedReader br = new BufferedReader(new FileReader(files.get(i)));
                boolean dataFlag = false;

                int attrIdx = 0;

                String attrName = null;
                String attrType = null;
                String line = null;

                while (true) {

                    // parse DATA if valid
                    if (dataFlag) {
                        // get all attribute types
                        for (ArffAttribute attr : attributes) {
                            attrTypes.add(attr.getType());
                        }
                        // let data reader control the bufferedReader
                        dataReader(br);
                    }

                    line = br.readLine();

                    if (line == null) // finish reading
                        break;
                    if (line.isEmpty() || line.startsWith("%")) // skip empty or
                        // annotation
                        continue;

                    String[] data = line.trim().split("[ \t]");

                    // parse RELATION
                    if (data[0].toUpperCase().equals("@RELATION")) {
                        relationName = data[1];
                    }

                    // parse ATTRIBUTE
                    else if (data[0].toUpperCase().equals("@ATTRIBUTE")) {
                        attrName = data[1];
                        attrType = data[2];
                        boolean isNominal = false;

                        // parse NOMINAL type
                        if (attrType.startsWith("{") && attrType.endsWith("}")) {
                            isNominal = true;
                        }
                        BiMap<String, Integer> colId = HashBiMap.create();
                        // if nominal type, set columnIds
                        if (isNominal) {
                            String nominalAttrs = attrType.substring(1, attrType.length() - 1);
                            int val = 0;
                            for (String attr : nominalAttrs.split(",")) {
                                colId.put(attr.trim(), val++);
                            }
                            attrType = "NOMINAL";
                        }
                        columnIds.add(colId);
                        attributes.add(new ArffAttribute(attrName, attrType.toUpperCase(), attrIdx++));
                    }
                    // set DATA flag (finish reading ATTRIBUTES)
                    else if (data[0].toUpperCase().equals("@DATA")) {
                        dataFlag = true;
                    }
                }
                br.close();
            } else { //read other files
                BufferedReader br = new BufferedReader(new FileReader(files.get(i)));
                boolean dataFlag = false;
                String line = null;
                //only parse Data
                while (true) {
                    if (dataFlag) {
                        dataReader(br);
                    }
                    line = br.readLine();
                    if (line == null) {
                        break;
                    }
                    if (line.isEmpty() || line.startsWith("%")) {
                        continue;
                    }
                    String[] data = line.trim().split("[ \t]");
                    if (data[0].toUpperCase().equals("@RELATION")) {
                        continue;
                    } else if (data[0].toUpperCase().equals("@ATTRIBUTE")) {
                        continue;
                    } else if (data[0].toUpperCase().equals("@DATA")) {
                        dataFlag = true;
                    }
                }
                br.close();
            }
        }
        // initialize attributes
        for (int i = 0; i < attributes.size(); i++) {
            attributes.get(i).setColumnSet(columnIds.get(i).keySet());
        }
        // initialize instance attributes
        ArffInstance.attrs = attributes;
    }

    /**
     * Parse @DATA part of the file.
     *
     * @param rd the reader of the input file.
     * @throws IOException
     */
    private void dataReader(Reader rd) throws IOException {
        ArrayList<String> dataLine = new ArrayList<>();
        StringBuilder subString = new StringBuilder();
        boolean isInQuote = false;
        boolean isInBracket = false;

        int c = 0;
        while ((c = rd.read()) != -1) {
            char ch = (char) c;
            // read line by line
            if (ch == '\n') {
                if (dataLine.size() != 0) { // check if empty line
                    if (!dataLine.get(0).startsWith("%")) { // check if annotation line
                        dataLine.add(subString.toString());
                        // raise error if inconsistent with attribute define
                        if (dataLine.size() != attrTypes.size()) {
                            throw new IOException("Read data error, inconsistent attribute number!");
                        }

                        // pul column value into columnIds, for one-hot encoding
                        for (int i = 0; i < dataLine.size(); i++) {
                            String col = dataLine.get(i).trim();
                            String type = attrTypes.get(i);
                            BiMap<String, Integer> colId = columnIds.get(i);
                            switch (type) {
                                case "NUMERIC":
                                case "REAL":
                                case "INTEGER":
                                    break;
                                case "STRING":
                                    int val = colId.containsKey(col) ? colId.get(col) : colId.size();
                                    colId.put(col, val);
                                    break;
                                case "NOMINAL":
                                    StringBuilder sb = new StringBuilder();
                                    String[] ss = col.split(",");
                                    for (int ns = 0; ns < ss.length; ns++) {
                                        String _s = ss[ns].trim();
                                        if (!colId.containsKey(_s)) {
                                            throw new IOException("Read data error, inconsistent nominal value!");
                                        }
                                        sb.append(_s);
                                        if (ns != ss.length - 1)
                                            sb.append(",");
                                    }
                                    col = sb.toString();
                                    break;
                            }
                            dataLine.set(i, col);
                        }

                        instances.add(new ArffInstance(dataLine));

                        subString = new StringBuilder();
                        dataLine = new ArrayList<>();
                    }
                }
            } else if (ch == '[' || ch == ']') {
                isInBracket = !isInBracket;
            } else if (ch == '\r') {
                // skip '\r'
            } else if (ch == '\"') {
                isInQuote = !isInQuote;
            } else if (ch == ',' && (!isInQuote && !isInBracket)) {
                dataLine.add(subString.toString());
                subString = new StringBuilder();
            } else {
                subString.append(ch);
            }
        }
    }

    @Override
    public void setUserMappingData(BiMap<String, Integer> userMappingData) {}

    @Override
    public void setItemMappingData(BiMap<String, Integer> itemMappingData) {}

    public ArrayList<ArffInstance> getAuxiliaryData() {
        return instances;
    }
}
