package research.data.spark;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SparkSession;

/**
 * spark基类
 * 
 * @author mengyifan
 *
 */
public class SparkBase {
	private static SparkSession SPARK_SESSION_INSTANCE;
	private static JavaSparkContext SPARK_CONTEXT_INSTANCE;
    private static String master;
    private static String appName;
    private String InputPath = "";
    private String OutputPath = "";
	static {
		// 控制spark日志级别
		Logger.getLogger("org").setLevel(Level.WARN);

		master = "local";
		appName = "librec";
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
    
    /** 
     * 设置Spark运行模式
     * 
     * @return bool
    */
    public static boolean setSparkMaster(String SparkMaster) {
        master = SparkMaster;
        if (master == null || master.isEmpty()){
            return false;
        } else {
            return true;
        }
    }

    /**
     * 设置Spark appName
     * 
     * @return bool
     */
    public static boolean setSparkappName(String SparkappName) {
        appName = SparkappName;
        if (appName == null || appName.isEmpty()){
            return false;
        } else {
            return true;
        }
    } 

    public  boolean setInputPath(String InputPath) {
        this.InputPath= InputPath;
        if (this.InputPath == null || this.InputPath.isEmpty()){
            return false;
        } else {
            return true;
        }
    } 

    public  boolean setOutputPath(String OutputPath) {
        this.OutputPath= OutputPath;
        if (this.OutputPath == null || this.OutputPath.isEmpty()){
            return false;
        } else {
            return true;
        }
    } 
}
