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
import javafx.scene.control.RadioButton;
import util.AbbreviateListFinder;

/**
 * <tt>SimilarityController</tt> controls the configuration of similarities.
 *
 * @author SunYatong
 */
public class SimilarityController {

    ObservableList<String> recList = FXCollections.observableArrayList(
            AbbreviateListFinder.getListByKeyWord("net.librec.similarity"));

    @FXML
    RadioButton userKeyButton;

    @FXML
    RadioButton itemKeyButton;

    @FXML
    ChoiceBox<String> simTypeBox;

    @FXML
    public void initialize() {
        simTypeBox.setItems(recList);
    }

    @FXML
    public void goBack() {
        if (userKeyButton.isSelected()) {
            Main.conf.set("rec.recommender.similarities", "user");
        }
        if (itemKeyButton.isSelected()) {
            Main.conf.set("rec.recommender.similarities", "item");
        }
        Main.conf.set("rec.similarity.class", simTypeBox.getValue());

        Main.showMainScene();
    }

    @FXML
    public void returnToMain() {
        Main.showMainScene();
    }
}
