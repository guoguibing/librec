/**
 * Copyright (C) 2017 LibRec
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
package net.librec.recommender.cf.rating;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import net.librec.common.LibrecException;
import net.librec.data.convertor.appender.AuxiliaryDataAppender;
import net.librec.data.model.ArffInstance;
import net.librec.math.structure.DenseMatrix;
import net.librec.math.structure.MatrixEntry;
import net.librec.recommender.MatrixFactorizationRecommender;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Jie Yang, Zhu Sun, Alessandro Bozzon and Jie Zhang <strong>Learning Hierarchical Feature Influence for Recommendation
 * by Recursive Regularization</strong>, RecSys 2016.
 *
 * @author SunYatong
 */
public class ReMFRecommender extends MatrixFactorizationRecommender {

    /** select user or item side recursive regularization */
    private String hierarchy_side;

    /** learning rate for the update of parameters g and s; Actually, the learning rate of each parameter can be different  */
    private double rate = 0.00005;

    /** the regularization coefficient to control the importance of recursive regularization */
    private double alpha;

    /** the number of the first, second, third layer */
    private int continentNum = 0, countryNum = 0, cityNum = 0;

    /** the total number of nodes; and the number of non_leaf nodes */
    private int total_node = 0, non_leaf = 0;

    /** contain all the nodes */
    private Map<Integer, ArrayList<ArrayList>> node = new HashMap<>();

    /** save all the continent string; */
    private ArrayList<String> continentList = new ArrayList<>();

    /** save all the country string */
    private ArrayList<String> countryList = new ArrayList<>();

    /** save all the city string */
    private ArrayList<String> cityList = new ArrayList<>();

    /** Map continent string to id */
    private Map<String, Integer> ContinentMap = new HashMap<>();

    /** Map country string to id */
    private Map<String, Integer> CountryMap = new HashMap<>();

    /** Map city string to id */
    private Map<String, Integer> CityMap = new HashMap<>();

    /** Map id to string */
    private Map<Integer, String> ConverseMap = new HashMap<>();

    /** save the hierarchy information */
    protected static Map<String, ArrayList<String>> hierarchy = new HashMap<>();

    /** map userIdx to userId */
    private Map<Integer, String> userIdxToUserId;

    /** map itemIdx to userId */
    private Map<Integer, String> itemIdxToItemId;

