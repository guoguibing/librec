package net.librec.data.convertor.appender;

import com.google.common.collect.*;
import net.librec.conf.Configuration;
import net.librec.conf.Configured;
import net.librec.data.FeatureAppender;
import net.librec.math.structure.SequentialAccessSparseMatrix;
//import net.librec.math.structure.SparseMatrix; // nasim
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
 * A <tt>ItemFeatureAppender</tt> is a class to process and store item features
 * data.
 *
 * Configuration notes:
 * data.itemfeature.path indicates the location of the file holding the features in tab, space or comma separated format
 *     Data is treated as integer-valued. The path is relative to dfs.data.dir.
 * data file is in item, feature, value format. It is assumed that all values are present for all items and the feature
 * ids are in some interval 0..k and are dense.
 *
 * @author RBurke
 */
public class ItemFeatureAppender extends Configured implements FeatureAppender {

    /** The size of the buffer */
    private static final int BSIZE = 1024 * 1024;

    /**
     * LOG
     */
    protected final Log LOG = LogFactory.getLog(this.getClass());


    /** a {@code DenseMatrix} object build by the user feature data
     * Note that we may decide this is better as a sparse matrix
     */
    protected SequentialAccessSparseMatrix m_itemFeatureMatrix;
//    protected SparseMatrix m_itemFeatureMatrix; // nasim

    /** The path of the appender data file */
    protected String m_inputDataPath;

    /** Feature {raw id, inner id} map from outer to inner ids */
    protected BiMap<String, Integer> m_featureIdMap;

    /** Item {raw id, inner id} map from outer item to inner ids */
    protected BiMap<String, Integer> m_itemIdMap;

    /**
     * Initializes a newly created {@code ItemFeatureAppender} object with null.
     */
    public ItemFeatureAppender() {
        this(null);
    }

    /**
     * Initializes a newly created {@code ItemFeatureAppender} object with a
     * {@code Configuration} object
     *
     * @param conf  {@code Configuration} object for construction
     */
    public ItemFeatureAppender(Configuration conf) {
        this.conf = conf;
    }

    /**
     * Process appender data.
     *
     * @throws IOException if I/O error occurs during processing
     */
    @Override
    public void processData() throws IOException {
        if (conf != null && StringUtils.isNotBlank(conf.get("data.itemfeature.path"))) {
            m_inputDataPath = conf.get("dfs.data.dir") + "/" + conf.get("data.itemfeature.path");
            readData(m_inputDataPath);
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
        // BiMap outer to inner feature id
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
                    String[] data = line.trim().split("[ \t,]+");
                    String outerItem = data[0];
                    String outerFeature = data[1];
                    int value = (data.length >= 3) ? Integer.valueOf(data[2]) : 1;

                    int innerFeature;
                    if (m_featureIdMap.containsKey(outerFeature)) {
                        innerFeature = m_featureIdMap.get(outerFeature);
                    } else {
                        innerFeature = m_featureIdMap.size();
                        m_featureIdMap.put(outerFeature, innerFeature);
                    }

                    if (m_itemIdMap.containsKey(outerItem)) {
                        int row = m_itemIdMap.get(outerItem);
                        int col = Integer.valueOf(innerFeature);
                        dataTable.put(row, col, value);
                        colMap.put(col, row);
                    } else {
                        LOG.info("In ItemFeatureAppender, no such item" + outerItem);
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
        int numRows = m_itemIdMap.size();

        int numCols = m_featureIdMap.size();

        // build feature matrix
//        m_itemFeatureMatrix = new SparseMatrix(numRows, numCols, dataTable, colMap); //nasim
        m_itemFeatureMatrix = new SequentialAccessSparseMatrix(numRows, numCols, dataTable);;
        // release memory of data table
        dataTable = null;
    }

    /**
     * Get item appender.
     *
     * @return the {@code SparseMatrix} object built by the item feature data.
     */
//    public SparseMatrix getItemFeatures() {
//        return m_itemFeatureMatrix;
//    }
    public SequentialAccessSparseMatrix getItemFeatures() { return m_itemFeatureMatrix; }

//    public SparseMatrix getUserFeatures() { return null; }
    public SequentialAccessSparseMatrix getUserFeatures() { return null; }

    public int getItemFeatureId(String outerFeatureId) {
        return (int) m_featureIdMap.get(outerFeatureId);
    }

    public int getUserFeatureId(String outerFeatureId) {
        return -1;
    }

    /**
     * Does nothing. User ids not used for item feature mapping
     */
     @Override
     public void setUserMappingData(BiMap<String, Integer> userMappingData) {
     }

    /**
     * Set item mapping data.
     *
     * @param itemMappingData
     *            item {raw id, inner id} map
     */
    @Override
    public void setItemMappingData(BiMap<String, Integer> itemMappingData) {
        this.m_itemIdMap = itemMappingData;
    }

    public BiMap<String, Integer> getUserFeatureMap() {
        return null;
    }

    public BiMap<String, Integer> getItemFeatureMap() {
        return m_featureIdMap;
    }
}
