/**
 * Copyright (C) 2016 LibRec
 *
 * This file is part of LibRec.
 * LibRec is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LibRec is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LibRec. If not, see <http://www.gnu.org/licenses/>.
 */
package net.librec.filter;

import net.librec.BaseTestCase;
import net.librec.common.LibrecException;
import net.librec.conf.Configuration;
import net.librec.conf.Configuration.Resource;
import net.librec.data.DataModel;
import net.librec.data.model.TextDataModel;
import net.librec.recommender.Recommender;
import net.librec.recommender.RecommenderContext;
import net.librec.recommender.cf.UserKNNRecommender;
import net.librec.recommender.item.GenericRecommendedItem;
import net.librec.recommender.item.RecommendedItem;
import net.librec.similarity.PCCSimilarity;
import net.librec.similarity.RecommenderSimilarity;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * GenericRecommendedFilter Test Case corresponds to GenericRecommendedFilter
 * {@link net.librec.filter.GenericRecommendedFilter}
 *
 * @author SunYatong
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class GenericRecommendedFilterTestCase extends BaseTestCase {

    private List<String> userIdList;
    private List<String> itemIdList;
    private List<RecommendedItem> recommendedList;

    @Before
    public void setup() throws Exception {
        super.setUp();

        userIdList = new ArrayList<>();
        itemIdList = new ArrayList<>();
        for (int i=1; i<=2; i++) {
            userIdList.add(Integer.toString(i));
            itemIdList.add(Integer.toString(4-i));
        }

        recommendedList = new ArrayList<>();
        int count = 1;
        for (int i=1; i<=3; i++) {
            for (int j=1; j<=3; j++){
                recommendedList.add(new GenericRecommendedItem(Integer.toString(i), Integer.toString(j), count));
                count++;
            }
        }
    }

    /**
     * Test filtering the recommended list with user type.
     *
     * @throws LibrecException
     */
    @Test
    public void test01UserFilter() throws LibrecException {
        GenericRecommendedFilter filter = new GenericRecommendedFilter();
        filter.setUserIdList(userIdList);
        List<RecommendedItem> filteredList = filter.filter(recommendedList);
        for (RecommendedItem recommendedItem : filteredList) {
            System.out.println("user:"+recommendedItem.getUserId() + " "
                    + "item:"+recommendedItem.getItemId() + " "
                    + "value:"+recommendedItem.getValue());
        }
        assertEquals(6, filteredList.size());
    }

    /**
     * Test filtering the recommended list with item type.
     *
     * @throws LibrecException
     */
    @Test
    public void test02ItemFilter() throws LibrecException {
        GenericRecommendedFilter filter = new GenericRecommendedFilter();
        filter.setItemIdList(itemIdList);
        List<RecommendedItem> filteredList = filter.filter(recommendedList);
        for (RecommendedItem recommendedItem : filteredList) {
            System.out.println("user:"+recommendedItem.getUserId() + " "
                    + "item:"+recommendedItem.getItemId() + " "
                    + "value:"+recommendedItem.getValue());
        }
        assertEquals(6, filteredList.size());
    }

    /**
     * Test filter with running an algorithm
     *
     * @throws Exception
     */
    @Test
    public void test03WithAlgorithm() throws Exception {
        Configuration conf = new Configuration();
        Resource resource = new Resource("rec/cf/userknn-test.properties");
        conf.addResource(resource);
        DataModel dataModel = new TextDataModel(conf);
        dataModel.buildDataModel();
        RecommenderContext context = new RecommenderContext(conf, dataModel);
        RecommenderSimilarity similarity = new PCCSimilarity();
        similarity.buildSimilarityMatrix(dataModel);
        context.setSimilarity(similarity);
        Recommender recommender = new UserKNNRecommender();
        recommender.setContext(context);
//        String filePath = conf.get("dfs.result.dir") + "/model-"
//                + DriverClassUtil.getDriverName(UserKNNRecommender.class);
//        recommender.loadModel(filePath);
        recommender.recommend(context);
        List<RecommendedItem> recommendedItemList = recommender.getRecommendedList();
        GenericRecommendedFilter filter = new GenericRecommendedFilter();
        filter.setUserIdList(userIdList);
        filter.setItemIdList(itemIdList);
        recommendedItemList = filter.filter(recommendedItemList);
        for (RecommendedItem recommendedItem : recommendedItemList) {
            if (StringUtils.equals(recommendedItem.getUserId(), "2")) {
                System.out.println("user:"+recommendedItem.getUserId() + " "
                        + "item:"+recommendedItem.getItemId() + " "
                        + "value:"+recommendedItem.getValue());
            }
        }
        System.out.println("---------------------------------------------------");
        for (RecommendedItem recommendedItem : recommendedItemList) {
            System.out.println("user:"+recommendedItem.getUserId() + " "
                    + "item:"+recommendedItem.getItemId() + " "
                    + "value:"+recommendedItem.getValue());
        }
    }
}
