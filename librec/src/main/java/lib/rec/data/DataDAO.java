package lib.rec.data;

import happy.coding.io.FileIO;
import happy.coding.io.Logs;
import happy.coding.io.Strings;
import happy.coding.math.Stats;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Table;

/**
 * An data access object (DAO) to a data file
 * 
 * @author guoguibing
 * 
 */
public class DataDAO {

	// path to data file
	private String dataPath;
	// store data as {user/item rate} matrix
	private SparseMatrix rateMatrix;

	// is item type as user
	private boolean isItemAsUser;

	// data scales
	private List<Double> scales;
	// scale distribution
	private Multiset<Double> scaleDist;

	// number of rates
	private int numRates;

	// user/item {raw id, inner id} map
	private BiMap<String, Integer> userIds, itemIds;

	// inverse views of userIds, itemIds
	private BiMap<Integer, String> idUsers, idItems;

	/**
	 * Constructor for a data DAO object
	 * 
	 * @param path
	 *            path to data file
	 * 
	 * @param userIds
	 *            user: {raw id, inner id} map
	 * @param itemIds
	 *            item: {raw id, inner id} map
	 */
	public DataDAO(String path, BiMap<String, Integer> userIds, BiMap<String, Integer> itemIds) {
		dataPath = path;

		if (userIds == null)
			this.userIds = HashBiMap.create();
		else
			this.userIds = userIds;

		if (itemIds == null)
			this.itemIds = HashBiMap.create();
		else
			this.itemIds = itemIds;

		scaleDist = HashMultiset.create();

		isItemAsUser = this.userIds == this.itemIds;
	}

	/**
	 * Contructor for data DAO object
	 * 
	 * @param path
	 *            path to data file
	 */
	public DataDAO(String path) {
		this(path, null, null);
	}

	/**
	 * Contructor for data DAO object
	 * 
	 */
	public DataDAO(String path, BiMap<String, Integer> userIds) {
		this(path, userIds, userIds);
	}

	/**
	 * Convert a data file separated by {@code sep} to a data file separated by
	 * {@code toSep}
	 * 
	 * @param sep
	 *            separator of the source file
	 * @param toSep
	 *            separtor of the target file
	 */
	public void convert(String sep, String toSep) throws Exception {
		BufferedReader br = FileIO.getReader(dataPath);

		String line = null;
		List<String> lines = new ArrayList<>();
		while ((line = br.readLine()) != null) {
			String newline = line.replaceAll(sep, toSep);

			lines.add(newline);

			if (lines.size() >= 1000) {
				FileIO.writeList(dataPath + "-converted", lines, null, true);
				lines.clear();
			}
		}

		if (lines.size() > 0)
			FileIO.writeList(dataPath + "-converted", lines, null, true);

		br.close();
	}

	/**
	 * default relevant columns {0: user column, 1: item column, 2: rate
	 * column}; otherwise try {@code readData(int[] rels)}
	 * 
	 * 
	 * @return a sparse matrix storing all the relevant data
	 */
	public SparseMatrix readData() throws Exception {
		return readData(new int[] { 0, 1, 2 });
	}

	/**
	 * read data from the data file
	 * 
	 * @param cols
	 *            the indexes of the relevant columns in the data file
	 * @return a sparse matrix storing all the relevant data
	 */
	public SparseMatrix readData(int[] cols) throws Exception {

		// Table {row-id, col-id, rate}
		Table<Integer, Integer, Double> dataTable = HashBasedTable.create();
		// Map {col-id, multiple row-id}: used to fast build rate matrix
		Multimap<Integer, Integer> colMap = HashMultimap.create();

		BufferedReader br = FileIO.getReader(dataPath);
		String line = null;
		while ((line = br.readLine()) != null) {
			String[] data = line.split("[ \t,]");

			String user = data[cols[0]];
			String item = data[cols[1]];
			Double rate = cols.length >= 3 ? Double.valueOf(data[cols[2]]) : 1.0;

			/*
			 * if (cols.length >= 4) { double weight =
			 * Double.parseDouble(data[cols[3]]); if (rate == 0 && weight < 1)
			 * continue; }
			 */

			scaleDist.add(rate);

			// inner id starting from 0
			int row = userIds.containsKey(user) ? userIds.get(user) : userIds.size();
			userIds.put(user, row);

			int col = itemIds.containsKey(item) ? itemIds.get(item) : itemIds.size();
			itemIds.put(item, col);

			dataTable.put(row, col, rate);
			colMap.put(col, row);
		}
		br.close();

		numRates = scaleDist.size();
		scales = new ArrayList<>(scaleDist.elementSet());
		Collections.sort(scales);

		int numRows = numUsers(), numCols = numItems();

		// if min-rate = 0.0, shift upper a scale
		double minRate = scales.get(0).doubleValue();
		double epsilon = minRate == 0.0 ? scales.get(1).doubleValue() - minRate : 0;
		if (epsilon > 0) {
			// shift upper a scale
			for (int i = 0, im = scales.size(); i < im; i++) {
				double val = scales.get(i);
				scales.set(i, val + epsilon);
			}
			// udpate data table
			for (int row = 0; row < numRows; row++) {
				for (int col = 0; col < numCols; col++) {
					if (dataTable.contains(row, col))
						dataTable.put(row, col, dataTable.get(row, col) + epsilon);
				}
			}
		}

		if (isItemAsUser) {
			Logs.debug("User amount: {}, scales: {{}}", numRows, Strings.toString(scales, ", "));
		} else {
			Logs.debug("User amount: {}, item amount: {}", numRows, numCols);
			Logs.debug("Rating amount: {}, scales: {{}}", numRates, Strings.toString(scales, ", "));
		}

		// build rating matrix
		rateMatrix = new SparseMatrix(numRows, numCols, dataTable, colMap);

		// release memory of data table
		dataTable = null;

		return rateMatrix;
	}

