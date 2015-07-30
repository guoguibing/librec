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

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * A configure class for .conf/.properties file
 * 
 * @author guoguibing
 * 
 */
public class FileConfiger extends StringMap {
	private Properties p = null;

	public FileConfiger(String conf) throws Exception {
		p = new Properties();
		p.load(new FileInputStream(FileIO.getResource(conf)));
	}

	public LineConfiger getParamOptions(String key) {
		String lineOptions = getString(key);
		return lineOptions == null ? null : new LineConfiger(lineOptions);
	}

	/**
	 * @return the key value as a trimmed string
	 * 
	 */
	public String getString(String key) {
		String str = p.getProperty(key);
		return str == null ? str : str.trim();
	}

	/**
	 * set a value to a specific key
	 * 
	 * @param key
	 *            property key
	 * @param val
	 *            property value
	 */
	public void setString(String key, String val) {
		p.setProperty(key, val);
	}

	/**
	 * @return the file IO path: supporting windows, linux and unix
	 */
	public String getPath(String key) {

		// first search key itself
		String path = getString(key);
		if (path != null)
			return path;

		// if not, considering the following cases
		switch (Systems.getOs()) {
		case Windows:
			return getString(key + ".wins");
		case Linux:
		case Mac:
		default:
			return getString(key + ".lins");
		}
	}

	/**
	 * return a set values in a specified range
	 * 
	 */
	public List<Float> getRange(String key) {

		// a set of values in two ways: one is individual value (e.g., "1", "0.5"); the other is range values in
		// the form of "a,b,c" (e.g, "0.5,0.8,0.6") *
		String delim = "[, \t]";
		String str = getString(key);
		StringTokenizer st = new StringTokenizer(str, delim);
		if (st.countTokens() > 1)
			return getMultiValues(str, delim);

		// regularized values: [min..step..max]: min, min+step, ..., max
		if (str.contains(".."))
			return getPlusValues(str);

		// regularized values: [min**step**max]: min, min*step, ..., max
		if (str.contains("**"))
			return getTimesValues(str);

		// single value only
		List<Float> res = new ArrayList<>();
		res.add(getFloat(key));

		return res;
	}

	private List<Float> getPlusValues(String str) {
		List<Float> vals = getMultiValues(str, "(\\.\\.)");
		if (vals.size() < 3)
			return vals;

		// value ranges
		float min = vals.get(0), step = vals.get(1), max = vals.get(2);
		vals.clear();

		if (min > max) {
			// inverse orer from max --> min
			while (min > max) {
				vals.add(min);
				min -= step;
			}
			vals.add(max);

		} else {

			while (min < max) {
				vals.add(min);
				min += step;
			}
			// no repeated values
			if (Math.abs(max + step - min) > 1e-6)
				vals.add(max);
		}
		return vals;
	}

	private List<Float> getTimesValues(String str) {

		List<Float> vals = getMultiValues(str, "(\\*\\*)");
		if (vals.size() < 3)
			return vals;

		// value ranges
		float min = vals.get(0), step = vals.get(1), max = vals.get(2);
		vals.clear();

		if (min > max) {
			// inverse orer from max --> min
			while (min > max) {
				vals.add(min);
				min /= step;
			}
			vals.add(max);

		} else {

			while (min < max) {
				vals.add(min);
				min *= step;
			}
			// no repeated values
			if (Math.abs(max + step - min) > 1e-6)
				vals.add(max);
		}
		return vals;
	}

	/**
	 * return a set of float values set for a key, separated by the string "reg"
	 * 
	 */
	public List<Float> getMultiValues(String val, String reg) {
		List<Float> values = new ArrayList<>();

		if (val != null) {
			String[] data = val.split(reg);

			for (int i = 0; i < data.length; i++) {
				values.add(new Float(data[i]));
			}
		}

		return values;
	}

	@Override
	public boolean contains(String key) {
		return p.containsKey(key);
	}

}
