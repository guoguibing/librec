package net.librec;

import net.librec.common.LibrecException;
import net.librec.conf.Configuration;
import net.librec.eval.Measure;
import net.librec.job.RecommenderJob;
import net.librec.recommender.Recommender;
import net.librec.util.DriverClassUtil;
import net.librec.util.FileUtil;
import net.librec.util.ReflectionUtil;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class TestResultGenerator extends BaseTestCase {
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    /**
     * test the all the recommenders
     *
     * @throws ClassNotFoundException
     * @throws LibrecException
     * @throws IOException
     */
    @Test
    public void testRecommender() throws ClassNotFoundException, LibrecException, IOException {
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm").format(Calendar.getInstance().getTime());
        List<String> filePaths = getResourceFolderFiles("rec/");
        List<String> recNames = new ArrayList<>();
        List<String> timeList = new ArrayList<>();
        List<Map<Measure.MeasureValue, Double>> evalMaps = new ArrayList<>();
        System.out.println(filePaths);

        DecimalFormat formatter = new DecimalFormat("#0.0000");
        String outputPath = "../result/rec-test-result-3.0-" + timeStamp;
        try {
            FileUtil.writeString(outputPath, "librec-3.0.0 test at: " + timeStamp + "\n", false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (String filePath : filePaths) {

            Recommender rec = null;
            Configuration conf = new Configuration();
            filePath = filePath.split("classes")[1];
            Configuration.Resource resource = new Configuration.Resource(filePath);
            conf.addResource(resource);
            try {
                rec = ReflectionUtil.newInstance((Class<Recommender>) getRecommenderClass(conf), conf);
            } catch (Exception e) {
                writeError(outputPath, filePath, e.getMessage());
                e.printStackTrace();
            }

            if (rec != null) {
                RecommenderJob job = new RecommenderJob(conf);
                try {
                    long startTime = System.nanoTime();
                    job.runJob();
                    long endTime   = System.nanoTime();
                    String time = formatter.format((endTime - startTime)/1000000d);
                    timeList.add(time);
                    Map<Measure.MeasureValue, Double> evalMap = job.getEvaluatedMap();
                    evalMaps.add(evalMap);
                    recNames.add(filePath);
                    writeResult(outputPath, filePath, time, evalMap, formatter);
                } catch (Exception e) {
                    writeError(outputPath, filePath, e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * List the all recommenders' info.
     *
     * @throws ClassNotFoundException
     * @throws LibrecException
     * @throws IOException
     */
    @Test
    public void listRecommenderInfo() throws ClassNotFoundException, LibrecException, IOException {
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm").format(Calendar.getInstance().getTime());
        List<String> filePaths = getResourceFolderFiles("rec/");
        List<String> recNames = new ArrayList<>();
        List<String> timeList = new ArrayList<>();
        List<Map<Measure.MeasureValue, Double>> evalMaps = new ArrayList<>();
        System.out.println(filePaths);

        DecimalFormat formatter = new DecimalFormat("#0.0000");
        String outputPath = "../result/rec-test-result-3.0-" + timeStamp;
        try {
            FileUtil.writeString(outputPath, "librec-3.0.0 test at: " + timeStamp + "\n", false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (String filePath : filePaths) {

            Recommender rec = null;
            Configuration conf = new Configuration();
            filePath = filePath.split("classes")[1];
            Configuration.Resource resource = new Configuration.Resource(filePath);
            conf.addResource(resource);
            try {
                rec = ReflectionUtil.newInstance((Class<Recommender>) getRecommenderClass(conf), conf);
            } catch (Exception e) {
                writeError(outputPath, filePath, e.getMessage());
                e.printStackTrace();
            }

            if (rec != null) {
                String dirRec = rec.getClass().getName().replaceFirst("net.librec.recommender.", "");
                String recName = dirRec.split("\\.")[dirRec.split("\\.").length - 1];
                String dirPath = dirRec.replaceAll("\\." + recName, "");

                String superName = rec.getClass().getSuperclass().getName().replaceFirst("net.librec.recommender.", "");
                String shortName = conf.get("rec.recommender.class");

                StringBuilder sb = new StringBuilder();
                sb.append(dirPath + " |   " + shortName + "   |   " + recName + "|");
                try {
                    FileUtil.writeString(outputPath, sb.toString(), true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * List the all recommenders' conf.
     *
     * @throws ClassNotFoundException
     * @throws LibrecException
     * @throws IOException
     */
    @Test
    public void listRecommenderConf() throws ClassNotFoundException, LibrecException, IOException {
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm").format(Calendar.getInstance().getTime());
        List<String> filePaths = getResourceFolderFiles("rec/");
        List<String> recNames = new ArrayList<>();
        List<String> timeList = new ArrayList<>();
        List<Map<Measure.MeasureValue, Double>> evalMaps = new ArrayList<>();
        System.out.println(filePaths);

        DecimalFormat formatter = new DecimalFormat("#0.0000");
        String outputPath = "../result/rec-test-result-3.0-" + timeStamp;
        try {
            FileUtil.writeString(outputPath, "librec-3.0.0 test at: " + timeStamp + "\n", false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (String filePath : filePaths) {

            Recommender rec = null;
            Configuration conf = new Configuration();
            String rawFilePath = new String(filePath);
            filePath = filePath.split("classes")[1];
            Configuration.Resource resource = new Configuration.Resource(filePath);
            conf.addResource(resource);
            try {
                rec = ReflectionUtil.newInstance((Class<Recommender>) getRecommenderClass(conf), conf);
            } catch (Exception e) {
                writeError(outputPath, filePath, e.getMessage());
                e.printStackTrace();
            }

            if (rec != null) {
                String dirRec = rec.getClass().getName().replaceFirst("net.librec.recommender.", "");
                String recName = dirRec.split("\\.")[dirRec.split("\\.").length - 1];

                StringBuilder sb = new StringBuilder();
                sb.append("#####" + recName + "\n");
                sb.append("```\n");

                try (Stream<String> stream = Files.lines(Paths.get(rawFilePath), StandardCharsets.UTF_8))
                {
                    stream.forEach(s -> sb.append(s).append("\n"));
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }

                sb.append("```\n");
                try {
                    FileUtil.writeString(outputPath, sb.toString(), true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void writeError(String outputPath, String recName, String errorPosition) {
        StringBuilder sb = new StringBuilder();
        sb.append(recName + " failed at " + errorPosition);
        try {
            FileUtil.writeString(outputPath, sb.toString(), true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void writeResult(String outputPath, String recName, String time, Map<Measure.MeasureValue, Double> evalMap, DecimalFormat formatter) {

        StringBuilder sb = new StringBuilder();
        sb.append(recName + " : [time(ms):" + time);
        for (Map.Entry<Measure.MeasureValue, Double> entry : evalMap.entrySet()) {
            if (entry != null && entry.getKey() != null) {
                sb.append(", " + entry.getKey().getMeasure() + ":" + formatter.format(entry.getValue()));
            }
        }
        sb.append("]");
        try {
            FileUtil.writeString(outputPath, sb.toString(), true);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static List<String> getResourceFolderFiles (String folder) {
        List<String> fNameList = new ArrayList<>();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        URL url = loader.getResource(folder);
        String path = url.getPath();
        List<File> fileList = listf(path);
        for (File file : fileList){
            String fileName = file.getPath();
            fNameList.add(fileName);
            // System.out.println(fileName);
        }
        return fNameList;
    }

    public Class<? extends Recommender> getRecommenderClass(Configuration conf) throws ClassNotFoundException, IOException {
        return (Class<? extends Recommender>) DriverClassUtil.getClass(conf.get("rec.recommender.class"));
    }


    public static List<File> listf(String directoryName) {

        File directory = new File(directoryName);

        List<File> finalFileList = new ArrayList<>();
        File[] fList = directory.listFiles();

        for (File file : fList) {
            if (file.isFile()) {
                finalFileList.add(file);
            } else if (file.isDirectory()) {
                finalFileList.addAll(listf(file.getAbsolutePath()));
            }
        }

        return finalFileList;
    }

}
