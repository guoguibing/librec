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
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.cli.*;

import net.librec.common.LibrecException;
import net.librec.conf.Configuration;
import net.librec.job.RecommenderJob;
import net.librec.tool.LibrecTool;

/**
 * RecDriver
 *
 * @author WangYuFeng
 */
public class RecDriver implements LibrecTool {

	/**
	 * Execute the command with the given arguments.
	 *
	 * @param args
	 *            command specific arguments.
	 * @return
	 * @throws LibrecException
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	public int run(String[] args) throws LibrecException, ClassNotFoundException, IOException, ParseException {
		// init options
		Options options = new Options();
		options.addOption("build", false, "build model");
		options.addOption("load", false, "load model");
		options.addOption("save", false, "save model");
		options.addOption("exec", false, "run job");
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
		//run job
		RecommenderJob job = new RecommenderJob(conf);
		job.runJob();
		System.out.print("Finished");
		return 0;
	}

	/**
	 * Parse the given arguments and invoke the corresponding method.
	 *
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		LibrecTool tool = new RecDriver();

		Options options = new Options();
		options.addOption("build", false, "build model");
		options.addOption("load", false, "load model");
		options.addOption("save", false, "save model");
		options.addOption("exec", false, "run job");
		options.addOption("conf", true, "the path of configuration file");
		options.addOption("jobconf", true, "a specified key-value pair for configuration");
		options.addOption("D", true, "a specified key-value pair for configuration");

		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse(options, args);

		if (cmd.hasOption("build")) {
			;
		} else if (cmd.hasOption("load")) {
			;
		} else if (cmd.hasOption("save")) {
			;
		} else if (cmd.hasOption("exec")) {
			tool.run(args);
		}
	}
}
