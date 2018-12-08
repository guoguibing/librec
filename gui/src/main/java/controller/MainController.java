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

import javafx.fxml.FXML;

/**
 * <tt>MainController</tt> controls the functions of the main scene.
 *
 * @author SunYatong
 */
public class MainController {

    @FXML
    private void goDataModel() {
        Main.showDataModelScene();
    }

    @FXML
    private void goEval() {
        Main.showEvalScene();
    }

    @FXML
    private void goFilter() {
        Main.showFilterScene();
    }

    @FXML
    private void goOutput() {
        Main.showOutputScene();
    }

    @FXML
    private void goRecommeder() {
        Main.showRecommenderScene();
    }

    @FXML
    private void goSimilarity() {
        Main.showSimilarityScene();
    }

    @FXML
    private void switchToChinese() {
        Main mainStage = new Main();
        mainStage.switchToChinese();
    }

    @FXML
    private void switchToEnglish() {
        Main mainStage = new Main();
        mainStage.switchToEngish();
    }

}
