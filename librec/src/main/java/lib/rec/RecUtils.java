package lib.rec;

import happy.coding.io.Configer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lib.rec.intf.Recommender;

/**
 * Recommender Utility Class for Configing Recommenders
 * 
 * @author guoguibing
 * 
 */
public class RecUtils {

	public static Map<String, List<Double>> buildParams(Configer cf) {
		Map<String, List<Double>> params = new HashMap<>();

		// diversity alpha
		String divKey = "val.diverse.alpha";
		List<Double> as = cf.getRange(divKey);
		if (as.size() > 1)
			params.put(divKey, as);

		return params;
	}

	/**
	 * get the current value for key which supports multiple runs
	 * 
	 * @param params
	 *            parameter-values map
	 * @param key
	 *            parameter key
	 * @return current value for a parameter
	 */
	public static double getMKey(Map<String, List<Double>> params, String key) {
		double alpha = 0;
		if (params != null && params.containsKey(key)) {
			alpha = params.get(key).get(RecSys.paramIdx);
			RecSys.isMultRun = true;
		} else {
			alpha = Recommender.cf.getDouble(key);
			RecSys.isMultRun = false;
		}

		return alpha;
	}

}
