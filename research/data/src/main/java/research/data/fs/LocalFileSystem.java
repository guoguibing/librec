package research.data.fs;
import research.data.dataflow.*;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Created by zqmath1994 on 19/1/27.
 */
public class LocalFileSystem implements FileSystemInterFace{
    @Override
    public boolean exists(String path) throws IOException {
        return new File(path).exists();
    }

    @Override
    public Reader getReader(String path) throws IOException {
        return new BufferedReader(new FileReader(path));
    }

    @Override
    public Writer getWriter(String path) throws IOException {
        File parentFile = new File(path).getParentFile();
        if (parentFile != null) {
            mkdirs(parentFile.getAbsolutePath());
        }
        return new PrintWriter(path);
    }

    @Override
    public InputStream getInputStream(String path) throws IOException {
        return new FileInputStream(path);
    }

    @Override
    public OutputStream getOutputStream(String path) throws IOException {
        mkdirs(new File(path).getParentFile().getAbsolutePath());
        return new FileOutputStream(path);
    }

    @Override
    public List<String> recurGetPaths(List<String> paths) throws IOException {
        List<String> retList = new ArrayList<>();
        for (String path : paths) {
            File file = new File(path);
            if (!file.exists()) {
                throw new IOException("path:" + path + " does not exist");
            }
            if (!file.isDirectory()) {
                if (!file.isHidden()) {
                    retList.add(path);
                }
            } else {
                Iterator<File> fileIt = FileUtils.iterateFiles(file, null, true);
                while (fileIt.hasNext()) {
                    File afile = fileIt.next();
                    if (isHiddenFile(afile)) {
                        continue;
                    }
                    retList.add(afile.getAbsolutePath());
                }
            }
        }

        return retList;
    }

    @Override
    public List<Iterator<String>> read(List<String> paths) throws IOException {
        List<Iterator<String>> itList = new ArrayList<>();
        for (String path : paths) {
            List<String> recursivePaths = recurGetPaths(Arrays.asList(path));
            for (String rpath : recursivePaths) {
                BufferedReader reader = new BufferedReader(new FileReader(rpath));
                Iterator<String> it = new DataUtils.BufferedLineIterator(reader);
                itList.add(it);
            }
        }
        return itList;
    }

    @Override
    public List<Iterator<String>> selectRead(List<String> paths, int divisor, int remainer) throws IOException {
        List<Iterator<String>> itList = new ArrayList<>();
        for (String path : paths) {
            BufferedReader reader = new BufferedReader(new FileReader(path));
            Iterator<String> it = new DataUtils.SelectLineIterator(reader, divisor, remainer);
            itList.add(it);
        }
        return itList;
    }

    @Override
    public void delete(String path) throws IOException {
        FileUtils.forceDelete(new File(path));
    }

    @Override
    public void mkdirs(String path) throws IOException {
        new File(path).mkdirs();
    }

    private static boolean isHiddenFile(File file) {
        if (file.isHidden()) {
            return true;
        } else {
            File parent = file.getParentFile();
            if (parent == null) {
                return false;
            }
            return isHiddenFile(parent);
        }
    }

    public static void main(String []args) throws IOException {
        FileSystemInterFace fileSystem = new LocalFileSystem();
        System.out.println("____________________");
        System.out.println(fileSystem.recurGetPaths(Arrays.asList("/Users/")));
    }

}

