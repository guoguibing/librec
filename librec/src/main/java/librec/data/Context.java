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
 * Interface for contextual information.
 * 
 * <p>
 * Context information will be stored in the contextTable <UserId, ItemId, Context>, corresponding to dataTable <UserId,
 * ItemId, Rating>. If only user context is used, contextTable is reduced to <UserId, -1, UserContext>; If only item
 * context is used, contextTable is reduced to <-1, ItemId, ItemContext>. In this way, machine memory can be saved.
 * </p>
 * 
 * @author guoguibing
 * 
 */
public abstract class Context {

	/* context related user */
	private int user;
	/* context related item */
	private int item;

	public Context(int user, int item) {
		this.user = user;
		this.item = item;
	}

	/**
	 * @return the user
	 */
	public int getUser() {
		return user;
	}

	/**
	 * @param user
	 *            the user to set
	 */
	public void setUser(int user) {
		this.user = user;
	}

	/**
	 * @return the item
	 */
	public int getItem() {
		return item;
	}

	/**
	 * @param item
	 *            the item to set
	 */
	public void setItem(int item) {
		this.item = item;
	}

}
