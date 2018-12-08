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
public class BaseContextRatingDataEntry extends BaseRatingDataEntry {
    private int[][] contexts;
    public BaseContextRatingDataEntry(int userId, int[] itemIdsArray, int[][] contexts) {
        super(userId, itemIdsArray);
        this.contexts = contexts;
    }

    public int[][] getContexts() {
        return contexts;
    }

    public void setContexts(int[][] contexts) {
        this.contexts = contexts;
    }

    public int[] getContextAtPosition(int position){
        return contexts[position];
    }
}
