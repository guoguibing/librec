package net.librec.data.convertor.appender;

import com.google.common.collect.BiMap;
import net.librec.conf.Configuration;
import net.librec.conf.Configured;
import net.librec.data.DataAppender;
import net.librec.math.structure.SequentialAccessSparseMatrix;
import net.librec.recommender.item.KeyValue;
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
 * Created by jinyuanyuan on 2017/4/15.
 */
public class LocationDataAppender extends Configured implements DataAppender {
    private static final Log LOG = LogFactory.getLog(LocationDataAppender.class);
    /** The size of the buffer */
    private static final int BSIZE = 1024 * 1024;

     KeyValue<Double,Double>[] locationArray;

    /** The path of the appender data file */
    private String inputDataPath;

    /** User {raw id, inner id} map from rating data */
    private BiMap<String, Integer> userIds;

    /** Item {raw id, inner id} map from rating data */
    private BiMap<String, Integer> itemIds;
    /**
     * Initializes a newly created {@code SocialDataAppender} object with null.
     */
    public LocationDataAppender() {
        this(null);
    }

    /**
     * Initializes a newly created {@code SocialDataAppender} object with a
     * {@code Configuration} object
     *
     * @param conf  {@code Configuration} object for construction
     */
    public LocationDataAppender(Configuration conf) {
        this.conf = conf;
    }

    /**
     * Process appender data.
     *
     * @throws IOException if I/O error occurs during processing
     */
    @Override
    public void processData() throws IOException {
        if (conf != null && StringUtils.isNotBlank(conf.get("data.appender.path"))) {
            inputDataPath = conf.get("dfs.data.dir") + "/" + conf.get("data.appender.path");
            LOG.info(inputDataPath);
            locationArray = new KeyValue[itemIds.size()];
            readData(inputDataPath);
        }
    }

    /**
     * Read data from the data file. Note that we didn't take care of the
     * duplicated lines.
     *
     * @param inputDataPath
     *            the path of the data file
     * @throws IOException if I/O error occurs during reading
     */
    private void readData(String inputDataPath) throws IOException {

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
                    String POIId = data[0];
                    Double latitude = Double.parseDouble(data[1]);
                    Double longtitude = Double.parseDouble(data[2]);

                    if (itemIds.containsKey(POIId)) {
                        locationArray[itemIds.get(POIId)] = new KeyValue<Double, Double>(latitude, longtitude);//innerid as the index of locaitonArray
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
    }

    /**
     * Get user appender.
     *
     * @return the {@code SparseMatrix} object built by the social data.
     */
    public  KeyValue<Double,Double>[] getLocationAppender() {
        return locationArray;
    }

    /**
     * Get item appender.
     *
     * @return null
     */
    public SequentialAccessSparseMatrix getItemAppender() {
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
        this.userIds = userMappingData;
    }

    /**
     * Set item mapping data.
     *
     * @param itemMappingData
     *            item {raw id, inner id} map
     */
    @Override
    public void setItemMappingData(BiMap<String, Integer> itemMappingData) {
        this.itemIds = itemMappingData;
    }
}
