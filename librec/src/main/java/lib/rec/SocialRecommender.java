package lib.rec;

import happy.coding.io.FileIO;
import happy.coding.io.Logs;
import happy.coding.io.Strings;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import no.uib.cipr.matrix.sparse.CompRowMatrix;

/**
 * Abstract class for social recommender where social information is enabled.
 * 
 * @author guoguibing
 * 
 */
public abstract class SocialRecommender extends Recommender {

	protected CompRowMatrix socialMatrix;
	// a list of social scales
	protected static List<Double> socialScales;

	public SocialRecommender(CompRowMatrix trainMatrix,
			CompRowMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);

		try {
			readSocialData(cf.getPath("dataset.social"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Read Social Data
	 * 
	 * @param path
	 *            path to social data
	 */
	protected void readSocialData(String path) throws Exception {
		Table<String, String, Double> dataTable = HashBasedTable.create();
		BufferedReader br = FileIO.getReader(path);
		String line = null;
		while ((line = br.readLine()) != null) {
			String[] data = line.split("[ \t,]");

			String u = data[0];
			String v = data[1];
			Double rate = Double.valueOf(data[2]);

			if (!socialScales.contains(rate))
				socialScales.add(rate); // rating scales

			dataTable.put(u, v, rate);
			if (!userIds.containsKey(u))
				userIds.put(u, userIds.size());

			if (!userIds.containsKey(v))
				userIds.put(v, userIds.size());

		}
		br.close();

		Collections.sort(socialScales);

		int numSocial = dataTable.rowKeySet().size();
		Logs.debug("Social users: {}, scales: {{}}", numSocial,
				Strings.toString(socialScales, ", "));

		// build rating matrix
		int[][] nz = new int[numSocial][];

		BiMap<Integer, String> idUsers = userIds.inverse();

		for (int uid = 0; uid < nz.length; uid++) {
			String u = idUsers.get(uid);

			nz[uid] = new int[dataTable.row(u).size()];

			List<Integer> friends = new ArrayList<>();
			for (String v : dataTable.row(u).keySet()) {
				int iid = userIds.get(v);
				friends.add(iid);
			}
			Collections.sort(friends);

			for (int c = 0, cm = friends.size(); c < cm; c++)
				nz[uid][c] = friends.get(c);
		}

		socialMatrix = new CompRowMatrix(numSocial, numSocial, nz);
		for (int i = 0; i < numSocial; i++) {
			String u = idUsers.get(i);

			Map<String, Double> userRates = dataTable.row(u);
			for (Entry<String, Double> en : userRates.entrySet()) {
				int j = userIds.get(en.getKey());
				double rate = en.getValue();
				socialMatrix.set(i, j, rate);
			}
		}

		// release memory of data table
		dataTable = null;
	}
}
