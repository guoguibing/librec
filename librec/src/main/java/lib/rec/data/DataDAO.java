package lib.rec.data;

import happy.coding.io.FileIO;
import happy.coding.io.Logs;
import happy.coding.io.Strings;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import no.uib.cipr.matrix.Matrices;
import no.uib.cipr.matrix.sparse.CompRowMatrix;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashBiMap;
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
	private SparseMat rateMatrix;

	// is item type as user
	private boolean isItemAsUser;

	// data scales
	private List<Double> scales;
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

		scales = new ArrayList<>();

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

	public void convert(String sep) throws Exception {
		BufferedReader br = FileIO.getReader(dataPath);

		String line = null;
		List<String> lines = new ArrayList<>();
		while ((line = br.readLine()) != null) {
			String newline = line.replaceAll(sep, " ");

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

	public SparseMat readData() throws Exception {

		Table<String, String, Double> dataTable = HashBasedTable.create();
		BufferedReader br = FileIO.getReader(dataPath);
		String line = null;
		while ((line = br.readLine()) != null) {
			String[] data = line.split("[ \t,]");

			String user = data[0];
			String item = data[1];
			Double rate = Double.valueOf(data[2]);

			if (!scales.contains(rate))
				scales.add(rate); // rating scales

			dataTable.put(user, item, rate);
			if (!userIds.containsKey(user))
				userIds.put(user, userIds.size()); // inner user id starts from
													// 0

			if (!itemIds.containsKey(item))
				itemIds.put(item, itemIds.size()); // inner item id starts from
													// 0
		}
		br.close();

		Collections.sort(scales);
		int numRows = numUsers(), numCols = numItems();

		// if min-rate = 0.0, add a small value
		double epsilon = scales.get(0).doubleValue() == 0.0 ? 1e-5 : 0;
		if (epsilon > 0) {
			for (int i = 0, im = scales.size(); i < im; i++) {
				double val = scales.get(i);
				scales.set(i, val + epsilon);
			}
		}

		if (isItemAsUser) {
			Logs.debug("User amount: {}, scales: {{}}", numRows, Strings.toString(scales, ", "));
		} else {
			Logs.debug("User amount: {}, item amount: {}", numRows, numCols);
			Logs.debug("Rating Scales: {{}}", Strings.toString(scales, ", "));
		}

		// build rating matrix
		int[][] nz = new int[numRows][];

		for (int uid = 0; uid < nz.length; uid++) {
			String user = getUserId(uid);

			nz[uid] = new int[dataTable.row(user).size()];

			List<Integer> items = new ArrayList<>();
			for (String item : dataTable.row(user).keySet())
				items.add(getItemId(item));
			Collections.sort(items);

			for (int c = 0, cm = items.size(); c < cm; c++)
				nz[uid][c] = items.get(c);
		}

		rateMatrix = new SparseMat(numRows, numCols, nz);
		for (int i = 0; i < numRows; i++) {
			String user = getUserId(i);

			Map<String, Double> itemRates = dataTable.row(user);
			for (Entry<String, Double> en : itemRates.entrySet()) {
				int j = getItemId(en.getKey());
				double rate = en.getValue();
				rateMatrix.set(i, j, rate + epsilon);
			}
		}

		// release memory of data table
		dataTable = null;

		return rateMatrix;
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
		return Matrices.cardinality(rateMatrix);
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

	public CompRowMatrix getRateMatrix() {
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
}
