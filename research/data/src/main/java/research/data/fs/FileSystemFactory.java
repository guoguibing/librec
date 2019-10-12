package research.data.fs;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


/**
* 文件系统工厂方法
* @author mengyifan
*/

public class FileSystemFactory {
    public final static Set<String> HDFS_FILE_SYSTEM_SET = new HashSet<>();
    static {
        HDFS_FILE_SYSTEM_SET.addAll(Arrays.asList("file", "hdfs", "s3", "s3n",
                "kfs", "hftp", "hsftp", "webhdfs", "ftp", "ramfs", "har"));
    };

    public static FileSystemInterFace createFileSystem(URI uri) throws IOException {
        if (HDFS_FILE_SYSTEM_SET.contains(uri.toString().split(":")[0])) {
            return new HdfsFileSystem(uri.toString());
        } else if (uri.toString().equalsIgnoreCase("local")) {
            return new HdfsFileSystem("aaa");
        } else {
            throw new IOException("unknown file system uri:" + uri);

        }
    }
}
