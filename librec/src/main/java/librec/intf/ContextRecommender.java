package librec.intf;

import com.google.common.collect.Table;

import happy.coding.io.Logs;
import happy.coding.io.Strings;
import librec.data.Context;
import librec.data.SparseMatrix;

/**
 * Generic recommenders where contextual information is used. The context can be user-, item- and rating-related.
 * 
 * @author guoguibing
 * 
 */
public class ContextRecommender extends IterativeRecommender {

	// user, item, context
	protected static Table<Integer, Integer, Context> contextTable;

	// initialization
	static {

		try {
			readContext();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public ContextRecommender(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);
	}

	/**
	 * Read contextual information to contextTable; If necessary, we can have up to three contextTables, i.e., user-,
	 * item-, and ratingContext.
	 * 
	 * @throws Exception
	 */
	protected static void readContext() throws Exception {
		String contextPath = cf.getPath("dataset.context");
		Logs.debug("Contextual dataset: {}", Strings.last(contextPath, 38));

		// add your code to read contextual information
		// may be different method by method
	}

}
