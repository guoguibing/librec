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
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;

/**
 * <tt>EvaluatorController</tt> controls the configuration of <tt>Evaluator</tt>.
 *
 * @author SunYatong
 */
public class EvaluatorController {

    @FXML
    ToggleButton enableEvalButton;

    @FXML
    RadioButton rankingEvalButton;

    @FXML
    RadioButton ratingEvalButton;

    @FXML
    CheckBox aucBox;

    @FXML
    CheckBox precisionBox;

    @FXML
    CheckBox recallBox;

    @FXML
    CheckBox ndcgBox;

    @FXML
    CheckBox hrBox;

    @FXML
    CheckBox rrBox;

    @FXML
    CheckBox maeBox;

    @FXML
    CheckBox rmseBox;

    @FXML
    CheckBox mseBox;

    @FXML
    CheckBox mpeBox;

    @FXML
    TextField topNText;


    @FXML
    public void goBack() {
        Boolean enableEval = enableEvalButton.isSelected();
        Boolean rankingSelected = rankingEvalButton.isSelected();
        Boolean ratingSelected = ratingEvalButton.isSelected();
        Boolean aucSelected = aucBox.isSelected();
        Boolean precisionSelected = precisionBox.isSelected();
        Boolean recallSelected = recallBox.isSelected();
        Boolean ndcgSelected = ndcgBox.isSelected();
        Boolean hrSelected = hrBox.isSelected();
        Boolean rrSelected = rrBox.isSelected();
        Boolean maeSelected = maeBox.isSelected();
        Boolean rmseSelected = rmseBox.isSelected();
        Boolean mseSelected = mseBox.isSelected();
        Boolean mpeSelected = mpeBox.isSelected();

        Main.conf.setBoolean("rec.eval.enable", enableEval);
        Main.conf.setBoolean("rec.recommender.isranking", rankingSelected);

        StringBuilder evals = new StringBuilder();
        if (rankingSelected) {
            Main.conf.setInt("rec.recommender.ranking.topn", Integer.parseInt(topNText.getText()));
            if (aucSelected) {
                evals.append("auc").append(" ");
            }
            if (precisionSelected) {
                evals.append("precision").append(" ");
            }
            if (recallSelected) {
                evals.append("recall").append(" ");
            }
            if (ndcgSelected) {
                evals.append("ndcg").append(" ");
            }
            if (hrSelected) {
                evals.append("hr").append(" ");
            }
            if (rrSelected) {
                evals.append("rr");
            }
        }
        if (ratingSelected) {
            if (maeSelected) {
                evals.append("mae").append(" ");
            }
            if (rmseSelected) {
                evals.append("rmse").append(" ");
            }
            if (mseSelected) {
                evals.append("mse").append(" ");
            }
            if (mpeSelected) {
                evals.append("mpe").append(" ");
            }
        }
        if (enableEval) {
            Main.conf.set("rec.eval.classes", evals.toString().trim());
        }

        Main.showMainScene();
    }

    @FXML
    public void returnToMain() {
        Main.showMainScene();
    }
}
