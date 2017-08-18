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

package net.librec.util;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import net.librec.math.algorithm.Maths;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;

/**
 * String Utility Class
 *
 * @author Guo Guibing
 */
public class StringUtil {
    public static final String EMPTY = "";
    private static String SEP = "\n";
    private static final DecimalFormat intFormatter = new DecimalFormat("#,###");
    final public static String[] emptyStringArray = {};

    /**
     * The maximum size to which the padding constant(s) can expand.
     */
    private static final int PAD_LIMIT = 8192;

    /**
     * get the last substring of string str with maximum length
     *
     * @param str       source string
     * @param maxLength maximum length of strings
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
     * @param objs the objects to be concatenated
     * @param sep  the separator between strings
     * @return the concatenated strings
     */
    public static String toString(Object[] objs, String sep) {
        return Joiner.on(sep).skipNulls().join(objs);
    }

    /**
     * default sep="," between all objects
     *
     * @param strings  the strings to be concatenated
     * @return  the concatenated strings
     */
    public static String toString(Object[] strings) {
        return toString(strings, ", ");
    }

    /**
     * Returns padding using the specified delimiter repeated to a given length.
     * <pre>
     * StringUtils.repeat(0, 'e')  = ""
     * StringUtils.repeat(3, 'e')  = "eee"
     * StringUtils.repeat(-2, 'e') = ""
     * </pre>
     * <p>
     * Note: this method doesn't not support padding with <a
     * href="http://www.unicode.org/glossary/#supplementary_character">Unicode Supplementary Characters</a> as they
     * require a pair of {@code char}s to be represented. If you are needing to support full I18N of your applications
     * consider using {@link #repeat(String, int)} instead.
     *
     * @param ch     character to repeat
     * @param repeat number of times to repeat char, negative treated as zero
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
     * Repeat a String {@code repeat} times to form a new String.
     * <pre>
     * StringUtils.repeat(null, 2) = null
     * StringUtils.repeat("", 0)   = ""
     * StringUtils.repeat("", 2)   = ""
     * StringUtils.repeat("a", 3)  = "aaa"
     * StringUtils.repeat("ab", 2) = "abab"
     * StringUtils.repeat("a", -2) = ""
     * </pre>
     *
     * @param str    the String to be repeated, may be null
     * @param repeat number of times to repeat {@code str}, negative treated as zero
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

    /**
     * Parse a {@code double} data into string
     *
     * @param data the input data
     * @return string data
     */
    public static String toString(double data) {
        return toString(data, 4);
    }

    /**
     * Parse a {@code long} data into string
     *
     * @param data the input data
     * @return string data
     */
    public static String toString(long data) {
        return intFormatter.format(data);
    }

    /**
     * Parse a {@code double[][]} data into string
     *
     * @param data the input data
     * @return string data
     */
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

    /**
     * Parse a {@code int[][]} data into string
     *
     * @param data the input data
     * @return string data
     */
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

    /**
     * Parse a {@code Number} data into string
     *
     * @param data the input data
     * @param bits number of bit
     * @return string data
     */
    public static String toString(Number data, int bits) {
        double val = data.doubleValue();
        if (Maths.isInt(val))
            return (int) val + "";

        String format = "%." + bits + "f";
        return String.format(format, val);
    }

    /**
     * Split the given string {@code str} into a list of strings with
     * separated by {@code reg}
     *
     * @param str a given string
     * @param reg the separator
     * @return a list of strings
     */
    public static List<String> toList(String str, String reg) {
        Iterable<String> iter = Splitter.on(reg).omitEmptyStrings().trimResults().split(str);

        return Lists.newArrayList(iter);
    }

    /**
     * Return a subset of the string with given length 50.
     *
     * @param input the input string
     * @return      the sub string
     */
    public static String shortStr(String input) {
        return shortStr(input, 50);
    }

    /**
     * Return a subset of the string with given length.
     *
     * @param input the input string
     * @param len   the length of the sub string
     * @return      the sub string
     */
    public static String shortStr(String input, int len) {
        int begin = 0;
        if (input.length() > len)
            begin = input.length() - len;

        return input.substring(begin);
    }


    /**
     * Parse a {@code Collection<T>} data into string
     *
     * @param ts   the input data
     * @param <T>  type parameter
     * @return  string of the input data
     */
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

