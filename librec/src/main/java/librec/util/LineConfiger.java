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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A configer for a line of string
 * 
 * @author Guo Guibing
 *
 */
public class LineConfiger extends StringMap {

	private Map<String, List<String>> params = null;
	private static final String headKey = "main.paramater";

	public LineConfiger(String setup) {
		this(setup.split("[,\t ]"));
	}

	public LineConfiger(String[] parameters) {
		params = new HashMap<>();

		// parameter head
		int i = 0;
		String head = parameters[i];
		if (!(head.startsWith("-") || head.startsWith("--"))) {
			params.put(headKey, Arrays.asList(head));
			i++;
		}

		// parameter options
		List<String> vals = null;
		for (; i < parameters.length; i++) {
			boolean isString = !Maths.isNumeric(parameters[i]);
			boolean isWithDash = parameters[i].startsWith("-") || parameters[i].startsWith("--");
			// remove cases like -1, -2 values
			if (isWithDash && isString) {
				vals = new ArrayList<>();
				params.put(parameters[i], vals);
			} else {
				vals.add(parameters[i]);
			}
		}
	}

	public List<String> getOptions(String key) {
		return params.containsKey(key) ? params.get(key) : null;
	}

	public String getMainParam() {
		return getString(headKey);
	}

	public boolean isMainOn() {
		return Strings.isOn(getMainParam());
	}

	public String getString(String key) {
		List<String> options = this.getOptions(key);

		if (options != null && options.size() > 0)
			return options.get(0);

		return null;
	}

	@Override
	public boolean contains(String key) {
		return params.containsKey(key);
	}

}
