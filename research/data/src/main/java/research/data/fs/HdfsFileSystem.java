package research.data.fs;

import research.data.dataflow.DataUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.hadoop.fs.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;



/**
* hdfs相关接口实现
* @author mengyifan
*/

public class HdfsFileSystem implements FileSystemInterFace {

    public static final Logger LOG = LoggerFactory.getLogger(HdfsFileSystem.class);

    protected FileSystem fs;
    public HdfsFileSystem(String uri) throws IOException {
        this.fs = DataUtils.getHdfsFileSystem(uri);

    }

    public boolean exists(String path) throws IOException {
        return fs.exists(new Path(path));
    }

    public Reader getReader(String path) throws IOException {
        return new InputStreamReader(fs.open(new Path(path)));
    }

    public Writer getWriter(String path) throws IOException {
        return new OutputStreamWriter(fs.create(new Path(path)));
    }


    public InputStream getInputStream(String path) throws IOException {
        return new FSDataInputStream(fs.open(new Path(path)));
    }


    public OutputStream getOutputStream(String path) throws IOException {
        return new FSDataOutputStream(fs.create(new Path(path)));
    }


    public List<String> recurGetPaths(List<String> paths) throws IOException {
        List<Path> hdfsPaths = new ArrayList<>();
        for (String path : paths) {
            hdfsPaths.addAll(getHdfsFiles(fs, new Path(path)));
        }
        return hdfsPath2Name(hdfsPaths);
    }


    public List<Iterator<String>> read(List<String> paths) throws IOException {
        return getHdfsFilesIterator(fs, hdfsName2Path(recurGetPaths(paths)));
    }


    public List<Iterator<String>> selectRead(List<String> paths, int divisor, int remainer) throws IOException {
        return getSelectHdfsFilesIterator(fs, hdfsName2Path(recurGetPaths(paths)), divisor, remainer);
    }


    public void delete(String path) throws IOException {
        fs.delete(new Path(path), true);
    }


    public void mkdirs(String path) throws IOException {
        fs.mkdirs(new Path(path));
    }

    public static BufferedReader getHdfsBufferReader(FileSystem fs, Path path) throws IOException {
        return new BufferedReader(new InputStreamReader(fs.open(path)));
    }


    public static PrintWriter getHdfsPrintWriter(FileSystem fs, Path path) throws IOException {
        return new PrintWriter((new OutputStreamWriter(fs.create(path))));
    }

    public static List<Iterator<String>> getHdfsFilesIterator(FileSystem fs, List<Path> pathList) throws IOException {
        List<Iterator<String>> itList = new ArrayList<>();
        for (Path path : pathList) {
            BufferedReader reader = getHdfsBufferReader(fs, path);
            Iterator<String> it = new DataUtils.BufferedLineIterator(reader);
            itList.add(it);
        }
        return itList;
    }

    public static List<Iterator<String>> getSelectHdfsFilesIterator(FileSystem fs, List<Path> pathList, int divisor, int remainer) throws IOException {
        List<Iterator<String>> itList = new ArrayList<>();
        for (Path path : pathList) {
            BufferedReader reader = getHdfsBufferReader(fs, path);
            Iterator<String> it = new DataUtils.SelectLineIterator(reader, divisor, remainer);
            itList.add(it);
        }
        return itList;
    }

    public static List<String> hdfsPath2Name(List<Path> hdfsPaths) {
        List<String> paths = new ArrayList<>();
        for (Path path : hdfsPaths) {
            paths.add(path.toUri().toString());
        }
        return paths;
    }

    public static List<Path> hdfsName2Path(List<String> hdfsNames) {
        List<Path> hdfsPaths = new ArrayList<>(hdfsNames.size());
        for (String name : hdfsNames) {
            hdfsPaths.add(new Path(name));
        }
        return hdfsPaths;
    }

    public static List<Path> getHdfsFiles(FileSystem fs, Path dataPath) throws IOException {
        List<Path> pathList = new ArrayList<>();
        RemoteIterator<LocatedFileStatus> iterator = fs.listFiles(dataPath, true);
        while(iterator.hasNext()) {
            LocatedFileStatus fileStatus = iterator.next();
            pathList.add(fileStatus.getPath());
        }

        return pathList;
    }

    public static List<Path> allocHdfsFiles(FileSystem fs, Path dataPath, int slaveNum, int rank) throws IOException {

        List<Path> pathList = getHdfsFiles(fs, dataPath);

        Collections.sort(pathList);

        List<Path> thisRankFileList = new ArrayList<>();
        int avgfile = pathList.size() / slaveNum;
        int modfile = pathList.size() % slaveNum;
        int fidxfile = 0;
        for (int i = 0; i < slaveNum; i++) {
            int tnum = i < modfile ? avgfile + 1 : avgfile;
            int from = fidxfile;
            int to = fidxfile + tnum;
            fidxfile += tnum;
            if (i == rank) {
                for (int r = from; r < to; r++) {
                    thisRankFileList.add(pathList.get(r));
                }
                break;
            }
        }
        return thisRankFileList;
    }

}
