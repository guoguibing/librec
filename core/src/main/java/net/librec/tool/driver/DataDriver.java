/**
 * Copyright (C) 2016 LibRec
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
package net.librec.tool.driver;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import net.librec.conf.Configuration;
import net.librec.data.model.TextDataModel;
import net.librec.tool.LibrecTool;

/**
 * DataDriver
 * 
 * @author KEVIN
 */
public class DataDriver implements LibrecTool {

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
			for (String name : prop.stringPropertyNames()){
				conf.set(name, prop.getProperty(name));
			}
		}
		if (cmd.hasOption("jobconf")) {
			String arg = cmd.getOptionValue("jobconf");
			String[] keyValuePair = arg.split("=");
			conf.set(keyValuePair[0], keyValuePair[1]);
		}
		if (cmd.hasOption("D")) {
			String arg = cmd.getOptionValue("D");
			String[] keyValuePair = arg.split("=");
			conf.set(keyValuePair[0], keyValuePair[1]);
		}
		TextDataModel dataModel  = new TextDataModel(conf);
		dataModel.buildDataModel();
		System.out.println("well done!!!");
		return 0;
	}

	/**
	 * @param args
	 * @throws Exception 
	 */
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
