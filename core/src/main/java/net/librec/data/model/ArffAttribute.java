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
package net.librec.data.model;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * A <tt>ArffAttribute</tt> is a class to represent
 * attribute of ARFF format input.
 *
 * @author Tang Jiaxi and Ma Chen
 */
public class ArffAttribute {

    /** valid types of attribute */
    private static final Set<String> VALID_TYPES = new HashSet<>(Arrays.asList(
            new String[]{"NUMERIC", "REAL", "INTEGER", "STRING", "NOMINAL"}
    ));

    /** attribute name */
    private String name;

    /** attribute type */
    private String type;

    /** attribute index */
    private int idx;

    /** attribute column set */
    private Set<String> columnSet;

    /**
     * Initializes a newly created {@code ArffAttribute} object
     * with the name type and index of a attribute.
     *
     * @param name
     *          attribute name
     * @param type
     *          attribute type
     * @param idx
     *          attribute index
     */
    public ArffAttribute(String name, String type, int idx) {
        // check if type is valid
        if (!VALID_TYPES.contains(type)) {
            throw new IllegalArgumentException("Invalid Type: " + type);
        }

        this.name = name;
        this.type = type;
        this.idx = idx;
    }

    /**
     * Return attribute name.
     * @return  attribute name
     */
    public String getName() {
        return name;
    }

    /**
     * Return attribute type.
     * @return  attribute type
     */
    public String getType() {
        return type;
    }

    /**
     * Return attribute index.
     * @return  attribute index
     */
    public int getIndex() {
        return idx;
    }

    /**
     * Return attribute column set.
     * @return  attribute column set
     */
    public Set<String> getColumnSet() {
        return columnSet;
    }

    /**
     * Set attribute column set.
     *
     * @param columnSet
     *              attribute column set
     */
    public void setColumnSet(Set<String> columnSet) {
        this.columnSet = columnSet;
    }
}
