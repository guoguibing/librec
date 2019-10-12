package research.data.spliter;


import research.data.spark.SparkBase;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.spark_project.guava.primitives.Doubles;

import java.util.List;
import java.util.stream.Collectors;

public class RatioDataSpliter {
    String InputPath = "";
    String OutputPath = "";

    boolean multi = false;
    double[] ratio_lst;
    public boolean setInputPath(String InputPath) {
        this.InputPath = InputPath;
        if (this.InputPath == null || this.InputPath.isEmpty()) {
            return false;
        } else {
            return true;
        }
    }

    public boolean setOutputPath(String OutputPath) {
        this.OutputPath = OutputPath;
        if (this.OutputPath == null || this.OutputPath.isEmpty()) {
            return false;
        } else {
            return true;
        }
    }

    RatioDataSpliter(String InputPath, String OutputPath) {
        this.InputPath = InputPath;
        this.OutputPath = OutputPath;
    }

    public List<Dataset<Row>> run(double[] ratio_lst) {
        Dataset<Row> dataset = SparkBase.getSparkSession().read().option("header", true).csv(this.InputPath);
        List<Dataset<Row>> randomSplit = dataset.randomSplitAsList(ratio_lst, 1);
        return randomSplit;
    }

    public void setOption() {
        if (this.ratio_lst.length == 1) {
            if (this.ratio_lst[0] <= 0 || this.ratio_lst[0] >= 1) {
                throw new RuntimeException("ratio must > 0 and must < 1");
            } else {
                this.multi = false;
            }
        } else {
            List<Double> ratio_list = Doubles.asList(this.ratio_lst);
            if (ratio_list.stream().anyMatch(n -> n <= 0)) {
                throw new RuntimeException("ratio must > 0");
            }
            Double sum = ratio_list.stream().mapToDouble(Double::doubleValue).sum();
            if (sum != 1.0) {
                for(int i = 0; i < this.ratio_lst.length; ++i){
                    this.ratio_lst[i] = this.ratio_lst[i] / sum ;
                }
            }
            this.multi = true;                    
         }

    }
    


}
