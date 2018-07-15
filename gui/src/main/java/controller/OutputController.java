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
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import net.librec.conf.Configuration;
import net.librec.job.RecommenderJob;
import util.MyCustomAppender;

import java.io.FileInputStream;
import java.util.Properties;

/**
 * <tt>OutputController</tt> controls the functions of the output scene.
 *
 * @author SunYatong
 */
public class OutputController {

    // protected final Log LOG = LogFactory.getLog(OutputController.class);

    @FXML
    TextField confPath;

    @FXML
    TextArea trainLog;


    public void initialize() {
        MyCustomAppender.logTextArea = trainLog;
    }

    @FXML
    public void execRec() throws Exception {

        Configuration conf = Main.conf;
        RecommenderJob job = new RecommenderJob(conf);

        job.runJob();
        conf.setBoolean("data.convert.read.ready", false);
    }

    @FXML
    public void goBack() {
        Main.showMainScene();
    }

    @FXML
    public void loadConf() throws Exception {
        String confFilePath = confPath.getText();
        Properties prop = new Properties();
        prop.load(new FileInputStream(confFilePath));
        for (String name : prop.stringPropertyNames()) {
            Main.conf.set(name, prop.getProperty(name));
        }
    }

}
