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
package net.librec.util;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.librec.conf.Configuration;

/**
 * GenericOptionsParser
 * 
 * @author WangYuFeng
 */
public class GenericOptionsParser {

	private static final Log LOG = LogFactory.getLog(GenericOptionsParser.class);
	private Configuration conf;
	private CommandLine commandLine;

	/**
	 * Create an options parser with the given options to parse the args.
	 * 
	 * @param opts
	 *            the options
	 * @param args
	 *            the command line arguments
	 * @throws IOException
	 */
	public GenericOptionsParser(Options opts, String[] args) throws IOException {
		this(new Configuration(), new Options(), args);
	}

	/**
	 * Create an options parser to parse the args.
	 * 
	 * @param args
	 *            the command line arguments
	 * @throws IOException
	 */
	public GenericOptionsParser(String[] args) throws IOException {
		this(new Configuration(), new Options(), args);
	}

	/**
	 * Create a
	 * <code>GenericOptionsParser<code> to parse only the generic Hadoop  
	 * arguments. 
	 * 
	 * The array of string arguments other than the generic arguments can be 
	 * obtained by {@link #getRemainingArgs()}.
	 * 
	 * @param conf the <code>Configuration</code> to modify.
	 * 
	 * @param args
	 *            command-line arguments.
	 * @throws IOException
	 */
	public GenericOptionsParser(Configuration conf, String[] args) throws IOException {
		this(conf, new Options(), args);
	}

	/**
	 * Create a <code>GenericOptionsParser</code> to parse given options as well
	 * as generic Hadoop options.
	 * 
	 * The resulting <code>CommandLine</code> object can be obtained by
	 * {@link #getCommandLine()}.
	 * 
	 * @param conf
	 *            the configuration to modify
	 * @param options
	 *            options built by the caller
	 * @param args
	 *            User-specified arguments
	 * @throws IOException
	 */
	public GenericOptionsParser(Configuration conf, Options options, String[] args) throws IOException {
//		parseGeneralOptions(options, conf, args);
		this.conf = conf;
	}

	/**
	 * Returns an array of Strings containing only application-specific
	 * arguments.
	 * 
	 * @return array of <code>String</code>s containing the un-parsed arguments
	 *         or <strong>empty array</strong> if commandLine was not defined.
	 */
	public String[] getRemainingArgs() {
		return (commandLine == null) ? new String[] {} : commandLine.getArgs();
	}

	/**
	 * Get the modified configuration
	 * 
	 * @return the configuration that has the modified parameters.
	 */
	public Configuration getConfiguration() {
		return conf;
	}

	/**
	 * Returns the commons-cli <code>CommandLine</code> object to process the
	 * parsed arguments.
	 * 
	 * Note: If the object is created with
	 * {@link #GenericOptionsParser(Configuration, String[])}, then returned
	 * object will only contain parsed generic options.
	 * 
	 * @return <code>CommandLine</code> representing list of arguments parsed
	 *         against Options descriptor.
	 */
	public CommandLine getCommandLine() {
		return commandLine;
	}

	/**
	 * Specify properties of each generic option
	 */
	@SuppressWarnings("static-access")
	private static Options buildGeneralOptions(Options opts) {
		Options options = new Options();
		options.addOption("help", false, "Print help");
		options.addOption("service", true, "Execute which service");
		

		return opts;
	}
//
//	/**
//	 * Modify configuration according user-specified generic options
//	 * 
//	 * @param conf
//	 *            Configuration to be modified
//	 * @param line
//	 *            User-specified generic options
//	 */
//	private void processGeneralOptions(Configuration conf, CommandLine line) throws IOException {
//		if (line.hasOption("fs")) {
//			FileSystem.setDefaultUri(conf, line.getOptionValue("fs"));
//		}
//		if (line.hasOption("conf")) {
//			String[] values = line.getOptionValues("conf");
//			for (String value : values) {
//				conf.addResource(new Path(value));
//			}
//		}
//	}
//
//	/**
//	 * takes input as a comma separated list of files and verifies if they
//	 * exist. It defaults for file:/// if the files specified do not have a
//	 * scheme. it returns the paths uri converted defaulting to file:///. So an
//	 * input of /home/user/file1,/home/user/file2 would return
//	 * file:///home/user/file1,file:///home/user/file2
//	 * 
//	 * @param files
//	 * @return
//	 */
//
//	/**
//	 * Parse the user-specified options, get the generic options, and modify
//	 * configuration accordingly
//	 * 
//	 * @param conf
//	 *            Configuration to be modified
//	 * @param args
//	 *            User-specified arguments
//	 * @return Command-specific arguments
//	 */
//	private String[] parseGeneralOptions(Options opts, Configuration conf, String[] args) throws IOException {
//		opts = buildGeneralOptions(opts);
//		CommandLineParser parser = new DefaultParser();
//		try {
//			commandLine = parser.parse(opts, args, true);
//			processGeneralOptions(conf, commandLine);
//			return commandLine.getArgs();
//		} catch (ParseException e) {
//			LOG.warn("options parsing failed: " + e.getMessage());
//			HelpFormatter formatter = new HelpFormatter();
//			formatter.printHelp("general options are: ", opts);
//		}
//		return args;
//	}

	/**
	 * Print the usage message for generic command-line options supported.
	 * 
	 * @param out
	 *            stream to print the usage message to.
	 */
//	public static void printGenericCommandUsage(PrintStream out) {
//		HelpFormatter helpFormatter = new HelpFormatter();
//		CommandLineParser parser = new PosixParser();
//		try {
//			CommandLine cmd = parser.parse(options, args);
//			if (cmd.hasOption("help")) {
//				helpFormatter.printHelp("Alarm Executor", options, true);
//			}
//		} catch (ParseException e) {
//			helpFormatter.printHelp("Alarm Executor", options, true);
//		}
//	}

}
