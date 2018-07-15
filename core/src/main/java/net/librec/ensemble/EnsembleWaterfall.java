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

package net.librec.ensemble;

import net.librec.recommender.item.GenericRecommendedItem;

import java.util.List;


/**
 * Emsemble learning Case
 * {@link EnsembleWaterfall}
 *
 * @author logicxin
 */

public class EnsembleWaterfall extends Ensemble{


    public EnsembleWaterfall(String configFile) throws Exception{

        super(configFile);
    }

    protected void setup(String configFile) throws Exception {
        //super.setup(configFile);
        //EnsembleWaterfallModel();
    }

    protected void EnsembleWaterfallModel() throws Exception {

        if(this.recommendedItemListOfAlgs.isEmpty()){
            System.out.print("The result of algorithms is null.");
            return;
        }
        else if( this.recommendedItemListResult.isEmpty() && !this.recommendedItemListOfAlgs.isEmpty()){

            this.recommendedItemListResult = (List) this.recommendedItemListOfAlgs.get(0);
        }
        List recommendedItemListTemp = this.recommendedItemListResult;
        for(int k= 0; k<getNumsOfAlg(); k++){
            for(int i=0; i< recommendedItemListTemp.size(); i++) {
                GenericRecommendedItem tem = (GenericRecommendedItem) recommendedItemListTemp.get(i);
                if(!InRecommendedItemListResult(tem.getUserId(), tem.getItemId(), k)){
                    this.recommendedItemListResult.remove(i);
                }
            }
        }
    }

    /*
     * Need Hash
     *
     * k is the label of the algorithm
     *
     */

    protected  Boolean InRecommendedItemListResult(String userId, String itemId, Integer k){
        List recommendedItemListResultTemp = this.recommendedItemListOfAlgs;
        List list= (List)recommendedItemListResultTemp.get(k);
        for(int j = 0; j < list.size(); j++)
        {
            GenericRecommendedItem tem = (GenericRecommendedItem)list.get(j);
            if(userId == tem.getUserId() && itemId == tem.getItemId()){
                return true;
            }
            continue;
        }
        return false;
    }

    protected List getRecommendResult() throws Exception {
        return this.recommendedItemListResult;
    }

}
