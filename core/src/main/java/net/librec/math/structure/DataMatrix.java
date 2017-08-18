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
package net.librec.math.structure;

/**
 * Data Matrix
 *
 * @author WangYuFeng
 */
public interface DataMatrix extends DataSet {

    /**
     * Set a value to entry [row, column]
     *
     * @param row    row id
     * @param column column id
     * @param val    value to set
     */
    public void set(int row, int column, double val);

    /**
     * Retrieve value at entry [row, column]
     *
     * @param row    row id
     * @param column column id
     * @return value at entry [row, column]
     */
    public double get(int row, int column);
}
