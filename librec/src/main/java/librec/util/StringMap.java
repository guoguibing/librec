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

/**
 * Abstract class that uses <string, string> as <key, value> parameter-value map
 * 
 * @author Guo Guibing
 *
 */
public abstract class StringMap {

	public abstract String getString(String key);

	public String getString(String key, String val) {
		String value = getString(key);

		return value == null ? val : value;
	}

	public float getFloat(String key) {
		return Strings.toFloat(getString(key));
	}

	public float getFloat(String key, float val) {
		return Strings.toFloat(getString(key), val);
	}

	public int getInt(String key) {
		return Strings.toInt(getString(key));
	}

	public int getInt(String key, int val) {
		return Strings.toInt(getString(key), val);
	}

	public double getDouble(String key) {
		return Strings.toDouble(getString(key));
	}

	public double getDouble(String key, double val) {
		return Strings.toDouble(getString(key), val);
	}
	
	public long getLong(String key){
		return Strings.toLong(key);
	}
	
	public long getLong(String key, long val){
		return Strings.toLong(getString(key), val);
	}

	public boolean isOn(String key) {
		return Strings.isOn(getString(key));
	}

	public boolean isOn(String key, boolean on) {
		String value = getString(key);

		return value != null ? Strings.isOn(value) : on;
	}

	public abstract boolean contains(String key);

}
