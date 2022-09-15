package net.librec.data.convertor.appender;

import com.google.common.collect.*;
import net.librec.conf.Configuration;
import net.librec.conf.Configured;
import net.librec.data.FeatureAppender;
import net.librec.math.structure.SequentialAccessSparseMatrix;
//import net.librec.math.structure.SparseMatrix;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;


/**
 * A <tt>UserFeatureAppender</tt> is a class to process and store user features
 * data.
 *
 * Configuration notes:
 * data.userfeature.path indicates the location of the file holding the features in tab, space or comma separated format
 *     Data is treated as integer-valued. The path is relative to dfs.data.dir.
 * data file is in user, feature, value format. It is assumed that all values are present for all users and the feature
 * ids are in some interval 0..k and are dense.
 *
 * @author RBurke
 */
public class UserFeatureAppender extends Configured implements FeatureAppender {

    protected final Log LOG = LogFactory.getLog(this.getClass());

    /** The size of the buffer */
    private static final int BSIZE = 1024 * 1024;

    /** a {@code DenseMatrix} object build by the user feature data
     * Note that we may decide this is better as a sparse matrix
     */
//    protected SparseMatrix m_userFeatureMatrix;
    protected SequentialAccessSparseMatrix m_userFeatureMatrix;

    /** The path of the appender data file */
    protected String m_inputDataPath;

    /** Feature {raw id, inner id} map from outer to inner id */
    protected BiMap<String, Integer> m_featureIdMap;

    /** User {raw id, inner id} map from outer to inner id */
    protected BiMap<String, Integer> m_userIdMap;

    /**
     * Initializes a newly created {@code UserFeatureAppender} object with null configuration.
     */
    public UserFeatureAppender() {
        this(null);
    }

    /**
     * Initializes a newly created {@code UserFeatureAppender} object with a
     * {@code Configuration} object
     *
     * @param conf  {@code Configuration} object for construction
     */
    public UserFeatureAppender(Configuration conf) {
        this.conf = conf;
    }

    /**
     * Process appender data.
     *
     * @throws IOException if I/O error occurs during processing
     */
    @Override
    public void processData() throws IOException {
        if (conf != null && StringUtils.isNotBlank(conf.get("data.userfeature.path"))) {
            m_inputDataPath = conf.get("dfs.data.dir") + "/" + conf.get("data.userfeature.path");
            readData(m_inputDataPath);
        }
    }

    /**
     *
     * is a variable Integer or not?
     *
     * @param str
     * @return boolean value
     */
    public static boolean isInteger(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }

    /**
     * Adapted from SocialDataAppender.java
     *
     * @param inputDataPath
     *            the path of the data file
     * @throws IOException if I/O error occurs during reading
     * Not sure the multi-file aspect is necessary for this purpose
     */
    private void readData(String inputDataPath) throws IOException {
        // Table {row-id, col-id, rate}
        Table<Integer, Integer, Integer> dataTable = HashBasedTable.create();
        // Map {col-id, multiple row-id}: used to fast build a rating matrix
        Multimap<Integer, Integer> colMap = HashMultimap.create();
        // BiMap outer to inner featureIds
        m_featureIdMap = HashBiMap.create();
        final List<File> files = new ArrayList<File>();
        final ArrayList<Long> fileSizeList = new ArrayList<Long>();

        SimpleFileVisitor<Path> finder = new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                fileSizeList.add(file.toFile().length());
                files.add(file.toFile());
                return super.visitFile(file, attrs);
            }
        };
        Files.walkFileTree(Paths.get(inputDataPath), finder);
        long allFileSize = 0;
        for (Long everyFileSize : fileSizeList) {
            allFileSize = allFileSize + everyFileSize.longValue();
        }
        // loop every dataFile collecting from walkFileTree
        for (File dataFile : files) {
            FileInputStream fis = new FileInputStream(dataFile);
            FileChannel fileRead = fis.getChannel();
            ByteBuffer buffer = ByteBuffer.allocate(BSIZE);
            int len;
            String bufferLine = new String();
            byte[] bytes = new byte[BSIZE];
            while ((len = fileRead.read(buffer)) != -1) {
                buffer.flip();
                buffer.get(bytes, 0, len);
                bufferLine = bufferLine.concat(new String(bytes, 0, len)).replaceAll("\r", "\n");
                String[] bufferData = bufferLine.split("(\n)+");
                boolean isComplete = bufferLine.endsWith("\n");
                int loopLength = isComplete ? bufferData.length : bufferData.length - 1;
                for (int i = 0; i < loopLength; i++) {
                    String line = new String(bufferData[i]);
                    // Allow comments
                    if (line.charAt(0) != '#') {
                        String[] data = line.trim().split("[ \t,]+");
                        String outerUser = data[0];
                        String outerFeature = data[1];

                        // output a warning if the input is not Integer
                        if (!isInteger(data[2])) {
                            LOG.info("In UserFeatureAppender, Integer value is expected.");
                        }
                        int value = (data.length >= 3) ? Integer.valueOf(data[2]) : 1;

                        // build featureMap
                        int innerFeature;
                        if (m_featureIdMap.containsKey(outerFeature)) {
                            innerFeature = m_featureIdMap.get(outerFeature);
                        } else {
                            innerFeature = m_featureIdMap.size();
                            m_featureIdMap.put(outerFeature, innerFeature);
                        }

                        if (m_userIdMap.containsKey(outerUser)) {
                            int row = m_userIdMap.get(outerUser);
                            int col = Integer.valueOf(innerFeature);
                            dataTable.put(row, col, value);
                            colMap.put(col, row);
                        } else {
                            LOG.info("In UserFeatureAppender, no such user" + outerUser);
                        }
                    }
                }
                if (!isComplete) {
                    bufferLine = bufferData[bufferData.length - 1];
                }
                buffer.clear();
            }
            fileRead.close();
            fis.close();
        }
        int numRows = m_userIdMap.size();

        int numCols = m_featureIdMap.size();

        // build feature matrix
//        m_userFeatureMatrix = new SparseMatrix(numRows, numCols, dataTable, colMap);
        m_userFeatureMatrix = new SequentialAccessSparseMatrix(numRows, numCols, dataTable);
        // release memory of data table
        dataTable = null;
    }

    /**
     * Get user appender.
     *
     * @return the {@code SparseMatrix} object built by the user feature data.
     */
//    public SparseMatrix getUserFeatures() {
//        return m_userFeatureMatrix;
//    }
    public SequentialAccessSparseMatrix getUserFeatures() { return m_userFeatureMatrix; }

//    public SparseMatrix getItemFeatures() {
//        return null;
//    }
    public SequentialAccessSparseMatrix getItemFeatures() { return null; }

    public int getUserFeatureId(String outerFeatureId) {
        return (int) m_featureIdMap.get(outerFeatureId);
    }

    public int getItemFeatureId(String outerFeatureId) {
        return -1;
    }

    public BiMap<String, Integer> getUserFeatureMap() {
        return m_featureIdMap;
    }

    public BiMap<String, Integer> getItemFeatureMap() {
        return null;
    }

    /**
     * Set user mapping data.
     *
     * @param userMappingData
     *            user {raw id, inner id} map
     */
    @Override
    public void setUserMappingData(BiMap<String, Integer> userMappingData) {
        this.m_userIdMap = userMappingData;
    }

    /**
     * Set item mapping data.
     *
     * Does nothing because we don't use item mapping data for user features
     */
    @Override
    public void setItemMappingData(BiMap<String, Integer> itemMappingData) {

    }
}
