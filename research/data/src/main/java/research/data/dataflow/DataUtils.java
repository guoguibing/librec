package research.data.dataflow;


import org.python.core.Py;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.python.core.PyFunction;
import org.python.util.PythonInterpreter;
import java.io.BufferedReader;
import java.io.*;
import java.util.*;
import java.net.URI;
import java.util.function.Function;
/**
 * Created by zqmath1994 on 19/1/27.
 */
public class DataUtils {
    public static final Logger LOG = LoggerFactory.getLogger(DataUtils.class);

    public static class BufferedLineIterator implements Iterator<String> {
        BufferedReader reader;
        String line;
        public BufferedLineIterator(BufferedReader reader) {
            this.reader = reader;
        }


        public boolean hasNext() {
            try {
                line = reader.readLine();
            } catch (IOException e) {
                LOG.error("read data error!");
                e.printStackTrace();
                close();
                return false;
            }

            if (line == null) {
                close();
            }
            return line != null;
        }


        public String next() {
            return line;
        }

        private void close() {
            if (reader != null) {
                try{
                    reader.close();
                } catch (IOException e){
                    LOG.error("reader close error!");
                    e.printStackTrace();
                }
            }
        }

    }

    public static class SelectLineIterator implements Iterator<String> {
        BufferedReader reader;
        String line;
        int divisor;
        long readedLines = 0;
        int remainder;
        public  SelectLineIterator(BufferedReader reader, int divisor, int remainder) {
            this.reader = reader;
            this.divisor = divisor;
            this.remainder = remainder;
        }

        @Override
        public boolean hasNext() {
            try {
                for (;;){
                    line = reader.readLine();
                    readedLines ++;
                    if (((readedLines - 1) % divisor) == remainder) {
                        break;
                    }

                }
            } catch (IOException e) {
                LOG.error("read data error!");
                e.printStackTrace();
                close();
                return false;
            }

            if (line == null) {
                close();
            }
            return line != null;
        }

        @Override
        public String next() {
            return line;
        }


        public void close() {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    LOG.error("reader close error!");
                    e.printStackTrace();
                }
            }
        }

    }

    public static PyFunction getTranformFunction(boolean needPyTransform, String pyTransformScript) {
        PythonInterpreter interpreter;
        PyFunction transformFunc = null;
        if (needPyTransform) {
            interpreter = new PythonInterpreter();
            interpreter.execfile(pyTransformScript);
            transformFunc = interpreter.get("transform", PyFunction.class);
        }

        return transformFunc;
    }

    public static boolean hdfsPathPrefixValie(String path) {
        if (!(path.startsWith("hdfs://") || path.startsWith("file:///"))) {
            return false;
        }
        return true;
    }

    public static boolean pathNotNull(String path) {
        return path != null;
    }

    public static FileSystem getHdfsFileSystem(String scheme) throws IOException {
        Configuration conf = new Configuration();
        conf.set("fs.hdfs.impl", org.apache.hadoop.hdfs.DistributedFileSystem.class.getName());
        conf.set("fs.file.impl", org.apache.hadoop.fs.LocalFileSystem.class.getName());
        // TODO: @孟一凡
//        conf.set("fs.s3.impl", org.apache.hadoop.fs.s3.S3FileSystem.class.getName());
//        conf.set("fs.s3n.impl", org.apache.hadoop.fs.s3native.NativeS3FileSystem.class.getName());
        return FileSystem.get(URI.create(scheme), conf);
    }

    public static boolean isHdfsProtocol(String path) {
        return path.trim().startsWith("hdfs");
    }

    public static int[][] avgAssign(int amount, int bins){
        int assign[][] = new int[bins][2];
        int avg = amount / bins;
        int mod = amount % bins;
        int fidx = 0;
        for (int t = 0; t < bins; t++){
            int tnum = t < mod ? avg + 1 : avg;
            assign[t][0] = fidx;
            assign[t][1] = fidx + tnum;
            fidx += tnum;
        }
        return assign;
    }

    public static <T, R> void travel(Function<T, R> func, List<Iterator<T>> iterators) {
        for (Iterator<T> it :  iterators){
            while (it.hasNext()) {
                func.apply(it.next());
            }
        }
    }

    public  static void randomBitset(BitSet bitSet, int seed, double thre, int to) {
        Random rand = new Random();
        bitSet.clear();
        for (int i = 0; i < to; i ++){
            if (rand.nextDouble() <= thre) {
                bitSet.set(i);
            }
        }
    }

    public static void randomBitset(BitSet bitSet, double thre, int to) {
        Random rand = new Random();
        bitSet.clear();
        for (int i = 0; i < to; i++) {
            if (rand.nextDouble() <= thre) {
                bitSet.set(i);
            }
        }
    }



    public static void main(String []args) throws IOException {

        //String path = "hdfs://f04/research_pub/ytk-learn/datasets/news20.binary";
//        String path = "hdfs://f04";
//        getHdfsFileSystem(path);

//        List<String> list = Arrays.asList("1", "2", "3");
//        Map<String, Integer> map = new HashMap<>();
//        travel(line -> map.put(line, map.size()), Arrays.asList(list.iterator()));
//        System.out.println(map);
    }


}
