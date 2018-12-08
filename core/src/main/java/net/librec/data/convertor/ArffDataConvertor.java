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
import net.librec.data.model.ArffAttribute;
import net.librec.data.model.ArffInstance;
import net.librec.math.structure.DataFrame;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A <tt>ArffDataConvertor</tt> is a class to convert
 * a data file from ARFF format to a target format.
 *
 * @author Tang Jiaxi, Ma Chen, and Liuxz
 */
public class ArffDataConvertor extends AbstractDataConvertor {

    /**
     * Log
     */
    private static final Log LOG = LogFactory.getLog(ArffDataConvertor.class);
    /**
     * The path of the input file
     */
    private String[] inputDataPath;

    /** The relation name of input data */
    private String relationName;

    /** The instances of the input data */
    private ArrayList<ArffInstance> instances;

    /** The attributes the input data */
    private ArrayList<ArffAttribute> attributes;

    /** The attribute types of the input data */
    private ArrayList<String> attrTypes;

    /** The column ids of the input data */
    private ArrayList<BiMap<String, Integer>> columnIds;

    /** The user column index */
    private int userCol;

    /** The item column index */
    private int itemCol;

    /** The rating column index */
    private int ratingCol;

    // user, item, appender {raw id, inner id} mapping
    private ArrayList<BiMap<String, Integer>> featuresInnerMapping;

    /**
     * Initializes a newly created {@code ArffDataConvertor} object
     * with the path of the input data file.
     *
     * @param path  path of the input data file.
     */
    public ArffDataConvertor(String... path) {
        inputDataPath = path;
        instances = new ArrayList<>();
        attributes = new ArrayList<>();
        columnIds = new ArrayList<>();
        attrTypes = new ArrayList<>();

        userCol = -1;
        itemCol = -1;
        ratingCol = -1;
    }

    public ArffDataConvertor(String path, ArrayList<BiMap<String, Integer>> featureMapping) {
        this(path);
        this.featuresInnerMapping = featureMapping;
    }

    public void readData() throws IOException {
        this.readData(inputDataPath);
    }

    /**
     * Read data from the data file.
     *
     * @throws IOException
     *         if the path is not valid
     */
    public void readData(String... inputDataPath) throws IOException {
        LOG.info(String.format("Dataset: %s", Arrays.toString(inputDataPath)));
        matrix = new DataFrame();
        List<String> attrTypeList = new ArrayList<>();
        final List<File> files = new ArrayList<File>();
        SimpleFileVisitor<Path> finder = new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                files.add(file.toFile());
                return super.visitFile(file, attrs);
            }
        };
        for (String path: inputDataPath){
            Files.walkFileTree(Paths.get(path.trim()), finder);
        }
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
                        matrix.setAttrType(attrTypes);
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
                        matrix.setName(data[1]);
                    }

                    // parse ATTRIBUTE
                    else if (data[0].toUpperCase().equals("@ATTRIBUTE")) {
                        attrName = data[1];
                        attrType = data[2];
                        boolean isNominal = false;
                        if (attrName.equals("user"))
                            userCol = attrIdx;
                        if (attrName.equals("item"))
                            itemCol = attrIdx;
                        if (attrName.equals("rating"))
                            ratingCol = attrIdx;
                        // parse NOMINAL type
                        if (attrType.startsWith("{") && attrType.endsWith("}")) {
                            isNominal = true;
                        }
                        BiMap<String, Integer> colId = DataFrame.getInnerMapping(attrName);

                        // if nominal type, set columnIds
                        if (isNominal) {
                            String nominalAttrs = attrType.substring(1, attrType.length() - 1);
                            int val = 0;
                            for (String attr : nominalAttrs.split(",")) {
//                                System.out.println(attr.trim()+colId.size());
                                if (!colId.keySet().contains(attr.trim()))
                                    colId.put(attr.trim(), colId.size());
                            }
                            attrType = "NOMINAL";
                        }
                        matrix.addHeader(attrName);
                        columnIds.add(colId);
                        attributes.add(new ArffAttribute(attrName, attrType.toUpperCase(), attrIdx++));
                        attrTypeList.add(attrType.toUpperCase());
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
            attributes.get(i).setColumnSet(DataFrame.getInnerMapping(attributes.get(i).getName()).keySet());
        }
        // initialize instance attributes
        ArffInstance.attrs = attributes;
//        matrix.setAttrType(attrTypeList);
    }

    /**
     * Parse @DATA part of the file.
     *
     * @param rd  the reader of the input file.
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
                    if (!dataLine.get(0).startsWith("%")) { // check if
                        // annotation line
                        dataLine.add(subString.toString());
                        // raise error if inconsistent with attribute define
                        if (dataLine.size() != attrTypes.size()) {
                            throw new IOException("Read data error, inconsistent attribute number!");
                        }

                        // pul column value into columnIds, for one-hot encoding
                        for (int i = 0; i < dataLine.size(); i++) {
                            String col = dataLine.get(i).trim();
                            String type = attrTypes.get(i);
                            switch (type) {
                                case "NUMERIC":
                                case "REAL":
                                case "INTEGER":
                                    break;
                                case "STRING":
                                    DataFrame.setId(col, attributes.get(i).getName());
                                    break;
                                case "NOMINAL":
                                    BiMap<String, Integer> colId = DataFrame.getInnerMapping(attributes.get(i).getName());
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
                                default:
                                    break;
                            }
                            dataLine.set(i, col);
                        }
                        instances.add(new ArffInstance(dataLine));
                        matrix.add(dataLine.toArray(new String[dataLine.size()]));
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

    /**
     * Process the input data.
     *
     * @throws IOException
     *         if the path is not valid
     */
    public void processData() throws IOException {
        readData();
    }

    @Override
    public void progress() {
        // TODO Auto-generated method stub
        // getJobStatus().setProgress(loadAllFileRate);
    }

//    @Override
//    public SparseTensor getSparseTensor() {
//        if (null == sparseTensor){
//            sparseTensor = getMatrix().toSparseTensor();
//        }
//        return sparseTensor;
//    }

    /**
     * Return the relation name of input data.
     *
     * @return {@link #relationName}
     */
    public String getRelationName() {
        return relationName;
    }

    /**
     * Return the instances of the input data.
     *
     * @return {@link #instances}
     */
    public ArrayList<ArffInstance> getInstances() {
        return instances;
    }

    /**
     * Return the attributes the input data.
     *
     * @return {@link #attributes}
     */
    public ArrayList<ArffAttribute> getAttributes() {
        return attributes;
    }

    /**
     * Return user {rawid, inner id} mappings
     *
     * @return the mapping between row id and inner id of users
     */
    public BiMap<String, Integer> getUserIds() {
        return DataFrame.getInnerMapping("user");
        //        return featuresInnerMapping.get(userCol);
    }

    /**
     * Return item {rawid, inner id} mappings
     *
     * @return the mapping between row id and inner id of items
     */
    public BiMap<String, Integer> getItemIds() {
        return DataFrame.getInnerMapping("item");
//        return featuresInnerMapping.get(itemCol);
    }

    /**
     * Return user, item, appender {raw id, inner id} mapping
     *
     * @return the mapping between row id and inner id of each columns in data set
     */
    public ArrayList<BiMap<String, Integer>> getAllFeatureIds() {
        return featuresInnerMapping;
    }
}