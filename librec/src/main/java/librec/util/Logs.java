// Copyright (C) 2014-2015 Guibing Guo
//
// This file is part of LibRec.
//
// LibRec is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// LibRec is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with LibRec. If not, see <http://www.gnu.org/licenses/>.
//

package librec.util;

import org.apache.log4j.Level;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.xml.DOMConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Apply delegate mode
 * 
 * @author guoguibing
 * 
 */
public class Logs {
	private final static Logger logger = LoggerFactory.getLogger(Logs.class);
	private static String conf = null;

	// default configuration
	static {
		if ((conf = FileIO.getResource("log4j.properties")) != null)
			config(conf, false);
		else if ((conf = FileIO.getResource("log4j.xml")) != null)
			config(conf, true);
	}

	public static Logger config(String config, boolean isXml) {
		if (isXml)
			DOMConfigurator.configure(config);
		else
			PropertyConfigurator.configure(config);

		return logger;
	}

	public static void debug(double data) {
		logger.debug(Strings.toString(data));
	}

	public static void debug(Object msg) {
		logger.debug(msg.toString());
	}

	public static void debug(String msg) {
		logger.debug(msg);
	}

	public static void debug(String format, Object arg) {
		logger.debug(format, arg);
	}

	public static void debug(String format, Object... args) {
		logger.debug(format, args);
	}

	public static void debug() {
		debug("");
	}

	public static void error() {
		error("");
	}

	public static void warn() {
		warn("");
	}

	public static void info() {
		info("");
	}

	public static void info(double data) {
		logger.info(Strings.toString(data));
	}

	public static void info(Object msg) {
		if (msg == null)
			logger.info("");
		else
			logger.info(msg.toString());
	}

	public static void info(String format, Object arg) {
		logger.info(format, arg);
	}

	public static void info(String format, Object... args) {
		logger.info(format, args);
	}

	public static void error(double data) {
		logger.error(Strings.toString(data));
	}

	public static void error(Object msg) {
		logger.error(msg.toString());
	}

	public static void warn(String msg) {
		logger.warn(msg);
	}

	public static void warn(String format, Object arg) {
		logger.warn(format, arg);
	}

	public static void warn(String format, Object... args) {
		logger.warn(format, args);
	}

	public static void warn(double data) {
		logger.warn(Strings.toString(data));
	}

	public static void warn(Object msg) {
		logger.warn(msg.toString());
	}

	public static void error(String msg) {
		logger.error(msg);
	}

	public static void error(String format, Object arg) {
		logger.error(format, arg);
	}

	public static void error(String format, Object... args) {
		logger.error(format, args);
	}

	public static void off() {
		org.apache.log4j.Logger.getRootLogger().setLevel(Level.OFF);
	}

	public static void on() {
		org.apache.log4j.Logger.getRootLogger().setLevel(Level.DEBUG);
	}
}
