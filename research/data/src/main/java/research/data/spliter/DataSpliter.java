package research.data.spliter;

import research.data.spark.SparkBase;
import java.util.ArrayList;
import java.util.List;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;


public class DataSpliter {
    String InputPath = "";
    String OutputPath = "";
    DataSpliter(String InputPath, String OutputPath){
        this.InputPath = InputPath;
        this.OutputPath = OutputPath;
    }
    public List<Dataset<Row>> run() {
        Dataset<Row> dataset = SparkBase.getSparkSession().read().option("header", true).csv(this.InputPath);
        List<Dataset<Row>> dataset_split  = new ArrayList<Dataset<Row>> ();
        //dataset_split = dataset.randomSplitAsList([0.8, 0.2]);
        return dataset_split;
    }
    
    
    public boolean setInputPath(String InputPath) {
        this.InputPath = InputPath;
        if (this.InputPath == null || this.InputPath.isEmpty()){
            return false;
        } else {
            return true;
        }
    } 

    public boolean setOutputPath(String OutputPath) {
        this.OutputPath= OutputPath;
        if (this.OutputPath == null || this.OutputPath.isEmpty()){
            return false;
        } else {
            return true;
        }
    }

 
}
