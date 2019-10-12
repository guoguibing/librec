package research.data.fs;
import java.io.*;
import java.util.Iterator;
import java.util.List;
import java.lang.String;
/**
 * Created by zqmath1994 on 19/1/27.
 */
public interface FileSystemInterFace {
    public boolean exists(String path) throws IOException;
    public Reader getReader(String path) throws IOException;
    public Writer getWriter(String path) throws IOException;
    public InputStream getInputStream(String path) throws IOException;
    public OutputStream getOutputStream(String path) throws IOException;
    public List<String> recurGetPaths(List<String> paths) throws IOException;
    public List<Iterator<String>> read(List<String> paths) throws IOException;
    public List<Iterator<String>> selectRead(List<String> paths, int divisor, int remainer) throws IOException;
    public void delete(String path) throws IOException;
    public void mkdirs(String path) throws IOException;
}
