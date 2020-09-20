package net.librec.data;

import com.google.common.collect.BiMap;
//import net.librec.math.structure.SparseMatrix;
import net.librec.math.structure.SequentialAccessSparseMatrix;

public interface FeatureAppender extends DataAppender {

    /**
     *
     *
     * @return inner user feature id
     */
    public int getUserFeatureId(String outerUserFeatureId);

    /**
     * @return inner item feature id
     */
    public int getItemFeatureId(String outerItemFeatureId);

    /**
     * Get item mapping data.
     *
     * @return  the item {raw id, inner id} map of user features.
     */
    public BiMap<String, Integer> getUserFeatureMap();

    /**
     * Get item mapping data.
     *
     * @return  the item {raw id, inner id} map of item features.
     */
    public BiMap<String, Integer> getItemFeatureMap();

    /**
     * @return user x feature values
     */
//    public SparseMatrix getUserFeatures();
    public SequentialAccessSparseMatrix getUserFeatures();

    /**
     * @return item x feature values
     */
//    public SparseMatrix getItemFeatures();
    public SequentialAccessSparseMatrix getItemFeatures();

}
