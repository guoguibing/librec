package research.core.tool;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SparkSession;

/**
 * spark工具
 * 
 * @author liweigu714@163.com
 *
 */
public class SparkTool {
	private static SparkSession SPARK_SESSION_INSTANCE;
	private static JavaSparkContext SPARK_CONTEXT_INSTANCE;

	static {
		// 控制spark日志级别
		Logger.getLogger("org").setLevel(Level.WARN);

		String master = "local";
		String appName = "librec";
		SparkConf sparkConf = new SparkConf().setAppName(appName).setMaster(master);
		// .set("spark.cores.max", "4");
		SPARK_CONTEXT_INSTANCE = new JavaSparkContext(sparkConf);
		SPARK_SESSION_INSTANCE = SparkSession.builder().sparkContext(JavaSparkContext.toSparkContext(SPARK_CONTEXT_INSTANCE)).getOrCreate();
	}

	/**
	 * 获取SparkSession
	 * 
	 * @return SparkSession
	 */
	public static SparkSession getSparkSession() {
		return SPARK_SESSION_INSTANCE;
	}

	/**
	 * 获取SparkContext
	 * 
	 * @return SparkContext
	 */
	public static JavaSparkContext getSparkContext() {
		return SPARK_CONTEXT_INSTANCE;
	}
}
