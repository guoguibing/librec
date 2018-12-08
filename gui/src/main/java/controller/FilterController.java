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
 * <tt>EvaluatorController</tt> controls the configuration of <tt>Filter</tt>.
 *
 * @author SunYatong
 */
public class FilterController {

    ObservableList<String> filterList = FXCollections.observableArrayList("generic");

    @FXML
    TextField userListField;

    @FXML
    TextField itemListField;

    @FXML
    ChoiceBox<String> filterChoiceBox;

    public void initialize() {
        filterChoiceBox.setItems(filterList);
    }

    public void goBack() {
        String filterType = filterChoiceBox.getValue();
        String userListString = userListField.getText();
        String itemListString = itemListField.getText();

        Main.conf.set("rec.filter.class", filterType);
        Main.conf.set("rec.filter.userlist", userListString);
        Main.conf.set("rec.filter.itemlist", itemListString);

        Main.showMainScene();
    }

    @FXML
    public void returnToMain() {
        Main.showMainScene();
    }
}
