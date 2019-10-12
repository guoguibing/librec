/**
 * 
 */
package research.data.reader;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import okio.BufferedSource;
import okio.Okio;
import okio.Source;

/**
 * CSV文件读取器
 * @author ZhengYangPing
 *
 */
public class CSVReader {

	/**
	 * 日志
	 */
	private static final Logger LOG = LoggerFactory.getLogger(CSVReader.class);
    
    /**
     * The default format of input data file
     */
    private static final String DATA_COLUMN_DEFAULT_FORMAT = "UIR";

    /**
     * 数据列格式，默认为UIR
     */
    private String dataColumnFormat;
    
    /**
     * the path of the input data file
     */
    private String[] inputDataPath;
    
    private String[] header;
    
    private String[] attr;
    
    /**
     * 分隔符,默认为逗号
     */
    private String sep;
    
    /**
     * Initializes a newly created {@code CSVReader} object with the
     * path of the input data file.
     *
     * @param inputDataPath the path of the input data file
     */
    public CSVReader(String inputDataPath) {
        this(DATA_COLUMN_DEFAULT_FORMAT, inputDataPath, ",");
    }
    
    public CSVReader(String dataColumnFormat, String inputDataPath) {
        this(dataColumnFormat, inputDataPath, " ");
    }
    
    public CSVReader(String dataColumnFormat, String inputDataPath, String sep) {
        this.dataColumnFormat = dataColumnFormat;
        this.inputDataPath = inputDataPath.split(",");
        this.sep = sep;
    }
    
    public CSVReader(String[] header, String[] attr, String inputDataPath, String sep) {
        this.header = header;
        this.attr = attr;
        this.inputDataPath = inputDataPath.split(",");
        this.sep = sep;
    }
    
    public CSVReader(String[] inputDataPath) {
        this(DATA_COLUMN_DEFAULT_FORMAT, inputDataPath, ",");
    }

    public CSVReader(String[] inputDataPath, String sep) {
        this(DATA_COLUMN_DEFAULT_FORMAT, inputDataPath, sep);
    }

    public CSVReader(String dataColumnFormat, String[] inputDataPath, String sep) {
        this.dataColumnFormat = dataColumnFormat;
        this.inputDataPath = inputDataPath;
        this.sep = sep;
    }
    
    /**
     * 读取CSV文件数据
     * @return
     * @throws IOException
     */
    public DataSet readData() throws IOException {
        LOG.info(String.format("Dataset: %s", Arrays.toString(inputDataPath)));
        DataSet matrix = new DataSet();
        if (Objects.isNull(header)) {
            if (dataColumnFormat.toLowerCase().equals("uirt")) {
                header = new String[]{"user", "item", "rating", "datetime"};
                attr = new String[]{"STRING", "STRING", "NUMERIC", "DATE"};
            } else {
                header = new String[]{"user", "item", "rating"};
                attr = new String[]{"STRING", "STRING", "NUMERIC"};
            }
        }

        matrix.setAttrType(attr);
        matrix.setHeader(header);
        List<File> files = new ArrayList<>();
        SimpleFileVisitor<Path> finder = new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                files.add(file.toFile());
                return super.visitFile(file, attrs);
            }
        };
        for (String path : inputDataPath) {
            Files.walkFileTree(Paths.get(path.trim()), finder);
        }
        Pattern pattern = Pattern.compile(sep);
        for (File file : files) {
            try (Source fileSource = Okio.source(file);
                 BufferedSource bufferedSource = Okio.buffer(fileSource)) {
                String temp;
                while ((temp = bufferedSource.readUtf8Line()) != null) {
                    if ("".equals(temp.trim())) {
                        break;
                    }
                    String[] eachRow = pattern.split(temp);
                    for (int i = 0; i < header.length; i++) {
                        if (Objects.equals(attr[i], "STRING")) {
                            DataSet.setId(eachRow[i], matrix.getHeader(i));
                        }
                    }
                    matrix.add(eachRow);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        List<Double> ratingScale = matrix.getRatingScale();
        if (ratingScale != null) {
            LOG.info(String.format("rating Scale: %s", ratingScale.toString()));
        }
        LOG.info(String.format("user number: %d,\t item number is: %d", matrix.numUsers(), matrix.numItems()));
        return matrix;
    }
    
}
