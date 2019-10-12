package research.data.spliter;


import research.data.spark.SparkBase;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import java.util.List;


public class DateDataSpliter {
    String InputPath = "";
    String OutputPath = "";
   
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
    DateDataSpliter(String InputPath, String OutputPath){
        this.InputPath = InputPath;
        this.OutputPath = OutputPath;
    }

    
    public List<Dataset<Row>> run(double[] arraySplit) {
        Dataset<Row> dataset = SparkBase.getSparkSession().read().option("header", true).csv(this.InputPath);
        List<Dataset<Row>> randomSplit =  dataset.randomSplitAsList(arraySplit, 1);
        return randomSplit;
    }
    
    
    

}
