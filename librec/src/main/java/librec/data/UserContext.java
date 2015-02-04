// Copyright (C) 2014 Guibing Guo
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
package librec.data;

import java.util.HashMap;
import java.util.Map;

/**
 * User-related Contextual Information
 * 
 * @author guoguibing
 * 
 */
public class UserContext extends Context {

	/* user's social relations */
	private Map<Integer, Double> socialMap;

	/**
	 * @param user
	 *            user id
	 * @param item
	 *            fixed as -1
	 */
	private UserContext(int user, int item) {
		super(user, -1);
	}

	public UserContext(int user) {
		this(user, -1);
	}

	/**
	 * add social relations
	 * 
	 * @param user
	 *            user id socially related with
	 * @param val
	 *            social strength with this user
	 */
	public void addSocial(int user, double val) {
		if (socialMap == null)
			socialMap = new HashMap<>();

		socialMap.put(user, val);
	}

	/**
	 * get the strength of a social relation
	 * 
	 * @param user
	 *            user id
	 * @return social strength
	 */
	public double getSocial(int user) {
		double val = Double.NaN;

		if (socialMap != null && socialMap.containsKey(user))
			return socialMap.get(user);

		return val;
	}

	/**
	 * @return the socialMap
	 */
	public Map<Integer, Double> getSocialMap() {
		return socialMap;
	}

}
