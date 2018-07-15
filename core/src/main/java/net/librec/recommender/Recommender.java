/**
 * Copyright (C) 2016 LibRec
 * <p>
 * This file is part of LibRec.
 * LibRec is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * LibRec is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with LibRec. If not, see <http://www.gnu.org/licenses/>.
 */
package net.librec.recommender;

import net.librec.common.LibrecException;
import net.librec.data.DataModel;
import net.librec.data.structure.AbstractBaseDataEntry;
import net.librec.data.structure.LibrecDataList;
import net.librec.math.structure.DataSet;
import net.librec.recommender.item.RecommendedList;

/**
 * General recommenders
 *
 * @author WangYuFeng and Keqiang Wang
 */
public interface Recommender {
    /**
     * train recommender model
     *
     * @param context recommender context
     * @throws LibrecException if error occurs during recommending
     */
    void train(RecommenderContext context) throws LibrecException;

    RecommendedList recommendRating(DataSet predictDataSet) throws LibrecException;

    RecommendedList recommendRating(LibrecDataList<AbstractBaseDataEntry> dataList) throws LibrecException;

    RecommendedList recommendRank() throws LibrecException;

    RecommendedList recommendRank(LibrecDataList<AbstractBaseDataEntry> dataList) throws LibrecException;

    /**
     * get DataModel
     *
     * @return data model
     */
    DataModel getDataModel();

    /**
     * load Model
     *
     * @param filePath file path
     */
    void loadModel(String filePath);

    /**
     * save Model
     *
     * @param filePath file path
     */
    void saveModel(String filePath);

//    /**
//     * get Recommended List
//     *
//     * @return recommended list
//     */
//    List<RecommendedItem> getRecommendedList(RecommendedList recommendedList);


    /**
     * set Context
     *
     * @param context recommender context
     */
    void setContext(RecommenderContext context);
}
