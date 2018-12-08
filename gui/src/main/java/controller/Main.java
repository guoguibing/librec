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

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import net.librec.conf.Configuration;
import org.apache.log4j.PropertyConfigurator;

import java.io.InputStream;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;

/**
 * <tt>Main</tt> consists all operations of this GUI project.
 *
 * @author SunYatong
 */
public class Main extends Application {

    static Stage window;
    static Scene mainScene, dataModelScene, similarityScene, recommenderScene, evaluatorScene, filterScene, outputScene;
    public static Configuration conf;

    @Override
    public void start(Stage primaryStage) throws Exception {
        // config log4j
        Properties loggingProperties = new Properties();
        loggingProperties.load( getClass().getResourceAsStream( "/log4j.properties") );
        PropertyConfigurator.configure( loggingProperties );
        // config I18N
        ResourceBundle resourceBundle = ResourceBundle.getBundle("internationalization", new Locale("zh", "CN"));

        buildStages(primaryStage, resourceBundle);
    }

    public void buildStages(Stage primaryStage, ResourceBundle resourceBundle) throws Exception {
        int width = 800, height = 459;

        window = primaryStage;

        
        Parent root = FXMLLoader.load(getClass().getResource("/main.fxml"), resourceBundle);
        window.setTitle("LibRec V2.0");

        mainScene = new Scene(root, width, height);
        window.setScene(mainScene);
        window.show();

        root = FXMLLoader.load(getClass().getResource("/dataModel.fxml"), resourceBundle);
        dataModelScene = new Scene(root, width, height);

        root = FXMLLoader.load(getClass().getResource("/evaluator.fxml"), resourceBundle);
        evaluatorScene = new Scene(root, width, height);

        root = FXMLLoader.load(getClass().getResource("/filter.fxml"), resourceBundle);
        filterScene = new Scene(root, width, height);

        root = FXMLLoader.load(getClass().getResource("/output_1.fxml"), resourceBundle);
        outputScene = new Scene(root, width, height);

        root = FXMLLoader.load(getClass().getResource("/recommender.fxml"), resourceBundle);
        recommenderScene = new Scene(root, width, height);

        root = FXMLLoader.load(getClass().getResource("/similarity.fxml"), resourceBundle);
        similarityScene = new Scene(root, width, height);

        conf = new Configuration();
    }

    public static void showDataModelScene() {
        window.setScene(dataModelScene);
        window.show();
    }

    public static void showEvalScene() {
        window.setScene(evaluatorScene);
        window.show();
    }

    public static void showFilterScene() {
        window.setScene(filterScene);
        window.show();
    }

    public static void showMainScene() {
        window.setScene(mainScene);
        window.show();
    }

    public static void showOutputScene() {
        window.setScene(outputScene);
        window.show();
    }

    public static void showRecommenderScene() {
        window.setScene(recommenderScene);
        window.show();
    }

    public static void showSimilarityScene() {
        window.setScene(similarityScene);
        window.show();
    }

    public void switchToChinese() {
        try {
            ResourceBundle resourceBundle = ResourceBundle.getBundle("internationalization", new Locale("zh", "CN"));
            buildStages(window, resourceBundle);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void switchToEngish() {
        try {
            ResourceBundle resourceBundle = ResourceBundle.getBundle("internationalization", new Locale("en", "EN"));
            buildStages(window, resourceBundle);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void exit() {
        window.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
