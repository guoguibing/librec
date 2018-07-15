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
package controller;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextArea;
import org.apache.commons.lang.StringUtils;
import util.AbbreviateListFinder;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * <tt>RecommenderController</tt> controls the configuration of recommenders.
 *
 * @author SunYatong
 */
public class RecommenderController {

    ObservableList<String> recList = FXCollections.observableArrayList(
            AbbreviateListFinder.getListByKeyWord("net.librec.recommender"));

    @FXML
    ChoiceBox<String> recommenderTypeBox;

    @FXML
    TextArea recConfArea;

    @FXML
    public void initialize() {
        recommenderTypeBox.setItems(recList);
        recommenderTypeBox.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                getDefaultConf(newValue.intValue());
            }
        });
    }

    @FXML
    public void getDefaultConf(int newValue) {
        // get abbreviate name of the recommender
        String abbrevRecName = AbbreviateListFinder.getListByKeyWord("net.librec.recommender").get(newValue);
        if (StringUtils.isEmpty(abbrevRecName)) {
            return;
        }
        Main.conf.set("rec.recommender.class", abbrevRecName);
        // read default configuration
        InputStream is = null;
        Properties prop = new Properties();
        String[] dir1 = new String[]{"baseline", "cf", "content", "context", "ensemble", "ext", "hybrid", "incremental", "nn", "poi"};
        String[] dir2 = new String[]{"ranking", "rating", ""};
        try {
            doubleLoop:
            for (int i=0; i<dir1.length; i++) {
                for (int j = 0; j<dir2.length; j++) {
                    String path = "";
                    if (j!=2) {
                        path = "rec/" + dir1[i] + "/" + dir2[j] + "/" + abbrevRecName + "-test.properties";
                    } else {
                        path = "rec/" + dir1[i] + "/" + abbrevRecName + "-test.properties";
                    }
                    is = RecommenderController.class.getClassLoader().getResourceAsStream(path);
                    if (is != null) {
                        break doubleLoop;
                    }
                }
            }
            prop.load(is);
        } catch (IOException var12) {
            var12.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException var11) {
                var11.printStackTrace();
            }
        }
        //print default configuration
        StringBuilder defaultConf = new StringBuilder();
        Iterator<Map.Entry<Object, Object>> propIter = prop.entrySet().iterator();

        while (propIter.hasNext()) {
            Map.Entry<Object, Object> entry = propIter.next();
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            defaultConf.append(key + "=" + value + "\n");
        }
        recConfArea.setText(defaultConf.toString());
    }

    @FXML
    public void goBack() throws Exception {
        if (!recConfArea.getText().isEmpty()) {
            String[] recConfs = recConfArea.getText().split("\n");
            for (String recConf : recConfs) {
                String key = recConf.split("=")[0];
                String value = recConf.split("=")[1];
                Main.conf.set(key, value);
            }
        }
        Main.showMainScene();
    }

    @FXML
    public void returnToMain() {
        Main.showMainScene();
    }
}
