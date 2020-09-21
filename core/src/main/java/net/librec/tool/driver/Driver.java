package net.librec.tool.driver;

import net.librec.conf.Configuration;
import net.librec.job.RecommenderJob;
import net.librec.math.algorithm.Randoms;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;


public class Driver {

    // needs to point to the configuration file
//    public static String CONFIG_FILE = "./Users/naso1989/Downloads/librec-3.0.0 (1)/librec-3.0.0/conf/biased_mf.properties";
//    public static String CONFIG_FILE = "./conf/biased_mf.properties";
//    public static String CONFIG_FILE = "/Users/naso1989/Downloads/librec/conf/biased_mf.properties";
    public static String CONFIG_FILE = "/Users/naso1989/Downloads/librec/conf/config-fair.properties";

//    public static void writeFile(String outlog, String mystr) throws IOException {
//        PrintWriter pw = new PrintWriter(new FileWriter(outlog, true));
//        pw.write(mystr + "\n");
//        pw.close();
//        System.out.println("Saved Config setting into file" + outlog);
//    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();

        String confFilePath = CONFIG_FILE;

        Properties prop = new Properties();
        prop.load(new FileInputStream(confFilePath));

        String all = "";
        String outlog = "";

        for (String name : prop.stringPropertyNames()) {
            conf.set(name, prop.getProperty(name));
            //save it all in a string and then save it to file
            all += name + " = " + prop.getProperty(name) + '\n';
        }

        Randoms.seed(20180616);
        RecommenderJob job = new RecommenderJob(conf);
        job.runJob();


        outlog = conf.get("data.log.out.path");

        //save it to file
//        writeFile(outlog, all);

//        System.out.print("Finished");
    }
}
