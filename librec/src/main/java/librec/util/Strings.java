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

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import librec.util.FileIO.Converter;
import librec.util.FileIO.MapWriter;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;

/**
 * String Utility Class
 * 
 * @author gbguo
 *
 */
public class Strings {
	public static final String HELLO = "Hello World!";
	public static final String EMPTY = "";

	private static String separator = "\n";
	private static final DecimalFormat intFormatter = new DecimalFormat("#,###");

	/**
	 * <p>
	 * The maximum size to which the padding constant(s) can expand.
	 * </p>
	 */
	private static final int PAD_LIMIT = 8192;

	/**
	 * get the last substring of string str with maximum length
	 * 
	 * @param str
	 *            source string
	 * @param maxLength
	 *            maximum length of strings
	 * @return the last substring of string str with maximum length; if greater; then "..." is padded to start position
	 */
	public static String last(String str, int maxLength) {
		if (str.length() + 3 <= maxLength)
			return str;
		return "..." + str.substring(str.length() - maxLength + 3);
	}

	public static float toFloat(String str) {
		return Float.parseFloat(str);
	}

	public static float toFloat(String str, float val) {
		return str != null ? Float.parseFloat(str) : val;
	}

	public static int toInt(String str) {
		return Integer.parseInt(str);
	}

	public static int toInt(String str, int val) {
		return str != null ? Integer.parseInt(str) : val;
	}

	public static long toLong(String str) {
		return Long.parseLong(str);
	}

	public static long toLong(String str, long val) {
		return str != null ? Long.parseLong(str) : val;
	}

	public static double toDouble(String str) {
		return Double.parseDouble(str);
	}

	public static double toDouble(String str, double val) {
		return str != null ? Double.parseDouble(str) : val;
	}

	public static void toClipboard(String data) throws Exception {
		Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
		StringSelection ss = new StringSelection(data);
		cb.setContents(ss, ss);
	}

	public static boolean isOn(String option) {
		switch (option.toLowerCase()) {
		case "on":
		case "true":
			return true;

		case "off":
		case "false":
		default:
			return false;
		}
	}

	/**
	 * Concatenates an array of string
	 * 
	 * @param objs
	 *            the objects to be concatenated
	 * @param sep
	 *            the separator between strings
	 * @return the concatenated strings
	 */
	public static String toString(Object[] objs, String sep) {
		return Joiner.on(sep).skipNulls().join(objs);
	}

	/**
	 * default sep="," between all objects
	 * 
	 */
	public static String toString(Object[] strings) {
		return toString(strings, ", ");
	}

	/**
	 * <p>
	 * Returns padding using the specified delimiter repeated to a given length.
	 * </p>
	 * 
	 * <pre>
	 * StringUtils.repeat(0, 'e')  = ""
	 * StringUtils.repeat(3, 'e')  = "eee"
	 * StringUtils.repeat(-2, 'e') = ""
	 * </pre>
	 * 
	 * <p>
	 * Note: this method doesn't not support padding with <a
	 * href="http://www.unicode.org/glossary/#supplementary_character">Unicode Supplementary Characters</a> as they
	 * require a pair of {@code char}s to be represented. If you are needing to support full I18N of your applications
	 * consider using {@link #repeat(String, int)} instead.
	 * </p>
	 * 
	 * @param ch
	 *            character to repeat
	 * @param repeat
	 *            number of times to repeat char, negative treated as zero
	 * @return String with repeated character
	 * @see #repeat(String, int)
	 */
	public static String repeat(char ch, int repeat) {
		char[] buf = new char[repeat];
		for (int i = repeat - 1; i >= 0; i--) {
			buf[i] = ch;
		}
		return new String(buf);
	}

	/**
	 * <p>
	 * Repeat a String {@code repeat} times to form a new String.
	 * </p>
	 * 
	 * <pre>
	 * StringUtils.repeat(null, 2) = null
	 * StringUtils.repeat("", 0)   = ""
	 * StringUtils.repeat("", 2)   = ""
	 * StringUtils.repeat("a", 3)  = "aaa"
	 * StringUtils.repeat("ab", 2) = "abab"
	 * StringUtils.repeat("a", -2) = ""
	 * </pre>
	 * 
	 * @param str
	 *            the String to be repeated, may be null
	 * @param repeat
	 *            number of times to repeat {@code str}, negative treated as zero
	 * @return a new String consisting of the original String repeated, {@code null} if null String input
	 */
	public static String repeat(String str, int repeat) {
		if (str == null) {
			return null;
		}
		if (repeat <= 0) {
			return EMPTY;
		}
		int inputLength = str.length();
		if (repeat == 1 || inputLength == 0) {
			return str;
		}
		if (inputLength == 1 && repeat <= PAD_LIMIT) {
			return repeat(str.charAt(0), repeat);
		}

		int outputLength = inputLength * repeat;
		switch (inputLength) {
		case 1:
			return repeat(str.charAt(0), repeat);
		case 2:
			char ch0 = str.charAt(0);
			char ch1 = str.charAt(1);
			char[] output2 = new char[outputLength];
			for (int i = repeat * 2 - 2; i >= 0; i--, i--) {
				output2[i] = ch0;
				output2[i + 1] = ch1;
			}
			return new String(output2);
		default:
			StringBuilder buf = new StringBuilder(outputLength);
			for (int i = 0; i < repeat; i++) {
				buf.append(str);
			}
			return buf.toString();
		}
	}

