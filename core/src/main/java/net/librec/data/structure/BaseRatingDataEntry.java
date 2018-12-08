/**
 * Copyright (C) 2016 LibRec
 * <p>
 * This file is part of LibRec.
 * LibRec is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * LibRec is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with LibRec. If not, see <http://www.gnu.org/licenses/>.
 */

package net.librec.data.structure;

/**
 * Data Entry
 *
 * @author Keqiang Wang (sei.wkq2008@gmail.com)
 */
public class BaseRatingDataEntry extends AbstractBaseDataEntry  {
    /**
     * user id
     */
    private int userId;

    /**
     * item id
     */
    private int[] itemIdsArray;

    public BaseRatingDataEntry(int userId, int[] itemIdsArray) {
        this.userId = userId;
        this.itemIdsArray = itemIdsArray;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int[] getItemIdsArray() {
        return itemIdsArray;
    }

    public void setItemIdsArray(int[] itemIdsArray) {
        this.itemIdsArray = itemIdsArray;
    }

    @Override
    public int number() {
        return 1;
    }
}
