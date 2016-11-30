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

import net.librec.data.model.ArffAttribute;
import net.librec.data.model.ArffInstance;
import net.librec.math.structure.DenseVector;
import net.librec.math.structure.SparseMatrix;
import net.librec.math.structure.SparseTensor;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

import com.google.common.collect.*;
import com.google.common.collect.BiMap;

/**
 * ArffData Convertor
 * 
 * @author Ma Chen
 */
public class ArffDataConvertor extends AbstractDataConvertor {

	public String dataPath;
	public String relationName;
	protected ArrayList<ArffInstance> instances;
	protected ArrayList<ArffAttribute> attributes;
	protected ArrayList<String> attrTypes;

	private ArrayList<BiMap<String, Integer>> columnIds;

	private int userCol;
	private int itemCol;
	private int ratingCol;

	public SparseMatrix oneHotFeatureMatrix;
	public DenseVector oneHotRatingVector;

	public ArffDataConvertor(String path) {
		dataPath = path;
		instances = new ArrayList<>();
		attributes = new ArrayList<>();
		columnIds = new ArrayList<>();
		attrTypes = new ArrayList<>();

		userCol = -1;
		itemCol = -1;
		ratingCol = -1;
	}

	public void readData() throws IOException {
		final List<File> files = new ArrayList<File>();
		SimpleFileVisitor<Path> finder = new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				files.add(file.toFile());
				return super.visitFile(file, attrs);
			}
		};
		Files.walkFileTree(Paths.get(dataPath), finder);
		for (int i = 0; i< files.size(); i++) {
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
					}
					else if (data[0].toUpperCase().equals("@ATTRIBUTE")) {
						continue;
					}
					else if (data[0].toUpperCase().equals("@DATA")) {
						dataFlag = true;
					}
				}
			}
		}

		// BufferedReader br = FileIO.getReader(dataPath);


		// initialize attributes
		for (int i = 0; i < attributes.size(); i++) {
			attributes.get(i).setColumnSet(columnIds.get(i).keySet());
		}
		// initialize instance attributes
		ArffInstance.attrs = attributes;

		// generate sparse tensor
		sparseTensor = generateFeatureTensor();
	}

	private void dataReader(Reader rd) throws IOException {
		ArrayList<String> dataLine = new ArrayList<>();
		StringBuilder subString = new StringBuilder();
		boolean isInQuote = false;
		boolean isInBracket = false;
//		// get all attribute types
//		for (ArffAttribute attr : attributes) {
//			attrTypes.add(attr.getType());
//		}
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

	public void processData() throws IOException {
		readData();
	}

	@Override
	public void progress() {
		// TODO Auto-generated method stub
		// getJobStatus().setProgress(loadAllFileRate);
	}

	public void oneHotEncoding() {
		Table<Integer, Integer, Double> dataTable = HashBasedTable.create();
		Multimap<Integer, Integer> colMap = HashMultimap.create();

		int numRows = instances.size();
		int numCols = 0;
		int numAttrs = attributes.size();

		double[] ratings = new double[numRows];

		// set numCols
		for (int i = 0; i < attributes.size(); i++) {
			// skip rating column
			if (i == ratingCol)
				continue;

			ArffAttribute attr = attributes.get(i);
			numCols += attr.getColumnSet().size() == 0 ? 1 : attr.getColumnSet().size();
		}

		// build one-hot encoding matrix
		for (int row = 0; row < numRows; row++) {
			ArffInstance instance = instances.get(row);
			int colPrefix = 0;
			int col = 0;
			for (int i = 0; i < numAttrs; i++) {
				String type = attrTypes.get(i);
				Object val = instance.getValueByIndex(i);

				// rating column
				if (i == ratingCol) {
					ratings[row] = (double) val;
					continue;
				}

				// feature column
				switch (type) {
				case "NUMERIC":
				case "REAL":
				case "INTEGER":
					col = colPrefix;
					dataTable.put(row, col, (double) val);
					colMap.put(col, row);
					colPrefix += 1;
					break;
				case "STRING":
					col = colPrefix + columnIds.get(i).get(val);
					dataTable.put(row, col, 1d);
					colMap.put(col, row);
					colPrefix += columnIds.get(i).size();
					break;
				case "NOMINAL":
					for (String v : (ArrayList<String>) val) {
						col = colPrefix + columnIds.get(i).get(v);
						colMap.put(col, row);
						dataTable.put(row, col, 1d);
					}
					colPrefix += columnIds.get(i).size();
					break;
				}
			}
		}
		oneHotFeatureMatrix = new SparseMatrix(numRows, numCols, dataTable, colMap);
		oneHotRatingVector = new DenseVector(ratings);

		// release memory
		dataTable = null;
		colMap = null;
	}

	private SparseTensor generateFeatureTensor() {

		int numRows = instances.size();
		int numAttrs = attributes.size();

		List<Double> ratings = new ArrayList<Double>();

		// n-dimensional keys
		List<Integer>[] nDKeys = (List<Integer>[]) new List<?>[numAttrs];

		for (int d = 0; d < numAttrs; d++) {
			nDKeys[d] = new ArrayList<Integer>();
		}

		// each set stores a series of values of an attribute
		ArrayList<HashSet<Integer>> setOfAttrs = new ArrayList<HashSet<Integer>>();
		for (int i = 0; i < numAttrs - 1; i++) {
			setOfAttrs.add(new HashSet<Integer>());
		}

		// set keys for each dimension
		for (int row = 0; row < numRows; row++) {
			ArffInstance instance = instances.get(row);
			for (int i = 0; i < numAttrs; i++) {
				if (i == userCol) {
					double userId = (double) instance.getValueByIndex(userCol);
					nDKeys[i].add((int) (userId));
					setOfAttrs.get(i).add((int) userId);
				} else if (i == itemCol) {
					double itemId = (double) instance.getValueByIndex(itemCol);
					nDKeys[i].add((int) (itemId));
					setOfAttrs.get(i).add((int) itemId);
				} else if (i == ratingCol) {
					double rating = (double) instance.getValueByIndex(ratingCol);
					ratings.add(rating);
				} else {
					String attrType = attrTypes.get(i);
					if (attrType.equals("STRING")) {
						String strAttr = (String) instance.getValueByIndex(i);
						int val = columnIds.get(i).get(strAttr);
						nDKeys[i].add(val);
						setOfAttrs.get(i).add(val);
					} else {
						double val = (double) instance.getValueByIndex(i);
						nDKeys[i].add((int) val);
						setOfAttrs.get(i).add((int) val);
					}
				}
			}
		}

		// set dimension of tensor
		int[] dims = new int[numAttrs - 1];
		for (int i = 0; i < numAttrs - 1; i++) {
			dims[i] = setOfAttrs.get(i).size();
		}

		return new SparseTensor(dims, nDKeys, ratings);
	}

	public String getRelationName() {
		return relationName;
	}

	public ArrayList<ArffInstance> getInstances() {
		return instances;
	}

	public ArrayList<ArffAttribute> getAttributes() {
		return attributes;
	}
}
