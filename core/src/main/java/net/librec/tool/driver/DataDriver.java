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
package net.librec.tool.driver;

import net.librec.conf.Configuration;
import net.librec.data.model.TextDataModel;
import net.librec.tool.LibrecTool;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;

import java.io.FileInputStream;
import java.util.Properties;

/**
 * DataDriver
 *
 * @author WangYuFeng
 */
public class DataDriver implements LibrecTool {

    /**
     * Execute the command with the given arguments.
     *
     * @param args command specific arguments.
     * @return exit code.
     * @throws Exception if error occurs
     */
    public int run(String[] args) throws Exception {
        // init options
        Options options = new Options();
        options.addOption("build", false, "build model");
        options.addOption("load", false, "load model");
        options.addOption("save", false, "save model");
        options.addOption("conf", true, "the path of configuration file");
        options.addOption("jobconf", true, "a specified key-value pair for configuration");
        options.addOption("D", true, "a specified key-value pair for configuration");
        // parse options
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args, false);
        // init configuration
        Configuration conf = new Configuration();
        if (cmd.hasOption("conf")) {
            String confFilePath = cmd.getOptionValue("conf");
            Properties prop = new Properties();
            prop.load(new FileInputStream(confFilePath));
            for (String name : prop.stringPropertyNames()) {
                conf.set(name, prop.getProperty(name));
            }
        }
        if (cmd.hasOption("jobconf")) {
            String[] optionValues = cmd.getOptionValues("jobconf");
            for (String optionValue : optionValues) {
                String[] keyValuePair = optionValue.split("=");
                conf.set(keyValuePair[0], keyValuePair[1]);
            }
        }
        if (cmd.hasOption("D")) {
            String[] optionValues = cmd.getOptionValues("D");
            for (String optionValue : optionValues) {
                String[] keyValuePair = optionValue.split("=");
                conf.set(keyValuePair[0], keyValuePair[1]);
            }
        }
        TextDataModel dataModel = new TextDataModel(conf);
        dataModel.buildDataModel();
        System.out.println("well done!!!");
        return 0;
    }

    public static void main(String[] args) throws Exception {
        LibrecTool tool = new DataDriver();

        Options options = new Options();
        options.addOption("build", false, "build model");
        options.addOption("load", false, "load model");
        options.addOption("save", false, "save model");
        options.addOption("conf", true, "the path of configuration file");
        options.addOption("jobconf", true, "a specified key-value pair for configuration");
        options.addOption("D", true, "a specified key-value pair for configuration");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption("build")) {
            tool.run(args);
        } else if (cmd.hasOption("load")) {
            ;
        } else if (cmd.hasOption("save")) {
            ;
        }
    }

}
