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

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;

/**
 * <tt>DataModelController</tt> controls the configuration of <tt>DataModel</tt>.
 *
 * @author SunYatong
 */
public class DataModelController {

    @FXML
    private TextField dataDfs;

    @FXML
    private TextField dataInputPath;

    @FXML
    private ChoiceBox<String> colFormat;
    ObservableList<String> colFormatList = FXCollections.observableArrayList("UIR", "UIRT");

    @FXML
    private ChoiceBox<String> dataModelType;
    ObservableList<String> dataModelList = FXCollections.observableArrayList("text", "arff");

    @FXML
    private ChoiceBox<String> splitterType;
    ObservableList<String> splitterList = FXCollections.observableArrayList("ratio", "kcv", "givenn", "loocv", "testset");


    @FXML
    private TextField splitterConf;

    @FXML
    private TextField binarizeThreshold;

    @FXML
    private TextField randomSeed;

    @FXML
    private TextField resultPath;

    @FXML
    public void initialize() {
        colFormat.setItems(colFormatList);
        dataModelType.setItems(dataModelList);
        splitterType.setItems(splitterList);
    }

    @FXML
    public void goBack() {
        String dataDfsString = dataDfs.getText();
        String dataInputPathString = dataInputPath.getText();
        String colFormatString = colFormat.getValue();
        String dataModelTypeString = dataModelType.getValue();
        String splitterTypeString = splitterType.getValue();
        String splitterConfString = splitterConf.getText();
        String thresholdString = binarizeThreshold.getText();
        String randomSeedString = randomSeed.getText();
        String resultPathString = resultPath.getText();

        if (!dataDfsString.isEmpty()) {
            Main.conf.set("dfs.data.dir", dataDfsString);
        }

        if (!dataInputPathString.isEmpty()) {
            Main.conf.set("data.input.path", dataInputPathString);
        }

        if (!colFormatString.isEmpty()) {
            Main.conf.set("data.column.format", colFormatString);
        }

        if (!dataModelTypeString.isEmpty()) {
            Main.conf.set("data.model.format", dataModelTypeString);
        }

        if (!dataDfsString.isEmpty()) {
            Main.conf.set("dfs.data.dir", dataDfsString);
        }

        if (!splitterTypeString.isEmpty()) {
            Main.conf.set("data.model.splitter", splitterTypeString);
        }

        if (!splitterTypeString.isEmpty()) {
            //
        }

        if (!thresholdString.isEmpty()) {
            Main.conf.set("data.convert.binarize.threshold", thresholdString);
        }

        if (!randomSeedString.isEmpty()) {
            Main.conf.set("rec.random.seed", randomSeedString);
        }

        if (!resultPathString.isEmpty()) {
            Main.conf.set("dfs.result.dir", resultPathString);
        }

        Main.showMainScene();
    }

    @FXML
    public void returnToMain() {
        Main.showMainScene();
    }

}