	/**
	 * write the rate data to another data file given by the path {@code toPath}
	 * 
	 * @param toPath
	 *            the data file to write to
	 * @param sep
	 *            the sparator of the written data file
	 */
	public void writeData(String toPath, String sep) throws Exception {
		FileIO.deleteFile(toPath);

		List<String> lines = new ArrayList<>(1500);
		for (MatrixEntry me : rateMatrix) {
			String line = Strings.toString(new Object[] { me.row() + 1, me.column() + 1, (float) me.get() }, sep);
			lines.add(line);

			if (lines.size() >= 1000) {
				FileIO.writeList(toPath, lines, null, true);
				lines.clear();
			}
		}

		if (lines.size() > 0)
			FileIO.writeList(toPath, lines, null, true);

		Logs.debug("Data has been exported to {}", toPath);
	}

	/**
	 * default sep=" " is adopted
	 */
	public void writeData(String toPath) throws Exception {
		writeData(toPath, " ");
	}

	/**
	 * write rate matrix to a data file with format ".arff" which can be used by
	 * the PREA toolkit
	 * 
	 * @param relation
	 *            relation name of dataset
	 * @param toPath
	 *            data file path
	 */
	public void writeArff(String relation, String toPath) throws Exception {
		FileIO.deleteFile(toPath);

		BufferedWriter bw = FileIO.getWriter(toPath);

		bw.write("@RELATION " + relation + "\n\n");
		bw.write("@ATTRIBUTE UserId NUMERIC\n\n");
		bw.write("@DATA\n");

		StringBuilder sb = new StringBuilder();
		int count = 0;
		for (int u = 0, um = numUsers(); u < um; u++) {
			sb.append("{0 " + (u + 1));

			for (int j = 0, jm = numItems(); j < jm; j++) {
				double rate = rateMatrix.get(u, j);
				if (rate != 0)
					sb.append(", " + (j + 1) + " " + rate);

				if (j == jm - 1)
					sb.append("}\n");
			}

			if (count++ >= 500) {
				bw.write(sb.toString());
				count = 0;
				sb = new StringBuilder();
			}
		}

		if (count > 0)
			bw.write(sb.toString());

		bw.close();

		Logs.debug("Data has been exported to {}", toPath);
	}

	/**
	 * print out specifications of the dataset
	 */
	public void printSpecs() throws Exception {
		if (rateMatrix == null)
			readData();

		List<String> sps = new ArrayList<>();

		int users = numUsers();
		int items = numItems();

		sps.add(String.format("Dataset: %s", Strings.last(dataPath, 38)));
		sps.add("User amount: " + users + ", " + FileIO.formatSize(users));
		if (!isItemAsUser)
			sps.add("Item amount: " + items + ", " + FileIO.formatSize(items));
		sps.add("Rate amount: " + numRates + ", " + FileIO.formatSize(numRates));
		sps.add("Scales dist: " + scaleDist.toString());
		sps.add(String.format("Data density: %.6f", numRates / (users * items + 0.0)));

		// user/item mean
		double[] data = rateMatrix.getData();
		double mean = Stats.sum(data) / numRates;
		double std = Stats.sd(data);
		double mode = Stats.mode(data);
		double median = Stats.median(data);

		sps.add(String.format("Mean: %.6f", mean));
		sps.add(String.format("Std : %.6f", std));
		sps.add(String.format("Mode: %.6f", mode));
		sps.add(String.format("Median: %.6f", median));

		List<Integer> userCnts = new ArrayList<>();
		for (int u = 0, um = numUsers(); u < um; u++) {
			int size = rateMatrix.rowSize(u);
			if (size > 0)
				userCnts.add(size);
		}
		sps.add(String.format("User mean: %.6f", Stats.mean(userCnts)));
		sps.add(String.format("User Std : %.6f", Stats.sd(userCnts)));

		if (!isItemAsUser) {
			List<Integer> itemCnts = new ArrayList<>();
			for (int j = 0, jm = numItems(); j < jm; j++) {
				int size = rateMatrix.columnSize(j);
				if (size > 0)
					itemCnts.add(size);
			}
			sps.add(String.format("Item mean: %.6f", Stats.mean(itemCnts)));
			sps.add(String.format("Item Std : %.6f", Stats.sd(itemCnts)));
		}

		Logs.info(Strings.toSection(sps));
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
	 * @return number of rates
	 */
	public int numRates() {
		return numRates;
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
	 * @param innerId
	 *            inner user id as int
	 * @return raw user id as String
	 */
	public String getUserId(int innerId) {

		if (idUsers == null)
			idUsers = userIds.inverse();

		return idUsers.get(innerId);
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
	 * @param innerId
	 *            inner user id as int
	 * @return raw item id as String
	 */
	public String getItemId(int innerId) {

		if (idItems == null)
			idItems = itemIds.inverse();

		return idItems.get(innerId);
	}

	public String getDataPath() {
		return dataPath;
	}

	public SparseMatrix getRateMatrix() {
		return rateMatrix;
	}

	public boolean isItemAsUser() {
		return isItemAsUser;
	}

	public List<Double> getScales() {
		return scales;
	}

	public BiMap<String, Integer> getUserIds() {
		return userIds;
	}

	public BiMap<String, Integer> getItemIds() {
		return itemIds;
	}

	public static void main(String[] args) throws Exception {
		String dirPath = "D:\\Java\\Datasets\\Flixster\\";
		DataDAO dao = new DataDAO(dirPath + "ratings.txt");

		dao.readData();
		//dao.printSpecs();
		//dao.writeData(dirPath + "ratings.txt");
		dao.writeArff("Flixster", dirPath + "flixster.arff");
	}
}