	public static String toString(double data) {
		return toString(data, 4);
	}

	public static String toString(long data) {
		return intFormatter.format(data);
	}

	public static String toString(double[][] data) {
		int rows = data.length;
		StringBuilder sb = new StringBuilder();
		sb.append("Dimension: " + rows + " x " + data[0].length + "\n");

		for (int i = 0; i < rows; i++) {
			sb.append("[");
			for (int j = 0; j < data[i].length; j++) {
				sb.append((float) data[i][j]);
				if (j < data[i].length - 1)
					sb.append("\t");
			}
			sb.append("]\n");
		}

		return sb.toString();
	}

	public static String toString(int[][] data) {
		int rows = data.length;
		StringBuilder sb = new StringBuilder();
		sb.append("Dimension: " + rows + " x " + data[0].length + "\n");

		for (int i = 0; i < rows; i++) {
			sb.append("[");
			for (int j = 0; j < data[i].length; j++) {
				sb.append(data[i][j]);
				if (j < data[i].length - 1)
					sb.append("\t");
			}
			sb.append("]\n");
		}

		return sb.toString();
	}

	public static String toString(Number data, int bits) {
		double val = data.doubleValue();
		if (Maths.isInt(val))
			return (int) val + "";

		String format = "%." + bits + "f";
		return String.format(format, val);
	}

	public static List<String> toList(String str, String reg) {
		Iterable<String> iter = Splitter.on(reg).omitEmptyStrings().trimResults().split(str);

		return Lists.newArrayList(iter);
	}

	public static String shortStr(String input) {
		return shortStr(input, 50);
	}

	public static String shortStr(String input, int len) {
		int begin = 0;
		if (input.length() > len)
			begin = input.length() - len;

		return input.substring(begin);
	}

	public static <T> String toString(Collection<T> ts) {

		if (ts instanceof Multiset<?>) {

			StringBuilder sb = new StringBuilder();
			Multiset<T> es = (Multiset<T>) ts;

			for (T e : es.elementSet()) {
				int count = es.count(e);
				sb.append(e + ", " + count + "\n");
			}

			return sb.toString();
		}

		return toString(ts, ",");
	}

	public static <T> String toString(Collection<T> ts, String sep) {
		return Joiner.on(sep).skipNulls().join(ts);
	}

	public static <T> String toString(Collection<T> ts, Converter<T, String> lw) throws Exception {
		if (ts == null || ts.size() == 0)
			return null;
		StringBuilder sb = new StringBuilder();

		int N = ts.size(), i = 0;
		for (T t : ts) {
			String line = lw != null ? lw.transform(t) : t.toString();

			sb.append(line);
			if (i++ < N - 1)
				sb.append(separator);
		}
		return sb.toString();
	}

	public static <K, V> String toString(Map<K, V> map) {
		return toString(map, "\n");
	}

	public static <K, V> String toString(Map<K, V> map, String sep) {
		return Joiner.on(sep).withKeyValueSeparator(" -> ").join(map);
	}

	public static <K, V> String toString(Map<K, V> map, MapWriter<K, V> mw) {
		StringBuilder sb = new StringBuilder();

		int size = map.size();
		int count = 0;
		for (Entry<K, V> en : map.entrySet()) {
			K key = en.getKey();
			V val = en.getValue();
			String line = mw != null ? mw.processEntry(key, val) : key + " -> " + val;

			sb.append(line);
			if (count++ < size - 1)
				sb.append(separator);
		}

		return sb.toString();
	}

	public static String toString(double[] data) {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (int i = 0; i < data.length; i++) {
			sb.append(toString(data[i]));
			if (i < data.length - 1)
				sb.append(", ");
		}
		sb.append("]");

		return sb.toString();
	}

	public static String toString(int[] data) {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (int i = 0; i < data.length; i++) {
			sb.append(data[i]);
			if (i < data.length - 1)
				sb.append(", ");
		}
		sb.append("]");

		return sb.toString();
	}

	/**
	 * convert to a section of message
	 */
	public static String toSection(List<String> msgs) {
		StringBuilder sb = new StringBuilder();

		int repeat = 50;

		sb.append(" *\n");
		for (String msg : msgs) {
			sb.append(" * " + msg + "\n");
			if (msg.length() > repeat)
				repeat = msg.length();
		}
		sb.append(" *\n");

		String stars = Strings.repeat('*', repeat);
		String head = "\n/*" + stars + "\n";
		sb.insert(0, head);

		String tail = " *" + stars + "/";
		sb.append(tail);

		return sb.toString();
	}


}
