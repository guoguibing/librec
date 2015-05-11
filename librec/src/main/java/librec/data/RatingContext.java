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

/**
 * Rating-related Context Information
 * 
 * @author guoguibing
 * 
 */
public class RatingContext extends Context implements Comparable<RatingContext> {

	// rating time stamp, we prefer long to Date or Timestamp for computational convenience  
	private long timestamp;

	// location when giving ratings
	private String location;

	// accompany user id
	private int accompany;

	// mood when giving rating
	private String mood;

	/**
	 * @param user
	 *            user id
	 * @param item
	 *            item id
	 */
	public RatingContext(int user, int item) {
		super(user, item);
	}

	public RatingContext(int user, int item, long timestamp) {
		this(user, item);
		this.timestamp = timestamp;
	}

	/**
	 * @return the location
	 */
	public String getLocation() {
		return location;
	}

	/**
	 * @param location
	 *            the location to set
	 */
	public void setLocation(String location) {
		this.location = location;
	}

	/**
	 * @return the accompany
	 */
	public int getAccompany() {
		return accompany;
	}

	/**
	 * @param accompany
	 *            the accompany to set
	 */
	public void setAccompany(int accompany) {
		this.accompany = accompany;
	}

	/**
	 * @return the mood
	 */
	public String getMood() {
		return mood;
	}

	/**
	 * @param mood
	 *            the mood to set
	 */
	public void setMood(String mood) {
		this.mood = mood;
	}

	/**
	 * @return the timestamp in million seconds
	 */
	public long getTimestamp() {
		return timestamp;
	}

	/**
	 * @param timestamp
	 *            the timestamp in million seconds
	 */
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	@Override
	public int compareTo(RatingContext that) {
		double res = this.timestamp - that.timestamp;

		if (res > 0)
			return 1;
		else if (res < 0)
			return -1;

		return 0;
	}

}