    @Override
    protected void setup() throws LibrecException {
        super.setup();
        alpha = conf.getDouble("rec.alpha");
        hierarchy_side = conf.get("rec.side");
        userFactors.init(0.6);
        itemFactors.init(0.6);
        userIdxToUserId = context.getDataModel().getUserMappingData().inverse();
        itemIdxToItemId = context.getDataModel().getItemMappingData().inverse();
        try {
            hierarchy = readHierarchy();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void trainModel() throws LibrecException {
        getLayers();
        getIDs();
        createHierarchy();
        //initial g and s; the first column represents g; and the second column represents s;
        double[][] coef = new double [non_leaf][2];
        for (int i = 0; i < coef.length; i++) {
            coef[i][0] = 0.5;
            coef[i][1] = 0.5;
        }

        for (int iter = 1; iter <= numIterations; iter++) {

            loss = 0;

            DenseMatrix PS = new DenseMatrix(numUsers, numFactors);
            DenseMatrix QS = new DenseMatrix(numItems, numFactors);

            double[][] transfer = new double [non_leaf][2];

            for (MatrixEntry me : trainMatrix) {
                int u = me.row();
                int j = me.column();
                double ruj = me.get();

                if (ruj <= 0) continue;
                double pred = predict(u, j);
                double euj = pred - ruj;

                loss += euj * euj;

                for (int f = 0; f < numFactors; f++) {
                    double puf = userFactors.get(u, f);
                    double qjf = itemFactors.get(j, f);
                    PS.plus(u, f, euj * qjf + regUser * puf );
                    QS.plus(j, f, euj * puf + regItem * qjf );
                    loss += regUser * puf * puf + regItem * qjf * qjf;
                }
            }

            //Recursive regularization

            //First: user or item in the same leaf node
            Table<Integer, Integer, Double> L2node = HashBasedTable.create();
            for (int nodeid = non_leaf; nodeid <= total_node; nodeid++) {
                if (!node.containsKey(nodeid)) continue;
                Object[] id = node.get(nodeid).get(1).toArray();
                if (id.length >= 2) {
                    double value = 0.0;
                    for (int j = 0; j < id.length; j++) {
                        for (int g = j+1; g < id.length; g++) {
                            int idj = (int) id[j];
                            int idg = (int) id[g];
                            for (int f = 0; f < numFactors; f++) {
                                double ejg = 0.0;
                                if (hierarchy_side.equals("user")) {
                                    ejg = userFactors.get(idj, f) - userFactors.get(idg, f);
                                    PS.plus(idj, f, alpha * ejg);
                                }
                                if (hierarchy_side.equals("item")) {
                                    ejg = itemFactors.get(idj, f) - itemFactors.get(idg, f);
                                    QS.plus(idj, f, alpha * ejg);
                                }

                                loss += alpha * ejg * ejg;
                                value += ejg * ejg;
                            }
                        }
                    }
                    L2node.put(nodeid, nodeid, value);
                }
            }

            //Second: the users/items in the different leaf node
            for (int i = non_leaf; i <= total_node; i++) {
                if (!node.containsKey(i)) continue;
                for (int j = i + 1; j <= total_node; j++) {
                    if (!node.containsKey(j)) continue;
                    ArrayList<Integer> list = new ArrayList<Integer>();
                    for (Object pi : node.get(i).get(0)) {
                        for (Object pj : node.get(j).get(0)) {
                            if (pi == pj) list.add((int)pi);
                        }
                    }
                    double value = 0.0;
                    for (Object idi: node.get(i).get(1)) {
                        for (Object idj: node.get(j).get(1)) {
                            int lastid = list.size() - 1;
                            double reg = coef[list.get(lastid)][0];
                            for (int num = lastid - 1; num >= 0; num--) {
                                reg += coef[list.get(num)][0] + reg * coef[list.get(num)][1];
                            }
                            int idi_1 = (int) idi;
                            int idj_1 = (int) idj;
                            for (int f = 0; f < numFactors; f++) {
                                double eij = 0.0;
                                if (hierarchy_side.equals("user")) {
                                    eij = userFactors.get(idi_1, f) - userFactors.get(idj_1, f);
                                    PS.plus(idi_1, f, alpha * reg * eij);
                                }
                                if (hierarchy_side.equals("item")) {
                                    eij = itemFactors.get(idi_1, f) - itemFactors.get(idj_1, f);
                                    QS.plus(idi_1, f, alpha * reg * eij);
                                }
                                loss += alpha * reg * eij * eij;
                                value += eij * eij;
                            }
                        }
                    }
                    L2node.put(i, j, value);
                    L2node.put(j, i, value);
                }
            }

            //Update g_p and s_p

            //First: update the node of country
            for (int i = continentNum + 1; i < non_leaf; i++) {
                if (!node.containsKey(i)) continue;
                double value = getValueG(i, coef);

                double L2g = 0.0;
                Object[] city = node.get(i).get(2).toArray();
                for (int x = 0; x < city.length; x++) {
                    int cityx = (int) city[x];
                    if (L2node.contains(cityx, cityx)) L2g += L2node.get(cityx, cityx);
                    for (int y = x+1; y < city.length; y++) {
                        int cityy = (int) city[y];
                        if(L2node.contains(cityy, cityy)) L2g += L2node.get(cityx, cityy);
                    }
                }
                L2node.put(i, i, L2g);
                transfer[i][0] = L2g * value;
            }

            //Second: update the node of continent;
            for (int i = 1; i <= continentNum; i++) {
                if (!node.containsKey(i)) continue;
                double value = getValueG(i, coef);
                double L2g = 0.0;
                Object[] country = node.get(i).get(2).toArray();
                for (int x = 0; x < country.length; x++) {
                    int countryx = (int) country[x];
                    if(L2node.contains(countryx, countryx)) {L2g += L2node.get(countryx, countryx);}
                    for (int y = x+1; y < country.length; y++) {
                        int countryy = (int) country[y];
                        for (Object cityx : node.get(countryx).get(2)) {
                            for (Object cityy : node.get(countryy).get(2)) {
                                if(L2node.contains(cityx, cityy)) {L2g += L2node.get(cityx, cityy);}
                            }
                        }
                    }
                }
                L2node.put(i, i, L2g);
                transfer[i][0] = L2g * value;
            }

            //Third: update the node of root
            double L2g = 0.0;
            for (int i = 1; i <= continentNum; i++) {
                if (!node.containsKey(i)) continue;
                if(L2node.contains(i, i)) L2g += L2node.get(i, i);

            }
            transfer[0][0] = L2g;


            //Final Update
            for (int i = 0; i < coef.length; i++) {
                coef[i][0] = coef[i][0] - rate * Math.sqrt((Math.sqrt(transfer[i][0])) );
                if (coef[i][0] < 0) {
                    coef[i][0] = 0;
                }
                else if (coef[i][0] > 1)
                {
                    coef[i][0] = 1;
                }
                coef[i][1] = 1 - coef[i][0];
                int number = 0;
                if(node.containsKey(i)){
                    number = node.get(i).get(1).size();
                }
            }

            userFactors = userFactors.plus(PS.times(-learnRate));
            itemFactors = itemFactors.plus(QS.times(-learnRate));

            loss *= 0.5;
            if (isConverged(iter) && earlyStop) {
                break;
            }
            updateLRate(iter);
            LOG.info("learnRate:" + learnRate);
        }
    }

    @Override
    protected double predict (int userIdx, int itemIdx) throws LibrecException {
        double pred = userFactors.row(userIdx).dot(itemFactors.row(itemIdx));
        return pred;
    }

    /**
     * Get the continent/country/city list and their corresponding sizes
     */
    protected void getLayers() {
        for (String user: hierarchy.keySet()) {
            String continent = hierarchy.get(user).get(0);
            String country = hierarchy.get(user).get(1);
            String city = hierarchy.get(user).get(2);
            if (!continentList.contains(continent)) continentList.add(continent);
            if (!countryList.contains(country)) countryList.add(country);
            if (!cityList.contains(city)) cityList.add(city);
        }
        continentNum = continentList.size();
        countryNum = countryList.size();
        cityNum = cityList.size();
        total_node = continentNum + countryNum + cityNum;
        non_leaf = continentNum + countryNum + 1;

        LOG.info("continentNum: "+ continentNum+"; countryNum: "+countryNum+"; cityNum: "+cityNum);
        LOG.info("total number of nodes: "+total_node + "; the number of non_leaf nodes: "+ non_leaf);
    }

    /**
     * Map the continent/country/city id
     */
    protected void getIDs() {
        int count = 1 ; //the id flag
        for ( String continent : continentList) {
            ContinentMap.put(continent, count);
            ConverseMap.put(count, continent);
            count++;
        }

        for (String country: countryList) {
            CountryMap.put(country, count);
            ConverseMap.put(count, country);
            count++;
        }

        for (String city: cityList) {
            CityMap.put(city, count);
            ConverseMap.put(count, city);
            count++;
        }
    }

    /**
     * divide users/items into different node that they belong to
     * @param nodeid
     * @param parentid
     * @param element
     */
    protected void divideUI(int nodeid, int parentid, int element ) {
        if (!node.containsKey(nodeid)) {
            ArrayList<ArrayList> lists = new ArrayList<ArrayList>();
            ArrayList parent = new ArrayList<>();
            ArrayList child = new ArrayList<>();
            ArrayList temp = new ArrayList<>();
            parent.addAll(node.get(parentid).get(0));
            parent.add(parentid);
            temp.add(element);
            lists.add(parent);
            lists.add(temp);
            lists.add(child);
            node.put(nodeid, lists);
        }
        else {
            node.get(nodeid).get(1).add(element);
        }
        // plus child_id to its parent node
        if (!node.get(parentid).get(2).contains(nodeid)) {
            node.get(parentid).get(2).add(nodeid);
        }
    }

    /**
     * build layers except the first layer
     *
     * @param begin
     * @param end
     * @param layer
     */
    protected void buildLayer(int begin, int end, int layer) {

        for (int num = begin; num < end; num++) {
            if (!node.containsKey(num)) continue;
            if (node.get(num).get(1).size() >= 2) {
                for (Object innerid: node.get(num).get(1).toArray()) {
                    String rawid = null;

                    if (hierarchy_side.equals("user")) rawid = userIdxToUserId.get(innerid);
                    if (hierarchy_side.equals("item")) rawid = itemIdxToItemId.get(innerid);
                    String feature_rawid = hierarchy.get(rawid).get(layer - 1);
                    int feature_innerid = 0;
                    if (layer == 2){
                        feature_innerid = CountryMap.get(feature_rawid);
                    }
                    if (layer == 3) {
                        feature_innerid = CityMap.get(feature_rawid);
                    }
                    divideUI(feature_innerid, num, (int) innerid);
                }
            }
        }
    }

    /**
     * create the hierarchical structure
     */
    protected void createHierarchy(){

        //root node
        ArrayList<ArrayList> Lists = new ArrayList<ArrayList>();
        ArrayList<Integer> parent = new ArrayList<>();
        ArrayList<Integer> element = new ArrayList<>();
        ArrayList<Integer> child = new ArrayList<>();
        Lists.add(parent);
        Lists.add(element);
        Lists.add(child);
        node.put(0, Lists);

        //nodes at the first layer -- continents
        if (hierarchy_side.equals("user")) {
            for (int uid=0; uid<numUsers; uid++) {
                String user = userIdxToUserId.get(uid);
                if (hierarchy.containsKey(user)) {
                    String continent = hierarchy.get(user).get(0);
                    int continentid = ContinentMap.get(continent);
                    divideUI(continentid, 0, uid);
                }
            }
        }
        if (hierarchy_side.equals("item")) {
            for (int iid=0; iid<numItems; iid++) {
                String item = itemIdxToItemId.get(iid);
                if (hierarchy.containsKey(item)) {
                    String continent = hierarchy.get(item).get(0);
                    int continentid = ContinentMap.get(continent);
                    divideUI(continentid, 0, iid);
                }
            }
        }

        //nodes at the second layer -- countries
        buildLayer(0, continentNum, 2);

        //nodes at the third layer -- cities
        buildLayer(continentNum + 1, continentNum + countryNum, 3);

    }

    /**
     * get the coefficient for updating g_p
     *
     * @param id
     * @param coef
     * @return
     */
    protected double getValueG(int id, double[][]coef) {
        double valueg = 1.0;
        for (Object parentid : node.get(id).get(0)) {
            int pid = (int) parentid;
            valueg = valueg * coef[pid][1];
        }
        return valueg;
    }

    /**
     * Read hierarchy information
     * @return
     */
    protected Map<String, ArrayList<String>> readHierarchy() {
        Map<String, ArrayList<String>> hierarchy = new HashMap<String, ArrayList<String>>();
        ArrayList<ArffInstance> auxiliaryData = ((AuxiliaryDataAppender) getDataModel().getDataAppender()).getAuxiliaryData();
        for (ArffInstance instance: auxiliaryData) {
            String userId = (String) instance.getValueByIndex(0);
            String continent = (String) instance.getValueByIndex(1);
            String country = (String) instance.getValueByIndex(2);
            String city = (String) instance.getValueByIndex(3);

            ArrayList<String> temp = new ArrayList<String>();
            temp.add(continent);
            temp.add(country);
            temp.add(city);
            hierarchy.put(userId, temp);
        }

        return hierarchy;
    }
}
