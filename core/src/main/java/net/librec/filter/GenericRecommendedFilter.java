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
package net.librec.filter;

import net.librec.recommender.item.RecommendedItem;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Recommended Filter
 *
 * @author WangYuFeng
 */
public class GenericRecommendedFilter implements RecommendedFilter {
    /**
     * filter RecommendedItem from recommendedList by userId list.
     */
    private List<String> userIdList;
    /**
     * filter RecommendedItem from recommendedList by itemId list.
     */
    private List<String> itemIdList;

    /**
     * Filter the recommended list.
     *
     * @param recommendedList recommendedItem list to be filtered
     * @return  filtered recommendedItem list
     */
    @Override
    public List<RecommendedItem> filter(List<RecommendedItem> recommendedList) {
        if (recommendedList != null && recommendedList.size() > 0) {
            if ((userIdList != null && userIdList.size() > 0) || (itemIdList != null && itemIdList.size() > 0)) {
                Set<RecommendedItem> filterRecommendedSet = new HashSet<>();
                filter(userIdList, recommendedList, filterRecommendedSet, "user");
                filter(itemIdList, recommendedList, filterRecommendedSet, "item");
                return new ArrayList<>(filterRecommendedSet);
            }
        }
        return recommendedList;
    }

    /**
     * filter the recommended list by specified type of the filter.
     *
     * @param filterIdList          filter id list
     * @param recommendedList       recommended item list
     * @param filterRecommendedSet  filter recommended set
     * @param filterType            type of the filter
     */
    private void filter(List<String> filterIdList, List<RecommendedItem> recommendedList, Set<RecommendedItem> filterRecommendedSet, String filterType) {
        if (filterIdList != null && filterIdList.size() > 0) {
            for (String filterId : filterIdList) {
                for (RecommendedItem recommendedItem : recommendedList) {
                    String recommendedId = null;
                    if (StringUtils.equals("user", filterType)) {
                        recommendedId = recommendedItem.getUserId();
                    } else if (StringUtils.equals("item", filterType)) {
                        recommendedId = recommendedItem.getItemId();
                    }
                    if (StringUtils.equals(filterId, recommendedId)) {
                        filterRecommendedSet.add(recommendedItem);
                    }
                }
            }
        }
    }

    /**
     * Set the userId list.
     *
     * @param userIdList the userIdList to set
     */
    public void setUserIdList(List<String> userIdList) {
        this.userIdList = userIdList;
    }

    /**
     * Set the itemId list.
     *
     * @param itemIdList the itemIdList to set
     */
    public void setItemIdList(List<String> itemIdList) {
        this.itemIdList = itemIdList;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((itemIdList == null) ? 0 : itemIdList.hashCode());
        result = prime * result + ((userIdList == null) ? 0 : userIdList.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        GenericRecommendedFilter other = (GenericRecommendedFilter) obj;
        if (itemIdList == null) {
            if (other.itemIdList != null)
                return false;
        } else if (!itemIdList.equals(other.itemIdList))
            return false;
        if (userIdList == null) {
            if (other.userIdList != null)
                return false;
        } else if (!userIdList.equals(other.userIdList))
            return false;
        return true;
    }
}