    /**
     * Parse a {@code Collection<T>} data into string
     *
     * @param ts   the input data
     * @param <T>  type parameter
     * @param sep  separator
     * @return  string of the input data
     */
    public static <T> String toString(Collection<T> ts, String sep) {
        return Joiner.on(sep).skipNulls().join(ts);
    }

    /**
     * Parse a {@code Map<K, V>} data into string
     *
     * @param map the input data
     * @param <K> type parameter
     * @param <V> type parameter
     * @return  string of the input data
     */
    public static <K, V> String toString(Map<K, V> map) {
        return toString(map, "\n");
    }

    /**
     * Parse a {@code Map<K, V>} data into string
     *
     * @param map the input data
     * @param <K> type parameter
     * @param <V> type parameter
     * @param sep separator
     * @return  string of the input data
     */
    public static <K, V> String toString(Map<K, V> map, String sep) {
        return Joiner.on(sep).withKeyValueSeparator(" -> ").join(map);
    }

    /**
     * Parse a {@code double[]} data into string
     *
     * @param data the input data
     * @return string of the input data
     */
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

    /**
     * Parse a {@code int[]} data into string
     *
     * @param data the input data
     * @return string of the input data
     */
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
     *
     * @param msgs a list of messages
     * @return  a section of message
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

        String stars = StringUtil.repeat('*', repeat);
        String head = "\n/*" + stars + "\n";
        sb.insert(0, head);

        String tail = " *" + stars + "/";
        sb.append(tail);

        return sb.toString();
    }

    /**
     * Returns an arraylist of strings.
     *
     * @param str the comma seperated string values
     * @return the arraylist of the comma seperated string values
     */
    public static String[] getStrings(String str) {
        Collection<String> values = getStringCollection(str);
        if (values.size() == 0) {
            return null;
        }
        return values.toArray(new String[values.size()]);
    }

    /**
     * Returns a collection of strings.
     *
     * @param str comma seperated string values
     * @return an <code>ArrayList</code> of string values
     */
    public static Collection<String> getStringCollection(String str) {
        String delim = ",";
        return getStringCollection(str, delim);
    }

    /**
     * Returns a collection of strings.
     *
     * @param str   String to parse
     * @param delim delimiter to separate the values
     * @return Collection of parsed elements.
     */
    public static Collection<String> getStringCollection(String str, String delim) {
        List<String> values = new ArrayList<String>();
        if (str == null)
            return values;
        StringTokenizer tokenizer = new StringTokenizer(str, delim);
        while (tokenizer.hasMoreTokens()) {
            values.add(tokenizer.nextToken());
        }
        return values;
    }

    /**
     * Given an array of strings, return a comma-separated list of its elements.
     *
     * @param strs Array of strings
     * @return Empty string if strs.length is 0, comma separated list of strings
     * otherwise
     */
    public static String arrayToString(String[] strs) {
        if (strs.length == 0) {
            return "";
        }
        StringBuilder sbuf = new StringBuilder();
        sbuf.append(strs[0]);
        for (int idx = 1; idx < strs.length; idx++) {
            sbuf.append(",");
            sbuf.append(strs[idx]);
        }
        return sbuf.toString();
    }

    /**
     * Given an array of int, return a comma-separated list of its elements.
     *
     * @param ints Array of int
     * @return Empty string if strs.length is 0, comma separated list of strings
     * otherwise
     */
    public static String arrayToString(int[] ints) {
        if (ints.length == 0) {
            return "";
        }
        StringBuilder sbuf = new StringBuilder();
        sbuf.append(ints[0]);
        for (int idx = 1; idx < ints.length; idx++) {
            sbuf.append(",");
            sbuf.append(ints[idx]);
        }
        return sbuf.toString();
    }

    /**
     * Splits a comma separated value <code>String</code>, trimming leading and trailing whitespace on each value.
     *
     * @param str a comma separated {@code String} with values
     * @return an array of <code>String</code> values
     */
    public static String[] getTrimmedStrings(String str) {
        if (null == str || str.trim().isEmpty()) {
            return emptyStringArray;
        }

        return str.trim().split("\\s*,\\s*");
    }

}
